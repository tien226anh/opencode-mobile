package ai.opencode.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val name: String = "",
    val icon: String = "",
    val directory: String = "",
    val worktree: String = "",
    val vcs: VcsInfo? = null,
    val time: ProjectTimeInfo = ProjectTimeInfo(),
)

@Serializable
data class VcsInfo(
    val branch: String = "",
    val sha: String = "",
    val dirty: Boolean = false,
)

@Serializable
data class ProjectTimeInfo(
    val created: Long = 0,
    val updated: Long = 0,
)

@Serializable
data class ServerConfig(
    val serverUrl: String = "",
    val basicAuth: String = "",
    val isConnected: Boolean = false,
)