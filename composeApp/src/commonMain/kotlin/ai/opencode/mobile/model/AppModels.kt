package ai.opencode.mobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
    val git: Boolean = false,
    val hostname: String = "",
    val path: AppPath = AppPath(),
    val time: AppTime = AppTime(),
)

@Serializable
data class AppPath(
    val config: String = "",
    val cwd: String = "",
    val data: String = "",
    val root: String = "",
    val state: String = "",
)

@Serializable
data class AppTime(
    val initialized: Long? = null,
)

@Serializable
data class Provider(
    val id: String = "",
    val name: String = "",
    val env: List<String> = emptyList(),
    val models: Map<String, ModelInfo> = emptyMap(),
    val api: String = "",
    val npm: String = "",
)

@Serializable
data class ModelInfo(
    val id: String = "",
    val name: String = "",
    val attachment: Boolean = false,
    val reasoning: Boolean = false,
    val temperature: Boolean = false,
    @SerialName("tool_call") val toolCall: Boolean = false,
    @SerialName("release_date") val releaseDate: String = "",
    val cost: ModelCost = ModelCost(),
    val limit: ModelLimit = ModelLimit(),
    val options: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
)

@Serializable
data class ModelCost(
    val input: Double = 0.0,
    val output: Double = 0.0,
    @SerialName("cache_read") val cacheRead: Double? = null,
    @SerialName("cache_write") val cacheWrite: Double? = null,
)

@Serializable
data class ModelLimit(
    val context: Int = 0,
    val output: Int = 0,
)

@Serializable
data class ProvidersResponse(
    val default: Map<String, String> = emptyMap(),
    val providers: List<Provider> = emptyList(),
)

@Serializable
data class Mode(
    val name: String = "",
    val tools: Map<String, Boolean> = emptyMap(),
    val model: ModeModel? = null,
    val prompt: String? = null,
    val temperature: Double? = null,
    val description: String? = null,
)

@Serializable
data class ModeModel(
    @SerialName("modelID") val modelId: String = "",
    @SerialName("providerID") val providerId: String = "",
)

@Serializable
data class ServerConfig(
    val serverHost: String = "",
    val serverPort: String = "4096",
    val username: String = "",
    val password: String = "",
    val isConnected: Boolean = false,
    val providerId: String = "",
    val modelId: String = "",
    val modeName: String = "",
) {
    /** Computed full URL from host + port, e.g. "http://10.0.2.2:4096" */
    val serverUrl: String get() {
        if (serverHost.isBlank()) return ""
        val host = serverHost.trimEnd('/')
        return if (serverPort.isNotBlank()) "$host:$serverPort" else host
    }
}