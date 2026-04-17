package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.MessageInfo
import ai.opencode.mobile.model.MessageResponseItem
import ai.opencode.mobile.model.Mode
import ai.opencode.mobile.model.ModeModel
import ai.opencode.mobile.model.Part
import ai.opencode.mobile.model.Provider
import ai.opencode.mobile.model.ProvidersResponse
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

class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testMessages = listOf(
        MessageResponseItem(
            info = MessageInfo(id = "m1", role = "user", sessionId = "test-session"),
            parts = listOf(Part(type = "text", text = "Hello")),
        ),
        MessageResponseItem(
            info = MessageInfo(id = "m2", role = "assistant", sessionId = "test-session"),
            parts = listOf(Part(type = "text", text = "Hi there")),
        ),
    )

    private lateinit var fakeRepository: FakeChatTestRepository
    private lateinit var viewModel: ChatViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeChatTestRepository()
        viewModel = ChatViewModel(fakeRepository, "test-session")
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadMessagesSuccess() {
        fakeRepository.messagesResult = Result.success(testMessages)

        viewModel.loadMessages()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.messages.size)
        assertEquals("m1", state.messages[0].info.id)
        assertEquals("m2", state.messages[1].info.id)
    }

    @Test
    fun testLoadMessagesFailure() {
        fakeRepository.messagesResult = Result.failure(Exception("Load failed"))

        viewModel.loadMessages()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Load failed", state.error)
    }

    @Test
    fun testSendMessageAddsOptimisticMessage() {
        fakeRepository.sendMessageResult = Result.success(
            MessageInfo(id = "m-new", role = "assistant", sessionId = "test-session")
        )
        fakeRepository.messagesResult = Result.success(testMessages)

        viewModel.sendMessage("Hello world")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSending)
        assertTrue(state.messages.size >= 2)
    }

    @Test
    fun testSendMessageFailure() {
        fakeRepository.sendMessageResult = Result.failure(Exception("Send failed"))

        viewModel.sendMessage("Hello world")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSending)
        assertEquals("Send failed", state.error)
    }

    @Test
    fun testClearError() {
        fakeRepository.messagesResult = Result.failure(Exception("Error"))
        viewModel.loadMessages()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Error", viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun testSetModel() {
        viewModel.setModel("gpt-4", "openai")

        val state = viewModel.state.value
        assertEquals("gpt-4", state.currentModelId)
        assertEquals("openai", state.currentProviderId)
    }

    @Test
    fun testAbortSession() {
        fakeRepository.abortResult = Result.success(true)

        viewModel.abortSession()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun testInitialModelState() {
        val state = viewModel.state.value
        assertEquals("", state.currentModelId)
        assertEquals("", state.currentProviderId)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isSending)
        assertFalse(state.isStreaming)
        assertNull(state.error)
    }

    @Test
    fun testInitialStreamingState() {
        assertFalse(viewModel.state.value.isStreaming)
    }

    @Test
    fun testStopStreaming() {
        viewModel.stopStreaming()
        assertFalse(viewModel.state.value.isStreaming)
    }

    @Test
    fun testAbortSessionStopsStreaming() {
        fakeRepository.abortResult = Result.success(true)

        viewModel.abortSession()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isStreaming)
    }
}

class FakeChatTestRepository : SessionRepository {
    var messagesResult: Result<List<MessageResponseItem>> = Result.success(emptyList())
    var sendMessageResult: Result<MessageInfo> = Result.success(MessageInfo(id = "m-new", role = "assistant", sessionId = "test"))
    var abortResult: Result<Boolean> = Result.success(true)

    override suspend fun getSessions(): Result<List<Session>> = Result.success(emptyList())
    override suspend fun createSession(): Result<Session> = Result.failure(Exception("Not implemented"))
    override suspend fun deleteSession(sessionId: String): Result<Boolean> = Result.failure(Exception("Not implemented"))
    override suspend fun getMessages(sessionId: String): Result<List<MessageResponseItem>> = messagesResult
    override suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String): Result<MessageInfo> = sendMessageResult
    override suspend fun abortSession(sessionId: String): Result<Boolean> = abortResult
    override suspend fun shareSession(sessionId: String): Result<Session> = Result.failure(Exception("Not implemented"))
    override suspend fun unshareSession(sessionId: String): Result<Session> = Result.failure(Exception("Not implemented"))
    override suspend fun getProviders(): Result<ProvidersResponse> = Result.success(ProvidersResponse())
    override suspend fun getModes(): Result<List<Mode>> = Result.success(emptyList())
}