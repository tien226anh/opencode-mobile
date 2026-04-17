package ai.opencode.mobile.model

import kotlinx.serialization.Serializable

/**
 * Represents a file diff in a session.
 * Retrieved from GET /session/:id/diff
 */
@Serializable
data class FileDiff(
    val file: String = "",
    val before: String = "",
    val after: String = "",
    val additions: Int = 0,
    val deletions: Int = 0,
)

/**
 * Response from the session diff endpoint.
 */
@Serializable
data class SessionDiffResponse(
    val files: List<FileDiff> = emptyList(),
)