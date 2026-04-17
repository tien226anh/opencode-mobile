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
    suspend fun respondPermission(sessionId: String, permissionId: String, allow: Boolean): Result<Boolean>
    suspend fun revertMessage(sessionId: String, messageId: String, partId: String? = null): Result<Session>
    suspend fun unrevertMessage(sessionId: String): Result<Session>
    suspend fun getSessionDiff(sessionId: String): Result<SessionDiffResponse>
    suspend fun forkSession(sessionId: String, messageId: String? = null): Result<Session>
    suspend fun getSessionChildren(sessionId: String): Result<List<Session>>
    suspend fun getTodoList(sessionId: String): Result<List<Todo>>
    suspend fun listCommands(): Result<List<SlashCommand>>
    suspend fun executeCommand(sessionId: String, command: String, arguments: String = "", messageId: String? = null, agent: String? = null, model: String? = null): Result<Boolean>
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

    override suspend fun respondPermission(sessionId: String, permissionId: String, allow: Boolean): Result<Boolean> =
        apiClient.respondPermission(sessionId, permissionId, allow)

    override suspend fun revertMessage(sessionId: String, messageId: String, partId: String?): Result<Session> {
        val request = SessionRevertRequest(messageId = messageId, partId = partId)
        return apiClient.revertMessage(sessionId, request)
    }

    override suspend fun unrevertMessage(sessionId: String): Result<Session> =
        apiClient.unrevertSession(sessionId)

    override suspend fun getSessionDiff(sessionId: String): Result<SessionDiffResponse> =
        apiClient.getSessionDiff(sessionId)

    override suspend fun forkSession(sessionId: String, messageId: String?): Result<Session> =
        apiClient.forkSession(sessionId, messageId)

    override suspend fun getSessionChildren(sessionId: String): Result<List<Session>> =
        apiClient.getSessionChildren(sessionId)

    override suspend fun getTodoList(sessionId: String): Result<List<Todo>> =
        apiClient.getTodoList(sessionId)

    override suspend fun listCommands(): Result<List<SlashCommand>> =
        apiClient.listCommands()

    override suspend fun executeCommand(sessionId: String, command: String, arguments: String, messageId: String?, agent: String?, model: String?): Result<Boolean> =
        apiClient.executeCommand(sessionId, command, arguments, messageId, agent, model)
}