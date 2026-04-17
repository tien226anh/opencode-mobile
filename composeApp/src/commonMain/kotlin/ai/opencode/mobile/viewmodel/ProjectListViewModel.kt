package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.Project
import ai.opencode.mobile.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectListState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for selecting which project to work with.
 *
 * The OpenCode server can manage multiple projects (git worktrees).
 * This ViewModel loads the list of available projects and lets the user
 * select one. Sessions are then filtered by the selected project's directory.
 *
 * UX flow: Settings → ProjectList (this screen) → SessionList (filtered by project) → Chat
 */
class ProjectListViewModel(
    private val repository: SessionRepository,
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(ProjectListState())
    val state: StateFlow<ProjectListState> = _state.asStateFlow()

    fun loadProjects() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getProjects()
            result.fold(
                onSuccess = { projects ->
                    _state.value = ProjectListState(
                        projects = projects.sortedByDescending { it.time.updated },
                        isLoading = false,
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load projects",
                    )
                },
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}