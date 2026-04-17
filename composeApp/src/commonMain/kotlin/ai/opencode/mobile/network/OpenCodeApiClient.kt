package ai.opencode.mobile.network

import ai.opencode.mobile.model.*
import ai.opencode.mobile.platform.normalizeServerUrl
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class OpenCodeApiClient(
    private val httpClient: HttpClient,
    private var baseUrl: String,
    private var basicAuth: String = "",
) {
    /** The HTTP engine used by this client, exposed so SSEClient can share it. */
    val engine: HttpClientEngine get() = httpClient.engine
    fun updateConfig(baseUrl: String, username: String = "", password: String = "") {
        // Normalize: strip trailing slashes, remap localhost for Android emulator
        this.baseUrl = normalizeServerUrl(baseUrl.trimEnd('/'))
        this.basicAuth = if (username.isNotEmpty() || password.isNotEmpty()) {
            encodeCredentials(username, password)
        } else {
            ""
        }
    }

    private suspend fun validateJsonResponse(response: HttpResponse): HttpResponse {
        val contentType = response.contentType()
        val requestUrl = response.request.url.toString()

        // If the response is not 2xx, read the body and throw a meaningful error
        // regardless of content-type — the server may return errors with
        // non-JSON content-types like application/octet-stream.
        if (!response.status.isSuccess()) {
            val body = try { response.bodyAsText() } catch (_: Exception) { "" }
            val errorMessage = parseServerErrorBody(body)
            throw IllegalStateException(
                "Server error (${response.status.value}): $errorMessage\n\nURL: ${requestUrl.take(100)}"
            )
        }

        if (contentType == null) {
            val body = response.bodyAsText()
            throw IllegalStateException(
                "Server returned an unexpected response (no Content-Type).\n\n" +
                "URL tried: ${requestUrl.take(100)}\n\n" +
                "This usually means:\n" +
                "• The server URL is incorrect — make sure to include the full URL\n" +
                "  (e.g. https://xxx.trycloudflare.com, not just the hostname)\n" +
                "• A proxy/tunnel is serving an HTML page instead of the API\n" +
                "• Authentication is required but not configured\n\n" +
                "Response preview: ${body.take(200)}"
            )
        }
        // Accept JSON, plain text, and also octet-stream (some server versions
        // return error bodies with application/octet-stream but the body is JSON).
        val isJsonLikeContentType = contentType.match(ContentType.Application.Json) ||
            contentType.match(ContentType.Text.Plain) ||
            contentType.match(ContentType.Application.OctetStream)

        if (!isJsonLikeContentType) {
            val body = response.bodyAsText()
            if (contentType.match(ContentType.Text.Html)) {
                val hint = when {
                    requestUrl.startsWith("http://") && !requestUrl.contains("localhost") ->
                        "\n\nHint: If using Cloudflare tunnel, use https:// not http://\n" +
                        "  Example: https://xxx-xxx.trycloudflare.com"
                    !requestUrl.contains(".") ->
                        "\n\nHint: The URL looks incomplete. Make sure to enter the full URL\n" +
                        "  Example: https://your-tunnel.trycloudflare.com"
                    else -> ""
                }
                throw IllegalStateException(
                    "Server returned HTML instead of JSON.\n\n" +
                    "URL tried: ${requestUrl.take(100)}\n\n" +
                    "This usually means:\n" +
                    "• The server URL points to the web console, not the API\n" +
                    "• A Cloudflare tunnel is serving an interstitial page\n" +
                    "• The OpenCode server is not running at this URL\n" +
                    "• Authentication failed — check your username and password$hint\n\n" +
                    "Make sure the OpenCode server is running and the URL is correct."
                )
            }
            throw IllegalStateException(
                "Server returned ${contentType} instead of JSON.\n\n" +
                "URL tried: ${requestUrl.take(100)}\n\n" +
                "Expected: application/json\nGot: ${contentType}\n\n" +
                "Response preview: ${body.take(200)}"
            )
        }
        return response
    }

    /**
     * Attempt to extract a human-readable error message from a server error response body.
     * Handles various OpenCode server error formats:
     *   - {"error": "message"}
     *   - {"error": {"message": "...", "name": "..."}}
     *   - {"error": [{"code": "...", "message": "..."}]}
     *   - {"message": "..."}
     *   - Plain text
     */
    private fun parseServerErrorBody(body: String): String {
        if (body.isBlank()) return "Unknown error (empty response)"
        return try {
            val element = Json.parseToJsonElement(body)
            when (element) {
                is kotlinx.serialization.json.JsonObject -> {
                    val errorField = element["error"]
                    when (errorField) {
                        is kotlinx.serialization.json.JsonPrimitive -> errorField.content
                        is kotlinx.serialization.json.JsonObject -> {
                            // e.g. {"error": {"message": "...", "name": "..."}}
                            errorField["message"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                ?: errorField["name"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                ?: errorField.toString()
                        }
                        is kotlinx.serialization.json.JsonArray -> {
                            // e.g. {"error": [{"code": "invalid_union", "message": "..."}]}
                            errorField.firstOrNull()?.let { first ->
                                when (first) {
                                    is kotlinx.serialization.json.JsonObject -> {
                                        val msg = (first["message"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                        val code = (first["code"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                        if (msg != null && code != null) "$code: $msg" else msg ?: first.toString()
                                    }
                                    else -> first.toString()
                                }
                            } ?: errorField.toString()
                        }
                        else -> errorField?.toString() ?: body.take(200)
                    }
                        ?: (element["message"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                        ?: body.take(200)
                }
                else -> body.take(200)
            }
        } catch (_: Exception) {
            body.take(200)
        }
    }

    //region App endpoints (matching official SDK, with fallbacks for newer server versions)
    suspend fun getAppInfo(): Result<AppInfo> = runCatching {
        // Try new endpoint first (OpenCode v1.4+ uses /global/health)
        val healthResponse = httpClient.get("$baseUrl/global/health") { addAuthHeader(this) }
        
        if (healthResponse.status.value == 401) {
            throw IllegalStateException(
                "Authentication failed (401 Unauthorized).\n\n" +
                "The server requires authentication. Please check:\n" +
                "• Username: must match OPENCODE_SERVER_USERNAME (default: \"opencode\")\n" +
                "• Password: must match OPENCODE_SERVER_PASSWORD set on the server\n\n" +
                "If you don't know the credentials, contact the server administrator."
            )
        }

        val healthContentType = healthResponse.contentType()
        if (healthContentType != null &&
            (healthContentType.match(ContentType.Application.Json) || healthContentType.match(ContentType.Text.Plain))) {
            // New server: /global/health returns { healthy: true, version: "..." }
            // Return a minimal AppInfo — the important thing is the connection works
            return Result.success(AppInfo())
        }

        // Fallback: try old endpoint /app (OpenCode v1.3 and earlier)
        var response = httpClient.get("$baseUrl/app") { addAuthHeader(this) }
        
        // Check for 401 Unauthorized specifically - means auth is required but failed
        if (response.status.value == 401) {
            throw IllegalStateException(
                "Authentication failed (401 Unauthorized).\n\n" +
                "The server requires authentication. Please check:\n" +
                "• Username: must match OPENCODE_SERVER_USERNAME (default: \"opencode\")\n" +
                "• Password: must match OPENCODE_SERVER_PASSWORD set on the server\n\n" +
                "If you don't know the credentials, contact the server administrator."
            )
        }
        
        // If /app returns HTML (web console), try /session as fallback health check
        val contentType = response.contentType()
        if (contentType != null && contentType.match(ContentType.Text.Html)) {
            // The server has a web UI that's catching /app before the API
            // Try listing sessions as a fallback health check
            val sessionResponse = httpClient.get("$baseUrl/session") { addAuthHeader(this) }
            if (sessionResponse.status.value == 401) {
                throw IllegalStateException(
                    "Authentication failed (401 Unauthorized).\n\n" +
                    "The server requires authentication. Please check:\n" +
                    "• Username: must match OPENCODE_SERVER_USERNAME (default: \"opencode\")\n" +
                    "• Password: must match OPENCODE_SERVER_PASSWORD set on the server\n\n" +
                    "If you don't know the credentials, contact the server administrator."
                )
            }
            val sessionContentType = sessionResponse.contentType()
            if (sessionContentType != null && 
                (sessionContentType.match(ContentType.Application.Json) || sessionContentType.match(ContentType.Text.Plain))) {
                // Server is reachable and returns JSON — but /app is not an API endpoint
                // This is likely an older version or custom deployment
                return Result.success(AppInfo())
            }
            // Both /app and /session return HTML — the URL might be wrong
            throw IllegalStateException(
                "Server returned HTML instead of JSON for both /app and /session.\n\n" +
                "URL tried: $baseUrl\n\n" +
                "This usually means:\n" +
                "• The URL points to the web console, not the API server\n" +
                "  (OpenCode API and web console share the same URL - this should work)\n" +
                "• A Cloudflare tunnel is serving an interstitial page\n" +
                "• The OpenCode server is not running at this URL\n" +
                "• Authentication failed — check your username and password\n\n" +
                "To verify, open this URL in a browser:\n" +
                "$baseUrl/session\n\n" +
                "If you see JSON, the server is working. If you see HTML, check the URL."
            )
        }
        
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

    suspend fun respondPermission(sessionId: String, permissionId: String, allow: Boolean): Result<Boolean> = runCatching {
        val response = httpClient.post("$baseUrl/session/$sessionId/permissions/$permissionId") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(PermissionResponse(response = if (allow) "allow" else "deny"))
        }
        validateJsonResponse(response)
        response.body<Boolean>()
    }

    suspend fun getSessionDiff(sessionId: String): Result<SessionDiffResponse> = runCatching {
        val response = httpClient.get("$baseUrl/session/$sessionId/diff") { addAuthHeader(this) }
        // The server might return an array directly or wrapped in an object
        validateJsonResponse(response)
        val responseBody = response.bodyAsText()
        val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(responseBody)
        // If it's an array, wrap it in an object
        if (jsonElement is kotlinx.serialization.json.JsonArray) {
            SessionDiffResponse(files = Json { ignoreUnknownKeys = true }.decodeFromJsonElement(jsonElement))
        } else {
            Json { ignoreUnknownKeys = true }.decodeFromString<SessionDiffResponse>(responseBody)
        }
    }

    suspend fun forkSession(sessionId: String, messageId: String? = null): Result<Session> = runCatching {
        val requestBody = if (messageId != null) {
            buildJsonObject { put("messageID", JsonPrimitive(messageId)) }
        } else {
            buildJsonObject {}
        }
        val response = httpClient.post("$baseUrl/session/$sessionId/fork") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        validateJsonResponse(response)
        response.body<Session>()
    }

    suspend fun getSessionChildren(sessionId: String): Result<List<Session>> = runCatching {
        val response = httpClient.get("$baseUrl/session/$sessionId/children") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<List<Session>>()
    }

    suspend fun getTodoList(sessionId: String): Result<List<Todo>> = runCatching {
        val response = httpClient.get("$baseUrl/session/$sessionId/todo") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<List<Todo>>()
    }

    suspend fun listCommands(): Result<List<SlashCommand>> = runCatching {
        val response = httpClient.get("$baseUrl/command") { addAuthHeader(this) }
        validateJsonResponse(response)
        response.body<List<SlashCommand>>()
    }

    suspend fun executeCommand(
        sessionId: String,
        command: String,
        arguments: String = "",
        messageId: String? = null,
        agent: String? = null,
        model: String? = null,
    ): Result<Boolean> = runCatching {
        val requestBody = buildJsonObject {
            put("command", JsonPrimitive(command))
            put("arguments", JsonPrimitive(arguments))
            messageId?.let { put("messageID", JsonPrimitive(it)) }
            agent?.let { put("agent", JsonPrimitive(it)) }
            model?.let { put("model", JsonPrimitive(it)) }
        }
        val response = httpClient.post("$baseUrl/session/$sessionId/command") {
            addAuthHeader(this)
            contentType(ContentType.Application.Json)
            setBody(requestBody)
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

    /**
     * Get available modes/agents.
     * In newer OpenCode versions, /mode was renamed to /agent.
     * Try /agent first, fall back to /mode for older versions.
     */
    suspend fun getModes(): Result<List<Mode>> = runCatching {
        // Try new endpoint first (OpenCode v1.4+)
        val agentResponse = httpClient.get("$baseUrl/agent") { addAuthHeader(this) }
        val agentContentType = agentResponse.contentType()

        if (agentContentType != null &&
            (agentContentType.match(ContentType.Application.Json) || agentContentType.match(ContentType.Text.Plain))) {
            return Result.success(agentResponse.body<List<Mode>>())
        }

        // Fallback to old endpoint (OpenCode v1.3 and earlier)
        val modeResponse = httpClient.get("$baseUrl/mode") { addAuthHeader(this) }
        validateJsonResponse(modeResponse)
        modeResponse.body<List<Mode>>()
    }
    //endregion

    private fun addAuthHeader(builder: HttpRequestBuilder) {
        if (basicAuth.isNotEmpty()) {
            builder.header("Authorization", "Basic $basicAuth")
        }
    }

    companion object {
        const val DEFAULT_PORT = 4096
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