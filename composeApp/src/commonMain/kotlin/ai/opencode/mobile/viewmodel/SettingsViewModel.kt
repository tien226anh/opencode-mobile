package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.Mode
import ai.opencode.mobile.model.ModelInfo
import ai.opencode.mobile.model.Provider
import ai.opencode.mobile.model.ServerConfig
import ai.opencode.mobile.network.OpenCodeApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val serverUrl: String = OpenCodeApiClient.DEFAULT_URL,
    val username: String = "",
    val password: String = "",
    val isTesting: Boolean = false,
    val isConnectionSuccessful: Boolean? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    val providers: List<Provider> = emptyList(),
    val modes: List<Mode> = emptyList(),
    val selectedProviderId: String = "",
    val selectedModelId: String = "",
    val selectedModeName: String = "",
) {
    val selectedProvider: Provider? get() = providers.find { it.id == selectedProviderId }
    val selectedProviderModels: List<ModelInfo> get() =
        selectedProvider?.models?.values?.toList() ?: emptyList()
}

class SettingsViewModel(
    private val apiClient: OpenCodeApiClient,
    private val initialConfig: ServerConfig = ServerConfig(),
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(SettingsState(
        serverUrl = initialConfig.serverUrl.ifBlank { OpenCodeApiClient.DEFAULT_URL },
        username = initialConfig.username,
        password = initialConfig.password,
        selectedProviderId = initialConfig.providerId,
        selectedModelId = initialConfig.modelId,
        selectedModeName = initialConfig.modeName,
    ))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun updateServerUrl(url: String) {
        _state.value = _state.value.copy(serverUrl = url, isConnectionSuccessful = null, isSaved = false)
    }

    fun updateUsername(username: String) {
        _state.value = _state.value.copy(username = username, isConnectionSuccessful = null, isSaved = false)
    }

    fun updatePassword(password: String) {
        _state.value = _state.value.copy(password = password, isConnectionSuccessful = null, isSaved = false)
    }

    fun updateProvider(providerId: String) {
        _state.value = _state.value.copy(
            selectedProviderId = providerId,
            selectedModelId = "",
            isSaved = false,
        )
    }

    fun updateModel(modelId: String) {
        _state.value = _state.value.copy(selectedModelId = modelId, isSaved = false)
    }

    fun updateMode(modeName: String) {
        _state.value = _state.value.copy(selectedModeName = modeName, isSaved = false)
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTesting = true, isConnectionSuccessful = null, error = null)
            apiClient.updateConfig(_state.value.serverUrl, _state.value.username, _state.value.password)
            val result = apiClient.getAppInfo()
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        isTesting = false,
                        isConnectionSuccessful = true,
                    )
                    // Auto-fetch providers and modes on successful connection
                    loadProviders()
                    loadModes()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isTesting = false,
                        isConnectionSuccessful = false,
                        error = error.message ?: "Connection failed",
                    )
                },
            )
        }
    }

    private fun loadProviders() {
        viewModelScope.launch {
            apiClient.getProviders().fold(
                onSuccess = { response ->
                    val currentState = _state.value
                    // Auto-select first provider and its first model if none selected
                    val autoProviderId = currentState.selectedProviderId.ifBlank {
                        response.providers.firstOrNull()?.id ?: ""
                    }
                    val autoModelId = currentState.selectedModelId.ifBlank {
                        response.default[autoProviderId] ?: ""
                    }
                    _state.value = currentState.copy(
                        providers = response.providers,
                        selectedProviderId = autoProviderId,
                        selectedModelId = autoModelId,
                    )
                },
                onFailure = { /* Silently fail - providers are optional */ },
            )
        }
    }

    private fun loadModes() {
        viewModelScope.launch {
            apiClient.getModes().fold(
                onSuccess = { modes ->
                    val currentMode = _state.value.selectedModeName
                    _state.value = _state.value.copy(
                        modes = modes,
                        selectedModeName = currentMode.ifBlank { modes.firstOrNull()?.name ?: "" },
                    )
                },
                onFailure = { /* Silently fail - modes are optional */ },
            )
        }
    }

    fun saveSettings(): ServerConfig {
        val config = ServerConfig(
            serverUrl = _state.value.serverUrl,
            username = _state.value.username,
            password = _state.value.password,
            isConnected = _state.value.isConnectionSuccessful == true,
            providerId = _state.value.selectedProviderId,
            modelId = _state.value.selectedModelId,
            modeName = _state.value.selectedModeName,
        )
        apiClient.updateConfig(config.serverUrl, config.username, config.password)
        _state.value = _state.value.copy(isSaved = true)
        return config
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}