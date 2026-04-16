package ai.opencode.mobile

import ai.opencode.mobile.network.OpenCodeApiClient
import ai.opencode.mobile.repository.SettingsStorage
import io.ktor.client.engine.okhttp.OkHttp

actual fun createOpenCodeApiClient(settingsStorage: SettingsStorage): OpenCodeApiClient {
    val config = settingsStorage.load()
    val engine = OkHttp.create()
    val httpClient = OpenCodeApiClient.createHttpClient(engine)
    val defaultUrl = config.serverUrl.ifBlank { "http://localhost:4096" }
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