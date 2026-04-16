package ai.opencode.mobile.viewmodel

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
    val serverUrl: String = "http://localhost:4096",
    val username: String = "",
    val password: String = "",
    val isTesting: Boolean = false,
    val isConnectionSuccessful: Boolean? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
)

class SettingsViewModel(
    private val apiClient: OpenCodeApiClient,
    private val initialConfig: ServerConfig = ServerConfig(),
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(SettingsState(
        serverUrl = initialConfig.serverUrl.ifBlank { "http://localhost:4096" },
        username = initialConfig.username,
        password = initialConfig.password,
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

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTesting = true, isConnectionSuccessful = null, error = null)
            val config = ServerConfig(
                serverUrl = _state.value.serverUrl,
                username = _state.value.username,
                password = _state.value.password,
            )
            apiClient.updateConfig(config.serverUrl, config.username, config.password)
            val result = apiClient.healthCheck()
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        isTesting = false,
                        isConnectionSuccessful = true,
                    )
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

    fun saveSettings(): ServerConfig {
        val config = ServerConfig(
            serverUrl = _state.value.serverUrl,
            username = _state.value.username,
            password = _state.value.password,
            isConnected = _state.value.isConnectionSuccessful == true,
        )
        apiClient.updateConfig(config.serverUrl, config.username, config.password)
        _state.value = _state.value.copy(isSaved = true)
        return config
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}