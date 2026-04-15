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

    suspend fun healthCheck(): Result<HealthResponse> = runCatching {
        val response = httpClient.get("$baseUrl/global/health")
        response.body<HealthResponse>()
    }

    //region Session endpoints
    suspend fun listSessions(): Result<List<Session>> = runCatching {
        val response = httpClient.get("$baseUrl/session")
        val body = response.body<List<Session>>()
        body
    }

    suspend fun getSession(sessionId: String): Result<Session> = runCatching {
        val response = httpClient.get("$baseUrl/session/$sessionId")
        response.body<Session>()
    }

    suspend fun createSession(request: CreateSessionRequest = CreateSessionRequest()): Result<Session> = runCatching {
        val response = httpClient.post("$baseUrl/session") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        response.body<Session>()
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        httpClient.delete("$baseUrl/session/$sessionId")
    }

    suspend fun sendChatMessage(
        sessionId: String,
        request: ChatRequest,
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/message") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun listMessages(sessionId: String): Result<List<Message>> = runCatching {
        val response = httpClient.get("$baseUrl/session/$sessionId/message")
        response.body<MessageListResponse>().messages
    }

    suspend fun abortSession(sessionId: String): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/abort")
    }

    suspend fun revertMessage(
        sessionId: String,
        request: RevertRequest,
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/revert") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun unrevertSession(sessionId: String): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/unrevert")
    }

    suspend fun shareSession(sessionId: String): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/share")
    }

    suspend fun initSession(
        sessionId: String,
        modelId: String,
        providerId: String,
    ): Result<Unit> = runCatching {
        httpClient.post("$baseUrl/session/$sessionId/init") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("modelID" to modelId, "providerID" to providerId))
        }
    }
    //endregion

    //region Project endpoints
    suspend fun listProjects(): Result<List<Project>> = runCatching {
        val response = httpClient.get("$baseUrl/project")
        response.body<List<Project>>()
    }

    suspend fun getCurrentProject(): Result<Project> = runCatching {
        val response = httpClient.get("$baseUrl/project/current")
        response.body<Project>()
    }
    //endregion

    //region Config endpoints
    suspend fun getConfig(): Result<String> = runCatching {
        val response = httpClient.get("$baseUrl/config")
        response.bodyAsText()
    }
    //endregion

    companion object {
        fun createHttpClient(
            engine: HttpClientEngine,
            basicAuth: String = "",
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
                defaultRequest {
                    if (basicAuth.isNotEmpty()) {
                        header("Authorization", "Basic $basicAuth")
                    }
                }
            }
        }
    }
}