package dev.profilebridge.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.AppRules

/** Home (tab 0) and Settings (tab 2). Apps (tab 1) is its own screen with the same bottom bar. */
@Composable
fun MainScreen(tab: Int, state: BridgeState, actions: BridgeActions) {
    BridgeTheme {
        state.report?.let { ReportDialog(it, actions) }
        // Bottom navigation exits through Home: back on another tab returns to Home first.
        BackNavigation(onBack = { actions.selectTab(0) }, enabled = tab != 0) {
            Scaffold(bottomBar = { BottomBar(tab, actions::selectTab) }) { insets ->
                AnimatedContent(targetState = tab, label = "tab",
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }) { current ->
                    Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding()
                        .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (current == 2) SettingsContent(state, actions) else HomeContent(state, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(state: BridgeState, actions: BridgeActions) {
    ScreenTitle(stringResource(R.string.home_title))
    Notice(state.message)
    if (state.space == Space.MAIN) StatusCard(state, actions) else ReceiverCard(state, actions)
    LinkCard(state, actions)
}

fun destinationIcon(destination: Destination) = when (destination) {
    Destination.WORK_PROFILE -> R.drawable.ic_work
    Destination.SECURE_FOLDER -> R.drawable.ic_secure_folder
    Destination.ASK -> R.drawable.ic_ask
}

fun destinationSummary(destination: Destination) = when (destination) {
    Destination.WORK_PROFILE -> R.string.summary_work
    Destination.SECURE_FOLDER -> R.string.summary_secure
    Destination.ASK -> R.string.summary_ask
}

@Composable
private fun StatusCard(state: BridgeState, actions: BridgeActions) {
    val remaining = state.checks().remaining
    val ready = remaining == 0
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (ready) scheme.primaryContainer else scheme.tertiaryContainer,
        label = "status")
    val content by animateColorAsState(if (ready) scheme.onPrimaryContainer else scheme.onTertiaryContainer,
        label = "statusText")
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = container, contentColor = content) {
        Column(Modifier.animateContentSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedContent(targetState = ready, label = "statusIcon") { isReady ->
                    Icon(painterResource(if (isReady) R.drawable.ic_check_circle else destinationIcon(state.destination)),
                        contentDescription = null, modifier = Modifier.size(36.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (ready) stringResource(R.string.status_ready) else stringResource(R.string.status_left, remaining),
                        style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(destinationSummary(state.destination)), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (!ready) FilledTonalButton(onClick = { actions.selectTab(2) }) {
                Text(stringResource(R.string.status_finish))
            }
        }
    }
}

@Composable
private fun ReceiverCard(state: BridgeState, actions: BridgeActions) {
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(painterResource(if (state.space == Space.SECURE_FOLDER) R.drawable.ic_secure_folder else R.drawable.ic_work),
                contentDescription = null, modifier = Modifier.size(36.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.receiver_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(if (state.openImmediately) R.string.open_now_on else R.string.open_now_off),
                    style = MaterialTheme.typography.bodyMedium)
            }
            val label = stringResource(R.string.open_now)
            OpenImmediatelyConsent(state, actions) { request ->
                Switch(checked = state.openImmediately, onCheckedChange = request,
                    modifier = Modifier.semantics { contentDescription = label })
            }
        }
    }
}

/**
 * Turning on "open immediately" in Secure Folder needs a separate consent: shares there are not
 * authenticated, any app of the folder can hand a store link to Bridge.
 */
@Composable
fun OpenImmediatelyConsent(state: BridgeState, actions: BridgeActions, control: @Composable ((Boolean) -> Unit) -> Unit) {
    var asking by remember { mutableStateOf(false) }
    if (asking) AlertDialog(onDismissRequest = { asking = false },
        title = { Text(stringResource(R.string.open_now_consent_title)) },
        text = { Text(stringResource(R.string.open_now_consent)) },
        confirmButton = { TextButton(onClick = { asking = false; actions.setOpenImmediately(true) }) {
            Text(stringResource(R.string.enable))
        } },
        dismissButton = { TextButton(onClick = { asking = false }) { Text(stringResource(R.string.cancel)) } })
    control { enabled ->
        if (enabled && state.space == Space.SECURE_FOLDER) asking = true else actions.setOpenImmediately(enabled)
    }
}

@Composable
private fun LinkCard(state: BridgeState, actions: BridgeActions) {
    SettingsGroup {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = state.url, onValueChange = actions::changeUrl, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(16.dp),
                label = { Text(state.linkApp?.let { stringResource(R.string.link_of, it) } ?: stringResource(R.string.link)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_link), contentDescription = null) },
                trailingIcon = if (state.url.isEmpty()) null else { {
                    IconButton(onClick = { actions.changeUrl("") }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.clear))
                    }
                } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
            Notice(state.linkMessage, error = state.linkMessage != R.string.open_ok)
            AnimatedVisibility(state.url.isNotBlank(), enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.space == Space.MAIN) SendButtons(state, actions)
                    TextButton(onClick = actions::findHandlers) { Text(stringResource(R.string.open_here)) }
                    state.handlers.forEach { handler ->
                        OutlinedButton(onClick = { actions.openHandler(handler) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.open_named, appLabel(handler.packageName)))
                        }
                    }
                }
            }
            if (state.url.isBlank()) Text(stringResource(
                if (state.space == Space.MAIN) R.string.link_hint else R.string.receiver_hint), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun appLabel(pkg: String) = AppRules.apps.firstOrNull { it.packageName == pkg }?.label ?: pkg

/** Send actions for the chosen destination only; "Ask" shows both. */
@Composable
fun SendButtons(state: BridgeState, actions: BridgeActions) {
    val work = state.destination != Destination.SECURE_FOLDER
    val secure = state.destination == Destination.SECURE_FOLDER ||
        (state.destination == Destination.ASK && state.samsung)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (work) when {
            state.copies.isEmpty() -> Text(stringResource(R.string.send_no_copy),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            state.copies.size == 1 -> SendButton(R.drawable.ic_work, stringResource(R.string.send_work)) {
                actions.sendToCopy(state.copies.first())
            }
            else -> state.copies.forEachIndexed { index, copy ->
                SendButton(R.drawable.ic_work, stringResource(R.string.send_work_copy, index + 1)) { actions.sendToCopy(copy) }
            }
        }
        if (secure) SendButton(R.drawable.ic_secure_folder, stringResource(R.string.send_secure), actions::sendToSecureFolder)
    }
}

@Composable
private fun SendButton(icon: Int, text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(text)
    }
}

@Composable
private fun SettingsContent(state: BridgeState, actions: BridgeActions) {
    ScreenTitle(stringResource(R.string.nav_settings))
    Notice(state.message)
    if (state.space == Space.MAIN) {
        SectionLabel(R.string.dest_title)
        DestinationChoices(state.destination, state.samsung, actions::setDestination)
        // Only what the chosen destination needs; switching modes animates the section.
        AnimatedContent(targetState = state.destination, label = "destinationSettings",
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }) { destination ->
            val shown = state.copy(destination = destination)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel(R.string.prepare_title)
                ChecklistGroup(shown, actions)
                if (destination != Destination.SECURE_FOLDER && state.copies.size > 1) {
                    SectionLabel(R.string.copy_title)
                    SettingsGroup {
                        state.copies.forEachIndexed { index, copy ->
                            if (index > 0) GroupDivider()
                            SettingRow(R.drawable.ic_copy, R.string.copy_item,
                                if (copy == state.target) stringResource(R.string.copy_selected, index + 1)
                                else stringResource(R.string.copy_number, index + 1)) { actions.selectCopy(copy) }
                        }
                    }
                }
            }
        }
    } else {
        SectionLabel(R.string.space_title)
        SpaceChoices(state.space, actions::setSpace)
        SectionLabel(R.string.received_title)
        SettingsGroup {
            OpenImmediatelyConsent(state, actions) { request ->
                SwitchSetting(R.drawable.ic_bolt, R.string.open_now, R.string.open_now_hint, state.openImmediately, request)
            }
        }
    }
    SectionLabel(R.string.more_title)
    SettingsGroup {
        SwitchSetting(R.drawable.ic_window, R.string.compact_window, R.string.compact_window_hint,
            state.compactWindow, actions::setCompactWindow)
        GroupDivider()
        SettingRow(R.drawable.ic_replay, R.string.setup_again, onClick = actions::openSetup)
        GroupDivider()
        SwitchSetting(R.drawable.ic_info, R.string.diagnostics, R.string.diagnostics_hint,
            state.diagnostics, actions::setDiagnostics)
        AnimatedVisibility(state.diagnostics) {
            Column {
                GroupDivider()
                SettingRow(R.drawable.ic_open_in_new, R.string.report_title, onClick = actions::previewReport)
            }
        }
    }
    Text(stringResource(R.string.privacy, state.version), Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ReportDialog(report: String, actions: BridgeActions) {
    AlertDialog(onDismissRequest = actions::dismissReport,
        title = { Text(stringResource(R.string.report_title)) },
        text = { Text(report, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { actions.saveReport(report) }) { Text(stringResource(R.string.save_report)) }
                TextButton(onClick = { actions.shareReport(report) }) { Text(stringResource(R.string.share_report)) }
            }
        },
        dismissButton = { TextButton(onClick = actions::dismissReport) { Text(stringResource(R.string.close)) } })
}
