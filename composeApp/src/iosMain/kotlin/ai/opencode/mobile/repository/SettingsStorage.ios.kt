package ai.opencode.mobile.repository

import ai.opencode.mobile.model.ServerConfig
import platform.Foundation.NSUserDefaults

actual class SettingsStorage actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults()

    actual fun load(): ServerConfig {
        // Migrate from old single-url format: if server_url exists but server_host doesn't, parse it
        val oldUrl = defaults.stringForKey("server_url") ?: ""
        val host = defaults.stringForKey("server_host") ?: ""
        val port = defaults.stringForKey("server_port") ?: ""

        if (host.isBlank() && oldUrl.isNotBlank()) {
            // Migration: parse old URL into host + port
            val parsedHost = oldUrl.substringBeforeLast(":").ifBlank { oldUrl }
            val parsedPort = oldUrl.substringAfterLast(":", "").ifBlank { "4096" }
            return ServerConfig(
                serverHost = parsedHost,
                serverPort = if (parsedPort.contains("/")) "4096" else parsedPort,
                username = defaults.stringForKey("username") ?: "",
                password = defaults.stringForKey("password") ?: "",
                isConnected = defaults.boolForKey("is_connected"),
            )
        }

        return ServerConfig(
            serverHost = host.ifBlank { "http://localhost" },
            serverPort = port.ifBlank { "4096" },
            username = defaults.stringForKey("username") ?: "",
            password = defaults.stringForKey("password") ?: "",
            isConnected = defaults.boolForKey("is_connected"),
        )
    }

    actual fun save(config: ServerConfig) {
        defaults.setObject(config.serverHost, forKey = "server_host")
        defaults.setObject(config.serverPort, forKey = "server_port")
        defaults.setObject(config.username, forKey = "username")
        defaults.setObject(config.password, forKey = "password")
        defaults.setBool(config.isConnected, forKey = "is_connected")
    }
}