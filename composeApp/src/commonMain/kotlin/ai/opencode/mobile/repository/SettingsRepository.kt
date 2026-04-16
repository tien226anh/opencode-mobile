package ai.opencode.mobile.repository

import ai.opencode.mobile.model.ServerConfig

interface SettingsRepository {
    fun getServerConfig(): ServerConfig
    suspend fun saveServerUrl(url: String)
    suspend fun saveBasicAuth(auth: String)
    suspend fun testConnection(): Result<Boolean>
}