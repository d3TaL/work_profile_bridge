package dev.profilebridge.app.ui

import android.os.UserHandle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.LocalHandler
import dev.profilebridge.app.platform.ProfileAccess

@Composable
fun BridgeScreen(
    openSetup: () -> Unit,
    targets: List<UserHandle>, message: Int?, device: String, profileRole: String,
    url: String, linkError: Int?, handlers: List<LocalHandler>, refresh: () -> Unit,
    changeUrl: (String) -> Unit, inspect: () -> Unit, clear: () -> Unit,
    openCopy: (UserHandle) -> Unit, openHandler: (LocalHandler) -> Unit,
    shareReport: (String) -> Unit,
    profileAccess: ProfileAccess, requestProfileAccess: () -> Unit, sendToCopy: (UserHandle) -> Unit,
    autoOpenStores: Boolean, setAutoOpenStores: (Boolean) -> Unit,
    autoOpenShares: Boolean, setAutoOpenShares: (Boolean) -> Unit,
    debugEnabled: Boolean, setDebugEnabled: (Boolean) -> Unit, debugReport: String?,
    previewDebug: () -> Unit, dismissDebug: () -> Unit,
    autoForward: Boolean, setAutoForward: (Boolean) -> Unit,
    shareRoute: Boolean, setShareRoute: (Boolean) -> Unit, shareLink: () -> Unit,
    compactMode: Boolean, setCompactMode: (Boolean) -> Unit,
    selectedTarget: UserHandle?, selectTarget: (UserHandle) -> Unit,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var diagnostics by rememberSaveable { mutableStateOf(false) }
    MaterialTheme {
        debugReport?.let { report ->
            AlertDialog(onDismissRequest = dismissDebug,
                title = { Text(stringResource(R.string.report_title)) },
                text = { Text(report, Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { shareReport(report) }) { Text(stringResource(R.string.share_report)) } },
                dismissButton = { TextButton(onClick = dismissDebug) { Text(stringResource(R.string.close_report)) } })
        }
        Scaffold { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.title), style = MaterialTheme.typography.headlineSmall)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(profileRole, style = MaterialTheme.typography.titleMedium)
                        val index = targets.indexOf(selectedTarget)
                        Text(when {
                            shareRoute -> stringResource(R.string.home_knox)
                            index >= 0 -> stringResource(R.string.home_direct, index + 1)
                            else -> stringResource(R.string.home_no_target)
                        })
                        Text(stringResource(if (autoForward) R.string.home_auto else R.string.home_manual))
                        Button(onClick = openSetup) { Text(stringResource(R.string.home_setup)) }
                    }
                }
                message?.let { Text(stringResource(it)) }
                linkError?.let { Text(stringResource(it)) }
                Text(stringResource(R.string.link_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = url, onValueChange = changeUrl, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.url)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
                if (url.isNotBlank()) {
                    if (shareRoute) {
                        Button(onClick = shareLink) { Text(stringResource(R.string.share_link_title)) }
                    } else {
                        targets.forEachIndexed { index, target ->
                            Button(onClick = { sendToCopy(target) }) { Text(stringResource(R.string.profile_send, index + 1)) }
                        }
                    }
                    OutlinedButton(onClick = inspect) { Text(stringResource(R.string.inspect)) }
                    OutlinedButton(onClick = clear) { Text(stringResource(R.string.clear)) }
                    handlers.forEach { handler ->
                        OutlinedButton(onClick = { openHandler(handler) }) { Text(stringResource(R.string.handler, handler.packageName)) }
                    }
                } else Text(stringResource(R.string.home_share_hint))
                OutlinedButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (advanced) R.string.home_hide_advanced else R.string.home_advanced))
                }
                if (advanced) {
                Text(stringResource(R.string.intro))
                Text(stringResource(R.string.profile_note))
                Text(stringResource(R.string.profile_send_note))
                Text(stringResource(R.string.profiles, targets.size))
                Text(stringResource(R.string.profile_note))
                Text(stringResource(when (profileAccess) {
                    ProfileAccess.GRANTED -> R.string.profile_access_granted
                    ProfileAccess.REQUESTABLE -> R.string.profile_access_requestable
                    ProfileAccess.NOT_AVAILABLE -> R.string.profile_settings_unavailable
                    ProfileAccess.UNKNOWN -> R.string.profile_access_unknown
                }))
                OutlinedButton(onClick = requestProfileAccess, enabled = profileAccess == ProfileAccess.REQUESTABLE) {
                    Text(stringResource(R.string.profile_request_access))
                }
                targets.forEachIndexed { index, handle ->
                    OutlinedButton(onClick = { openCopy(handle) }) { Text(stringResource(R.string.open_copy, index + 1)) }
                    OutlinedButton(onClick = { selectTarget(handle) }) {
                        Text(stringResource(if (selectedTarget == handle) R.string.auto_target_selected else R.string.auto_target, index + 1))
                    }
                }
                Text(stringResource(R.string.auto_forward))
                Switch(checked = autoForward, onCheckedChange = setAutoForward)
                Text(stringResource(R.string.auto_forward_note))
                Text(stringResource(R.string.share_route))
                Switch(checked = shareRoute, onCheckedChange = setShareRoute)
                Text(stringResource(R.string.share_route_note))
                Text(stringResource(R.string.compact_mode))
                Switch(checked = compactMode, onCheckedChange = setCompactMode)
                Text(stringResource(R.string.compact_note))
                OutlinedButton(onClick = shareLink, enabled = url.isNotBlank()) {
                    Text(stringResource(R.string.share_link_title))
                }
                Text(stringResource(R.string.auto_open_stores))
                Switch(checked = autoOpenStores, onCheckedChange = setAutoOpenStores)
                Text(stringResource(R.string.auto_open_stores_note))
                Text(stringResource(R.string.auto_open_shares))
                Switch(checked = autoOpenShares, onCheckedChange = setAutoOpenShares)
                Text(stringResource(R.string.auto_open_shares_note))
                }
                OutlinedButton(onClick = { diagnostics = !diagnostics }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (diagnostics) R.string.home_hide_diagnostics else R.string.home_diagnostics))
                }
                if (diagnostics) {
                Text(device)
                Text(stringResource(R.string.profiles, targets.size))
                OutlinedButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                Text(stringResource(R.string.debug_mode))
                Switch(checked = debugEnabled, onCheckedChange = setDebugEnabled)
                Text(stringResource(R.string.debug_note))
                OutlinedButton(onClick = previewDebug, enabled = debugEnabled) { Text(stringResource(R.string.report_title)) }
                }
                Text(stringResource(R.string.privacy))
            }
        }
    }
}
