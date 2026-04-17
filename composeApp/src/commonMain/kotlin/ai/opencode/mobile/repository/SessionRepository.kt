package ai.opencode.mobile.repository

import ai.opencode.mobile.model.*
import ai.opencode.mobile.network.OpenCodeApiClient

interface SessionRepository {
    suspend fun getSessions(): Result<List<Session>>
    suspend fun createSession(): Result<Session>
    suspend fun deleteSession(sessionId: String): Result<Boolean>
    suspend fun getMessages(sessionId: String): Result<List<MessageResponseItem>>
    suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String, mode: String? = null): Result<MessageInfo>
    suspend fun abortSession(sessionId: String): Result<Boolean>
    suspend fun shareSession(sessionId: String): Result<Session>
    suspend fun unshareSession(sessionId: String): Result<Session>
    suspend fun getProviders(): Result<ProvidersResponse>
    suspend fun getModes(): Result<List<Mode>>
}

class DefaultSessionRepository(
    private val apiClient: OpenCodeApiClient,
) : SessionRepository {

    override suspend fun getSessions(): Result<List<Session>> =
        apiClient.listSessions()

    override suspend fun createSession(): Result<Session> =
        apiClient.createSession()

    override suspend fun deleteSession(sessionId: String): Result<Boolean> =
        apiClient.deleteSession(sessionId)

    override suspend fun getMessages(sessionId: String): Result<List<MessageResponseItem>> =
        apiClient.listMessages(sessionId)

    override suspend fun sendMessage(
        sessionId: String,
        text: String,
        modelId: String,
        providerId: String,
        mode: String?,
    ): Result<MessageInfo> {
        val request = ChatRequest(
            modelId = modelId,
            providerId = providerId,
            parts = listOf(TextPartInput(text = text)),
            mode = mode,
        )
        return apiClient.sendChatMessage(sessionId, request)
    }

    override suspend fun abortSession(sessionId: String): Result<Boolean> =
        apiClient.abortSession(sessionId)

    override suspend fun shareSession(sessionId: String): Result<Session> =
        apiClient.shareSession(sessionId)

    override suspend fun unshareSession(sessionId: String): Result<Session> =
        apiClient.unshareSession(sessionId)

    override suspend fun getProviders(): Result<ProvidersResponse> =
        apiClient.getProviders()

    override suspend fun getModes(): Result<List<Mode>> =
        apiClient.getModes()
}