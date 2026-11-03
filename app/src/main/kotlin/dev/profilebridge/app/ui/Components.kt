package dev.profilebridge.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.ProfileAccess
import dev.profilebridge.app.platform.ProfileManager

private val CardShape = RoundedCornerShape(20.dp)

/** True on short windows (landscape, split screen): headers stay compact and big icons hide. */
val LocalShortScreen = staticCompositionLocalOf { false }

/**
 * One-handed layout (as in One UI): the top part of the screen is a large header that is only
 * read, controls start below it, and short screens keep [bottom] next to the thumb. Once the large
 * title scrolls away a small one fades in; it is drawn in a layer, so scrolling does not recompose.
 */
@Composable
fun ReachableColumn(
    modifier: Modifier = Modifier,
    title: String? = null,
    header: @Composable ColumnScope.() -> Unit = {},
    horizontalPadding: Dp = 16.dp,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val short = maxHeight < 480.dp
        val headerHeight = if (short) 0.dp else (maxHeight * 0.28f).coerceAtLeast(96.dp)
        CompositionLocalProvider(LocalShortScreen provides short) {
        Column(Modifier.fillMaxWidth().verticalScroll(scroll).heightIn(min = maxHeight)
            .padding(horizontal = horizontalPadding), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.fillMaxWidth().heightIn(min = headerHeight).padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                    title?.let {
                        Text(it, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    }
                    header()
                }
                content()
            }
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                content = bottom)
        }
        }
        if (title != null) {
            val fadeStart = with(LocalDensity.current) { headerHeight.coerceAtLeast(96.dp).toPx() * 0.6f }
            Box(Modifier.fillMaxWidth().clearAndSetSemantics {}.graphicsLayer {
                alpha = ((scroll.value - fadeStart) / fadeStart.coerceAtLeast(1f) * 3f).coerceIn(0f, 1f)
            }.background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = horizontalPadding, vertical = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** One-tap destination switch for Home: the same choice as in Settings, in thumb reach. */
@Composable
fun DestinationSwitch(selected: Destination, samsung: Boolean, choose: (Destination) -> Unit) {
    val options = buildList {
        add(Destination.WORK_PROFILE)
        if (samsung || selected == Destination.SECURE_FOLDER) add(Destination.SECURE_FOLDER)
        add(Destination.ASK)
    }
    val scheme = MaterialTheme.colorScheme
    // Equal tile heights even when one label wraps to two lines.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val on = option == selected
            val container by animateColorAsState(if (on) scheme.primaryContainer else scheme.surfaceContainerLow,
                label = "switch")
            val content by animateColorAsState(if (on) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                label = "switchText")
            Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = 88.dp).clip(CardShape).background(container)
                .selectable(selected = on, role = Role.RadioButton) { choose(option) }
                .padding(horizontal = 6.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                Icon(painterResource(destinationIcon(option)), contentDescription = null,
                    modifier = Modifier.size(24.dp), tint = content)
                Text(stringResource(when (option) {
                    Destination.WORK_PROFILE -> R.string.dest_work
                    Destination.SECURE_FOLDER -> R.string.dest_secure
                    Destination.ASK -> R.string.dest_ask_short
                }), color = content, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun SectionLabel(text: Int) {
    Text(stringResource(text), Modifier.padding(start = 4.dp, top = 12.dp),
        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(content = content)
    }
}

@Composable
fun GroupDivider() = HorizontalDivider(Modifier.padding(start = 56.dp),
    color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun IconBadge(icon: Int, highlighted: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(if (highlighted) scheme.primary else scheme.secondaryContainer,
        label = "badge")
    val tint by animateColorAsState(if (highlighted) scheme.onPrimary else scheme.onSecondaryContainer,
        label = "badgeTint")
    Box(Modifier.size(44.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
    }
}

/** One option of a single-choice group: tapping it selects it (radio semantics for TalkBack). */
@Composable
fun ChoiceCard(icon: Int, title: Int, subtitle: Int?, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
        label = "choice")
    val content by animateColorAsState(if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        label = "choiceText")
    Surface(
        Modifier.fillMaxWidth().clip(CardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = CardShape, color = container, contentColor = content,
        border = if (selected) BorderStroke(2.dp, scheme.primary) else null,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconBadge(icon, selected)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                subtitle?.let {
                    Text(stringResource(it), style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.75f))
                }
            }
            AnimatedVisibility(selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null, tint = scheme.primary)
            }
        }
    }
}

@Composable
fun DestinationChoices(selected: Destination?, samsung: Boolean, choose: (Destination) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceCard(R.drawable.ic_work, R.string.dest_work, R.string.dest_work_hint,
            selected == Destination.WORK_PROFILE) { choose(Destination.WORK_PROFILE) }
        if (samsung || selected == Destination.SECURE_FOLDER) {
            ChoiceCard(R.drawable.ic_secure_folder, R.string.dest_secure, R.string.dest_secure_hint,
                selected == Destination.SECURE_FOLDER) { choose(Destination.SECURE_FOLDER) }
        }
        ChoiceCard(R.drawable.ic_ask, R.string.dest_ask,
            if (samsung) R.string.dest_ask_hint else R.string.dest_ask_hint_single,
            selected == Destination.ASK) { choose(Destination.ASK) }
    }
}

@Composable
fun SpaceChoices(selected: Space?, choose: (Space) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceCard(R.drawable.ic_work, R.string.dest_work, R.string.space_work_hint,
            selected == Space.WORK_PROFILE) { choose(Space.WORK_PROFILE) }
        ChoiceCard(R.drawable.ic_secure_folder, R.string.dest_secure, R.string.space_secure_hint,
            selected == Space.SECURE_FOLDER) { choose(Space.SECURE_FOLDER) }
    }
}

@Composable
fun SettingRow(icon: Int, title: Int, value: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            value?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onClick != null) Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SwitchSetting(icon: Int, title: Int, value: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
        .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            value?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** A short status line; keeps its last text while it fades out. */
@Composable
fun Notice(text: Int?, error: Boolean = false) {
    val last = remember { mutableStateOf(text) }
    SideEffect { if (text != null) last.value = text }
    AnimatedVisibility(text != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val shown = text ?: last.value ?: return@AnimatedVisibility
        val scheme = MaterialTheme.colorScheme
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            color = if (error) scheme.errorContainer else scheme.secondaryContainer,
            contentColor = if (error) scheme.onErrorContainer else scheme.onSecondaryContainer) {
            Text(stringResource(shown), Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CheckIcon(status: CheckStatus) {
    Crossfade(status, label = "checkStatus") { current ->
        val scheme = MaterialTheme.colorScheme
        val (icon, tint) = when (current) {
            CheckStatus.DONE -> R.drawable.ic_check_circle to scheme.primary
            CheckStatus.TODO -> R.drawable.ic_circle_outline to scheme.outline
            CheckStatus.MANUAL -> R.drawable.ic_help to scheme.tertiary
        }
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
    }
}

/** One readiness item. Only actions that work right now are shown; never a disabled button. */
@Composable
fun CheckRow(check: Check, status: CheckStatus, state: BridgeState, actions: BridgeActions) {
    val statusText = stringResource(when (status) {
        CheckStatus.DONE -> R.string.check_done
        CheckStatus.TODO -> R.string.check_todo
        CheckStatus.MANUAL -> R.string.check_manual
    })
    Row(Modifier.fillMaxWidth().animateContentSize().semantics { stateDescription = statusText }
        .padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CheckIcon(status)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(checkTitle(check)), style = MaterialTheme.typography.titleMedium)
            Text(checkDetail(check, status, state), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (status != CheckStatus.DONE) {
                CheckAction(check, state, actions)
                checkHow(check, state)?.let { how -> HowTo(check, how) }
            }
        }
    }
}

/** A collapsed one-line "how to" for steps that happen outside Bridge. */
@Composable
private fun HowTo(check: Check, text: Int) {
    var open by rememberSaveable(check) { mutableStateOf(false) }
    Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { open = !open },
        contentAlignment = Alignment.CenterStart) {
        Text(stringResource(R.string.how), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
    }
    AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun checkHow(check: Check, state: BridgeState): Int? {
    val managers = state.managers.orEmpty()
    val island = ProfileManager.ISLAND in managers && ProfileManager.SHELTER !in managers
    return when (check) {
        Check.SECOND_COPY -> if (island) R.string.how_copy_island else R.string.how_copy_shelter
        Check.PROFILE_ACCESS -> when {
            state.access == ProfileAccess.REQUESTABLE -> null
            island -> R.string.how_access_island
            else -> R.string.how_access_shelter
        }
        Check.SECURE_FOLDER_COPY -> R.string.how_secure
        Check.LINKS -> null
    }
}

private fun checkTitle(check: Check) = when (check) {
    Check.SECOND_COPY -> R.string.check_copy
    Check.PROFILE_ACCESS -> R.string.check_access
    Check.SECURE_FOLDER_COPY -> R.string.check_secure
    Check.LINKS -> R.string.check_links
}

@Composable
private fun checkDetail(check: Check, status: CheckStatus, state: BridgeState): String = when (check) {
    Check.SECOND_COPY -> if (status == CheckStatus.DONE) {
        if (state.copies.size == 1) stringResource(R.string.check_copy_found)
        else stringResource(R.string.check_copies_found, state.copies.size)
    } else stringResource(R.string.check_copy_todo)
    Check.PROFILE_ACCESS -> stringResource(when {
        status == CheckStatus.DONE -> R.string.check_access_done
        state.access == ProfileAccess.REQUESTABLE -> R.string.check_access_request
        else -> R.string.check_access_manager
    })
    Check.SECURE_FOLDER_COPY -> stringResource(R.string.check_secure_todo)
    Check.LINKS -> when {
        state.links == null -> stringResource(R.string.check_links_unknown)
        status == CheckStatus.DONE -> stringResource(R.string.check_links_done, state.links.selected, state.links.total)
        else -> stringResource(R.string.check_links_todo)
    }
}

@Composable
private fun CheckAction(check: Check, state: BridgeState, actions: BridgeActions) {
    val managers = state.managers.orEmpty()
    when (check) {
        Check.SECOND_COPY -> managers.forEach { manager ->
            FilledTonalButton(onClick = { actions.openManager(manager) }) {
                Text(stringResource(R.string.open_named, manager.label))
            }
        }
        Check.PROFILE_ACCESS -> if (state.access == ProfileAccess.REQUESTABLE) {
            Button(onClick = actions::requestAccess) { Text(stringResource(R.string.allow)) }
        } else managers.forEach { manager ->
            FilledTonalButton(onClick = { actions.openManager(manager) }) {
                Text(stringResource(R.string.open_named, manager.label))
            }
        }
        Check.SECURE_FOLDER_COPY -> Unit
        Check.LINKS -> FilledTonalButton(onClick = actions::openLinkSettings) {
            Text(stringResource(R.string.configure))
        }
    }
}

@Composable
fun ChecklistGroup(state: BridgeState, actions: BridgeActions) {
    SettingsGroup {
        state.checks().forEachIndexed { index, (check, status) ->
            if (index > 0) GroupDivider()
            CheckRow(check, status, state, actions)
        }
    }
}
