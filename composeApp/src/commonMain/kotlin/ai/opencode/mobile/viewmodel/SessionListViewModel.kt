package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.Session
import ai.opencode.mobile.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionListState(
    val sessions: List<Session> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
)

class SessionListViewModel(
    private val sessionRepository: SessionRepository,
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(SessionListState())
    val state: StateFlow<SessionListState> = _state.asStateFlow()

    /** The directory filter — when set, only sessions for this project directory are shown. */
    var directoryFilter: String? = null

    fun loadSessions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = sessionRepository.getSessions(directoryFilter)
            result.fold(
                onSuccess = { sessions ->
                    _state.value = SessionListState(
                        sessions = sessions.sortedByDescending { it.time.updated },
                        isLoading = false,
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load sessions",
                    )
                },
            )
        }
    }

    fun refreshSessions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            val result = sessionRepository.getSessions(directoryFilter)
            result.fold(
                onSuccess = { sessions ->
                    _state.value = _state.value.copy(
                        sessions = sessions.sortedByDescending { it.time.updated },
                        isRefreshing = false,
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        error = error.message ?: "Failed to refresh sessions",
                    )
                },
            )
        }
    }

    fun createSession(onCreated: (Session) -> Unit) {
        viewModelScope.launch {
            val result = sessionRepository.createSession(directoryFilter)
            result.fold(
                onSuccess = { session ->
                    _state.value = _state.value.copy(
                        sessions = listOf(session) + _state.value.sessions,
                    )
                    onCreated(session)
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to create session",
                    )
                },
            )
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val result = sessionRepository.deleteSession(sessionId)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        sessions = _state.value.sessions.filter { it.id != sessionId },
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: "Failed to delete session",
                    )
                },
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}