package ai.opencode.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String = "",
    val version: String = "",
)

@Serializable
data class GlobalEvent(
    val type: String,
    val data: String = "",
    val timestamp: Long = 0,
)