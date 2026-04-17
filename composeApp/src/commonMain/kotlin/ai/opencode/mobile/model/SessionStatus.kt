package ai.opencode.mobile.model

import kotlinx.serialization.Serializable

/**
 * Status of a session, received via `session.status` SSE event.
 *
 * The SDK defines this as a union type:
 * - idle: session is not processing
 * - busy: session is actively processing a request
 * - retry: session encountered an error and will retry
 */
sealed class SessionStatus {
    /** Session is idle — not processing anything */
    data class Idle(val sessionId: String = "") : SessionStatus()

    /** Session is busy — actively processing a request */
    data class Busy(val sessionId: String = "") : SessionStatus()

    /** Session encountered an error and will retry */
    data class Retry(
        val sessionId: String = "",
        val attempt: Int = 0,
        val message: String = "",
        val next: Long = 0,
    ) : SessionStatus()

    /** Unknown status — shouldn't normally happen */
    data class Unknown(val type: String = "") : SessionStatus()
}