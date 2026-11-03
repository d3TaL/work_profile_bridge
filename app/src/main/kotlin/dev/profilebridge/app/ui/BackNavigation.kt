package dev.profilebridge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * The single back handler of a screen. Branching belongs in [onBack]; never register a second
 * handler for the same state. While a predictive back gesture that leaves the screen is in
 * progress ([animate]), the screen shrinks toward the swipe edge; cancelling restores it.
 */
@Composable
fun BackNavigation(
    onBack: () -> Unit,
    enabled: Boolean = true,
    animate: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = state, isBackEnabled = enabled, onBackCompleted = onBack)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceDim)) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            // Read the gesture state here, so a swipe redraws the layer without recomposition.
            val event = (state.transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent
            val progress = if (animate && event != null) event.progress else 0f
            val scale = 1f - progress * 0.1f
            scaleX = scale
            scaleY = scale
            translationX = progress * 24.dp.toPx() * when (event?.swipeEdge) {
                NavigationEvent.EDGE_LEFT -> 1f
                NavigationEvent.EDGE_RIGHT -> -1f
                else -> 0f
            }
            shape = RoundedCornerShape(28.dp * progress)
            clip = progress > 0f
        }) { content() }
    }
}
