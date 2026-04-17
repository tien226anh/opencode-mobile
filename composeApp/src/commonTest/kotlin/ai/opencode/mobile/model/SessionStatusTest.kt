package ai.opencode.mobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionStatusTest {

    @Test
    fun testIdleStatus() {
        val status = SessionStatus.Idle(sessionId = "sess-123")
        assertTrue(status is SessionStatus.Idle)
        assertEquals("sess-123", status.sessionId)
    }

    @Test
    fun testBusyStatus() {
        val status = SessionStatus.Busy(sessionId = "sess-456")
        assertTrue(status is SessionStatus.Busy)
        assertEquals("sess-456", status.sessionId)
    }

    @Test
    fun testRetryStatus() {
        val status = SessionStatus.Retry(
            sessionId = "sess-789",
            attempt = 3,
            message = "Rate limit exceeded",
            next = 1712345678,
        )
        assertTrue(status is SessionStatus.Retry)
        assertEquals("sess-789", status.sessionId)
        assertEquals(3, status.attempt)
        assertEquals("Rate limit exceeded", status.message)
        assertEquals(1712345678, status.next)
    }

    @Test
    fun testUnknownStatus() {
        val status = SessionStatus.Unknown(type = "something-weird")
        assertTrue(status is SessionStatus.Unknown)
        assertEquals("something-weird", status.type)
    }

    @Test
    fun testWhenExpression() {
        val statuses: List<SessionStatus> = listOf(
            SessionStatus.Idle("s1"),
            SessionStatus.Busy("s2"),
            SessionStatus.Retry("s3", 1, "error", 100),
        )

        val result = statuses.map { status ->
            when (status) {
                is SessionStatus.Idle -> "idle"
                is SessionStatus.Busy -> "busy"
                is SessionStatus.Retry -> "retry:${status.attempt}"
                is SessionStatus.Unknown -> "unknown"
            }
        }

        assertEquals(listOf("idle", "busy", "retry:1"), result)
    }
}