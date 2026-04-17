package ai.opencode.mobile.repository

import ai.opencode.mobile.model.MessageInfo
import ai.opencode.mobile.model.MessageResponseItem
import ai.opencode.mobile.model.Mode
import ai.opencode.mobile.model.ModeModel
import ai.opencode.mobile.model.Part
import ai.opencode.mobile.model.Provider
import ai.opencode.mobile.model.ProvidersResponse
import ai.opencode.mobile.model.Session
import ai.opencode.mobile.model.SessionDiffResponse
import ai.opencode.mobile.model.SessionTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionRepositoryTest {

    private val testSessions = listOf(
        Session(id = "s1", title = "Session 1", time = SessionTime(created = 1000, updated = 3000)),
        Session(id = "s2", title = "Session 2", time = SessionTime(created = 2000, updated = 2000)),
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
        val result = repo.createSession()
        assertTrue(result.isSuccess)
        assertEquals("new", result.getOrNull()!!.id)
    }

    @Test
    fun testFakeRepositoryGetMessages() = runTest {
        val messages = listOf(
            MessageResponseItem(
                info = MessageInfo(id = "m1", role = "user", sessionId = "s1"),
                parts = listOf(Part(type = "text", text = "Hello")),
            ),
        )
        val repo = FakeTestRepository(messages = messages)
        val result = repo.getMessages("s1")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun testFakeRepositorySendMessageSuccess() = runTest {
        val repo = FakeTestRepository()
        val result = repo.sendMessage("s1", "hello", "model", "provider", null)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryAbortSessionSuccess() = runTest {
        val repo = FakeTestRepository()
        val result = repo.abortSession("s1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryDeleteSession() = runTest {
        val repo = FakeTestRepository()
        val result = repo.deleteSession("s1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryShareSession() = runTest {
        val repo = FakeTestRepository()
        val result = repo.shareSession("s1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryUnshareSession() = runTest {
        val repo = FakeTestRepository()
        val result = repo.unshareSession("s1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryGetProviders() = runTest {
        val providers = ProvidersResponse(
            default = mapOf("openai" to "gpt-4o"),
            providers = listOf(Provider(id = "openai", name = "OpenAI")),
        )
        val repo = FakeTestRepository(providersResponse = providers)
        val result = repo.getProviders()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.providers.size)
    }

    @Test
    fun testFakeRepositoryGetModes() = runTest {
        val modes = listOf(Mode(name = "code", model = ModeModel(modelId = "gpt-4", providerId = "openai")))
        val repo = FakeTestRepository(modes = modes)
        val result = repo.getModes()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun testFakeRepositoryRespondPermissionAllow() = runTest {
        val repo = FakeTestRepository()
        val result = repo.respondPermission("session-1", "perm-123", true)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun testFakeRepositoryRespondPermissionDeny() = runTest {
        val repo = FakeTestRepository()
        val result = repo.respondPermission("session-1", "perm-456", false)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryRevertMessage() = runTest {
        val repo = FakeTestRepository()
        val result = repo.revertMessage("session-1", "msg-123")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryUnrevertMessage() = runTest {
        val repo = FakeTestRepository()
        val result = repo.unrevertMessage("session-1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testFakeRepositoryAllMethodsFailWhenConfigured() = runTest {
        val repo = FakeTestRepository(shouldFail = true)
        assertTrue(repo.getSessions().isFailure)
        assertTrue(repo.createSession().isFailure)
        assertTrue(repo.deleteSession("x").isFailure)
        assertTrue(repo.getMessages("x").isFailure)
        assertTrue(repo.sendMessage("x", "t", "m", "p", null).isFailure)
        assertTrue(repo.abortSession("x").isFailure)
        assertTrue(repo.shareSession("x").isFailure)
        assertTrue(repo.unshareSession("x").isFailure)
        assertTrue(repo.getProviders().isFailure)
        assertTrue(repo.getModes().isFailure)
        assertTrue(repo.respondPermission("x", "p", true).isFailure)
        assertTrue(repo.revertMessage("x", "m").isFailure)
        assertTrue(repo.unrevertMessage("x").isFailure)
        assertTrue(repo.getSessionDiff("x").isFailure)
    }
}

internal class FakeTestRepository(
    private val sessions: List<Session> = emptyList(),
    private val createResult: Session = Session(id = "default"),
    private val messages: List<MessageResponseItem> = emptyList(),
    private val providersResponse: ProvidersResponse = ProvidersResponse(),
    private val modes: List<Mode> = emptyList(),
    private val shouldFail: Boolean = false,
) : SessionRepository {

    override suspend fun getSessions(): Result<List<Session>> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(sessions)

    override suspend fun createSession(): Result<Session> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(createResult)

    override suspend fun deleteSession(sessionId: String): Result<Boolean> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(true)

    override suspend fun getMessages(sessionId: String): Result<List<MessageResponseItem>> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(messages)

    override suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String, mode: String?): Result<MessageInfo> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(MessageInfo(id = "m-new", role = "assistant", sessionId = sessionId))

    override suspend fun abortSession(sessionId: String): Result<Boolean> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(true)

    override suspend fun shareSession(sessionId: String): Result<Session> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Session(id = sessionId))

    override suspend fun unshareSession(sessionId: String): Result<Session> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Session(id = sessionId))

    override suspend fun getProviders(): Result<ProvidersResponse> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(providersResponse)

    override suspend fun getModes(): Result<List<Mode>> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(modes)

    override suspend fun respondPermission(sessionId: String, permissionId: String, allow: Boolean): Result<Boolean> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(true)

    override suspend fun revertMessage(sessionId: String, messageId: String, partId: String?): Result<Session> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Session(id = sessionId))

    override suspend fun unrevertMessage(sessionId: String): Result<Session> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(Session(id = sessionId))

    override suspend fun getSessionDiff(sessionId: String): Result<SessionDiffResponse> =
        if (shouldFail) Result.failure(Exception("Test error")) else Result.success(SessionDiffResponse())
}