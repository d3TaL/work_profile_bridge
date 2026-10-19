package dev.profilebridge.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R
import dev.profilebridge.app.platform.ProfileAccess

/** Informational first setup step; discovery is not evidence of delivery. */
@Composable
fun SetupScreen(role: String, count: Int, samsung: Boolean, access: ProfileAccess,
                refresh: () -> Unit, requestAccess: () -> Unit, done: () -> Unit) {
    MaterialTheme {
        Scaffold { insets ->
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.setup_here), style = MaterialTheme.typography.labelLarge)
                        Text(role, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.setup_detect_note))
                    }
                }
                Text(stringResource(R.string.setup_copies, count), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.setup_copies_note))
                OutlinedButton(onClick = refresh) { Text(stringResource(R.string.refresh)) }
                Text(stringResource(R.string.setup_clone), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.setup_clone_note))
                if (samsung) {
                    Text(stringResource(R.string.setup_samsung), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.setup_samsung_note))
                }
                Text(stringResource(when (access) {
                    ProfileAccess.GRANTED -> R.string.profile_access_granted
                    ProfileAccess.REQUESTABLE -> R.string.profile_access_requestable
                    ProfileAccess.NOT_AVAILABLE -> R.string.profile_settings_unavailable
                    ProfileAccess.UNKNOWN -> R.string.profile_access_unknown
                }))
                OutlinedButton(onClick = requestAccess, enabled = access == ProfileAccess.REQUESTABLE) {
                    Text(stringResource(R.string.profile_request_access))
                }
                Text(stringResource(R.string.setup_finish_note))
                Button(onClick = done, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_continue)) }
            }
        }
    }
}
