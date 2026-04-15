package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val slug: String = "",
    @SerialName("projectID") val projectId: String = "",
    @SerialName("workspaceID") val workspaceId: String? = null,
    val directory: String = "",
    @SerialName("parentID") val parentId: String? = null,
    val summary: String? = null,
    val share: ShareInfo? = null,
    val title: String = "",
    val version: Int = 0,
    val time: TimeInfo = TimeInfo(),
    val permission: PermissionInfo? = null,
    val revert: RevertInfo? = null,
)

@Serializable
data class ShareInfo(
    val url: String = "",
)

@Serializable
data class TimeInfo(
    val created: Long = 0,
    val updated: Long = 0,
)

@Serializable
data class PermissionInfo(
    val allow: List<String> = emptyList(),
    val deny: List<String> = emptyList(),
)

@Serializable
data class RevertInfo(
    @SerialName("messageID") val messageId: String = "",
)

@Serializable
data class SessionListResponse(
    val sessions: List<Session> = emptyList(),
)

@Serializable
data class CreateSessionRequest(
    val title: String? = null,
)

@Serializable
data class ChatRequest(
    @SerialName("modelID") val modelId: String,
    @SerialName("providerID") val providerId: String,
    val parts: List<PartInput>,
    @SerialName("messageID") val messageId: String? = null,
    val mode: String? = null,
    val system: String? = null,
    val tools: Map<String, Boolean>? = null,
)

@Serializable
data class PartInput(
    val type: String,
    val text: String? = null,
)

@Serializable
data class RevertRequest(
    @SerialName("messageID") val messageId: String,
    @SerialName("partID") val partId: String? = null,
)