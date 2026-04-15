package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val role: String,
    val parts: List<MessagePart> = emptyList(),
    @SerialName("sessionID") val sessionId: String = "",
    val time: MessageTimeInfo = MessageTimeInfo(),
)

@Serializable
data class MessageTimeInfo(
    val created: Long = 0,
    val updated: Long = 0,
)

@Serializable
data class MessagePart(
    val type: String,
    val text: String? = null,
    val reasoning: String? = null,
    @SerialName("toolCall") val toolCall: ToolCallInfo? = null,
    @SerialName("toolResult") val toolResult: ToolResultInfo? = null,
    @SerialName("sourceUrl") val sourceUrl: String? = null,
    @SerialName("file") val file: FileInfo? = null,
    @SerialName("stepStart") val stepStart: StepStartInfo? = null,
)

@Serializable
data class ToolCallInfo(
    @SerialName("toolCallID") val toolCallId: String,
    @SerialName("toolName") val toolName: String,
    val args: String = "{}",
    val state: String = "call",
)

@Serializable
data class ToolResultInfo(
    @SerialName("toolCallID") val toolCallId: String,
    @SerialName("toolName") val toolName: String,
    val result: String = "",
    val error: String? = null,
)

@Serializable
data class FileInfo(
    val name: String = "",
    val path: String = "",
    val content: String = "",
)

@Serializable
data class StepStartInfo(
    @SerialName("messageID") val messageId: String = "",
)

@Serializable
data class MessageListResponse(
    val messages: List<Message> = emptyList(),
)