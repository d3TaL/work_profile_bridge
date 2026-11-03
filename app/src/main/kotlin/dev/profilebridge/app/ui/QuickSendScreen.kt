package dev.profilebridge.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.profilebridge.app.R

/** Compact window for a received link that was not sent automatically: one tap per destination. */
@Composable
fun QuickSendScreen(state: BridgeState, actions: BridgeActions) {
    BridgeTheme {
        BackNavigation(onBack = actions::closeQuickSend) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                ReachableColumn(header = {
                    Text(state.linkApp ?: stringResource(R.string.link), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.quick_title), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold)
                }, bottom = {
                    SendButtons(state, actions)
                    TextButton(onClick = actions::closeQuickSend, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel))
                    }
                }) {
                    Notice(state.message)
                }
            }
        }
    }
}
