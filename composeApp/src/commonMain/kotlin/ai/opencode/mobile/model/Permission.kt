package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A permission request from the OpenCode server.
 *
 * When the AI wants to run a tool (bash, file edit, etc.),
 * the server sends a `permission.updated` SSE event with a Permission object.
 * The user must approve or deny it by POSTing to
 * `/session/:id/permissions/:permissionID`.
 */
@Serializable
data class Permission(
    val id: String = "",
    val type: String = "",
    /** Pattern can be a string or array of strings. */
    val pattern: JsonElement? = null,
    @SerialName("sessionID") val sessionId: String = "",
    @SerialName("messageID") val messageId: String = "",
    @SerialName("callID") val callId: String? = null,
    val title: String = "",
    /** Metadata — loose object {[key: string]: unknown}. */
    val metadata: JsonElement? = null,
    /** Time — object {created: number}. */
    val time: JsonElement? = null,
) {
    /** Extract metadata.command if present. */
    val metadataCommand: String? get() = try {
        (metadata as? kotlinx.serialization.json.JsonObject)?.get("command")
            ?.let { if (it is kotlinx.serialization.json.JsonPrimitive && it !is kotlinx.serialization.json.JsonNull) it.content else null }
    } catch (_: Exception) { null }

    /** Extract metadata.file if present. */
    val metadataFile: String? get() = try {
        (metadata as? kotlinx.serialization.json.JsonObject)?.get("file")
            ?.let { if (it is kotlinx.serialization.json.JsonPrimitive && it !is kotlinx.serialization.json.JsonNull) it.content else null }
    } catch (_: Exception) { null }

    /** Extract time.created as epoch seconds. */
    val timeCreated: Long get() = try {
        (time as? kotlinx.serialization.json.JsonObject)?.get("created")
            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() }
            ?: 0L
    } catch (_: Exception) { 0L }
}

/**
 * Request body for responding to a permission request.
 * POST /session/:id/permissions/:permissionID
 */
@Serializable
data class PermissionResponse(
    val response: String,
)