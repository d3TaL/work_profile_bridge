package dev.profilebridge.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R

/** Compact window for a received link that was not sent automatically: one tap per destination. */
@Composable
fun QuickSendScreen(state: BridgeState, actions: BridgeActions) {
    BridgeTheme {
        BackNavigation(onBack = actions::closeQuickSend) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(state.linkApp ?: stringResource(R.string.link), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.quick_title), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold)
                    Notice(state.message)
                    SendButtons(state, actions)
                    TextButton(onClick = actions::closeQuickSend, modifier = Modifier.fillMaxWidth()
                        .align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}
