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
    val serverHost: String = "http://localhost",
    val serverPort: String = "${OpenCodeApiClient.DEFAULT_PORT}",
    val username: String = "",
    val password: String = "",
    val isTesting: Boolean = false,
    val isConnectionSuccessful: Boolean? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
) {
    /** Computed full URL from host + port */
    val serverUrl: String get() {
        val host = serverHost.trimEnd('/')
        return if (serverPort.isNotBlank()) "$host:$serverPort" else host
    }
}

class SettingsViewModel(
    private val apiClient: OpenCodeApiClient,
    private val initialConfig: ServerConfig = ServerConfig(),
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(SettingsState(
        serverHost = initialConfig.serverHost.ifBlank { "http://localhost" },
        serverPort = initialConfig.serverPort.ifBlank { "${OpenCodeApiClient.DEFAULT_PORT}" },
        username = initialConfig.username,
        password = initialConfig.password,
    ))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun updateServerHost(host: String) {
        _state.value = _state.value.copy(serverHost = host, isConnectionSuccessful = null, isSaved = false)
    }

    fun updateServerPort(port: String) {
        _state.value = _state.value.copy(serverPort = port, isConnectionSuccessful = null, isSaved = false)
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
            apiClient.updateConfig(_state.value.serverUrl, _state.value.username, _state.value.password)
            val result = apiClient.getAppInfo()
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
            serverHost = _state.value.serverHost,
            serverPort = _state.value.serverPort,
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