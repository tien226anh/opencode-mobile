package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.MessageInfo
import ai.opencode.mobile.model.MessageResponseItem
import ai.opencode.mobile.model.Mode
import ai.opencode.mobile.model.ModelInfo
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

    private val testProviders = ProvidersResponse(
        default = mapOf("openai" to "gpt-4o"),
        providers = listOf(
            Provider(
                id = "openai",
                name = "OpenAI",
                models = mapOf(
                    "gpt-4o" to ModelInfo(id = "gpt-4o", name = "GPT-4o"),
                    "gpt-4o-mini" to ModelInfo(id = "gpt-4o-mini", name = "GPT-4o Mini"),
                ),
            ),
            Provider(
                id = "anthropic",
                name = "Anthropic",
                models = mapOf(
                    "claude-3.5-sonnet" to ModelInfo(id = "claude-3.5-sonnet", name = "Claude 3.5 Sonnet"),
                ),
            ),
        ),
    )

    private val testModes = listOf(
        Mode(name = "code", description = "Code mode"),
        Mode(name = "ask", description = "Ask mode"),
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

    // --- Existing tests (updated) ---

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
        assertEquals("", state.currentModeName)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isSending)
        assertFalse(state.isStreaming)
        assertNull(state.error)
        assertTrue(state.providers.isEmpty())
        assertTrue(state.modes.isEmpty())
        assertEquals("", state.selectedProviderId)
        assertEquals("", state.selectedModelId)
        assertEquals("", state.selectedModeName)
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

    // --- NEW: Provider loading tests ---

    @Test
    fun testLoadProvidersSuccess() {
        fakeRepository.providersResult = Result.success(testProviders)

        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoadingProviders)
        assertEquals(2, state.providers.size)
        assertEquals("openai", state.providers[0].id)
        assertEquals("anthropic", state.providers[1].id)
    }

    @Test
    fun testLoadProvidersAutoSelectsFirstProviderAndDefaultModel() {
        fakeRepository.providersResult = Result.success(testProviders)

        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        // Should auto-select first provider
        assertEquals("openai", state.selectedProviderId)
        // Should auto-select default model for the provider
        assertEquals("gpt-4o", state.selectedModelId)
    }

    @Test
    fun testLoadProvidersKeepsExistingSelection() {
        // Pre-select a provider before loading
        viewModel.updateProvider("anthropic")
        viewModel.updateModel("claude-3.5-sonnet")

        fakeRepository.providersResult = Result.success(testProviders)

        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        // Should keep existing selection
        assertEquals("anthropic", state.selectedProviderId)
        assertEquals("claude-3.5-sonnet", state.selectedModelId)
    }

    @Test
    fun testLoadProvidersFailure() {
        fakeRepository.providersResult = Result.failure(Exception("Network error"))

        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoadingProviders)
        assertTrue(state.providers.isEmpty())
        assertTrue(state.error?.contains("Failed to load providers") == true)
    }

    @Test
    fun testLoadProvidersEmptyList() {
        fakeRepository.providersResult = Result.success(ProvidersResponse())

        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoadingProviders)
        assertTrue(state.providers.isEmpty())
        assertEquals("", state.selectedProviderId)
        assertEquals("", state.selectedModelId)
    }

    // --- NEW: Mode loading tests ---

    @Test
    fun testLoadModesSuccess() {
        fakeRepository.modesResult = Result.success(testModes)

        viewModel.loadModes()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoadingModes)
        assertEquals(2, state.modes.size)
        assertEquals("code", state.modes[0].name)
        assertEquals("ask", state.modes[1].name)
    }

    @Test
    fun testLoadModesAutoSelectsFirstMode() {
        fakeRepository.modesResult = Result.success(testModes)

        viewModel.loadModes()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("code", state.selectedModeName)
    }

    @Test
    fun testLoadModesKeepsExistingSelection() {
        viewModel.updateMode("ask")

        fakeRepository.modesResult = Result.success(testModes)

        viewModel.loadModes()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("ask", state.selectedModeName)
    }

    @Test
    fun testLoadModesFailure() {
        fakeRepository.modesResult = Result.failure(Exception("Network error"))

        viewModel.loadModes()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoadingModes)
        assertTrue(state.modes.isEmpty())
        assertTrue(state.error?.contains("Failed to load modes") == true)
    }

    // --- NEW: Provider/Model/Mode selection tests ---

    @Test
    fun testUpdateProvider() {
        // First load providers so the list is available
        fakeRepository.providersResult = Result.success(testProviders)
        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        // Switch to Anthropic
        viewModel.updateProvider("anthropic")

        val state = viewModel.state.value
        assertEquals("anthropic", state.selectedProviderId)
        assertEquals("anthropic", state.currentProviderId)
        // Should auto-select first model for Anthropic
        assertEquals("claude-3.5-sonnet", state.selectedModelId)
        assertEquals("claude-3.5-sonnet", state.currentModelId)
    }

    @Test
    fun testUpdateProviderResetsModelSelection() {
        // First load providers
        fakeRepository.providersResult = Result.success(testProviders)
        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially has OpenAI + gpt-4o
        assertEquals("openai", viewModel.state.value.selectedProviderId)
        assertEquals("gpt-4o", viewModel.state.value.selectedModelId)

        // Switch to Anthropic - model should auto-select its first model
        viewModel.updateProvider("anthropic")

        assertEquals("anthropic", viewModel.state.value.selectedProviderId)
        assertEquals("claude-3.5-sonnet", viewModel.state.value.selectedModelId)
    }

    @Test
    fun testUpdateModel() {
        // Load providers first
        fakeRepository.providersResult = Result.success(testProviders)
        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        // Switch model within OpenAI
        viewModel.updateModel("gpt-4o-mini")

        val state = viewModel.state.value
        assertEquals("gpt-4o-mini", state.selectedModelId)
        assertEquals("gpt-4o-mini", state.currentModelId)
    }

    @Test
    fun testUpdateMode() {
        viewModel.updateMode("ask")

        val state = viewModel.state.value
        assertEquals("ask", state.selectedModeName)
        assertEquals("ask", state.currentModeName)
    }

    // --- NEW: sendMessage with mode test ---

    @Test
    fun testSendMessageIncludesModeWhenSelected() {
        fakeRepository.sendMessageResult = Result.success(
            MessageInfo(id = "m-new", role = "assistant", sessionId = "test-session")
        )

        // Set up provider/model/mode
        viewModel.updateProvider("openai")
        viewModel.updateModel("gpt-4o")
        viewModel.updateMode("code")

        viewModel.sendMessage("Hello")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the repository was called with the right parameters
        assertEquals("openai", fakeRepository.lastSendMessageProviderId)
        assertEquals("gpt-4o", fakeRepository.lastSendMessageModelId)
        assertEquals("code", fakeRepository.lastSendMessageMode)
    }

    @Test
    fun testSendMessageUsesDefaultsWhenNoSelection() {
        fakeRepository.sendMessageResult = Result.success(
            MessageInfo(id = "m-new", role = "assistant", sessionId = "test-session")
        )

        viewModel.sendMessage("Hello")
        testDispatcher.scheduler.advanceUntilIdle()

        // When nothing is selected, should use "default"
        assertEquals("default", fakeRepository.lastSendMessageProviderId)
        assertEquals("default", fakeRepository.lastSendMessageModelId)
        assertNull(fakeRepository.lastSendMessageMode)
    }

    // --- NEW: selectedProviderModels computed property ---

    @Test
    fun testSelectedProviderModelsReturnsModelsForSelectedProvider() {
        fakeRepository.providersResult = Result.success(testProviders)
        viewModel.loadProviders()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        // OpenAI has 2 models
        assertEquals(2, state.selectedProviderModels.size)
        assertEquals("gpt-4o", state.selectedProviderModels[0].id)
        assertEquals("gpt-4o-mini", state.selectedProviderModels[1].id)
    }

    @Test
    fun testSelectedProviderModelsEmptyWhenNoProviderSelected() {
        assertEquals(0, viewModel.state.value.selectedProviderModels.size)
    }
}

class FakeChatTestRepository : SessionRepository {
    var messagesResult: Result<List<MessageResponseItem>> = Result.success(emptyList())
    var sendMessageResult: Result<MessageInfo> = Result.success(MessageInfo(id = "m-new", role = "assistant", sessionId = "test"))
    var abortResult: Result<Boolean> = Result.success(true)
    var providersResult: Result<ProvidersResponse> = Result.success(ProvidersResponse())
    var modesResult: Result<List<Mode>> = Result.success(emptyList())

    // Capture last sendMessage call for verification
    var lastSendMessageModelId: String = ""
    var lastSendMessageProviderId: String = ""
    var lastSendMessageMode: String? = null

    override suspend fun getSessions(): Result<List<Session>> = Result.success(emptyList())
    override suspend fun createSession(): Result<Session> = Result.failure(Exception("Not implemented"))
    override suspend fun deleteSession(sessionId: String): Result<Boolean> = Result.failure(Exception("Not implemented"))
    override suspend fun getMessages(sessionId: String): Result<List<MessageResponseItem>> = messagesResult
    override suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String, mode: String?): Result<MessageInfo> {
        lastSendMessageModelId = modelId
        lastSendMessageProviderId = providerId
        lastSendMessageMode = mode
        return sendMessageResult
    }
    override suspend fun abortSession(sessionId: String): Result<Boolean> = abortResult
    override suspend fun shareSession(sessionId: String): Result<Session> = Result.failure(Exception("Not implemented"))
    override suspend fun unshareSession(sessionId: String): Result<Session> = Result.failure(Exception("Not implemented"))
    override suspend fun getProviders(): Result<ProvidersResponse> = providersResult
    override suspend fun getModes(): Result<List<Mode>> = modesResult
}