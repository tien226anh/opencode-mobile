package ai.opencode.mobile

import ai.opencode.mobile.network.OpenCodeApiClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createOpenCodeApiClient(): OpenCodeApiClient {
    val engine = OkHttp.create()
    val httpClient = OpenCodeApiClient.createHttpClient(engine)
    return OpenCodeApiClient(httpClient = httpClient, baseUrl = "http://localhost:4096")
}