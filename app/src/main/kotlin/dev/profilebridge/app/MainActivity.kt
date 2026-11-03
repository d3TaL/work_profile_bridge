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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import dev.profilebridge.app.platform.ProfileRole
import dev.profilebridge.app.platform.ProfileRoles
import dev.profilebridge.app.platform.ProfileManager
import dev.profilebridge.app.platform.ProfileManagers
import android.content.SharedPreferences
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import dev.profilebridge.app.platform.LinkHandling
import dev.profilebridge.app.platform.LinkSettings
import dev.profilebridge.app.ui.BridgeActions
import dev.profilebridge.app.ui.BridgeState
import dev.profilebridge.app.ui.Destination
import dev.profilebridge.app.ui.MainScreen
import dev.profilebridge.app.ui.QuickSendScreen
import dev.profilebridge.app.ui.Space
import dev.profilebridge.app.ui.SetupScreen
import dev.profilebridge.app.ui.TransferScreen
import dev.profilebridge.app.ui.AppsScreen
import dev.profilebridge.app.platform.AppRules
import dev.profilebridge.app.platform.AppMode
import dev.profilebridge.app.ui.ProbeViewModel
import dev.profilebridge.core.links.LinkParseResult
import dev.profilebridge.core.links.SafeLink
import dev.profilebridge.core.links.StoreLinks
import dev.profilebridge.core.links.StoreEntryPolicy
import dev.profilebridge.core.links.BankLinks
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
    private var profileManagers by mutableStateOf<List<ProfileManager>?>(null)
    private var managersJob: Job? = null
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
    private var autoOpenShares by mutableStateOf(false)
    private var pendingPublicShare = false
    private var autoForward by mutableStateOf(false)
    private var shareRoute by mutableStateOf(false)
    private var compactMode by mutableStateOf(false)
    private var targetSerial by mutableLongStateOf(-1L)
    private var role = ProfileRole.UNKNOWN
    private var transit by mutableStateOf(false)
    private var showSetup by mutableStateOf(false)
    private var transferApp by mutableStateOf<String?>(null)
    private var transferFailed by mutableStateOf(false)
    private var showApps by mutableStateOf(false)
    private var homeTab by mutableIntStateOf(0)
    private var appModes by mutableStateOf<Map<String, AppMode>>(emptyMap())
    private var destination by mutableStateOf(Destination.WORK_PROFILE)
    private var space by mutableStateOf(Space.MAIN)
    private var linkHandling by mutableStateOf<LinkHandling?>(null)
    private var linksJob: Job? = null
    private var retryLink: SafeLink? = null
    private var retryDeadline = 0L
    private var reportToSave: String? = null
    private val saveReportDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val report = reportToSave
        reportToSave = null
        if (uri != null && report != null) lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openOutputStream(uri, "w")?.use {
                        it.write(report.toByteArray(Charsets.UTF_8))
                        true
                    } ?: false
                } catch (_: java.io.IOException) { false }
                catch (_: SecurityException) { false }
                catch (_: IllegalArgumentException) { false }
            }
            message = if (ok) R.string.report_saved else R.string.open_failed
        }
    }
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
        role = ProfileRoles.detect(this)
        // Apply role defaults once. Never override a switch changed by the owner.
        if (!prefs.getBoolean("role-defaults-v1", false)) {
            val editor = prefs.edit().putBoolean("role-defaults-v1", true)
            if (role == ProfileRole.MAIN && !prefs.getBoolean("forward-manual", false))
                editor.putBoolean("auto-forward", true)
            if (role == ProfileRole.MANAGED && !prefs.getBoolean("open-manual", false))
                editor.putBoolean("auto-open", true)
            editor.apply()
        }
        if (role == ProfileRole.MAIN && !prefs.contains("target-serial")) {
            ProfileDiagnostics.targets(this).singleOrNull()?.let { target ->
                val value = serial(target)
                if (value >= 0) prefs.edit().putLong("target-serial", value).apply()
            }
        }
        compactEntry = prefs.getBoolean("compact", false) &&
            (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW)
        setTheme(if (compactEntry) R.style.Theme_ProfileBridge_Transit else R.style.Theme_ProfileBridge)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        // The compact entry is a floating dialog window; floating windows are not edge-to-edge.
        if (!compactEntry) {
            enableEdgeToEdge()
            // Bottom bars draw their own background down to the screen edge.
            window.isNavigationBarContrastEnforced = false
        }
        autoOpenStores = storeAutoOpenEnabled()
        autoOpenShares = prefs.getBoolean("auto-open-share", false)
        autoForward = prefs.getBoolean("auto-forward", false)
        shareRoute = prefs.getBoolean("share-route", false)
        compactMode = prefs.getBoolean("compact", false)
        targetSerial = prefs.getLong("target-serial", -1L)
        destination = loadDestination(prefs)
        space = loadSpace(prefs)
        showSetup = savedInstanceState?.getBoolean("setup-visible") ?: (intent.action == Intent.ACTION_MAIN &&
            !prefs.getBoolean(SETUP_DONE, false))
        debugEnabled = getSharedPreferences("store-routing", MODE_PRIVATE).getBoolean("debug", false)
        if (savedInstanceState == null) receive(intent)
        else setIntent(Intent(Intent.ACTION_MAIN).setClass(this, MainActivity::class.java))
        transit = compactEntry && (pendingForward || pendingStoreOpen)
        setContent {
            val screen = when {
                showApps -> Screen.APPS
                transferApp != null -> Screen.TRANSFER
                showSetup -> Screen.SETUP
                transit -> Screen.NONE
                compactEntry && space == Space.MAIN && url.isNotBlank() -> Screen.QUICK
                else -> Screen.MAIN
            }
            val state = uiState()
            // Keep the transfer label while its screen fades out.
            val lastTransfer = remember { mutableStateOf("") }
            SideEffect { transferApp?.let { lastTransfer.value = it } }
            Crossfade(targetState = screen, animationSpec = tween(200), label = "screen") { current ->
                when (current) {
                    Screen.APPS -> AppsScreen(appModes, apply = { packages, mode ->
                        AppRules.set(this, packages, mode)
                        appModes = AppRules.apps.associate { it.packageName to AppRules.mode(this, it.packageName) }
                    }, close = { selectHomeTab(0) }, selectTab = { selectHomeTab(it) })
                    Screen.TRANSFER -> TransferScreen(transferApp ?: lastTransfer.value, transferFailed,
                        expired = message == R.string.transfer_expired, retryEnabled = retryLink != null,
                        retry = ::retryShare, close = ::closeTransfer)
                    Screen.SETUP -> SetupScreen(state, actions, close = { showSetup = false }, done = ::finishSetup,
                        tryLink = ::tryLink)
                    Screen.QUICK -> QuickSendScreen(state, actions)
                    Screen.MAIN -> MainScreen(homeTab, state, actions)
                    Screen.NONE -> Unit
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        if (compactEntry) {
            val bounds = windowManager.currentWindowMetrics.bounds
            window.setLayout((bounds.width() * 0.9).toInt(), (bounds.height() * 0.65).toInt())
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("setup-visible", showSetup)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() { super.onResume(); autoOpenStores = storeAutoOpenEnabled(); refresh() }
    override fun onPostResume() {
        super.onPostResume()
        lifecycleScope.launch { lifecycle.withResumed { processPending() } }
    }
    override fun onNewIntent(intent: Intent) {
        homeTab = 0
        showApps = false
        showSetup = false
        super.onNewIntent(intent); setIntent(intent); receive(intent)
        transit = compactEntry && (pendingForward || pendingStoreOpen)
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

    private fun prefs(): SharedPreferences = getSharedPreferences("store-routing", MODE_PRIVATE)

    private fun loadDestination(prefs: SharedPreferences) = when (prefs.getString("destination", null)) {
        Destination.WORK_PROFILE.key -> Destination.WORK_PROFILE
        Destination.SECURE_FOLDER.key -> Destination.SECURE_FOLDER
        Destination.ASK.key -> Destination.ASK
        // Earlier versions stored only the switches: manual mode becomes "ask".
        else -> when {
            !autoForward -> Destination.ASK
            shareRoute -> Destination.SECURE_FOLDER
            else -> Destination.WORK_PROFILE
        }
    }

    private fun loadSpace(prefs: SharedPreferences) = when {
        role == ProfileRole.MAIN -> Space.MAIN
        prefs.getString("space", null) == SPACE_WORK -> Space.WORK_PROFILE
        prefs.getString("space", null) == SPACE_SECURE -> Space.SECURE_FOLDER
        prefs.getBoolean("knox-label", false) -> Space.SECURE_FOLDER
        role == ProfileRole.MANAGED -> Space.WORK_PROFILE
        else -> Space.SECURE_FOLDER
    }

    /** Changed only by an explicit tap, never by incoming text. */
    private fun applyDestination(value: Destination) {
        destination = value
        autoForward = value != Destination.ASK
        shareRoute = value == Destination.SECURE_FOLDER
        prefs().edit().putString("destination", value.key).putBoolean("auto-forward", autoForward)
            .putBoolean("share-route", shareRoute).putBoolean("forward-manual", true).apply()
    }

    private fun applySpace(value: Space) {
        if (role == ProfileRole.MAIN || value == Space.MAIN) return
        space = value
        val edit = prefs().edit().putString("space", if (value == Space.SECURE_FOLDER) SPACE_SECURE else SPACE_WORK)
        // Unauthenticated public shares must not stay auto-opened behind a work profile label.
        if (value == Space.WORK_PROFILE && autoOpenShares) {
            autoOpenShares = false
            edit.putBoolean("auto-open-share", false)
        }
        edit.apply()
    }

    /** Secure Folder: the UI has shown the separate consent before enabling. */
    private fun applyOpenImmediately(enabled: Boolean) {
        val edit = prefs().edit()
        if (space == Space.SECURE_FOLDER) {
            autoOpenShares = enabled
            edit.putBoolean("auto-open-share", enabled)
        } else {
            autoOpenStores = enabled
            edit.putBoolean("auto-open", enabled).putBoolean("open-manual", true)
        }
        edit.apply()
    }

    private fun finishSetup() {
        prefs().edit().putBoolean(SETUP_DONE, true).apply()
        showSetup = false
    }

    private fun tryLink() {
        finishSetup()
        selectHomeTab(0)
        edit(TEST_LINK)
        message = R.string.try_ready
    }

    private fun retryShare() {
        val selected = retryLink
        if (selected != null && android.os.SystemClock.elapsedRealtime() < retryDeadline) {
            url = selected.original
            shareLink()
        } else {
            retryLink = null
            transferFailed = true
            message = R.string.transfer_expired
        }
    }

    private fun closeTransfer() {
        transferApp = null
        retryLink = null
        if (compactEntry) finish()
    }

    private fun linkApp(value: String): String? {
        val parsed = (SafeLink.parseRouting(value) as? LinkParseResult.Accepted)?.link ?: return null
        val pkg = StoreLinks.packageFor(parsed) ?: return null
        return AppRules.apps.firstOrNull { it.packageName == pkg }?.label
    }

    private fun uiState() = BridgeState(
        space = space, destination = destination, copies = targets,
        chosenCopy = targets.firstOrNull { serial(it) == targetSerial }, access = profileAccess,
        managers = profileManagers, links = linkHandling,
        openImmediately = if (space == Space.SECURE_FOLDER) autoOpenShares else autoOpenStores,
        compactWindow = compactMode, diagnostics = debugEnabled, report = debugReport,
        samsung = Build.MANUFACTURER.equals("samsung", true), url = url, handlers = handlers,
        message = message, linkMessage = linkError, linkApp = linkApp(url), version = BuildConfig.VERSION_NAME,
    )

    private val actions = object : BridgeActions {
        override fun selectTab(tab: Int) = selectHomeTab(tab)
        override fun openSetup() { refresh(); showSetup = true }
        override fun setDestination(destination: Destination) = applyDestination(destination)
        override fun selectCopy(copy: UserHandle) {
            targetSerial = serial(copy)
            prefs().edit().putLong("target-serial", targetSerial).apply()
        }
        override fun requestAccess() = this@MainActivity.requestAccess()
        override fun openManager(manager: ProfileManager) {
            lifecycleScope.launch {
                if (!ProfileManagers.open(this@MainActivity, manager)) message = R.string.setup_manager_open_failed
            }
        }
        override fun openLinkSettings() = this@MainActivity.openLinkSettings()
        override fun setSpace(space: Space) = applySpace(space)
        override fun setOpenImmediately(enabled: Boolean) = applyOpenImmediately(enabled)
        override fun setCompactWindow(enabled: Boolean) {
            compactMode = enabled
            prefs().edit().putBoolean("compact", enabled).apply()
        }
        override fun setDiagnostics(enabled: Boolean) {
            debugEnabled = enabled; debugEvents.clear(); debugReport = null
            prefs().edit().putBoolean("debug", enabled).apply()
        }
        override fun previewReport() {
            debugReport = "Profile Bridge ${BuildConfig.VERSION_NAME}\n" + getString(R.string.device,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT, applicationInfo.targetSdkVersion) + "\n" +
                debugEvents.joinToString("\n")
        }
        override fun dismissReport() { debugReport = null }
        override fun saveReport(report: String) {
            reportToSave = report
            try { saveReportDocument.launch("profile-bridge-report.txt") }
            catch (_: ActivityNotFoundException) { reportToSave = null; message = R.string.open_failed }
        }
        override fun shareReport(report: String) = this@MainActivity.shareReport(report)
        override fun changeUrl(value: String) = edit(value)
        override fun pasteLink() = this@MainActivity.pasteLink()
        override fun findHandlers() = inspect()
        override fun openHandler(handler: LocalHandler) = this@MainActivity.openHandler(handler)
        override fun sendToCopy(copy: UserHandle) = this@MainActivity.sendToCopy(copy)
        override fun sendToSecureFolder() = shareLink()
        override fun closeQuickSend() {
            edit("")
            if (compactEntry) finish()
        }
    }

    private fun openLinkSettings() {
        val uri = android.net.Uri.fromParts("package", packageName, null)
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                startActivity(Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, uri))
            } else startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
        } catch (_: ActivityNotFoundException) {
            try { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)) }
            catch (_: ActivityNotFoundException) { message = R.string.open_failed }
            catch (_: SecurityException) { message = R.string.open_failed }
        } catch (_: SecurityException) { message = R.string.open_failed }
    }

    private fun requestAccess() {
        message = if (ProfileDiagnostics.requestAccess(this)) R.string.profile_settings_opened
            else R.string.profile_settings_unavailable
    }

    private fun pendingOpenAuthorized() = if (pendingPublicShare)
        getSharedPreferences("store-routing", MODE_PRIVATE).getBoolean("auto-open-share", false)
        else storeAutoOpenEnabled()

    private fun allowed(value: SafeLink, automatic: Boolean): Boolean {
        val pkg = StoreLinks.packageFor(value) ?: return true
        return when (AppRules.mode(this, pkg)) {
            AppMode.AUTOMATIC -> true
            AppMode.CONFIRM -> !automatic
            AppMode.DISABLED -> false
        }
    }

    private fun serial(target: UserHandle): Long =
        getSystemService(UserManager::class.java).getSerialNumberForUser(target)

    private fun processPending() {
        if (pendingForward) {
            pendingForward = false // one attempt; no resume/rotation retries
            if (shareRoute) { shareLink(); return }
            val target = ProfileDiagnostics.targets(this).firstOrNull { serial(it) == targetSerial }
            if (target == null) {
                debug("AUTO_FORWARD_TARGET_UNAVAILABLE")
                message = R.string.profile_target_unavailable
                transit = false
            } else sendToCopy(target)
        } else openPendingStore()
    }

    private fun openPendingStore() {
        if (!pendingStoreOpen || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        pendingStoreOpen = false
        if (!pendingOpenAuthorized()) { transit = false; return }
        val selected = (SafeLink.parseRouting(url) as? LinkParseResult.Accepted)?.link ?: return
        val target = StoreLinks.target(selected) ?: BankLinks.target(selected) ?: return
        debug("AUTO_OPEN_ATTEMPT")
        launchJob?.cancel()
        link = selected
        val handler = LocalHandler(target)
        launchJob = lifecycleScope.launch {
            val bankPackages = BankLinks.handlerPackages(selected)
            val actualHandler = if (bankPackages.isEmpty()) handler else withContext(Dispatchers.IO) {
                bankPackages.firstNotNullOfOrNull { pkg ->
                    LocalLinkLauncher.handlers(this@MainActivity, selected, pkg).firstOrNull()
                }
            }
            val ok = actualHandler != null && LocalLinkLauncher.open(this@MainActivity, selected, actualHandler) {
                pendingOpenAuthorized() && allowed(selected, true) && link === selected && url == selected.original
            }
            debug(if (ok) "ACTIVITY_REQUEST_SUBMITTED" else "ACTIVITY_NOT_OPENED")
            if (!ok) transit = false
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
    private fun pasteLink() {
        val clip = getSystemService(ClipboardManager::class.java).primaryClip
        // Text only, on an explicit tap: clipboard URIs and Intents are never resolved.
        val text = clip?.takeIf { it.itemCount == 1 }?.getItemAt(0)?.text?.toString()?.trim()
        val value = text?.let { StoreLinks.fromShareText(it) }
        if (value == null) linkError = R.string.paste_empty else edit(value)
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
    private fun shareLink() {
        val selected = (SafeLink.parseRouting(url) as? LinkParseResult.Accepted)?.link
        if (selected == null || StoreLinks.packageFor(selected) == null) {
            message = R.string.invalid_url
            transit = false
            return
        }
        if (!allowed(selected, false)) { message = R.string.apps_blocked; transit = false; return }
        pendingForward = false
        pendingStoreOpen = false
        if (BankLinks.target(selected) != null) {
            retryLink = null // Do not retain/retry payment payloads.
        } else if (retryLink?.original != selected.original) {
            retryLink = selected
            retryDeadline = android.os.SystemClock.elapsedRealtime() + 120_000L
            val deadline = retryDeadline
            lifecycleScope.launch {
                kotlinx.coroutines.delay(120_000L)
                if (retryDeadline == deadline) retryLink = null
            }
        }
        transferApp = when (StoreLinks.target(selected)) {
            "ru.ozon.app.android" -> "Ozon"
            "com.wildberries.ru" -> "Wildberries"
            else -> BankLinks.banks.firstOrNull { it.packageName == BankLinks.target(selected) }?.label
                ?: getString(R.string.app_name)
        }
        transferFailed = false
        transit = false
        try {
            // Fresh text-only SEND. Marker suppresses automatic re-sharing if the
            // user selects our source copy again; it grants no trust or permission.
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, selected.original)
                putExtra(IncomingLinkReader.SHARE_ONCE, true)
            }
            startActivity(Intent.createChooser(send, getString(R.string.share_link_title)))
            debug("SHARE_CHOOSER_SUBMITTED_UNKNOWN_OUTCOME")
            edit("")
            message = R.string.share_link_submitted
            // Keep a non-sensitive backdrop until the user closes it. No URL retained.
        } catch (_: ActivityNotFoundException) {
            transferFailed = true
            debug("SHARE_CHOOSER_UNAVAILABLE"); message = R.string.open_failed; transit = false
        } catch (_: SecurityException) {
            transferFailed = true
            debug("SHARE_CHOOSER_DENIED"); message = R.string.open_failed; transit = false
        }
    }
    private fun receive(incoming: Intent) {
        transferApp = null
        retryLink = null
        debug(if (trustedStoreEntry) "PROTECTED_ENTRY" else "PUBLIC_ENTRY")
        val value = IncomingLinkReader.read(this, incoming)
        pendingStoreOpen = false
        pendingForward = false
        launchJob?.cancel()
        pendingPublicShare = false
        setIntent(Intent(Intent.ACTION_MAIN).setClass(this, MainActivity::class.java))
        if (value != null) {
            edit("")
            val extracted = if (incoming.action == Intent.ACTION_SEND) StoreLinks.fromShareText(value) else value
            if (extracted != null && SafeLink.parseRouting(extracted) is LinkParseResult.Accepted) {
                edit(extracted)
                val parsed = (SafeLink.parseRouting(extracted) as LinkParseResult.Accepted).link
                val supported = (StoreLinks.target(parsed) != null || BankLinks.target(parsed) != null) && allowed(parsed, true)
                val delivery = trustedStoreEntry && incoming.action == Intent.ACTION_SEND && StoreDelivery.consume(this, incoming)
                val isSend = incoming.action == Intent.ACTION_SEND
                pendingPublicShare = !trustedStoreEntry && isSend && autoOpenShares
                pendingStoreOpen = StoreEntryPolicy.mayOpen(trustedStoreEntry, delivery, isSend,
                    storeAutoOpenEnabled(), autoOpenShares, supported)
                debug(when {
                    pendingStoreOpen && pendingPublicShare -> "PUBLIC_SHARE_AUTO_OPEN_PENDING"
                    !trustedStoreEntry -> "PUBLIC_MANUAL_ENTRY"
                    !delivery -> "DELIVERY_REJECTED"
                    !storeAutoOpenEnabled() -> "AUTO_OPEN_DISABLED"
                    !supported -> "ROUTE_MANUAL_ONLY"
                    else -> "AUTO_OPEN_PENDING"
                })
                pendingForward = !trustedStoreEntry && !pendingPublicShare && autoForward && supported &&
                    !IncomingLinkReader.isSharedOnce(this, incoming)
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
        managersJob?.cancel()
        profileManagers = null
        managersJob = lifecycleScope.launch {
            profileManagers = ProfileManagers.installed(this@MainActivity)
        }
        linksJob?.cancel()
        linksJob = lifecycleScope.launch { linkHandling = LinkSettings.read(this@MainActivity) }
    }
    private fun selectHomeTab(tab: Int) {
        homeTab = tab
        showApps = tab == 1
        if (showApps) appModes = AppRules.apps.associate { it.packageName to AppRules.mode(this, it.packageName) }
    }
    private fun sendToCopy(target: UserHandle) {
        val parsed = (SafeLink.parseRouting(url) as? LinkParseResult.Accepted)?.link
        if (parsed != null && !allowed(parsed, false)) { message = R.string.apps_blocked; transit = false; return }
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
        else transit = false
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
        val parsed = SafeLink.parseRouting(url)
        if (parsed !is LinkParseResult.Accepted) { linkError = R.string.invalid_url; return }
        val selected = parsed.link
        queryJob = lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) {
                val bankPackages = BankLinks.handlerPackages(selected)
                if (bankPackages.isEmpty()) LocalLinkLauncher.handlers(this@MainActivity, selected)
                else bankPackages.flatMap { pkg -> LocalLinkLauncher.handlers(this@MainActivity, selected, pkg) }
                    .distinctBy { it.packageName }
            }
            link = selected; handlers = found
            linkError = if (found.isEmpty()) R.string.no_handler else null
        }
    }
    private fun openHandler(handler: LocalHandler) {
        if (launchJob?.isActive == true) return
        val selected = link ?: return
        launchJob = lifecycleScope.launch {
            val ok = LocalLinkLauncher.open(this@MainActivity, selected, handler) {
                allowed(selected, false) && link === selected && selected.original == url && handler in handlers
            }
            if (link === selected) { edit(""); linkError = if (ok) R.string.open_ok else R.string.open_failed }
        }
    }

    private enum class Screen { MAIN, APPS, SETUP, TRANSFER, QUICK, NONE }

    private companion object {
        const val SETUP_DONE = "setup-v2-done"
        const val SPACE_WORK = "work"
        const val SPACE_SECURE = "secure-folder"
        const val TEST_LINK = "https://www.ozon.ru/"
        val Destination.key: String get() = when (this) {
            Destination.WORK_PROFILE -> "work"
            Destination.SECURE_FOLDER -> "secure-folder"
            Destination.ASK -> "ask"
        }
    }
}
