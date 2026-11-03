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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
 * have "Next". Saved destination and checks are settings, not a delivery confirmation.
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
                Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val progress by animateFloatAsState((step + 1) / steps.toFloat(), label = "setupProgress")
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.setup_step, step + 1, steps), Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.labelLarge)
                        if (!(main && step == steps - 1)) {
                            TextButton(onClick = done) { Text(stringResource(R.string.skip)) }
                        }
                    }
                    AnimatedContent(targetState = step, modifier = Modifier.weight(1f), label = "setupStep",
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { it * direction / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it * direction / 3 } + fadeOut())
                        }) { current ->
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (main) MainStep(current, state, actions, next = { step = current + 1 }, done, tryLink)
                            else ReceiverStep(current, state, actions, next = { step = current + 1 }, done)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeading(title: Int, subtitle: Int?) {
    Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    subtitle?.let {
        Text(stringResource(it), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ColumnScope.MainStep(step: Int, state: BridgeState, actions: BridgeActions, next: () -> Unit, done: () -> Unit,
                     tryLink: () -> Unit) {
    when (step) {
        0 -> {
            StepHeading(R.string.setup_where, R.string.setup_where_hint)
            DestinationChoices(state.destination, state.samsung) { actions.setDestination(it); next() }
        }
        1 -> {
            StepHeading(R.string.prepare_title, R.string.setup_prepare_hint)
            ChecklistGroup(state, actions)
            Button(onClick = next, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.next))
            }
        }
        else -> {
            Spacer(Modifier.height(24.dp))
            Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null,
                modifier = Modifier.size(72.dp).align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.setup_done), Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(destinationSummary(state.destination)), Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = tryLink, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.setup_try))
            }
            OutlinedButton(onClick = done, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.finish))
            }
        }
    }
}

@Composable
private fun ColumnScope.ReceiverStep(step: Int, state: BridgeState, actions: BridgeActions, next: () -> Unit, done: () -> Unit) {
    when (step) {
        0 -> {
            StepHeading(R.string.setup_space, R.string.setup_space_hint)
            SpaceChoices(state.space) { actions.setSpace(it); next() }
        }
        else -> {
            StepHeading(R.string.setup_open, R.string.setup_open_hint)
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
            Button(onClick = done, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.finish))
            }
        }
    }
}
