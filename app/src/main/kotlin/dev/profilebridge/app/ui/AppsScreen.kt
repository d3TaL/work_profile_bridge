package dev.profilebridge.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.AppMode
import dev.profilebridge.app.platform.AppRules
import dev.profilebridge.app.platform.SupportedApp
import dev.profilebridge.core.links.BankLinks

@Composable
fun AppsScreen(modes: Map<String, AppMode>, apply: (Set<String>, AppMode) -> Unit, close: () -> Unit,
               selectTab: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    // Packages whose mode dialog is open, and packages awaiting the bank consent dialog.
    var editing by remember { mutableStateOf<Set<String>?>(null) }
    var bankConsent by remember { mutableStateOf<Set<String>?>(null) }
    fun mode(pkg: String) = modes[pkg] ?: AppMode.AUTOMATIC
    fun finish() { selected = emptySet(); selecting = false }

    BackHandler(onBack = close)
    BackHandler(enabled = selecting) { finish() }

    BridgeTheme {
        editing?.let { packages ->
            val single = packages.singleOrNull()
            ModeDialog(
                title = single?.let { pkg -> AppRules.apps.firstOrNull { it.packageName == pkg }?.label }
                    ?: stringResource(R.string.apps_count, packages.size),
                iconPackage = single,
                current = packages.map(::mode).distinct().singleOrNull(),
                choose = { chosen ->
                    editing = null
                    if (chosen == AppMode.AUTOMATIC && packages.any { BankLinks.isPackage(it) }) {
                        bankConsent = packages
                    } else { apply(packages, chosen); finish() }
                },
                dismiss = { editing = null })
        }
        bankConsent?.let { packages ->
            AlertDialog(onDismissRequest = { bankConsent = null },
                title = { Text(stringResource(R.string.bank_auto_title)) },
                text = { Text(stringResource(R.string.bank_auto_note)) },
                confirmButton = { TextButton(onClick = {
                    apply(packages, AppMode.AUTOMATIC); bankConsent = null; finish()
                }) { Text(stringResource(R.string.bank_auto_accept)) } },
                dismissButton = { TextButton(onClick = { bankConsent = null }) { Text(stringResource(R.string.cancel)) } })
        }
        Scaffold(bottomBar = {
            Column {
                if (selecting) SelectionBar(count = selected.size,
                    selectAll = { selected = AppRules.apps.filter { it.label.contains(query, true) }
                        .map { it.packageName }.toSet() },
                    edit = { editing = selected }, cancel = { finish() })
                BottomBar(1, selectTab)
            }
        }) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.apps_subtitle), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { details = !details }) {
                        Icon(painterResource(R.drawable.ic_info), contentDescription = stringResource(R.string.setup_more))
                    }
                    if (!selecting) TextButton(onClick = { selecting = true; selected = emptySet() }) {
                        Text(stringResource(R.string.apps_select))
                    }
                }
                if (details) Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(stringResource(R.string.apps_note), Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                OutlinedTextField(query, { query = it.take(80) }, Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    placeholder = { Text(stringResource(R.string.apps_search)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                    trailingIcon = if (query.isEmpty()) null else { {
                        IconButton(onClick = { query = "" }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.clear))
                        }
                    } })
                val visible = AppRules.apps.filter { it.label.contains(query, true) }
                val (banks, shops) = visible.partition { BankLinks.isPackage(it.packageName) }
                if (visible.isEmpty()) Text(stringResource(R.string.apps_empty),
                    Modifier.fillMaxWidth().padding(vertical = 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(Triple(R.string.apps_shops, null, shops), Triple(R.string.apps_banks, R.string.apps_banks_note, banks))
                    .forEach { (title, note, apps) ->
                        if (apps.isNotEmpty()) {
                            Column(Modifier.padding(start = 4.dp, top = 4.dp)) {
                                Text(stringResource(title), style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                note?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Card(Modifier.fillMaxWidth()) {
                                apps.forEachIndexed { index, app ->
                                    if (index > 0) HorizontalDivider(Modifier.padding(start = 80.dp))
                                    AppRow(app, mode(app.packageName), selecting, app.packageName in selected) {
                                        if (selecting) {
                                            selected = if (app.packageName in selected)
                                                selected - app.packageName else selected + app.packageName
                                        } else { editing = setOf(app.packageName) }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun AppRow(app: SupportedApp, mode: AppMode, selecting: Boolean, checked: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AppIcon(app.packageName)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            ModeBadge(mode)
        }
        if (selecting) Checkbox(checked = checked, onCheckedChange = null)
        else Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ModeBadge(mode: AppMode) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (mode) {
        AppMode.AUTOMATIC -> scheme.primaryContainer to scheme.onPrimaryContainer
        AppMode.CONFIRM -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        AppMode.DISABLED -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(stringResource(modeText(mode)), Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SelectionBar(count: Int, selectAll: () -> Unit, edit: () -> Unit, cancel: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.apps_count, count), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = selectAll) { Text(stringResource(R.string.apps_all)) }
                TextButton(onClick = cancel) { Text(stringResource(R.string.cancel)) }
            }
            Button(onClick = edit, enabled = count > 0, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.apps_edit))
            }
        }
    }
}

@Composable
private fun ModeDialog(title: String, iconPackage: String?, current: AppMode?,
                       choose: (AppMode) -> Unit, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss,
        icon = if (iconPackage == null) null else { { AppIcon(iconPackage) } },
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppMode.entries.forEach { mode ->
                    Row(Modifier.fillMaxWidth()
                        .selectable(selected = mode == current, role = Role.RadioButton, onClick = { choose(mode) })
                        .padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
                        RadioButton(selected = mode == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(modeText(mode)), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(modeBody(mode)), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } })
}

private fun modeText(mode: AppMode): Int = when (mode) {
    AppMode.AUTOMATIC -> R.string.apps_auto
    AppMode.CONFIRM -> R.string.apps_confirm
    AppMode.DISABLED -> R.string.apps_disabled
}

private fun modeBody(mode: AppMode): Int = when (mode) {
    AppMode.AUTOMATIC -> R.string.apps_auto_body
    AppMode.CONFIRM -> R.string.apps_confirm_body
    AppMode.DISABLED -> R.string.apps_disabled_body
}
