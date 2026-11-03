package dev.profilebridge.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R

/**
 * Backdrop under the system share sheet. Receives only a fixed registry label, never the link.
 * Bridge cannot see what was chosen in the sheet, so nothing here claims delivery.
 */
@Composable
fun TransferScreen(appName: String, failed: Boolean, expired: Boolean, retryEnabled: Boolean,
                   retry: () -> Unit, close: () -> Unit) {
    BridgeTheme {
        // Back behaves like the Close button: no retry, no new chooser.
        BackNavigation(onBack = close) {
            Scaffold { insets ->
                Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)))
                    .padding(insets).verticalScroll(rememberScrollState()).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(32.dp))
                    Icon(painterResource(R.drawable.ic_secure_folder), contentDescription = null,
                        modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(appName, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(if (failed) R.string.transfer_failed else R.string.transfer_choose),
                        style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (expired) R.string.transfer_expired else R.string.transfer_unknown),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    AnimatedVisibility(retryEnabled) {
                        OutlinedButton(onClick = retry, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text(stringResource(R.string.transfer_retry))
                        }
                    }
                    Button(onClick = close, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}
