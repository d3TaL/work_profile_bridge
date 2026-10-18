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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.LocalHandler
import dev.profilebridge.app.platform.ProfileAccess

@Composable
fun BridgeScreen(
    probe: ProbeViewModel, targets: List<UserHandle>, message: Int?, device: String,
    url: String, linkError: Int?, handlers: List<LocalHandler>, refresh: () -> Unit,
    changeUrl: (String) -> Unit, inspect: () -> Unit, clear: () -> Unit,
    openCopy: (UserHandle) -> Unit, openHandler: (LocalHandler) -> Unit,
    shareReport: (String) -> Unit,
    copyCode: (String) -> Unit, pasteCode: () -> Unit,
    profileAccess: ProfileAccess, requestProfileAccess: () -> Unit, sendToCopy: (UserHandle) -> Unit,
    autoOpenStores: Boolean, setAutoOpenStores: (Boolean) -> Unit,
) {
    val state by probe.state.collectAsStateWithLifecycle()
    MaterialTheme {
        state.report?.let { report ->
            AlertDialog(onDismissRequest = probe::dismissReport,
                title = { Text(stringResource(R.string.report_title)) },
                text = { Text(report, Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { shareReport(report) }) { Text(stringResource(R.string.share_report)) } },
                dismissButton = { TextButton(onClick = probe::dismissReport) { Text(stringResource(R.string.close_report)) } })
        }
        Scaffold { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.intro))
                Text(device, style = MaterialTheme.typography.bodySmall)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.profiles, targets.size))
                         Text(stringResource(R.string.profile_note))
                         Text(stringResource(when (profileAccess) {
                             ProfileAccess.GRANTED -> R.string.profile_access_granted
                             ProfileAccess.REQUESTABLE -> R.string.profile_access_requestable
                             ProfileAccess.NOT_AVAILABLE -> R.string.profile_settings_unavailable
                             ProfileAccess.UNKNOWN -> R.string.profile_access_unknown
                         }))
                         OutlinedButton(onClick = requestProfileAccess,
                             enabled = profileAccess == ProfileAccess.REQUESTABLE) {
                             Text(stringResource(R.string.profile_request_access))
                         }
                        OutlinedButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                        targets.forEachIndexed { index, handle ->
                            Button(onClick = { openCopy(handle) }) { Text(stringResource(R.string.open_copy, index + 1)) }
                        }
                        message?.let { Text(stringResource(it)) }
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.tls_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.tls_note))
                        Button(onClick = probe::start, enabled = state.listener !in listOf(ProbeStatus.STARTING, ProbeStatus.LISTENING)) {
                            Text(stringResource(R.string.start))
                        }
                        if (state.sessionCode != null) {
                             OutlinedTextField(value = state.sessionCode.orEmpty(), onValueChange = {}, readOnly = true,
                                 label = { Text(stringResource(R.string.session_code)) }, modifier = Modifier.fillMaxWidth())
                             OutlinedButton(onClick = { state.sessionCode?.let(copyCode) }) { Text(stringResource(R.string.copy_code)) }
                            OutlinedButton(onClick = probe::stop) { Text(stringResource(R.string.stop)) }
                        }
                         Text(stringResource(statusText(state.listener)))
                         if (state.listener == ProbeStatus.LISTENING) Text(stringResource(R.string.remaining, state.remainingSeconds))
                         state.serverEvent?.let { Text("SERVER: ${it.stage} · ${it.status} · ${it.reason}") }
                         state.serverFailure?.let { Text("SERVER failure: ${it.stage} · ${it.reason}") }
                        OutlinedTextField(value = state.input, onValueChange = probe::edit, singleLine = true,
                            label = { Text(stringResource(R.string.input_code)) }, modifier = Modifier.fillMaxWidth(),
                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false))
                         OutlinedButton(onClick = pasteCode, enabled = state.result != ProbeStatus.CHECKING) {
                             Text(stringResource(R.string.paste_code))
                         }
                         OutlinedButton(onClick = { copyCode(state.input) }, enabled = state.input.isNotEmpty()) {
                             Text(stringResource(R.string.copy_code))
                         }
                        Button(onClick = probe::ping, enabled = state.result != ProbeStatus.CHECKING) { Text(stringResource(R.string.ping)) }
                         if (state.result != ProbeStatus.IDLE) Text(stringResource(statusText(state.result)))
                         state.clientEvent?.let { Text("${it.side}: ${it.stage} · ${it.status} · ${it.reason}") }
                         OutlinedButton(onClick = probe::selfCheck, enabled = state.result != ProbeStatus.CHECKING &&
                             state.listener !in listOf(ProbeStatus.STARTING, ProbeStatus.LISTENING)) {
                             Text(stringResource(R.string.self_check))
                         }
                         Text(stringResource(R.string.self_check_note))
                         OutlinedButton(onClick = probe::previewReport) { Text(stringResource(R.string.report_title)) }
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.link_title), style = MaterialTheme.typography.titleMedium)
                         Text(stringResource(R.string.link_note))
                         Text(stringResource(R.string.auto_open_stores))
                         Switch(checked = autoOpenStores, onCheckedChange = setAutoOpenStores)
                         Text(stringResource(R.string.auto_open_stores_note))
                        OutlinedTextField(value = url, onValueChange = changeUrl, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text(stringResource(R.string.url)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
                        Button(onClick = inspect, enabled = url.isNotBlank()) { Text(stringResource(R.string.inspect)) }
                         OutlinedButton(onClick = clear) { Text(stringResource(R.string.clear)) }
                         Text(stringResource(R.string.profile_send_note))
                         targets.forEachIndexed { index, target ->
                             OutlinedButton(onClick = { sendToCopy(target) }, enabled = url.isNotBlank()) {
                                 Text(stringResource(R.string.profile_send, index + 1))
                             }
                         }
                        linkError?.let { Text(stringResource(it)) }
                        handlers.forEach { handler ->
                            OutlinedButton(onClick = { openHandler(handler) }) { Text(stringResource(R.string.handler, handler.packageName)) }
                        }
                    }
                }
                Text(stringResource(R.string.privacy), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun statusText(status: ProbeStatus): Int = when (status) {
    ProbeStatus.IDLE -> R.string.idle
    ProbeStatus.STARTING -> R.string.starting
    ProbeStatus.LISTENING -> R.string.listening
    ProbeStatus.FINISHED -> R.string.finished
    ProbeStatus.CHECKING -> R.string.checking
    ProbeStatus.REACHABLE -> R.string.reachable
    ProbeStatus.UNAVAILABLE -> R.string.unavailable
    ProbeStatus.TLS_REJECTED -> R.string.tls_rejected
    ProbeStatus.INVALID -> R.string.invalid
    ProbeStatus.SELF -> R.string.self
    ProbeStatus.NOT_PERMITTED -> R.string.not_permitted
}
