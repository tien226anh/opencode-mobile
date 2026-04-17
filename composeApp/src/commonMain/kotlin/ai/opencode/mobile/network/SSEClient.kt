package ai.opencode.mobile.network

import ai.opencode.mobile.model.*
import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.header
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Client for the OpenCode Server-Sent Events endpoint (GET /event).
 *
 * Uses Ktor's built-in SSE plugin (`client.sse()`) for proper real-time
 * streaming. The previous `httpClient.get()` + `bodyAsText()` approach
 * buffered the entire response before parsing, defeating SSE streaming.
 */
class SSEClient(
    private val httpClient: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false }

    /**
     * Connect to the SSE event stream and emit events as they arrive.
     * The flow remains active until the connection is closed or an error occurs.
     * On error, the flow emits a [SSEEvent.Error] and then completes.
     */
    fun connect(
        baseUrl: String,
        basicAuth: String = "",
    ): Flow<SSEEvent> = flow {
        // Create a dedicated SSE client with extended/no timeout for long-lived connections.
        // HttpClient.config {} creates a new client sharing the same engine but with
        // different plugin configuration — the Ktor way to customize per-request.
        val sseClient = httpClient.config {
            install(SSE)
            install(HttpTimeout) {
                requestTimeoutMillis = 0 // No request timeout for SSE (long-lived connection)
                connectTimeoutMillis = 10_000
            }
        }

        sseClient.sse(
            urlString = "$baseUrl/event",
            request = {
                if (basicAuth.isNotEmpty()) {
                    header("Authorization", "Basic $basicAuth")
                }
            },
        ) {
            // `incoming` is a SharedFlow<ServerSentEvent> that emits events in real-time.
            // ServerSentEvent has: event: String?, data: String?, id: String?, retry: Int?
            incoming.collect { sseEvent ->
                val eventType = sseEvent.event ?: ""
                val eventData = sseEvent.data ?: ""
                if (eventData.isNotBlank()) {
                    val parsed = parseEvent(eventType, eventData)
                    emit(parsed)
                }
            }
        }
    }.catch { e ->
        // CancellationException must be re-thrown per Kotlin coroutines convention.
        // This happens when the user navigates away or calls stopStreaming().
        if (e is CancellationException) throw e
        emit(SSEEvent.Error("SSE connection error: ${e.message ?: "Unknown error"}"))
    }

    internal fun parseEvent(type: String, data: String): SSEEvent {
        if (data.isBlank()) return SSEEvent.Unknown(type)

        return try {
            val jsonElement = json.parseToJsonElement(data)
            val jsonObj = jsonElement as? JsonObject ?: return SSEEvent.Unknown(type)

            // Extract the nested properties object
            val properties = jsonObj["properties"] as? JsonObject ?: jsonObj
            val eventType = jsonObj["type"]?.jsonPrimitive?.content ?: type

            when (eventType) {
                "message.updated" -> {
                    val messageInfo = json.decodeFromJsonElement<MessageInfo>(properties["info"] ?: jsonObj)
                    SSEEvent.MessageUpdated(messageInfo)
                }
                "message.part.updated" -> {
                    val part = json.decodeFromJsonElement<Part>(properties["part"] ?: jsonObj)
                    SSEEvent.MessagePartUpdated(part)
                }
                "message.part.removed" -> {
                    val messageId = properties["messageID"]?.jsonPrimitive?.content ?: ""
                    val partId = properties["partID"]?.jsonPrimitive?.content ?: ""
                    SSEEvent.MessagePartRemoved(messageId, partId)
                }
                "message.removed" -> {
                    val messageId = properties["messageID"]?.jsonPrimitive?.content ?: ""
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: ""
                    SSEEvent.MessageRemoved(messageId, sessionId)
                }
                "session.updated" -> {
                    val session = json.decodeFromJsonElement<Session>(properties["info"] ?: jsonObj)
                    SSEEvent.SessionUpdated(session)
                }
                "session.deleted" -> {
                    val session = json.decodeFromJsonElement<Session>(properties["info"] ?: jsonObj)
                    SSEEvent.SessionDeleted(session)
                }
                "session.idle" -> {
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: ""
                    SSEEvent.SessionIdle(sessionId)
                }
                "session.error" -> {
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: ""
                    val errorName = (properties["error"] as? JsonObject)
                        ?.get("name")?.jsonPrimitive?.content ?: "UnknownError"
                    SSEEvent.SessionError(sessionId, errorName)
                }
                "file.edited" -> {
                    val file = properties["file"]?.jsonPrimitive?.content ?: ""
                    SSEEvent.FileEdited(file)
                }
                "permission.updated" -> {
                    val permission = json.decodeFromJsonElement<Permission>(properties)
                    SSEEvent.PermissionUpdated(permission)
                }
                "session.status" -> {
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: ""
                    val statusObj = properties["status"] as? JsonObject
                    val statusType = statusObj?.get("type")?.jsonPrimitive?.content ?: "idle"
                    val sessionStatus = when (statusType) {
                        "idle" -> SessionStatus.Idle(sessionId)
                        "busy" -> SessionStatus.Busy(sessionId)
                        "retry" -> {
                            val attempt = statusObj?.get("attempt")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                            val message = statusObj?.get("message")?.jsonPrimitive?.content ?: ""
                            val next = statusObj?.get("next")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                            SessionStatus.Retry(sessionId = sessionId, attempt = attempt, message = message, next = next)
                        }
                        else -> SessionStatus.Unknown(statusType)
                    }
                    SSEEvent.SessionStatusUpdated(sessionStatus)
                }
                "todo.updated" -> {
                    val todosElement = properties["todos"]
                    val todos = if (todosElement != null) {
                        json.decodeFromJsonElement<List<Todo>>(todosElement)
                    } else {
                        emptyList()
                    }
                    SSEEvent.TodoUpdated(todos)
                }
                "session.compacted" -> {
                    val sessionId = properties["sessionID"]?.jsonPrimitive?.content ?: ""
                    SSEEvent.SessionCompacted(sessionId)
                }
                else -> SSEEvent.Unknown(eventType)
            }
        } catch (e: Exception) {
            SSEEvent.Unknown(type)
        }
    }
}

/**
 * Sealed class representing SSE events from the OpenCode server.
 */
sealed class SSEEvent {
    data class MessageUpdated(val info: MessageInfo) : SSEEvent()
    data class MessagePartUpdated(val part: Part) : SSEEvent()
    data class MessagePartRemoved(val messageId: String, val partId: String) : SSEEvent()
    data class MessageRemoved(val messageId: String, val sessionId: String) : SSEEvent()
    data class SessionUpdated(val session: Session) : SSEEvent()
    data class SessionDeleted(val session: Session) : SSEEvent()
    data class SessionIdle(val sessionId: String) : SSEEvent()
    data class SessionError(val sessionId: String, val errorName: String) : SSEEvent()
    data class FileEdited(val file: String) : SSEEvent()
    data class PermissionUpdated(val permission: Permission) : SSEEvent()
    data class SessionStatusUpdated(val status: SessionStatus) : SSEEvent()
    data class TodoUpdated(val todos: List<Todo>) : SSEEvent()
    data class SessionCompacted(val sessionId: String) : SSEEvent()
    data class Error(val message: String) : SSEEvent()
    data class Unknown(val type: String) : SSEEvent()
}