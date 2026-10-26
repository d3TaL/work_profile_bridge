package dev.profilebridge.app.ui

import android.os.UserHandle
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.ProfileAccess
import dev.profilebridge.app.platform.ProfileManager

/** Short, resumable pages. Discovery and saved presets are not delivery ACKs. */
@Composable
fun SetupScreen(role: String, targets: List<UserHandle>, samsung: Boolean, access: ProfileAccess,
                managers: List<ProfileManager>?, openManager: (ProfileManager) -> Unit,
                main: Boolean, managed: Boolean, selectedTarget: UserHandle?, shareRoute: Boolean,
                applyPreset: (Int, UserHandle?) -> Unit,
                refresh: () -> Unit, requestAccess: () -> Unit, openLinkSettings: () -> Unit,
                prepareTest: () -> Unit, message: Int?, done: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var knoxConsent by rememberSaveable { mutableStateOf(false) }
    // A guide selection, not a claim that this app manages the device.
    var guide by rememberSaveable { mutableIntStateOf(0) }
    val manager = if (guide == 0) ProfileManager.SHELTER else ProfileManager.ISLAND
    BackHandler(enabled = step > 0) { step-- }
    BridgeTheme {
        Scaffold { insets ->
            key(step) {
                Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.wizard_step, step + 1, 9))
                    LinearProgressIndicator(progress = { (step + 1) / 9f }, modifier = Modifier.fillMaxWidth())
                    message?.let { Text(stringResource(it)) }
                    when (step) {
                        0 -> {
                            Heading(R.string.setup_routes_title)
                            Highlight(stringResource(R.string.setup_route_work), stringResource(R.string.setup_route_work_body))
                            if (samsung) Highlight(stringResource(R.string.setup_route_knox), stringResource(R.string.setup_route_knox_body))
                            Text(stringResource(R.string.setup_route_entry))
                        }
                        1 -> {
                            Heading(R.string.setup_modes_title)
                            Highlight(stringResource(R.string.apps_confirm), stringResource(R.string.setup_mode_manual_body))
                            Highlight(stringResource(R.string.apps_auto), stringResource(R.string.setup_mode_auto_body))
                            Details(R.string.wizard_banks_note)
                        }
                        2 -> {
                            Heading(R.string.setup_location_title)
                            Highlight(stringResource(R.string.setup_here), role)
                            Text(stringResource(R.string.setup_location_body))
                            Details(R.string.setup_detect_note)
                        }
                        3 -> {
                            Heading(R.string.setup_add_copy_title)
                            Text(stringResource(R.string.setup_copies, targets.size))
                            Text(stringResource(when {
                                managers == null -> R.string.setup_managers_loading
                                managers.isEmpty() -> R.string.setup_managers_none
                                else -> R.string.setup_managers_found
                            }))
                            managers.orEmpty().forEach { found ->
                                Button(onClick = { openManager(found) }) {
                                    Text(stringResource(R.string.setup_open_manager, found.label))
                                }
                            }
                            ManagerTabs(guide) { guide = it }
                            GuideLine(1, if (manager == ProfileManager.SHELTER) R.string.setup_clone_shelter_1 else R.string.setup_clone_island_1)
                            GuideLine(2, if (manager == ProfileManager.SHELTER) R.string.setup_clone_shelter_2 else R.string.setup_clone_island_2)
                            GuideLine(3, R.string.setup_clone_3)
                            OutlinedButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                            if (samsung) Details(R.string.setup_samsung_note, R.string.setup_samsung)
                            Details(R.string.setup_copies_note)
                            Details(R.string.setup_clone_note, R.string.setup_clone)
                        }
                        4 -> {
                            Heading(R.string.setup_exchange_title)
                            ManagerTabs(guide) { guide = it }
                            GuideLine(1, if (manager == ProfileManager.SHELTER) R.string.setup_exchange_shelter_1 else R.string.setup_exchange_island_1)
                            GuideLine(2, if (manager == ProfileManager.SHELTER) R.string.setup_exchange_shelter_2 else R.string.setup_exchange_island_2)
                            GuideLine(3, R.string.setup_exchange_3)
                            OutlinedButton(onClick = { openManager(manager) }, enabled = manager in managers.orEmpty()) {
                                Text(stringResource(R.string.setup_open_manager, manager.label))
                            }
                            Details(R.string.setup_exchange_detail)
                        }
                        5 -> {
                            Heading(R.string.preset_title)
                            if (main) {
                                Text(stringResource(if (shareRoute) R.string.preset_current_knox else R.string.preset_current_direct))
                                targets.forEachIndexed { index, target ->
                                    OutlinedButton(onClick = { applyPreset(1, target) }, modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(if (target == selectedTarget && !shareRoute)
                                            R.string.preset_direct_selected else R.string.preset_direct, index + 1))
                                    }
                                }
                                if (targets.isEmpty()) Text(stringResource(R.string.preset_no_target))
                                if (samsung) Button(onClick = { applyPreset(2, null) }) {
                                    Text(stringResource(R.string.setup_choose_knox))
                                }
                                Details(R.string.preset_sender_note)
                                if (samsung) Details(R.string.preset_knox_sender_note, R.string.preset_knox_sender)
                            } else {
                                if (managed) {
                                    Button(onClick = { applyPreset(3, null) }) { Text(stringResource(R.string.preset_work)) }
                                    Details(R.string.preset_work_note)
                                }
                                if (samsung) {
                                    OutlinedButton(onClick = { knoxConsent = !knoxConsent }) { Text(stringResource(R.string.preset_knox_receiver)) }
                                    if (knoxConsent) {
                                        Text(stringResource(R.string.preset_knox_receiver_note))
                                        Button(onClick = { applyPreset(4, null); knoxConsent = false }) { Text(stringResource(R.string.preset_apply)) }
                                    }
                                }
                            }
                            TextButton(onClick = { applyPreset(5, null) }) { Text(stringResource(R.string.preset_manual)) }
                        }
                        6 -> {
                            Heading(R.string.setup_permission_title)
                            Text(stringResource(when (access) {
                                ProfileAccess.GRANTED -> R.string.setup_access_yes
                                ProfileAccess.REQUESTABLE -> R.string.setup_access_ready
                                ProfileAccess.NOT_AVAILABLE -> R.string.setup_access_not_ready
                                ProfileAccess.UNKNOWN -> R.string.profile_access_unknown
                            }))
                            Button(onClick = requestAccess, enabled = access == ProfileAccess.REQUESTABLE) {
                                Text(stringResource(R.string.setup_allow))
                            }
                            OutlinedButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                            Text(stringResource(R.string.setup_knox_skip))
                            Details(R.string.setup_finish_note, R.string.wizard_android)
                        }
                        7 -> {
                            Heading(R.string.setup_links_title)
                            Text(stringResource(R.string.setup_links_body))
                            Button(onClick = openLinkSettings) { Text(stringResource(R.string.wizard_links)) }
                            Text(stringResource(R.string.setup_links_skip))
                            Details(R.string.wizard_links_note)
                        }
                        8 -> {
                            Heading(R.string.wizard_check)
                            GuideLine(1, R.string.setup_test_1)
                            GuideLine(2, R.string.setup_test_2)
                            GuideLine(3, R.string.setup_test_3)
                            OutlinedButton(onClick = prepareTest) { Text(stringResource(R.string.wizard_test)) }
                            Details(R.string.wizard_check_note)
                        }
                    }
                    Button(onClick = { if (step == 8) done() else step++ }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (step == 8) R.string.wizard_done else R.string.setup_continue))
                    }
                    if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.wizard_back)) }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: Int) { Text(stringResource(text), style = MaterialTheme.typography.headlineMedium) }

@Composable
private fun Highlight(title: String, body: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body)
        }
    }
}

@Composable
private fun GuideLine(number: Int, text: Int) { Text(stringResource(R.string.setup_guide_line, number, stringResource(text))) }

@Composable
private fun ManagerTabs(selected: Int, select: (Int) -> Unit) {
    ProfileManager.entries.forEachIndexed { index, manager ->
        TextButton(onClick = { select(index) }) {
            Text(if (selected == index) "✓ ${manager.label}" else manager.label)
        }
    }
}

@Composable
private fun Details(text: Int, title: Int = R.string.setup_more) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) { Text(stringResource(title)) }
    if (expanded) Text(stringResource(text))
}
