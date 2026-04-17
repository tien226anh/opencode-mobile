package ai.opencode.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = { component.viewModel.updateServerUrl(it) },
                label = { Text("Server URL") },
                placeholder = { Text("https://xxx.trycloudflare.com or http://localhost:54321") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                text = "Enter the full URL including https://. For Cloudflare tunnels, use the https://xxx-xxx.trycloudflare.com URL from the tunnel output.",
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

            // --- Provider/Model/Mode Selection (only visible after connection) ---
            if (state.isConnectionSuccessful == true && state.providers.isNotEmpty()) {
                HorizontalDivider()
                Text(text = "Provider & Model", style = MaterialTheme.typography.titleMedium)

                // Provider dropdown
                var providerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded },
                ) {
                    val providerName = state.providers.find { it.id == state.selectedProviderId }?.name?.ifEmpty { state.selectedProviderId } ?: state.selectedProviderId
                    OutlinedTextField(
                        value = providerName.ifEmpty { "Select provider" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false },
                    ) {
                        state.providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.name.ifEmpty { provider.id }) },
                                onClick = {
                                    component.viewModel.updateProvider(provider.id)
                                    providerExpanded = false
                                },
                            )
                        }
                    }
                }

                // Model dropdown (cascading from provider)
                if (state.selectedProviderModels.isNotEmpty()) {
                    var modelExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded },
                    ) {
                        val modelName = state.selectedProviderModels.find { it.id == state.selectedModelId }?.name?.ifEmpty { state.selectedModelId } ?: state.selectedModelId
                        OutlinedTextField(
                            value = modelName.ifEmpty { "Select model" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false },
                        ) {
                            state.selectedProviderModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.name.ifEmpty { model.id }) },
                                    onClick = {
                                        component.viewModel.updateModel(model.id)
                                        modelExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (state.isConnectionSuccessful == true && state.modes.isNotEmpty()) {
                // Mode dropdown
                var modeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = !modeExpanded },
                ) {
                    OutlinedTextField(
                        value = state.selectedModeName.ifEmpty { "Select mode" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false },
                    ) {
                        state.modes.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(mode.name)
                                        mode.description?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    component.viewModel.updateMode(mode.name)
                                    modeExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // --- Save ---
            Button(
                onClick = { component.saveAndPersist() },
                enabled = !state.isTesting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaved) "Saved" else "Save Settings")
            }

            HorizontalDivider()

            // --- About ---
            Text(text = "About", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "OpenCode Mobile v1.0.0\nConnect to any OpenCode server instance to manage your AI coding sessions on the go.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}