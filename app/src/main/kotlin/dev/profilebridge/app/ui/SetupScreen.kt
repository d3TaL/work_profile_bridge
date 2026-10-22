package dev.profilebridge.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import android.os.UserHandle
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.ProfileAccess

/** Informational first setup step; discovery is not evidence of delivery. */
@Composable
fun SetupScreen(role: String, targets: List<UserHandle>, samsung: Boolean, access: ProfileAccess,
                main: Boolean, managed: Boolean, selectedTarget: UserHandle?, shareRoute: Boolean,
                applyPreset: (Int, UserHandle?) -> Unit,
                refresh: () -> Unit, requestAccess: () -> Unit, openLinkSettings: () -> Unit,
                prepareTest: () -> Unit, message: Int?, done: () -> Unit) {
    var choice by rememberSaveable { mutableIntStateOf(0) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    MaterialTheme {
        Scaffold { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.wizard_step, step + 1, 4), style = MaterialTheme.typography.labelLarge)
                message?.let { Text(stringResource(it)) }
                if (step == 0) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.setup_here), style = MaterialTheme.typography.labelLarge)
                        Text(role, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.setup_detect_note))
                    }
                }
                Text(stringResource(R.string.setup_copies, targets.size), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.setup_copies_note))
                OutlinedButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                Text(stringResource(R.string.setup_clone), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.setup_clone_note))
                if (samsung) {
                    Text(stringResource(R.string.setup_samsung), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.setup_samsung_note))
                }
                }
                if (step == 2) {
                Text(stringResource(R.string.wizard_android), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(when (access) {
                    ProfileAccess.GRANTED -> R.string.profile_access_granted
                    ProfileAccess.REQUESTABLE -> R.string.profile_access_requestable
                    ProfileAccess.NOT_AVAILABLE -> R.string.profile_settings_unavailable
                    ProfileAccess.UNKNOWN -> R.string.profile_access_unknown
                }))
                OutlinedButton(onClick = requestAccess, enabled = access == ProfileAccess.REQUESTABLE) {
                    Text(stringResource(R.string.profile_request_access))
                }
                Text(stringResource(R.string.setup_finish_note))
                OutlinedButton(onClick = openLinkSettings) { Text(stringResource(R.string.wizard_links)) }
                Text(stringResource(R.string.wizard_links_note))
                OutlinedButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                }
                if (step == 1) {
                Text(stringResource(R.string.preset_title), style = MaterialTheme.typography.titleLarge)
                if (main) {
                    Text(stringResource(R.string.preset_sender_note))
                    Text(stringResource(if (shareRoute) R.string.preset_current_knox else R.string.preset_current_direct))
                    targets.forEachIndexed { index, target ->
                        OutlinedButton(onClick = { applyPreset(1, target) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (target == selectedTarget && !shareRoute)
                                R.string.preset_direct_selected else R.string.preset_direct, index + 1))
                        }
                    }
                    if (targets.isEmpty()) Text(stringResource(R.string.preset_no_target))
                    if (samsung) {
                        Text(stringResource(R.string.preset_knox_sender_note))
                        Button(onClick = { applyPreset(2, null) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.preset_knox_sender))
                        }
                    }
                } else {
                    if (managed) {
                        Text(stringResource(R.string.preset_work_note))
                        Button(onClick = { applyPreset(3, null) }) { Text(stringResource(R.string.preset_work)) }
                    }
                    if (samsung) {
                        OutlinedButton(onClick = { choice = if (choice == 4) 0 else 4 }) {
                            Text(stringResource(R.string.preset_knox_receiver))
                        }
                        if (choice == 4) {
                            Text(stringResource(R.string.preset_knox_receiver_note))
                            Button(onClick = { applyPreset(4, null); choice = 0 }) {
                                Text(stringResource(R.string.preset_apply))
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { applyPreset(5, null) }) { Text(stringResource(R.string.preset_manual)) }
                }
                if (step == 3) {
                    Text(stringResource(R.string.wizard_check), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.wizard_check_note))
                    OutlinedButton(onClick = prepareTest) { Text(stringResource(R.string.wizard_test)) }
                    Text(stringResource(R.string.wizard_banks_note))
                    Button(onClick = done, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.wizard_done)) }
                } else Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.setup_continue))
                }
                if (step > 0) OutlinedButton(onClick = { step-- }) { Text(stringResource(R.string.wizard_back)) }
            }
        }
    }
}
