package dev.profilebridge.app.ui

import android.os.UserHandle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.LocalHandler
import dev.profilebridge.app.platform.ProfileAccess

@Composable
fun BridgeScreen(
    tab: Int, selectTab: (Int) -> Unit,
    openSetup: () -> Unit,
    openApps: () -> Unit,
    targets: List<UserHandle>, message: Int?, device: String, profileRole: String,
    url: String, linkError: Int?, handlers: List<LocalHandler>, refresh: () -> Unit,
    changeUrl: (String) -> Unit, inspect: () -> Unit, clear: () -> Unit,
    openCopy: (UserHandle) -> Unit, openHandler: (LocalHandler) -> Unit,
    shareReport: (String) -> Unit,
    saveReport: (String) -> Unit,
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
    BridgeTheme {
        debugReport?.let { report ->
            AlertDialog(onDismissRequest = dismissDebug,
                title = { Text(stringResource(R.string.report_title)) },
                text = { Text(report, Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = {
                    Column {
                        TextButton(onClick = { saveReport(report) }) { Text(stringResource(R.string.save_report)) }
                        TextButton(onClick = { shareReport(report) }) { Text(stringResource(R.string.share_report)) }
                    }
                },
                dismissButton = { TextButton(onClick = dismissDebug) { Text(stringResource(R.string.close_report)) } })
        }
        Scaffold(bottomBar = { BottomBar(tab, selectTab) }) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (tab == 2) R.string.nav_settings else R.string.title),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                message?.let { MessageBanner(it) }
                if (tab == 0) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(profileRole, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val index = targets.indexOf(selectedTarget)
                            Text(when {
                                shareRoute -> stringResource(R.string.home_knox)
                                index >= 0 -> stringResource(R.string.home_direct, index + 1)
                                else -> stringResource(R.string.home_no_target)
                            })
                            Text(stringResource(if (autoForward) R.string.home_auto else R.string.home_manual),
                                style = MaterialTheme.typography.bodyMedium)
                            FilledTonalButton(onClick = openSetup, modifier = Modifier.padding(top = 6.dp)) {
                                Text(stringResource(R.string.home_setup))
                            }
                        }
                    }
                    linkError?.let { MessageBanner(it, error = it != R.string.open_ok) }
                    SectionCard(R.string.link_title) {
                        OutlinedTextField(value = url, onValueChange = changeUrl, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text(stringResource(R.string.url)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
                        if (url.isNotBlank()) {
                            if (shareRoute) {
                                Button(onClick = shareLink, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.share_link_title)) }
                            } else {
                                targets.forEachIndexed { index, target ->
                                    Button(onClick = { sendToCopy(target) }, modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.profile_send, index + 1))
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = inspect, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.inspect)) }
                                OutlinedButton(onClick = clear, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.clear)) }
                            }
                            handlers.forEach { handler ->
                                FilledTonalButton(onClick = { openHandler(handler) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.handler, handler.packageName))
                                }
                            }
                        } else Text(stringResource(R.string.home_share_hint), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = openApps, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.apps_title)) }
                }
                if (tab == 2) {
                    Button(onClick = openSetup, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_title)) }
                    SectionCard(R.string.settings_send) {
                        SwitchRow(R.string.auto_forward, autoForward, setAutoForward)
                        SettingHelp(R.string.auto_forward_note)
                        targets.forEachIndexed { index, handle ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { selectTarget(handle) }, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(if (selectedTarget == handle) R.string.auto_target_selected else R.string.auto_target, index + 1))
                                }
                                TextButton(onClick = { openCopy(handle) }) { Text(stringResource(R.string.open_copy, index + 1)) }
                            }
                        }
                        SwitchRow(R.string.share_route, shareRoute, setShareRoute)
                        SettingHelp(R.string.share_route_note)
                        OutlinedButton(onClick = shareLink, enabled = url.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.share_link_title))
                        }
                    }
                    SectionCard(R.string.settings_receive) {
                        SwitchRow(R.string.auto_open_stores, autoOpenStores, setAutoOpenStores)
                        SettingHelp(R.string.auto_open_stores_note)
                        SwitchRow(R.string.auto_open_shares, autoOpenShares, setAutoOpenShares)
                        SettingHelp(R.string.auto_open_shares_note)
                        SwitchRow(R.string.compact_mode, compactMode, setCompactMode)
                        SettingHelp(R.string.compact_note)
                    }
                    SectionCard(R.string.home_advanced) {
                        Text(stringResource(R.string.profiles, targets.size))
                        Text(stringResource(when (profileAccess) {
                            ProfileAccess.GRANTED -> R.string.profile_access_granted
                            ProfileAccess.REQUESTABLE -> R.string.profile_access_requestable
                            ProfileAccess.NOT_AVAILABLE -> R.string.profile_settings_unavailable
                            ProfileAccess.UNKNOWN -> R.string.profile_access_unknown
                        }), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = requestProfileAccess, enabled = profileAccess == ProfileAccess.REQUESTABLE,
                                modifier = Modifier.weight(1f)) { Text(stringResource(R.string.profile_request_access)) }
                            OutlinedButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                        }
                    }
                    SectionCard(R.string.home_diagnostics) {
                        Text(device, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SwitchRow(R.string.debug_mode, debugEnabled, setDebugEnabled)
                        Text(stringResource(R.string.debug_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = previewDebug, enabled = debugEnabled, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.report_title))
                        }
                    }
                    Text(stringResource(R.string.privacy), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingHelp(text: Int) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.setup_more)) }
    if (expanded) Text(stringResource(text), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
