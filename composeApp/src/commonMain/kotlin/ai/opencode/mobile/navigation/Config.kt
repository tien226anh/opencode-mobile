package ai.opencode.mobile.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Config {
    @Serializable
    data class SessionList(val directory: String? = null, val projectName: String = "") : Config

    @Serializable
    data class Chat(val sessionId: String, val sessionTitle: String = "") : Config

    @Serializable
    data object Settings : Config

    @Serializable
    data class ProjectList(val serverUrl: String = "", val username: String = "", val password: String = "") : Config
}