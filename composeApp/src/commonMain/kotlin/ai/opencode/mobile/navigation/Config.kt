package ai.opencode.mobile.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Config {
    @Serializable
    data object SessionList : Config

    @Serializable
    data class Chat(val sessionId: String, val sessionTitle: String = "") : Config

    @Serializable
    data object Settings : Config
}