package ai.opencode.mobile.repository

import ai.opencode.mobile.model.ServerConfig
import ai.opencode.mobile.OpenCodeApplication

actual class SettingsStorage actual constructor() {
    private val prefs = OpenCodeApplication.prefs

    actual fun load(): ServerConfig = ServerConfig(
        serverUrl = prefs.getString("server_url", "") ?: "",
        username = prefs.getString("username", "") ?: "",
        password = prefs.getString("password", "") ?: "",
        isConnected = prefs.getBoolean("is_connected", false),
    )

    actual fun save(config: ServerConfig) {
        prefs.edit()
            .putString("server_url", config.serverUrl)
            .putString("username", config.username)
            .putString("password", config.password)
            .putBoolean("is_connected", config.isConnected)
            .apply()
    }
}