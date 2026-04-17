package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val title: String = "",
    val version: String = "",
    /** The project this session belongs to. */
    @SerialName("projectID") val projectId: String = "",
    /** The directory/worktree path for this session. */
    val directory: String = "",
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
    /**
     * The model to use — nested object with providerID + modelID.
     * Matches the SDK's SessionPromptData.body.model field.
     */
    val model: ChatRequestModel? = null,
    /**
     * The agent/mode name (e.g. "code", "ask"). Called "agent" in current server versions,
     * was "mode" in older versions. Server accepts either.
     */
    val agent: String? = null,
    /** Legacy field name for older server versions. */
    val mode: String? = null,
    /** The message parts (text, file, agent, subtask). */
    val parts: List<TextPartInput>,
    /** Optional message ID to resume/resend. */
    @SerialName("messageID") val messageId: String? = null,
    /** Optional system prompt override. */
    val system: String? = null,
    /** Optional tool enable/disable map. */
    val tools: Map<String, Boolean>? = null,
)

@Serializable
data class ChatRequestModel(
    @SerialName("providerID") val providerId: String,
    @SerialName("modelID") val modelId: String,
)

@Serializable
data class TextPartInput(
    val type: String = "text",
    val text: String,
)

@Serializable
data class SessionInitRequest(
    @SerialName("messageID") val messageId: String,
    val model: ChatRequestModel,
)

@Serializable
data class SessionRevertRequest(
    @SerialName("messageID") val messageId: String,
    @SerialName("partID") val partId: String? = null,
)

@Serializable
data class SessionSummarizeRequest(
    val model: ChatRequestModel,
)