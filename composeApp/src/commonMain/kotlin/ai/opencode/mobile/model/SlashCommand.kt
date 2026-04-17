package ai.opencode.mobile.model

import kotlinx.serialization.Serializable

/**
 * Represents a slash command available in an OpenCode session.
 * Retrieved from GET /command
 * See: https://opencode.ai/docs/commands
 */
@Serializable
data class SlashCommand(
    val name: String = "",
    val description: String? = null,
    val agent: String? = null,
    val model: String? = null,
    val template: String = "",
    val subtask: Boolean? = null,
)