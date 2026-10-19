package dev.profilebridge.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.AppMode
import dev.profilebridge.app.platform.AppRules

@Composable
fun AppsScreen(modes: Map<String, AppMode>, apply: (Set<String>, AppMode) -> Unit, close: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    MaterialTheme {
        Scaffold { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.apps_note))
                OutlinedTextField(query, { query = it.take(80) }, label = { Text(stringResource(R.string.apps_search)) })
                OutlinedButton(onClick = { selecting = !selecting; selected = emptySet() }) {
                    Text(stringResource(if (selecting) R.string.apps_cancel else R.string.apps_select))
                }
                val visible = AppRules.apps.filter { it.label.contains(query, true) }
                if (selecting) {
                    Text(stringResource(R.string.apps_count, selected.size))
                    TextButton(onClick = { selected = selected + visible.map { it.packageName } }) {
                        Text(stringResource(R.string.apps_all))
                    }
                }
                visible.forEach { app ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(app.label, style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(modeText(modes[app.packageName] ?: AppMode.AUTOMATIC)))
                            if (selecting) Checkbox(checked = app.packageName in selected, onCheckedChange = {
                                selected = if (it) selected + app.packageName else selected - app.packageName
                            }) else TextButton(onClick = { selecting = true; selected = setOf(app.packageName) }) {
                                Text(stringResource(R.string.apps_edit))
                            }
                        }
                    }
                }
                if (selecting) AppMode.entries.forEach { mode ->
                    OutlinedButton(enabled = selected.isNotEmpty(), onClick = {
                        apply(selected, mode); selected = emptySet(); selecting = false
                    }) { Text(stringResource(modeText(mode))) }
                }
                Button(onClick = close) { Text(stringResource(R.string.close_report)) }
            }
        }
    }
}

private fun modeText(mode: AppMode): Int = when (mode) {
    AppMode.AUTOMATIC -> R.string.apps_auto
    AppMode.CONFIRM -> R.string.apps_confirm
    AppMode.DISABLED -> R.string.apps_disabled
}
