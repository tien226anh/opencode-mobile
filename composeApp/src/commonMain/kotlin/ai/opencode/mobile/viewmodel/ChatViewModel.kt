package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.Message
import ai.opencode.mobile.model.MessagePart
import ai.opencode.mobile.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val currentModelId: String = "",
    val currentProviderId: String = "",
)

class ChatViewModel(
    private val sessionRepository: SessionRepository,
    private val sessionId: String,
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var tempMessageCounter = 0L

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
            _state.value = _state.value.copy(isSending = true, error = null)

            // Optimistically add user message
            val optimisticMessage = Message(
                id = "temp-${tempMessageCounter++}",
                role = "user",
                parts = listOf(MessagePart(type = "text", text = text)),
                sessionId = sessionId,
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + optimisticMessage,
            )

            val result = sessionRepository.sendMessage(sessionId, text, modelId, providerId)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isSending = false)
                    // Reload messages to get the full conversation including assistant response
                    loadMessages()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        error = error.message ?: "Failed to send message",
                    )
                },
            )
        }
    }

    fun abortSession() {
        viewModelScope.launch {
            sessionRepository.abortSession(sessionId)
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
}