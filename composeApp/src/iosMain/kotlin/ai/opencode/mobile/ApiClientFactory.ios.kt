package ai.opencode.mobile

import ai.opencode.mobile.network.OpenCodeApiClient
import ai.opencode.mobile.repository.SettingsStorage
import io.ktor.client.engine.darwin.Darwin

actual fun createOpenCodeApiClient(settingsStorage: SettingsStorage): OpenCodeApiClient {
    val config = settingsStorage.load()
    val engine = Darwin.create()
    val httpClient = OpenCodeApiClient.createHttpClient(engine)
    val defaultUrl = config.serverUrl.ifBlank { OpenCodeApiClient.DEFAULT_URL }
    return OpenCodeApiClient(
        httpClient = httpClient,
        baseUrl = defaultUrl,
        basicAuth = if (config.username.isNotEmpty() || config.password.isNotEmpty()) {
            OpenCodeApiClient.encodeCredentials(config.username, config.password)
        } else {
            ""
        },
    )
}

actual fun createSettingsStorage(): SettingsStorage = SettingsStorage()