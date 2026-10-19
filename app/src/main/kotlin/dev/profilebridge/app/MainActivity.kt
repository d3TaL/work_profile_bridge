package dev.profilebridge.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
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
    private var debugEnabled by mutableStateOf(false)
    private var debugReport by mutableStateOf<String?>(null)
    private val debugEvents = ArrayDeque<String>()
    private fun debug(code: String) {
        if (!debugEnabled) return
        if (debugEvents.size >= 64) debugEvents.removeFirst()
        debugEvents.addLast(code)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        autoOpenStores = storeAutoOpenEnabled()
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
            )
        }
    }
    override fun onResume() { super.onResume(); autoOpenStores = storeAutoOpenEnabled(); refresh() }
    override fun onPostResume() {
        super.onPostResume()
        lifecycleScope.launch { lifecycle.withResumed { openPendingStore() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); receive(intent)
        lifecycleScope.launch { lifecycle.withResumed { openPendingStore() } }
    }
    override fun onStop() {
        pendingStoreOpen = false
        launchJob?.cancel()
        edit(""); probe.screenStopped(); super.onStop()
    }
    private fun storeAutoOpenEnabled() =
        getSharedPreferences("store-routing", MODE_PRIVATE).getBoolean("auto-open", false)

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
        launchJob?.cancel()
        setIntent(Intent(Intent.ACTION_MAIN).setClass(this, MainActivity::class.java))
        if (value != null) {
            edit("")
            val extracted = if (incoming.action == Intent.ACTION_SEND) StoreLinks.fromShareText(value) else value
            if (extracted != null && SafeLink.parse(extracted) is LinkParseResult.Accepted) {
                edit(extracted)
                pendingStoreOpen = trustedStoreEntry && incoming.action == Intent.ACTION_SEND &&
                    StoreDelivery.consume(this, incoming) && storeAutoOpenEnabled()
                debug(if (pendingStoreOpen) "AUTO_OPEN_PENDING" else "MANUAL_MODE_OR_DELIVERY_REJECTED")
                debug(if (StoreLinks.target((SafeLink.parse(extracted) as LinkParseResult.Accepted).link) != null)
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
        refresh()
    }
    private fun edit(value: String) {
        pendingStoreOpen = false
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
