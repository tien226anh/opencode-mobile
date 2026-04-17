package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A message from the API. The /session/{id}/message endpoint returns
 * SessionMessagesResponse (array of MessageResponseItem), where each item
 * has `info` (the message metadata) and `parts` (message content parts).
 */
@Serializable
data class MessageResponseItem(
    val info: MessageInfo,
    val parts: List<Part> = emptyList(),
)

@Serializable
data class MessageInfo(
    val id: String,
    val role: String,
    @SerialName("sessionID") val sessionId: String = "",
    @SerialName("modelID") val modelId: String? = null,
    @SerialName("providerID") val providerId: String? = null,
    val mode: String? = null,
    val cost: Double? = null,
    val path: MessagePath? = null,
    val time: MessageTime = MessageTime(),
    val tokens: MessageTokens? = null,
    /**
     * Summary field — can be:
     * - Boolean: true/false (older server versions)
     * - Object: {"diffs": [...]} (current server version)
     * Stored as JsonElement to handle both without crash.
     */
    val summary: JsonElement? = null,
    val error: MessageError? = null,
)

@Serializable
data class MessagePath(
    val cwd: String = "",
    val root: String = "",
)

@Serializable
data class MessageTime(
    val created: Long = 0,
    val completed: Long? = null,
)

@Serializable
data class MessageTokens(
    val input: Long = 0,
    val output: Long = 0,
    val reasoning: Long = 0,
    val cache: TokenCache? = null,
)

@Serializable
data class TokenCache(
    val read: Long = 0,
    val write: Long = 0,
)

@Serializable
data class MessageError(
    val name: String = "",
    val data: String? = null,
    /**
     * The raw error field from the server. Can be:
     * - A string: "error message"
     * - An object: {"message": "...", "code": "..."}
     * - An array: [{"code": "invalid_union", "message": "..."}]
     * Stored as JsonElement to handle all formats without deserialization crash.
     */
    val error: JsonElement? = null,
) {
    /** Extract a human-readable error message from any error format. */
    val errorMessage: String? get() = try {
        when (error) {
            is JsonPrimitive -> error?.jsonPrimitive?.content
            is JsonObject ->
                (error.jsonObject["message"] as? JsonPrimitive)?.content
                    ?: (error.jsonObject["name"] as? JsonPrimitive)?.content
            is kotlinx.serialization.json.JsonArray -> {
                val first = error?.jsonArray?.firstOrNull()
                when (first) {
                    is JsonObject -> {
                        val msg = (first.jsonObject["message"] as? JsonPrimitive)?.content
                        val code = (first.jsonObject["code"] as? JsonPrimitive)?.content
                        if (msg != null && code != null) "$code: $msg" else msg
                    }
                    else -> first?.toString()
                }
            }
            else -> null
        }
    } catch (_: Exception) { null }
}

/**
 * Union type for message parts. The `type` field determines which fields are populated.
 *
 * Supported types: text, tool, file, step-start, step-finish, snapshot, patch,
 * reasoning, agent, retry, compaction, subtask
 */
@Serializable
data class Part(
    val id: String = "",
    val type: String,
    @SerialName("messageID") val messageId: String = "",
    @SerialName("sessionID") val sessionId: String = "",
    // TextPart fields
    val text: String? = null,
    val synthetic: Boolean? = null,
    val time: PartTime? = null,
    // ToolPart fields
    val tool: String? = null,
    @SerialName("callID") val callId: String? = null,
    val state: ToolState? = null,
    // FilePart fields
    val mime: String? = null,
    val url: String? = null,
    val filename: String? = null,
    /** Source — can be a FileSource or SymbolSource object, or string. */
    val source: JsonElement? = null,
    // SnapshotPart fields
    val snapshot: String? = null,
    // PatchPart fields
    val files: List<String>? = null,
    val hash: String? = null,
    // StepFinishPart fields
    val cost: Double? = null,
    val tokens: PartTokens? = null,
    val reason: String? = null,
    // AgentPart fields
    val name: String? = null,
    // RetryPart fields
    val attempt: Int? = null,
    // CompactionPart fields
    val auto: Boolean? = null,
)

@Serializable
data class PartTime(
    val start: Long = 0,
    val end: Long? = null,
)

@Serializable
data class ToolState(
    val status: String = "",
    /** Tool input — can be a JSON object {[key: string]: unknown} or string. */
    val input: JsonElement? = null,
    val output: String? = null,
    val error: String? = null,
    val title: String? = null,
    /** Tool metadata — can be a JSON object {[key: string]: unknown} or absent. */
    val metadata: JsonElement? = null,
    val time: ToolTime? = null,
    val raw: String? = null,
    val attachments: JsonElement? = null,
)

@Serializable
data class ToolTime(
    val start: Long = 0,
    val end: Long = 0,
    val compacted: Long? = null,
)

@Serializable
data class PartTokens(
    val input: Long = 0,
    val output: Long = 0,
    val reasoning: Long = 0,
    val cache: TokenCache? = null,
)

/**
 * SSE Event types
 */
@Serializable
data class ServerEvent(
    val type: String,
    val properties: String = "", // JSON string of event-specific properties
)