package ai.opencode.mobile.repository

import ai.opencode.mobile.model.ServerConfig

interface SettingsRepository {
    fun getServerConfig(): ServerConfig
    suspend fun saveServerConfig(config: ServerConfig)
    suspend fun testConnection(config: ServerConfig): Result<Boolean>
}

expect class SettingsStorage() {
    fun load(): ServerConfig
    fun save(config: ServerConfig)
}