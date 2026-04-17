package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val pattern: String? = null,
    @SerialName("sessionID") val sessionId: String = "",
    @SerialName("messageID") val messageId: String = "",
    @SerialName("callID") val callId: String? = null,
    val title: String = "",
    val metadata: PermissionMetadata? = null,
    val time: Long = 0,
)

/**
 * Metadata associated with a permission request.
 * Contains details about what the tool wants to do.
 */
@Serializable
data class PermissionMetadata(
    val command: String? = null,
    val file: String? = null,
    val args: String? = null,
)

/**
 * Request body for responding to a permission request.
 * POST /session/:id/permissions/:permissionID
 */
@Serializable
data class PermissionResponse(
    val response: String,
)