package ai.opencode.mobile.network

import ai.opencode.mobile.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json

class OpenCodeApiClient(
    private val httpClient: HttpClient,
    private var baseUrl: String,
    private var basicAuth: String = "",
) {
    fun updateConfig(baseUrl: String, username: String = "", password: String = "") {
        this.baseUrl = baseUrl
        this.basicAuth = if (username.isNotEmpty() || password.isNotEmpty()) {
            encodeCredentials(username, password)
        } else {
            ""
        }
    }

    private suspend fun validateJsonResponse(response: HttpResponse): HttpResponse {
        val contentType = response.contentType()
        if (contentType == null) {
            val body = response.bodyAsText()
            throw IllegalStateException(
                "Server returned an unexpected response (no Content-Type).\n\n" +
                "This usually means:\n" +
                "• The server URL is incorrect\n" +
                "• A proxy/tunnel is serving an HTML page instead of the API\n" +
                "• Authentication is required but not configured\n\n" +
                "Response preview: ${body.take(200)}"
            )
        }
        if (!contentType.match(ContentType.Application.Json) && !contentType.match(ContentType.Text.Plain)) {
            val body = response.bodyAsText()
            if (contentType.match(ContentType.Text.Html)) {
                throw IllegalStateException(
                    "Server returned HTML instead of JSON.\n\n" +
                    "This usually means:\n" +
                    "• The server URL points to a web page, not the API\n" +
                    "• A Cloudflare tunnel is serving an interstitial page\n" +
                    "• The OpenCode server is not running at this URL\n\n" +
                    "Make sure you're using the correct URL and the server is running."
                )
            }
            throw IllegalStateException(
                "Server returned ${contentType} instead of JSON.\n\n" +
                "Expected: application/json\nGot: ${contentType}\n\n" +
                "Response preview: ${body.take(200)}"
            )
        }
        return response
    }

    //region App endpoints (matching official SDK)
    suspend fun getAppInfo(): Result<AppInfo> = runCatching {
        val response = httpClient.get("$baseUrl/app") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<AppInfo>()
    }
    //endregion

    //region Session endpoints (matching official SDK)
    suspend fun listSessions(): Result<List<Session>> = runCatching {
        val response = httpClient.get("$baseUrl/session") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<List<Session>>()
    }

    suspend fun createSession(): Result<Session> = runCatching {
        val response = httpClient.post("$baseUrl/session") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
        }
        validateJsonResponse(response)
        response.body<Session>()
    }

    suspend fun deleteSession(sessionId: String): Result<Boolean> = runCatching {
        val response = httpClient.delete("$baseUrl/session/$sessionId") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<Boolean>()
    }

    suspend fun sendChatMessage(sessionId: String, request: ChatRequest): Result<MessageInfo> = runCatching {
        val response = httpClient.post("$baseUrl/session/$sessionId/message") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        validateJsonResponse(response)
        response.body<MessageInfo>()
    }

    suspend fun listMessages(sessionId: String): Result<List<MessageResponseItem>> = runCatching {
        val response = httpClient.get("$baseUrl/session/$sessionId/message") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<List<MessageResponseItem>>()
    }

    suspend fun abortSession(sessionId: String): Result<Boolean> = runCatching {
        val response = httpClient.post("$baseUrl/session/$sessionId/abort") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<Boolean>()
    }

    suspend fun revertMessage(sessionId: String, request: SessionRevertRequest): Result<Session> = runCatching {
        val response = httpClient.post("$baseUrl/session/$sessionId/revert") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        validateJsonResponse(response)
        response.body<Session>()
    }

    suspend fun unrevertSession(sessionId: String): Result<Session> = runCatching {
        val response = httpClient.post("$baseUrl/session/$sessionId/unrevert") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<Session>()
    }

    suspend fun shareSession(sessionId: String): Result<Session> = runCatching {
        val response = httpClient.post("$baseUrl/session/$sessionId/share") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<Session>()
    }

    suspend fun unshareSession(sessionId: String): Result<Session> = runCatching {
        val response = httpClient.delete("$baseUrl/session/$sessionId/share") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<Session>()
    }

    suspend fun summarizeSession(sessionId: String, request: SessionSummarizeRequest): Result<Boolean> = runCatching {
        val response = httpClient.post("$baseUrl/session/$sessionId/summarize") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        validateJsonResponse(response)
        response.body<Boolean>()
    }
    //endregion

    //region Config/Provider endpoints
    suspend fun getConfig(): Result<String> = runCatching {
        val response = httpClient.get("$baseUrl/config") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.bodyAsText()
    }

    suspend fun getProviders(): Result<ProvidersResponse> = runCatching {
        val response = httpClient.get("$baseUrl/config/providers") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<ProvidersResponse>()
    }

    suspend fun getModes(): Result<List<Mode>> = runCatching {
        val response = httpClient.get("$baseUrl/mode") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<List<Mode>>()
    }
    //endregion

    private fun addAuthHeader(builder: HttpRequestBuilder) {
        if (basicAuth.isNotEmpty()) {
            builder.header("Authorization", "Basic $basicAuth")
        }
    }

    companion object {
        const val DEFAULT_PORT = 54321
        const val DEFAULT_URL = "http://localhost:$DEFAULT_PORT"

        @OptIn(ExperimentalEncodingApi::class)
        fun encodeCredentials(username: String, password: String): String {
            val credentials = "$username:$password"
            return Base64.encode(credentials.encodeToByteArray())
        }

        fun createHttpClient(engine: HttpClientEngine): HttpClient {
            return HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 10_000
                }
            }
        }
    }
}