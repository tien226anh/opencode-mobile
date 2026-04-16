package ai.opencode.mobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSessionRoundTrip() {
        val session = Session(
            id = "s-123",
            title = "My Session",
            directory = "/home/user/project",
            summary = "Working on feature X",
            time = TimeInfo(created = 1000, updated = 2000),
        )

        val jsonString = json.encodeToString(Session.serializer(), session)
        val decoded = json.decodeFromString(Session.serializer(), jsonString)

        assertEquals(session.id, decoded.id)
        assertEquals(session.title, decoded.title)
        assertEquals(session.directory, decoded.directory)
        assertEquals(session.summary, decoded.summary)
        assertEquals(session.time.created, decoded.time.created)
        assertEquals(session.time.updated, decoded.time.updated)
    }

    @Test
    fun testMessageRoundTrip() {
        val message = Message(
            id = "m-456",
            role = "user",
            parts = listOf(
                MessagePart(type = "text", text = "Hello, world!"),
            ),
            sessionId = "s-123",
        )

        val jsonString = json.encodeToString(Message.serializer(), message)
        val decoded = json.decodeFromString(Message.serializer(), jsonString)

        assertEquals(message.id, decoded.id)
        assertEquals(message.role, decoded.role)
        assertEquals(1, decoded.parts.size)
        assertEquals("text", decoded.parts[0].type)
        assertEquals("Hello, world!", decoded.parts[0].text)
    }

    @Test
    fun testMessageWithToolCallRoundTrip() {
        val toolCall = ToolCallInfo(
            toolCallId = "tc-1",
            toolName = "file_read",
            args = """{"path": "/src/main.kt"}""",
            state = "call",
        )

        val message = Message(
            id = "m-789",
            role = "assistant",
            parts = listOf(
                MessagePart(type = "tool-invocation", toolCall = toolCall),
            ),
        )

        val jsonString = json.encodeToString(Message.serializer(), message)
        val decoded = json.decodeFromString(Message.serializer(), jsonString)

        val decodedToolCall = decoded.parts[0].toolCall
        assertNotNull(decodedToolCall)
        assertEquals("tc-1", decodedToolCall.toolCallId)
        assertEquals("file_read", decodedToolCall.toolName)
        assertEquals("""{"path": "/src/main.kt"}""", decodedToolCall.args)
        assertEquals("call", decodedToolCall.state)
    }

    @Test
    fun testMessageWithToolResultRoundTrip() {
        val toolResult = ToolResultInfo(
            toolCallId = "tc-1",
            toolName = "file_read",
            result = "file content here",
            error = null,
        )

        val message = Message(
            id = "m-999",
            role = "assistant",
            parts = listOf(
                MessagePart(type = "tool-result", toolResult = toolResult),
            ),
        )

        val jsonString = json.encodeToString(Message.serializer(), message)
        val decoded = json.decodeFromString(Message.serializer(), jsonString)

        val decodedResult = decoded.parts[0].toolResult
        assertNotNull(decodedResult)
        assertEquals("tc-1", decodedResult.toolCallId)
        assertEquals("file_read", decodedResult.toolName)
        assertEquals("file content here", decodedResult.result)
        assertNull(decodedResult.error)
    }

    @Test
    fun testToolResultWithErrorRoundTrip() {
        val toolResult = ToolResultInfo(
            toolCallId = "tc-2",
            toolName = "bash",
            result = "",
            error = "Command failed with exit code 1",
        )

        val message = Message(
            id = "m-err",
            role = "assistant",
            parts = listOf(
                MessagePart(type = "tool-result", toolResult = toolResult),
            ),
        )

        val jsonString = json.encodeToString(Message.serializer(), message)
        val decoded = json.decodeFromString(Message.serializer(), jsonString)

        val decodedResult = decoded.parts[0].toolResult
        assertNotNull(decodedResult)
        assertNotNull(decodedResult!!.error)
        assertEquals("Command failed with exit code 1", decodedResult.error)
    }

    @Test
    fun testServerConfigDefaultValues() {
        val config = ServerConfig(
            serverUrl = "http://localhost:4096",
            basicAuth = "my-secret-token",
        )

        assertEquals("http://localhost:4096", config.serverUrl)
        assertEquals("my-secret-token", config.basicAuth)
        assertFalse(config.isConnected)
    }

    @Test
    fun testSessionWithDefaultValues() {
        val session = Session(id = "default-session")

        val jsonString = json.encodeToString(Session.serializer(), session)
        val decoded = json.decodeFromString(Session.serializer(), jsonString)

        assertEquals("default-session", decoded.id)
        assertEquals("", decoded.title)
        assertEquals("", decoded.directory)
        assertNull(decoded.summary)
    }
}