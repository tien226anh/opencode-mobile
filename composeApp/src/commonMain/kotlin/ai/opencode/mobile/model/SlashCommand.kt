package ai.opencode.mobile.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Represents a slash command available in an OpenCode session.
 * Retrieved from GET /command
 * See: https://opencode.ai/docs/commands
 *
 * Note: `template` can be either a string or a JSON object `{}` depending on
 * the server version. We store it as [JsonElement] and provide a convenience
 * [templateText] accessor that extracts the string value when possible.
 */
@Serializable
data class SlashCommand(
    val name: String = "",
    val description: String? = null,
    val agent: String? = null,
    val model: String? = null,
    val template: JsonElement = JsonPrimitive(""),
    val subtask: Boolean? = null,
    val hints: List<JsonElement> = emptyList(),
) {
    /** Extract the template as a plain string, if it is one. Returns "" for objects. */
    val templateText: String get() = try {
        template.jsonPrimitive.content
    } catch (_: Exception) {
        ""
    }
}