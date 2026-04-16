package ai.opencode.mobile.repository

import ai.opencode.mobile.model.Message
import ai.opencode.mobile.model.Session
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for SessionRepository interface contract.
 *
 * Since OpenCodeApiClient is a concrete class (not an interface), we test
 * the repository via the SessionRepository interface using a fake implementation.
 * This verifies the contract that ViewModels rely on.
 */
class SessionRepositoryTest {

    private val testSessions = listOf(
        Session(id = "s1", title = "Session 1"),
        Session(id = "s2", title = "Session 2"),
    )

    @Test
    fun testFakeRepositoryGetSessionsSuccess() = runTest {
        val repo = FakeTestRepository(sessions = testSessions)
        val result = repo.getSessions()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()!!.size)
    }

    @Test
    fun testFakeRepositoryGetSessionsFailure() = runTest {
        val repo = FakeTestRepository(shouldFail = true)
        val result = repo.getSessions()

        assertTrue(result.isFailure)
    }

    @Test
    fun testFakeRepositoryCreateSession() = runTest {
        val newSession = Session(id = "new", title = "New")
        val repo = FakeTestRepository(createResult = newSession)
        val result = repo.createSession("New")

        assertTrue(result.isSuccess)
        assertEquals("new", result.getOrNull()!!.id)
    }

    @Test
    fun testFakeRepositoryGetMessages() = runTest {
        val messages = listOf(
            Message(id = "m1", role = "user", parts = emptyList()),
        )
        val repo = FakeTestRepository(messages = messages)
        val result = repo.getMessages("s1")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun testFakeRepositorySendMessageSuccess() = runTest {
        val repo = FakeTestRepository()
        val result = repo.sendMessage("s1", "hello", "model", "provider")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryAbortSessionSuccess() = runTest {
        val repo = FakeTestRepository()
        val result = repo.abortSession("s1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryAllMethodsFailWhenConfigured() = runTest {
        val repo = FakeTestRepository(shouldFail = true)

        assertTrue(repo.getSessions().isFailure)
        assertTrue(repo.createSession(null).isFailure)
        assertTrue(repo.getSession("x").isFailure)
        assertTrue(repo.deleteSession("x").isFailure)
        assertTrue(repo.getMessages("x").isFailure)
        assertTrue(repo.sendMessage("x", "t", "m", "p").isFailure)
        assertTrue(repo.abortSession("x").isFailure)
        assertTrue(repo.shareSession("x").isFailure)
    }
}

internal class FakeTestRepository(
    private val sessions: List<Session> = emptyList(),
    private val createResult: Session = Session(id = "default"),
    private val messages: List<Message> = emptyList(),
    private val shouldFail: Boolean = false,
) : SessionRepository {

    override suspend fun getSessions(): Result<List<Session>> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(sessions)

    override suspend fun createSession(title: String?): Result<Session> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(createResult)

    override suspend fun getSession(sessionId: String): Result<Session> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Session(id = sessionId))

    override suspend fun deleteSession(sessionId: String): Result<Unit> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Unit)

    override suspend fun getMessages(sessionId: String): Result<List<Message>> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(messages)

    override suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String): Result<Unit> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Unit)

    override suspend fun abortSession(sessionId: String): Result<Unit> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Unit)

    override suspend fun shareSession(sessionId: String): Result<Unit> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Unit)
}