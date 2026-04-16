package ai.opencode.mobile.repository

import ai.opencode.mobile.model.ServerConfig
import platform.Foundation.NSUserDefaults

actual class SettingsStorage actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults()

    actual fun load(): ServerConfig {
        return ServerConfig(
            serverUrl = defaults.stringForKey("server_url") ?: "",
            username = defaults.stringForKey("username") ?: "",
            password = defaults.stringForKey("password") ?: "",
            isConnected = defaults.boolForKey("is_connected"),
        )
    }

    actual fun save(config: ServerConfig) {
        defaults.setObject(config.serverUrl, forKey = "server_url")
        defaults.setObject(config.username, forKey = "username")
        defaults.setObject(config.password, forKey = "password")
        defaults.setBool(config.isConnected, forKey = "is_connected")
    }
}