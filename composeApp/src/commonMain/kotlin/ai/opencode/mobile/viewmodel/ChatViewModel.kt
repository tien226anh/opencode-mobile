package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.Mode
import ai.opencode.mobile.model.ModelInfo
import ai.opencode.mobile.model.Provider
import ai.opencode.mobile.model.ProvidersResponse
import ai.opencode.mobile.model.MessageInfo
import ai.opencode.mobile.model.MessageResponseItem
import ai.opencode.mobile.model.MessageTime
import ai.opencode.mobile.model.Part
import ai.opencode.mobile.model.Permission
import ai.opencode.mobile.model.SessionStatus
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
    val currentModeName: String = "",
    val isStreaming: Boolean = false,
    // Provider & model selection
    val providers: List<Provider> = emptyList(),
    val modes: List<Mode> = emptyList(),
    val selectedProviderId: String = "",
    val selectedModelId: String = "",
    val selectedModeName: String = "",
    val isLoadingProviders: Boolean = false,
    val isLoadingModes: Boolean = false,
    // Permission handling
    val pendingPermission: Permission? = null,
    // Session status
    val sessionStatus: SessionStatus? = null,
) {
    val selectedProvider: Provider? get() = providers.find { it.id == selectedProviderId }
    val selectedProviderModels: List<ModelInfo> get() =
        selectedProvider?.models?.values?.toList() ?: emptyList()
}

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

    internal val _state = MutableStateFlow(ChatState())
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

    fun loadProviders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingProviders = true)
            sessionRepository.getProviders().fold(
                onSuccess = { response ->
                    val currentState = _state.value
                    // Auto-select first provider and its default model if none selected
                    val autoProviderId = currentState.selectedProviderId.ifBlank {
                        response.providers.firstOrNull()?.id ?: ""
                    }
                    val autoModelId = currentState.selectedModelId.ifBlank {
                        response.default[autoProviderId] ?: ""
                    }
                    _state.value = currentState.copy(
                        providers = response.providers,
                        selectedProviderId = autoProviderId,
                        selectedModelId = autoModelId,
                        isLoadingProviders = false,
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoadingProviders = false,
                        error = "Failed to load providers: ${error.message ?: "Unknown error"}",
                    )
                },
            )
        }
    }

    fun loadModes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingModes = true)
            sessionRepository.getModes().fold(
                onSuccess = { modes ->
                    val currentMode = _state.value.selectedModeName
                    _state.value = _state.value.copy(
                        modes = modes,
                        selectedModeName = currentMode.ifBlank { modes.firstOrNull()?.name ?: "" },
                        isLoadingModes = false,
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoadingModes = false,
                        error = "Failed to load modes: ${error.message ?: "Unknown error"}",
                    )
                },
            )
        }
    }

    fun updateProvider(providerId: String) {
        _state.value = _state.value.copy(
            selectedProviderId = providerId,
            selectedModelId = "",
            currentModelId = "",
            currentProviderId = providerId,
        )
        // Auto-select default model for this provider
        val provider = _state.value.providers.find { it.id == providerId }
        if (provider != null) {
            val defaultModel = provider.models.values.firstOrNull()
            if (defaultModel != null) {
                _state.value = _state.value.copy(
                    selectedModelId = defaultModel.id,
                    currentModelId = defaultModel.id,
                )
            }
        }
    }

    fun updateModel(modelId: String) {
        _state.value = _state.value.copy(
            selectedModelId = modelId,
            currentModelId = modelId,
        )
    }

    fun updateMode(modeName: String) {
        _state.value = _state.value.copy(
            selectedModeName = modeName,
            currentModeName = modeName,
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val modelId = _state.value.currentModelId.ifBlank { _state.value.selectedModelId.ifBlank { "default" } }
        val providerId = _state.value.currentProviderId.ifBlank { _state.value.selectedProviderId.ifBlank { "default" } }
        val modeName = _state.value.selectedModeName.ifBlank { _state.value.currentModeName }

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

            val result = sessionRepository.sendMessage(sessionId, text, modelId, providerId, modeName.ifBlank { null })
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
                    is SSEEvent.PermissionUpdated -> {
                        // Show permission dialog to user
                        _state.value = _state.value.copy(pendingPermission = event.permission)
                    }
                    is SSEEvent.SessionStatusUpdated -> {
                        _state.value = _state.value.copy(sessionStatus = event.status)
                        // If idle, stop streaming indicator
                        if (event.status is SessionStatus.Idle) {
                            _state.value = _state.value.copy(isStreaming = false)
                        }
                    }
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Respond to a pending permission request.
     * @param allow true to approve, false to deny
     */
    fun respondToPermission(allow: Boolean) {
        val permission = _state.value.pendingPermission ?: return
        viewModelScope.launch {
            val result = sessionRepository.respondPermission(sessionId, permission.id, allow)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(pendingPermission = null)
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        error = "Permission response failed: ${error.message ?: "Unknown error"}",
                        pendingPermission = null,
                    )
                },
            )
        }
    }

    /**
     * Undo (revert) a message. This removes the message and all subsequent messages,
     * rolling back to the specified message.
     */
    fun revertMessage(messageId: String, partId: String? = null) {
        viewModelScope.launch {
            val result = sessionRepository.revertMessage(sessionId, messageId, partId)
            result.fold(
                onSuccess = {
                    // Refresh messages after revert
                    loadMessages()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        error = "Revert failed: ${error.message ?: "Unknown error"}",
                    )
                },
            )
        }
    }

    /**
     * Restore an undone (reverted) message.
     */
    fun unrevertMessage() {
        viewModelScope.launch {
            val result = sessionRepository.unrevertMessage(sessionId)
            result.fold(
                onSuccess = {
                    // Refresh messages after unrevert
                    loadMessages()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        error = "Restore failed: ${error.message ?: "Unknown error"}",
                    )
                },
            )
        }
    }

    fun stopStreaming() {
        sseJob?.cancel()
        _state.value = _state.value.copy(isStreaming = false)
    }
}