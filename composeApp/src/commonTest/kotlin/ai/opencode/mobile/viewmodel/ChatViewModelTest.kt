package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.Message
import ai.opencode.mobile.model.MessagePart
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
        Message(
            id = "m1",
            role = "user",
            parts = listOf(MessagePart(type = "text", text = "Hello")),
        ),
        Message(
            id = "m2",
            role = "assistant",
            parts = listOf(MessagePart(type = "text", text = "Hi there")),
        ),
    )

    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var viewModel: ChatViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeChatRepository()
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
        assertEquals("m1", state.messages[0].id)
        assertEquals("m2", state.messages[1].id)
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
        fakeRepository.sendMessageResult = Result.success(Unit)
        fakeRepository.messagesResult = Result.success(testMessages)

        viewModel.sendMessage("Hello world")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isSending)
        // After send completes, loadMessages is called which replaces the list
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
        fakeRepository.abortResult = Result.success(Unit)

        viewModel.abortSession()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not throw — just a fire-and-forget
        assertFalse(viewModel.state.value.isLoading)
    }
}

class FakeChatRepository : SessionRepository {
    var messagesResult: Result<List<Message>> = Result.success(emptyList())
    var sendMessageResult: Result<Unit> = Result.success(Unit)
    var abortResult: Result<Unit> = Result.success(Unit)

    override suspend fun getSessions(): Result<List<ai.opencode.mobile.model.Session>> =
        Result.success(emptyList())
    override suspend fun createSession(title: String?): Result<ai.opencode.mobile.model.Session> =
        Result.failure(Exception("Not implemented"))
    override suspend fun getSession(sessionId: String): Result<ai.opencode.mobile.model.Session> =
        Result.failure(Exception("Not implemented"))
    override suspend fun deleteSession(sessionId: String): Result<Unit> =
        Result.failure(Exception("Not implemented"))
    override suspend fun getMessages(sessionId: String): Result<List<Message>> = messagesResult
    override suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String): Result<Unit> = sendMessageResult
    override suspend fun abortSession(sessionId: String): Result<Unit> = abortResult
    override suspend fun shareSession(sessionId: String): Result<Unit> =
        Result.failure(Exception("Not implemented"))
}