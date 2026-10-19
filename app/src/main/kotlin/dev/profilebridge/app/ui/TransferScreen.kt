package dev.profilebridge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R

/** Receives only a fixed registry label, never the link or sender-provided title. */
@Composable
fun TransferScreen(appName: String, failed: Boolean, expired: Boolean, retryEnabled: Boolean,
                   retry: () -> Unit, close: () -> Unit) {
    MaterialTheme {
        Scaffold { insets ->
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)))
                .padding(insets).verticalScroll(rememberScrollState()).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(stringResource(R.string.transfer_title), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(32.dp))
                Text(appName, style = MaterialTheme.typography.displayMedium)
                Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(painterResource(R.drawable.ic_secure_folder), contentDescription = null)
                        Text(stringResource(R.string.transfer_destination), style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Text(stringResource(if (failed) R.string.transfer_failed else R.string.transfer_choose),
                    style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.transfer_unknown))
                if (expired) Text(stringResource(R.string.transfer_expired))
                Spacer(Modifier.height(32.dp))
                OutlinedButton(onClick = retry, enabled = retryEnabled, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.transfer_retry))
                }
                Button(onClick = close, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.close_report))
                }
            }
        }
    }
}
