package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.MessageInfo
import ai.opencode.mobile.model.MessageResponseItem
import ai.opencode.mobile.model.MessageTime
import ai.opencode.mobile.model.Part
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