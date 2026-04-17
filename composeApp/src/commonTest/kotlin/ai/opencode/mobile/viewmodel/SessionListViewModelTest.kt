package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.MessageInfo
import ai.opencode.mobile.model.MessageResponseItem
import ai.opencode.mobile.model.Mode
import ai.opencode.mobile.model.ModeModel
import ai.opencode.mobile.model.Part
import ai.opencode.mobile.model.Provider
import ai.opencode.mobile.model.ProvidersResponse
import ai.opencode.mobile.model.SessionDiffResponse
import ai.opencode.mobile.model.Todo
import ai.opencode.mobile.model.Session
import ai.opencode.mobile.model.SessionTime
import ai.opencode.mobile.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testSessions = listOf(
        Session(id = "s1", title = "Test Session 1", time = SessionTime(created = 1000, updated = 3000)),
        Session(id = "s2", title = "Test Session 2", time = SessionTime(created = 2000, updated = 2000)),
    )

    private lateinit var fakeRepository: FakeSessionListTestRepository
    private lateinit var viewModel: SessionListViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSessionListTestRepository()
        viewModel = SessionListViewModel(fakeRepository)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadSessionsSuccess() {
        fakeRepository.sessionsResult = Result.success(testSessions)

        viewModel.loadSessions()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.sessions.size)
        // Should be sorted by updated descending
        assertEquals("s1", state.sessions[0].id)
        assertEquals("s2", state.sessions[1].id)
    }

    @Test
    fun testLoadSessionsFailure() {
        fakeRepository.sessionsResult = Result.failure(Exception("Network error"))

        viewModel.loadSessions()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
    }

    @Test
    fun testCreateSessionSuccess() {
        fakeRepository.sessionsResult = Result.success(testSessions)
        val newSession = Session(id = "s3", title = "New Session", time = SessionTime(created = 5000, updated = 5000))
        fakeRepository.createSessionResult = Result.success(newSession)

        var createdSession: Session? = null
        viewModel.loadSessions()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createSession { createdSession = it }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(newSession, createdSession)
        // New session should be prepended
        assertEquals(3, viewModel.state.value.sessions.size)
        assertEquals("s3", viewModel.state.value.sessions[0].id)
    }

    @Test
    fun testCreateSessionFailure() {
        fakeRepository.createSessionResult = Result.failure(Exception("Create failed"))

        viewModel.createSession {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Create failed", viewModel.state.value.error)
    }

    @Test
    fun testClearError() {
        fakeRepository.sessionsResult = Result.failure(Exception("Error"))
        viewModel.loadSessions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Error", viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun testRefreshSessions() {
        val refreshedSessions = listOf(
            Session(id = "s3", title = "Refreshed", time = SessionTime(created = 6000, updated = 6000)),
        )
        fakeRepository.sessionsResult = Result.success(refreshedSessions)

        viewModel.refreshSessions()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isRefreshing)
        assertEquals(1, state.sessions.size)
        assertEquals("s3", state.sessions[0].id)
    }
}

class FakeSessionListTestRepository : SessionRepository {
    var sessionsResult: Result<List<Session>> = Result.success(emptyList())
    var createSessionResult: Result<Session> = Result.failure(Exception("Not configured"))

    override suspend fun getSessions(): Result<List<Session>> = sessionsResult
    override suspend fun createSession(): Result<Session> = createSessionResult
    override suspend fun deleteSession(sessionId: String): Result<Boolean> = Result.failure(Exception("Not implemented"))
    override suspend fun getMessages(sessionId: String): Result<List<MessageResponseItem>> = Result.success(emptyList())
    override suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String, mode: String?): Result<MessageInfo> = Result.failure(Exception("Not implemented"))
    override suspend fun abortSession(sessionId: String): Result<Boolean> = Result.failure(Exception("Not implemented"))
    override suspend fun shareSession(sessionId: String): Result<Session> = Result.failure(Exception("Not implemented"))
    override suspend fun unshareSession(sessionId: String): Result<Session> = Result.failure(Exception("Not implemented"))
    override suspend fun getProviders(): Result<ProvidersResponse> = Result.success(ProvidersResponse())
    override suspend fun getModes(): Result<List<Mode>> = Result.success(emptyList())
    override suspend fun respondPermission(sessionId: String, permissionId: String, allow: Boolean): Result<Boolean> = Result.success(true)
    override suspend fun revertMessage(sessionId: String, messageId: String, partId: String?): Result<Session> = Result.success(Session(id = sessionId))
    override suspend fun unrevertMessage(sessionId: String): Result<Session> = Result.success(Session(id = sessionId))
    override suspend fun getSessionDiff(sessionId: String): Result<SessionDiffResponse> = Result.success(SessionDiffResponse())
    override suspend fun forkSession(sessionId: String, messageId: String?): Result<Session> = Result.success(Session(id = "forked-$sessionId"))
    override suspend fun getSessionChildren(sessionId: String): Result<List<Session>> = Result.success(emptyList())
    override suspend fun getTodoList(sessionId: String): Result<List<Todo>> = Result.success(emptyList())
    override suspend fun listCommands(): Result<List<ai.opencode.mobile.model.SlashCommand>> = Result.success(emptyList())
    override suspend fun executeCommand(sessionId: String, command: String, arguments: String, messageId: String?, agent: String?, model: String?): Result<Boolean> = Result.success(true)
}