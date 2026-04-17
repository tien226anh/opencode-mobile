package ai.opencode.mobile.repository

import ai.opencode.mobile.model.ServerConfig
import ai.opencode.mobile.OpenCodeApplication

actual class SettingsStorage actual constructor() {
    private val prefs = OpenCodeApplication.prefs

    actual fun load(): ServerConfig {
        // Migrate from old single-url format: if server_url exists but server_host doesn't, parse it
        val oldUrl = prefs.getString("server_url", "") ?: ""
        val host = prefs.getString("server_host", "") ?: ""
        val port = prefs.getString("server_port", "") ?: ""

        if (host.isBlank() && oldUrl.isNotBlank()) {
            // Migration: parse old URL into host + port
            val parsedHost = oldUrl.substringBeforeLast(":").ifBlank { oldUrl }
            val parsedPort = oldUrl.substringAfterLast(":", "").ifBlank { "4096" }
            return ServerConfig(
                serverHost = parsedHost,
                serverPort = if (parsedPort.contains("/")) "4096" else parsedPort,
                username = prefs.getString("username", "") ?: "",
                password = prefs.getString("password", "") ?: "",
                isConnected = prefs.getBoolean("is_connected", false),
            )
        }

        return ServerConfig(
            serverHost = host.ifBlank { "http://localhost" },
            serverPort = port.ifBlank { "4096" },
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: "",
            isConnected = prefs.getBoolean("is_connected", false),
        )
    }

    actual fun save(config: ServerConfig) {
        prefs.edit()
            .putString("server_host", config.serverHost)
            .putString("server_port", config.serverPort)
            .putString("username", config.username)
            .putString("password", config.password)
            .putBoolean("is_connected", config.isConnected)
            .apply()
    }
}