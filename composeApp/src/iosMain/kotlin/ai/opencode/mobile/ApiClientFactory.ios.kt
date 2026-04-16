package ai.opencode.mobile

import ai.opencode.mobile.network.OpenCodeApiClient
import io.ktor.client.engine.darwin.Darwin

actual fun createOpenCodeApiClient(): OpenCodeApiClient {
    val engine = Darwin.create()
    val httpClient = OpenCodeApiClient.createHttpClient(engine)
    return OpenCodeApiClient(httpClient = httpClient, baseUrl = "http://localhost:4096")
}