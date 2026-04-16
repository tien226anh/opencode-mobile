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
import kotlinx.serialization.json.Json

class OpenCodeApiClient(
    private val httpClient: HttpClient,
    private var baseUrl: String,
    private var basicAuth: String = "",
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun updateConfig(baseUrl: String, username: String = "", password: String = "") {
        this.baseUrl = baseUrl
        this.basicAuth = if (username.isNotEmpty() || password.isNotEmpty()) {
            encodeCredentials(username, password)
        } else {
            ""
        }
    }

    suspend fun healthCheck(): Result<HealthResponse> = runCatching {
        val response = httpClient.get("$baseUrl/global/health") {
            addAuthHeader(this)
        }
        response.body<HealthResponse>()
    }

    //region Session endpoints
    suspend fun listSessions(): Result<List<Session>> = runCatching {
        val response = httpClient.get("$baseUrl/session") {
            addAuthHeader(this)
        }
        val body = response.body<List<Session>>()
        body
    }

    suspend fun getSession(sessionId: String): Result<Session> = runCatching {
        val response = httpClient.get("$baseUrl/session/$sessionId") {
            addAuthHeader(this)
        }
        response.body<Session>()
    }

    suspend fun createSession(request: CreateSessionRequest = CreateSessionRequest()): Result<Session> = runCatching {
        val response = httpClient.post("$baseUrl/session") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        response.body<Session>()
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        httpClient.delete("$baseUrl/session/$sessionId") {
            addAuthHeader(this)
        }
    }

    suspend fun sendChatMessage(
        sessionId: String,
        request: ChatRequest,
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/message") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun listMessages(sessionId: String): Result<List<Message>> = runCatching {
        val response = httpClient.get("$baseUrl/session/$sessionId/message") {
            addAuthHeader(this)
        }
        response.body<MessageListResponse>().messages
    }

    suspend fun abortSession(sessionId: String): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/abort") {
            addAuthHeader(this)
        }
    }

    suspend fun revertMessage(
        sessionId: String,
        request: RevertRequest,
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/revert") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun unrevertSession(sessionId: String): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/unrevert") {
            addAuthHeader(this)
        }
    }

    suspend fun shareSession(sessionId: String): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/share") {
            addAuthHeader(this)
        }
    }

    suspend fun initSession(
        sessionId: String,
        modelId: String,
        providerId: String,
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/init") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(mapOf("modelID" to modelId, "providerID" to providerId))
        }
    }
    //endregion

    //region Project endpoints
    suspend fun listProjects(): Result<List<Project>> = runCatching {
        val response = httpClient.get("$baseUrl/project") {
            addAuthHeader(this)
        }
        response.body<List<Project>>()
    }

    suspend fun getCurrentProject(): Result<Project> = runCatching {
        val response = httpClient.get("$baseUrl/project/current") {
            addAuthHeader(this)
        }
        response.body<Project>()
    }
    //endregion

    //region Config endpoints
    suspend fun getConfig(): Result<String> = runCatching {
        val response = httpClient.get("$baseUrl/config") {
            addAuthHeader(this)
        }
        response.bodyAsText()
    }
    //endregion

    private fun addAuthHeader(builder: HttpRequestBuilder) {
        if (basicAuth.isNotEmpty()) {
            builder.header("Authorization", "Basic $basicAuth")
        }
    }

    companion object {
        fun encodeCredentials(username: String, password: String): String {
            val credentials = "$username:$password"
            return encodeBase64(credentials)
        }

        fun createHttpClient(
            engine: HttpClientEngine,
        ): HttpClient {
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

        private fun encodeBase64(input: String): String {
            // KMP-compatible Base64 encoding
            val bytes = input.toByteArray(Charsets.UTF_8)
            val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            val result = StringBuilder()
            var i = 0
            while (i < bytes.size) {
                val b0 = bytes[i].toInt() and 0xFF
                val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
                val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
                val padCount = if (i + 1 >= bytes.size) 2 else if (i + 2 >= bytes.size) 1 else 0
                val triple = (b0 shl 16) or (b1 shl 8) or b2
                result.append(table[(triple shr 18) and 0x3F])
                result.append(table[(triple shr 12) and 0x3F])
                result.append(if (padCount >= 2) '=' else table[(triple shr 6) and 0x3F])
                result.append(if (padCount >= 1) '=' else table[triple and 0x3F])
                i += 3
            }
            return result.toString()
        }
    }
}