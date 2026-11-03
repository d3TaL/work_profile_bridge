package dev.profilebridge.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R

/**
 * First-run setup. Main copy: where links go, a live checklist, done. Receiving copy: which space
 * this is, whether links open immediately. Choosing a card is the step's action; info steps only
 * have "Next". The heading sits in the upper third and every button is at the bottom.
 * Saved destination and checks are settings, not a delivery confirmation.
 */
@Composable
fun SetupScreen(state: BridgeState, actions: BridgeActions, close: () -> Unit, done: () -> Unit,
                tryLink: () -> Unit, initialStep: Int = 0) {
    val main = state.space == Space.MAIN
    val steps = if (main) 3 else 2
    var step by rememberSaveable { mutableIntStateOf(initialStep.coerceIn(0, steps - 1)) }
    BridgeTheme {
        // Back returns to the previous step; on the first step it leaves the setup for Home.
        BackNavigation(onBack = { if (step > 0) step-- else close() }, animate = step == 0) {
            Scaffold { insets ->
                Column(Modifier.fillMaxSize().padding(insets)) {
                    Row(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val progress by animateFloatAsState((step + 1) / steps.toFloat(), label = "setupProgress")
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.setup_step, step + 1, steps), Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.labelLarge)
                    }
                    AnimatedContent(targetState = step, modifier = Modifier.weight(1f), label = "setupStep",
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { it * direction / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it * direction / 3 } + fadeOut())
                        }) { current ->
                        val next = { step = current + 1 }
                        if (main) MainStep(current, state, actions, next, done, tryLink)
                        else ReceiverStep(current, state, actions, next, done)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHint(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ColumnScope.WideButton(text: Int, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(text)) }
}

@Composable
private fun ColumnScope.SkipButton(done: () -> Unit) {
    TextButton(onClick = done, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.skip)) }
}

@Composable
private fun MainStep(step: Int, state: BridgeState, actions: BridgeActions, next: () -> Unit, done: () -> Unit,
                     tryLink: () -> Unit) {
    when (step) {
        0 -> ReachableColumn(title = stringResource(R.string.setup_where), horizontalPadding = 24.dp,
            header = { StepHint(R.string.setup_where_hint) }, bottom = { SkipButton(done) }) {
            DestinationChoices(state.destination, state.samsung) { actions.setDestination(it); next() }
        }
        1 -> ReachableColumn(title = stringResource(R.string.prepare_title), horizontalPadding = 24.dp,
            header = { StepHint(R.string.setup_prepare_hint) },
            bottom = { WideButton(R.string.next, next); SkipButton(done) }) {
            ChecklistGroup(state, actions)
        }
        else -> ReachableColumn(horizontalPadding = 24.dp, header = {
            Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null,
                modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.setup_done), style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold)
            StepHint(destinationSummary(state.destination))
        }, bottom = {
            WideButton(R.string.setup_try, tryLink)
            OutlinedButton(onClick = done, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.finish))
            }
        }) {}
    }
}

@Composable
private fun ReceiverStep(step: Int, state: BridgeState, actions: BridgeActions, next: () -> Unit, done: () -> Unit) {
    when (step) {
        0 -> ReachableColumn(title = stringResource(R.string.setup_space), horizontalPadding = 24.dp,
            header = { StepHint(R.string.setup_space_hint) }, bottom = { SkipButton(done) }) {
            SpaceChoices(state.space) { actions.setSpace(it); next() }
        }
        else -> ReachableColumn(title = stringResource(R.string.setup_open), horizontalPadding = 24.dp,
            header = { StepHint(R.string.setup_open_hint) }, bottom = { WideButton(R.string.finish, done) }) {
            OpenImmediatelyConsent(state, actions) { request ->
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceCard(R.drawable.ic_bolt, R.string.open_now, R.string.open_now_hint, state.openImmediately) {
                        request(true)
                    }
                    ChoiceCard(R.drawable.ic_send, R.string.open_on_tap, R.string.open_on_tap_hint, !state.openImmediately) {
                        request(false)
                    }
                }
            }
        }
    }
}
