package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A project in OpenCode — represents a codebase/workspace that the AI agent works with.
 *
 * The OpenCode server can manage multiple projects (git repos/worktrees).
 * The mobile app lets the user select which project to work with,
 * then shows sessions filtered by that project's directory.
 *
 * API endpoints:
 * - GET /project → List all projects
 * - GET /project/current → Get the currently active project
 * - PATCH /project/{projectID} → Update project properties
 *
 * Sessions belong to a project via Session.projectID and Session.directory.
 * Use ?directory=<worktree> query param to filter sessions by project.
 */
@Serializable
data class Project(
    val id: String = "",
    /** The git worktree path — this is the project directory. */
    val worktree: String = "",
    @SerialName("vcsDir") val vcsDir: String? = null,
    val vcs: String? = null,
    val name: String? = null,
    val icon: ProjectIcon? = null,
    val commands: ProjectCommands? = null,
    val time: ProjectTime = ProjectTime(),
    val sandboxes: List<String> = emptyList(),
)

@Serializable
data class ProjectIcon(
    val url: String? = null,
    val override: String? = null,
    val color: String? = null,
)

@Serializable
data class ProjectCommands(
    val start: String? = null,
)

@Serializable
data class ProjectTime(
    val created: Long = 0,
    val updated: Long = 0,
    val initialized: Long? = null,
)