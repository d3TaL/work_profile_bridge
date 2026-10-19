package dev.profilebridge.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.core.content.FileProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.os.PersistableBundle
import dev.profilebridge.core.tls.PairingCode
import android.content.ActivityNotFoundException
import java.io.File
import dev.profilebridge.app.platform.IncomingLinkReader
import dev.profilebridge.app.platform.LocalHandler
import dev.profilebridge.app.platform.LocalLinkLauncher
import dev.profilebridge.app.platform.ProfileDiagnostics
import dev.profilebridge.app.platform.ProfileAccess
import dev.profilebridge.app.platform.ProfileSendResult
import dev.profilebridge.app.platform.StoreDelivery
import dev.profilebridge.app.ui.BridgeScreen
import dev.profilebridge.app.ui.ProbeViewModel
import dev.profilebridge.core.links.LinkParseResult
import dev.profilebridge.core.links.SafeLink
import dev.profilebridge.core.links.StoreLinks
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.withResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class MainActivity : ComponentActivity() {
    protected open val trustedStoreEntry: Boolean = false
    private val probe: ProbeViewModel by viewModels()
    private var targets by mutableStateOf<List<UserHandle>>(emptyList())
    private var message by mutableStateOf<Int?>(null)
    private var profileAccess by mutableStateOf(ProfileAccess.UNKNOWN)
    private var url by mutableStateOf("")
    private var linkError by mutableStateOf<Int?>(null)
    private var handlers by mutableStateOf<List<LocalHandler>>(emptyList())
    private var link: SafeLink? = null
    private var queryJob: Job? = null
    private var launchJob: Job? = null
    private var pendingStoreOpen = false
    private var autoOpenStores by mutableStateOf(false)
    private var autoForward by mutableStateOf(false)
    private var compactMode by mutableStateOf(false)
    private var targetSerial by mutableStateOf(-1L)
    private var compactEntry = false
    private var pendingForward = false
    private var debugEnabled by mutableStateOf(false)
    private var debugReport by mutableStateOf<String?>(null)
    private val debugEvents = ArrayDeque<String>()
    private fun debug(code: String) {
        if (!debugEnabled) return
        if (debugEvents.size >= 64) debugEvents.removeFirst()
        debugEvents.addLast(code)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("store-routing", MODE_PRIVATE)
        compactEntry = prefs.getBoolean("compact", false) &&
            (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW)
        if (compactEntry) setTheme(R.style.Theme_ProfileBridge_Compact)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (!compactEntry) enableEdgeToEdge()
        autoOpenStores = storeAutoOpenEnabled()
        autoForward = prefs.getBoolean("auto-forward", false)
        compactMode = prefs.getBoolean("compact", false)
        targetSerial = prefs.getLong("target-serial", -1L)
        debugEnabled = getSharedPreferences("store-routing", MODE_PRIVATE).getBoolean("debug", false)
        if (savedInstanceState == null) receive(intent)
        else setIntent(Intent(Intent.ACTION_MAIN).setClass(this, MainActivity::class.java))
        setContent {
            BridgeScreen(
                targets = targets, message = message,
                device = getString(R.string.device, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, applicationInfo.targetSdkVersion),
                url = url, linkError = linkError, handlers = handlers,
                refresh = ::refresh, changeUrl = ::edit, inspect = ::inspect, clear = { edit("") },
                openCopy = { target -> message = if (ProfileDiagnostics.open(this, target)) R.string.open_ok else R.string.open_failed },
                openHandler = ::openHandler,
                shareReport = ::shareReport,
                profileAccess = profileAccess,
                requestProfileAccess = {
                    message = if (ProfileDiagnostics.requestAccess(this)) R.string.profile_settings_opened
                        else R.string.profile_settings_unavailable
                },
                sendToCopy = ::sendToCopy,
                autoOpenStores = autoOpenStores,
                setAutoOpenStores = { enabled ->
                    autoOpenStores = enabled
                    getSharedPreferences("store-routing", MODE_PRIVATE).edit().putBoolean("auto-open", enabled).apply()
                },
                debugEnabled = debugEnabled,
                setDebugEnabled = { enabled ->
                    debugEnabled = enabled; debugEvents.clear(); debugReport = null
                    getSharedPreferences("store-routing", MODE_PRIVATE).edit().putBoolean("debug", enabled).apply()
                },
                debugReport = debugReport,
                previewDebug = { debugReport = "Profile Bridge ${BuildConfig.VERSION_NAME}\n" + debugEvents.joinToString("\n") },
                dismissDebug = { debugReport = null },
                autoForward = autoForward,
                setAutoForward = { enabled ->
                    autoForward = enabled
                    prefs.edit().putBoolean("auto-forward", enabled).apply()
                },
                compactMode = compactMode,
                setCompactMode = { enabled ->
                    compactMode = enabled
                    prefs.edit().putBoolean("compact", enabled).apply()
                },
                selectedTarget = targets.firstOrNull { serial(it) == targetSerial },
                selectTarget = { target ->
                    targetSerial = serial(target)
                    prefs.edit().putLong("target-serial", targetSerial).apply()
                },
            )
        }
    }
    override fun onStart() {
        super.onStart()
        if (compactEntry) {
            val bounds = windowManager.currentWindowMetrics.bounds
            window.setLayout((bounds.width() * 0.9).toInt(), (bounds.height() * 0.65).toInt())
        }
    }
    override fun onResume() { super.onResume(); autoOpenStores = storeAutoOpenEnabled(); refresh() }
    override fun onPostResume() {
        super.onPostResume()
        lifecycleScope.launch { lifecycle.withResumed { processPending() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); receive(intent)
        lifecycleScope.launch { lifecycle.withResumed { processPending() } }
    }
    override fun onStop() {
        pendingStoreOpen = false
        pendingForward = false
        launchJob?.cancel()
        edit(""); probe.screenStopped(); super.onStop()
    }
    private fun storeAutoOpenEnabled() =
        getSharedPreferences("store-routing", MODE_PRIVATE).getBoolean("auto-open", false)

    private fun serial(target: UserHandle): Long =
        getSystemService(UserManager::class.java).getSerialNumberForUser(target)

    private fun processPending() {
        if (pendingForward) {
            pendingForward = false // one attempt; no resume/rotation retries
            val target = ProfileDiagnostics.targets(this).firstOrNull { serial(it) == targetSerial }
            if (target == null) {
                debug("AUTO_FORWARD_TARGET_UNAVAILABLE")
                message = R.string.profile_target_unavailable
            } else sendToCopy(target)
        } else openPendingStore()
    }

    private fun openPendingStore() {
        if (!pendingStoreOpen || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        pendingStoreOpen = false
        if (!storeAutoOpenEnabled()) return
        val selected = (SafeLink.parse(url) as? LinkParseResult.Accepted)?.link ?: return
        val target = StoreLinks.target(selected) ?: return
        debug("AUTO_OPEN_ATTEMPT")
        launchJob?.cancel()
        link = selected
        val handler = LocalHandler(target)
        launchJob = lifecycleScope.launch {
            val ok = LocalLinkLauncher.open(this@MainActivity, selected, handler) {
                storeAutoOpenEnabled() && link === selected && url == selected.original
            }
            debug(if (ok) "ACTIVITY_REQUEST_SUBMITTED" else "ACTIVITY_NOT_OPENED")
            if (link === selected) {
                if (ok) edit("")
                linkError = if (ok) R.string.open_ok else R.string.store_open_failed
            }
            if (ok && compactEntry) finish()
        }
    }
    private fun copyCode(value: String) {
        val clip = ClipData.newPlainText(getString(R.string.session_code), value)
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }
    private fun pasteCode() {
        val clip = getSystemService(ClipboardManager::class.java).primaryClip ?: return
        if (clip.itemCount != 1) return
        // Read text only: never resolve clipboard URI/Intent or coerce external content.
        val value = clip.getItemAt(0).text ?: return
        if (value.length <= PairingCode.MAX_LENGTH) probe.edit(value.toString())
    }
    private fun shareReport(report: String) {
        lifecycleScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    val folder = File(cacheDir, "diagnostic-reports").apply { mkdirs() }
                    folder.listFiles()?.forEach { it.delete() }
                    val file = File.createTempFile("report-", ".txt", folder)
                    file.writeText(report)
                    FileProvider.getUriForFile(this@MainActivity, "$packageName.diagnostics", file)
                }
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri("Diagnostic report", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(send, getString(R.string.share_report)))
            } catch (_: ActivityNotFoundException) { message = R.string.open_failed }
            catch (_: java.io.IOException) { message = R.string.open_failed }
            catch (_: SecurityException) { message = R.string.open_failed }
            catch (_: IllegalArgumentException) { message = R.string.open_failed }
        }
    }
    private fun receive(incoming: Intent) {
        debug(if (trustedStoreEntry) "PROTECTED_ENTRY" else "PUBLIC_ENTRY")
        val value = IncomingLinkReader.read(this, incoming)
        pendingStoreOpen = false
        pendingForward = false
        launchJob?.cancel()
        setIntent(Intent(Intent.ACTION_MAIN).setClass(this, MainActivity::class.java))
        if (value != null) {
            edit("")
            val extracted = if (incoming.action == Intent.ACTION_SEND) StoreLinks.fromShareText(value) else value
            if (extracted != null && SafeLink.parse(extracted) is LinkParseResult.Accepted) {
                edit(extracted)
                val parsed = (SafeLink.parse(extracted) as LinkParseResult.Accepted).link
                val supported = StoreLinks.target(parsed) != null
                val delivery = trustedStoreEntry && incoming.action == Intent.ACTION_SEND && StoreDelivery.consume(this, incoming)
                pendingStoreOpen = delivery && storeAutoOpenEnabled() && supported
                debug(when {
                    !trustedStoreEntry -> "PUBLIC_MANUAL_ENTRY"
                    !delivery -> "DELIVERY_REJECTED"
                    !storeAutoOpenEnabled() -> "AUTO_OPEN_DISABLED"
                    !supported -> "ROUTE_MANUAL_ONLY"
                    else -> "AUTO_OPEN_PENDING"
                })
                pendingForward = !trustedStoreEntry && autoForward && supported
                if (pendingForward) debug("AUTO_FORWARD_PENDING")
                debug(if (supported)
                    "SHOP_ROUTE_SUPPORTED" else "SHOP_ROUTE_UNSUPPORTED")
                message = R.string.link_received
            } else linkError = R.string.invalid_url
        }
    }
    private fun refresh() {
        targets = ProfileDiagnostics.targets(this)
        profileAccess = ProfileDiagnostics.access(this)
    }
    private fun sendToCopy(target: UserHandle) {
        val result = ProfileDiagnostics.send(this, target, url)
        debug("SEND_${result.name}")
        message = when (result) {
            ProfileSendResult.SUBMITTED_UNKNOWN_OUTCOME -> R.string.profile_submitted
            ProfileSendResult.INVALID_LINK -> R.string.invalid_url
            ProfileSendResult.NO_TARGET -> R.string.profile_target_unavailable
            ProfileSendResult.ACCESS_DENIED -> R.string.profile_access_denied
            ProfileSendResult.UNAVAILABLE -> R.string.open_failed
            ProfileSendResult.UNKNOWN_OUTCOME -> R.string.profile_unknown_outcome
        }
        if (result == ProfileSendResult.SUBMITTED_UNKNOWN_OUTCOME || result == ProfileSendResult.UNKNOWN_OUTCOME) edit("")
        if (result == ProfileSendResult.SUBMITTED_UNKNOWN_OUTCOME && compactEntry) finish()
        refresh()
    }
    private fun edit(value: String) {
        pendingStoreOpen = false
        pendingForward = false
        queryJob?.cancel()
        link = null; handlers = emptyList(); linkError = null
        if (value.length <= SafeLink.MAX_BYTES) url = value
    }
    private fun inspect() {
        queryJob?.cancel()
        val parsed = SafeLink.parse(url)
        if (parsed !is LinkParseResult.Accepted) { linkError = R.string.invalid_url; return }
        val selected = parsed.link
        queryJob = lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) { LocalLinkLauncher.handlers(this@MainActivity, selected) }
            link = selected; handlers = found
            linkError = if (found.isEmpty()) R.string.no_handler else null
        }
    }
    private fun openHandler(handler: LocalHandler) {
        if (launchJob?.isActive == true) return
        val selected = link ?: return
        launchJob = lifecycleScope.launch {
            val ok = LocalLinkLauncher.open(this@MainActivity, selected, handler) {
                link === selected && selected.original == url && handler in handlers
            }
            if (link === selected) { edit(""); linkError = if (ok) R.string.open_ok else R.string.open_failed }
        }
    }
}
