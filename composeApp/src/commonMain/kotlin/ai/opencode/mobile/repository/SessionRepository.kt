package ai.opencode.mobile.repository

import ai.opencode.mobile.model.CreateSessionRequest
import ai.opencode.mobile.model.Message
import ai.opencode.mobile.model.Session
import ai.opencode.mobile.network.OpenCodeApiClient

interface SessionRepository {
    suspend fun getSessions(): Result<List<Session>>
    suspend fun createSession(title: String?): Result<Session>
    suspend fun getSession(sessionId: String): Result<Session>
    suspend fun deleteSession(sessionId: String): Result<Unit>
    suspend fun getMessages(sessionId: String): Result<List<Message>>
    suspend fun sendMessage(sessionId: String, text: String, modelId: String, providerId: String): Result<Unit>
    suspend fun abortSession(sessionId: String): Result<Unit>
    suspend fun shareSession(sessionId: String): Result<Unit>
}

class DefaultSessionRepository(
    private val apiClient: OpenCodeApiClient,
) : SessionRepository {

    override suspend fun getSessions(): Result<List<Session>> =
        apiClient.listSessions()

    override suspend fun createSession(title: String?): Result<Session> =
        apiClient.createSession(CreateSessionRequest(title = title))

    override suspend fun getSession(sessionId: String): Result<Session> =
        apiClient.getSession(sessionId)

    override suspend fun deleteSession(sessionId: String): Result<Unit> =
        apiClient.deleteSession(sessionId)

    override suspend fun getMessages(sessionId: String): Result<List<Message>> =
        apiClient.listMessages(sessionId)

    override suspend fun sendMessage(
        sessionId: String,
        text: String,
        modelId: String,
        providerId: String,
    ): Result<Unit> {
        val request = ai.opencode.mobile.model.ChatRequest(
            modelId = modelId,
            providerId = providerId,
            parts = listOf(ai.opencode.mobile.model.PartInput(type = "text", text = text)),
        )
        return apiClient.sendChatMessage(sessionId, request)
    }

    override suspend fun abortSession(sessionId: String): Result<Unit> =
        apiClient.abortSession(sessionId)

    override suspend fun shareSession(sessionId: String): Result<Unit> =
        apiClient.shareSession(sessionId)
}