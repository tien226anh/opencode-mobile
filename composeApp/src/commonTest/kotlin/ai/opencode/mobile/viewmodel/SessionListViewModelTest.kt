package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.Session
import ai.opencode.mobile.model.TimeInfo
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
        Session(
            id = "s1",
            title = "Test Session 1",
            directory = "/tmp/project1",
            time = TimeInfo(created = 1000, updated = 3000),
        ),
        Session(
            id = "s2",
            title = "Test Session 2",
            directory = "/tmp/project2",
            time = TimeInfo(created = 2000, updated = 2000),
        ),
    )

    private lateinit var fakeRepository: FakeSessionRepository
    private lateinit var viewModel: SessionListViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSessionRepository()
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
        val newSession = Session(id = "s3", title = "New Session", time = TimeInfo(created = 5000, updated = 5000))
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
}

class FakeSessionRepository : SessionRepository {
    var sessionsResult: Result<List<Session>> = Result.success(emptyList())
    var createSessionResult: Result<Session> = Result.failure(Exception("Not configured"))
    var messagesResult: Result<List<ai.opencode.mobile.model.Message>> = Result.success(emptyList())
    var sendMessageResult: Result<Unit> = Result.success(Unit)
    var abortResult: Result<Unit> = Result.success(Unit)
    var shareResult: Result<Unit> = Result.success(Unit)

    override suspend fun getSessions(): Result<List<Session>> = sessionsResult
    override suspend fun createSession(title: String?): Result<Session> = createSessionResult
    override suspend fun getSession(sessionId: String): Result<Session> =
        Result.failure(Exception("Not implemented"))
    override suspend fun deleteSession(sessionId: String): Result<Unit> =
        Result.failure(Exception("Not implemented"))
    override suspend fun getMessages(sessionId: String): Result<List<ai.opencode.mobile.model.Message>> = messagesResult
    override suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String): Result<Unit> = sendMessageResult
    override suspend fun abortSession(sessionId: String): Result<Unit> = abortResult
    override suspend fun shareSession(sessionId: String): Result<Unit> = shareResult
}