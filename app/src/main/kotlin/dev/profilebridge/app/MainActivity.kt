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
import dev.profilebridge.app.ui.BridgeScreen
import dev.profilebridge.app.ui.ProbeViewModel
import dev.profilebridge.core.links.LinkParseResult
import dev.profilebridge.core.links.SafeLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        receive(intent)
        setContent {
            BridgeScreen(
                probe = probe, targets = targets, message = message,
                device = getString(R.string.device, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, applicationInfo.targetSdkVersion),
                url = url, linkError = linkError, handlers = handlers,
                refresh = ::refresh, changeUrl = ::edit, inspect = ::inspect, clear = { edit("") },
                openCopy = { target -> message = if (ProfileDiagnostics.open(this, target)) R.string.open_ok else R.string.open_failed },
                openHandler = ::openHandler,
                shareReport = ::shareReport,
                copyCode = ::copyCode, pasteCode = ::pasteCode,
                profileAccess = profileAccess,
                requestProfileAccess = {
                    message = if (ProfileDiagnostics.requestAccess(this)) R.string.profile_settings_opened
                        else R.string.profile_settings_unavailable
                },
                sendToCopy = ::sendToCopy,
            )
        }
    }
    override fun onResume() { super.onResume(); refresh() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    override fun onStop() { edit(""); probe.screenStopped(); super.onStop() }
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
        val value = IncomingLinkReader.read(this, incoming)
        setIntent(Intent(Intent.ACTION_MAIN).setClass(this, MainActivity::class.java))
        if (value != null) {
            edit("")
            if (SafeLink.parse(value) is LinkParseResult.Accepted) {
                edit(value)
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
