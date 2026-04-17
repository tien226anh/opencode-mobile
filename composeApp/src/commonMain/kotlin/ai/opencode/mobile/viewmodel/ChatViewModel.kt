package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.MessageInfo
import ai.opencode.mobile.model.MessageResponseItem
import ai.opencode.mobile.model.MessageTime
import ai.opencode.mobile.model.Part
import ai.opencode.mobile.network.SSEClient
import ai.opencode.mobile.network.SSEEvent
import ai.opencode.mobile.platform.currentTimeSeconds
import ai.opencode.mobile.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<MessageResponseItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val currentModelId: String = "",
    val currentProviderId: String = "",
    val isStreaming: Boolean = false,
)

class ChatViewModel(
    private val sessionRepository: SessionRepository,
    private val sessionId: String,
    private val sseClient: SSEClient? = null,
    private val baseUrl: String = "",
    private val basicAuth: String = "",
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var tempMessageCounter = 0L
    private var sseJob: kotlinx.coroutines.Job? = null

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = sessionRepository.getMessages(sessionId)
            result.fold(
                onSuccess = { messages ->
                    _state.value = _state.value.copy(
                        messages = messages,
                        isLoading = false,
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load messages",
                    )
                },
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val modelId = _state.value.currentModelId.ifBlank { "default" }
        val providerId = _state.value.currentProviderId.ifBlank { "default" }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true, isStreaming = true, error = null)

            // Optimistically add user message
            val optimisticMessage = MessageResponseItem(
                info = MessageInfo(
                    id = "temp-${tempMessageCounter++}",
                    role = "user",
                    sessionId = sessionId,
                    time = MessageTime(created = currentTimeSeconds()),
                ),
                parts = listOf(Part(type = "text", text = text)),
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + optimisticMessage,
            )

            val result = sessionRepository.sendMessage(sessionId, text, modelId, providerId)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isSending = false)
                    // Start SSE streaming to get real-time updates
                    startSSEStreaming()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        isStreaming = false,
                        error = error.message ?: "Failed to send message",
                    )
                },
            )
        }
    }

    /**
     * Start listening to SSE events for real-time message updates.
     * This replaces polling after sending a message - we get streaming
     * updates for parts (text, tool calls) as they arrive.
     */
    private fun startSSEStreaming() {
        sseJob?.cancel()
        val client = sseClient ?: run {
            // No SSE client available, fall back to polling
            loadMessages()
            _state.value = _state.value.copy(isStreaming = false)
            return
        }

        sseJob = viewModelScope.launch {
            client.connect(baseUrl, basicAuth).collect { event ->
                when (event) {
                    is SSEEvent.MessagePartUpdated -> {
                        // Update the part in our current messages list
                        updatePart(event.part)
                    }
                    is SSEEvent.MessageUpdated -> {
                        // A message was updated — refresh from server
                        loadMessages()
                    }
                    is SSEEvent.SessionIdle -> {
                        // Session is done processing — stop streaming
                        _state.value = _state.value.copy(isStreaming = false)
                        sseJob?.cancel()
                        loadMessages() // Final refresh to get complete state
                    }
                    is SSEEvent.SessionError -> {
                        _state.value = _state.value.copy(
                            isStreaming = false,
                            error = event.errorName,
                        )
                        sseJob?.cancel()
                        loadMessages()
                    }
                    is SSEEvent.MessageRemoved -> {
                        _state.value = _state.value.copy(
                            messages = _state.value.messages.filter { it.info.id != event.messageId }
                        )
                    }
                    is SSEEvent.MessagePartRemoved -> {
                        removePart(event.messageId, event.partId)
                    }
                    is SSEEvent.SessionUpdated -> {
                        // Session metadata changed, no action needed for chat
                    }
                    is SSEEvent.SessionDeleted -> {
                        _state.value = _state.value.copy(
                            isStreaming = false,
                            error = "Session was deleted",
                        )
                        sseJob?.cancel()
                    }
                    is SSEEvent.FileEdited -> { /* No action needed for chat */ }
                    is SSEEvent.PermissionUpdated -> { /* No action needed for chat */ }
                    is SSEEvent.Error -> {
                        _state.value = _state.value.copy(isStreaming = false)
                        // Don't show SSE errors to user — fall back to polling
                        loadMessages()
                    }
                    is SSEEvent.Unknown -> { /* Ignore unknown events */ }
                }
            }
        }
    }

    private fun updatePart(updatedPart: Part) {
        val currentMessages = _state.value.messages
        val updatedMessages = currentMessages.map { message ->
            val partIndex = message.parts.indexOfFirst { it.id == updatedPart.id && it.id.isNotEmpty() }
            if (partIndex >= 0) {
                // Replace the existing part with the updated one
                message.copy(parts = message.parts.toMutableList().apply { this[partIndex] = updatedPart })
            } else if (updatedPart.messageId == message.info.id || updatedPart.sessionId == message.info.sessionId) {
                // New part for this message — append it
                message.copy(parts = message.parts + updatedPart)
            } else {
                message
            }
        }
        _state.value = _state.value.copy(messages = updatedMessages)
    }

    private fun removePart(messageId: String, partId: String) {
        val currentMessages = _state.value.messages
        val updatedMessages = currentMessages.map { message ->
            if (message.info.id == messageId) {
                message.copy(parts = message.parts.filter { it.id != partId })
            } else {
                message
            }
        }
        _state.value = _state.value.copy(messages = updatedMessages)
    }

    fun abortSession() {
        viewModelScope.launch {
            sessionRepository.abortSession(sessionId)
            _state.value = _state.value.copy(isStreaming = false)
            sseJob?.cancel()
        }
    }

    fun setModel(modelId: String, providerId: String) {
        _state.value = _state.value.copy(
            currentModelId = modelId,
            currentProviderId = providerId,
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun stopStreaming() {
        sseJob?.cancel()
        _state.value = _state.value.copy(isStreaming = false)
    }
}