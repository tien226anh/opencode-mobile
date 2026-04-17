package ai.opencode.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ai.opencode.mobile.navigation.SettingsComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    component: SettingsComponent,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by component.viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            component.viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Server Connection ---
            Text(text = "Server Connection", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.serverHost,
                    onValueChange = { component.viewModel.updateServerHost(it) },
                    label = { Text("Host") },
                    placeholder = { Text("http://localhost") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.serverPort,
                    onValueChange = { component.viewModel.updateServerPort(it) },
                    label = { Text("Port") },
                    placeholder = { Text("4096") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            Text(
                text = "\u2022 Android emulator: localhost auto-maps to 10.0.2.2 (host machine)\n" +
                    "\u2022 Physical device: Use your computer's LAN IP (e.g. http://192.168.1.x)\n" +
                    "\u2022 Cloudflare tunnel: Use https:// and full URL (e.g. https://xxx.trycloudflare.com), leave port blank\n" +
                    "\u2022 Server must listen on 0.0.0.0 for LAN access: opencode serve --hostname 0.0.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            // --- Authentication ---
            Text(text = "Authentication (optional)", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Leave empty if the server has no authentication configured.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = { component.viewModel.updateUsername(it) },
                label = { Text("Username") },
                placeholder = { Text("opencode") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = { component.viewModel.updatePassword(it) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { component.viewModel.testConnection() },
                    enabled = !state.isTesting,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp).height(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Test Connection")
                    }
                }

                if (state.isConnectionSuccessful != null) {
                    Text(
                        text = if (state.isConnectionSuccessful!!) "\u2705 Connected" else "\u274C Failed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isConnectionSuccessful!!) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }

            HorizontalDivider()

            // --- Save & Connect ---
            Button(
                onClick = { component.saveAndGoBack() },
                enabled = !state.isTesting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isConnectionSuccessful == true) "Save & Connect" else "Save Settings")
            }

            if (state.isSaved) {
                Text(
                    text = "\u2713 Settings saved. Tap \"Save & Connect\" to return to sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider()

            // --- About ---
            Text(text = "About", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "OpenCode Mobile v1.0.0\nConnect to any OpenCode server instance to manage your AI coding sessions on the go.\n\nProvider, model, and mode are configurable per session in the chat screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}