package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val title: String = "",
    val version: String = "",
    val time: SessionTime = SessionTime(),
    @SerialName("parentID") val parentId: String? = null,
    val revert: RevertInfo? = null,
    val share: ShareInfo? = null,
)

@Serializable
data class SessionTime(
    val created: Long = 0,
    val updated: Long = 0,
)

@Serializable
data class ShareInfo(
    val url: String = "",
)

@Serializable
data class RevertInfo(
    @SerialName("messageID") val messageId: String = "",
    @SerialName("partID") val partId: String? = null,
    val diff: String? = null,
    val snapshot: String? = null,
)

@Serializable
data class ChatRequest(
    @SerialName("modelID") val modelId: String,
    @SerialName("providerID") val providerId: String,
    val parts: List<TextPartInput>,
    @SerialName("messageID") val messageId: String? = null,
    val mode: String? = null,
    val system: String? = null,
    val tools: Map<String, Boolean>? = null,
)

@Serializable
data class TextPartInput(
    val type: String = "text",
    val text: String,
)

@Serializable
data class SessionInitRequest(
    @SerialName("messageID") val messageId: String,
    @SerialName("modelID") val modelId: String,
    @SerialName("providerID") val providerId: String,
)

@Serializable
data class SessionRevertRequest(
    @SerialName("messageID") val messageId: String,
    @SerialName("partID") val partId: String? = null,
)

@Serializable
data class SessionSummarizeRequest(
    @SerialName("modelID") val modelId: String,
    @SerialName("providerID") val providerId: String,
)