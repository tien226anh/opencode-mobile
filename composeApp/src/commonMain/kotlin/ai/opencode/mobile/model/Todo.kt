package ai.opencode.mobile.model

import kotlinx.serialization.Serializable

/**
 * Represents a todo item in a session.
 * Retrieved from GET /session/:id/todo
 */
@Serializable
data class Todo(
    val id: String = "",
    val content: String = "",
    val status: String = "pending",
    val priority: String = "normal",
)