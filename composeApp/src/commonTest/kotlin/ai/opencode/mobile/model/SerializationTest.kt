package ai.opencode.mobile.model

import ai.opencode.mobile.network.OpenCodeApiClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // --- Session tests ---
    @Test
    fun testSessionRoundTrip() {
        val session = Session(
            id = "s-123",
            title = "My Session",
            version = "1",
            time = SessionTime(created = 1000, updated = 2000),
        )
        val jsonString = json.encodeToString(Session.serializer(), session)
        val decoded = json.decodeFromString(Session.serializer(), jsonString)
        assertEquals(session.id, decoded.id)
        assertEquals(session.title, decoded.title)
        assertEquals(session.version, decoded.version)
        assertEquals(session.time.created, decoded.time.created)
        assertEquals(session.time.updated, decoded.time.updated)
    }

    @Test
    fun testSessionWithDefaults() {
        val session = Session(id = "default-session")
        val jsonString = json.encodeToString(Session.serializer(), session)
        val decoded = json.decodeFromString(Session.serializer(), jsonString)
        assertEquals("default-session", decoded.id)
        assertEquals("", decoded.title)
        assertEquals("", decoded.version)
        assertNull(decoded.parentId)
        assertNull(decoded.revert)
        assertNull(decoded.share)
    }

    @Test
    fun testSessionWithRevertAndShare() {
        val session = Session(
            id = "s-456",
            title = "Shared Session",
            parentId = "s-100",
            revert = RevertInfo(messageId = "m-1", partId = "p-1"),
            share = ShareInfo(url = "https://share.example.com/abc"),
        )
        val jsonString = json.encodeToString(Session.serializer(), session)
        val decoded = json.decodeFromString(Session.serializer(), jsonString)
        assertEquals("s-456", decoded.id)
        assertEquals("s-100", decoded.parentId)
        assertNotNull(decoded.revert)
        assertEquals("m-1", decoded.revert!!.messageId)
        assertNotNull(decoded.share)
        assertEquals("https://share.example.com/abc", decoded.share!!.url)
    }

    // --- MessageResponseItem / MessageInfo / Part tests ---
    @Test
    fun testMessageResponseItemRoundTrip() {
        val item = MessageResponseItem(
            info = MessageInfo(id = "m-1", role = "user", sessionId = "s-1"),
            parts = listOf(Part(type = "text", text = "Hello, world!")),
        )
        val jsonString = json.encodeToString(MessageResponseItem.serializer(), item)
        val decoded = json.decodeFromString(MessageResponseItem.serializer(), jsonString)
        assertEquals("m-1", decoded.info.id)
        assertEquals("user", decoded.info.role)
        assertEquals(1, decoded.parts.size)
        assertEquals("text", decoded.parts[0].type)
        assertEquals("Hello, world!", decoded.parts[0].text)
    }

    @Test
    fun testTextPart() {
        val part = Part(type = "text", text = "Hello", id = "p-1", messageId = "m-1")
        val jsonString = json.encodeToString(Part.serializer(), part)
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertEquals("text", decoded.type)
        assertEquals("Hello", decoded.text)
        assertEquals("p-1", decoded.id)
    }

    @Test
    fun testToolPart() {
        val part = Part(
            type = "tool",
            id = "p-2",
            tool = "file_read",
            callId = "tc-1",
            state = ToolState(
                status = "completed",
                input = """{"path":"/src/main.kt"}""",
                output = "file content here",
            ),
        )
        val jsonString = json.encodeToString(Part.serializer(), part)
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertEquals("tool", decoded.type)
        assertEquals("file_read", decoded.tool)
        assertEquals("tc-1", decoded.callId)
        assertNotNull(decoded.state)
        assertEquals("completed", decoded.state!!.status)
        assertEquals("""{"path":"/src/main.kt"}""", decoded.state!!.input)
    }

    @Test
    fun testToolPartWithError() {
        val part = Part(
            type = "tool",
            tool = "bash",
            state = ToolState(
                status = "error",
                error = "Command failed with exit code 1",
            ),
        )
        val jsonString = json.encodeToString(Part.serializer(), part)
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertNotNull(decoded.state)
        assertEquals("error", decoded.state!!.status)
        assertEquals("Command failed with exit code 1", decoded.state!!.error)
    }

    @Test
    fun testMessageInfoDefaults() {
        val info = MessageInfo(id = "m-1", role = "assistant")
        val jsonString = json.encodeToString(MessageInfo.serializer(), info)
        val decoded = json.decodeFromString(MessageInfo.serializer(), jsonString)
        assertEquals("m-1", decoded.id)
        assertEquals("assistant", decoded.role)
        assertEquals("", decoded.sessionId)
        assertNull(decoded.modelId)
        assertNull(decoded.providerId)
        assertNull(decoded.mode)
        assertNull(decoded.cost)
        assertNull(decoded.tokens)
    }

    @Test
    fun testMessageInfoWithTokens() {
        val info = MessageInfo(
            id = "m-2",
            role = "assistant",
            modelId = "gpt-4",
            providerId = "openai",
            cost = 0.05,
            tokens = MessageTokens(input = 100, output = 50, reasoning = 20),
        )
        val jsonString = json.encodeToString(MessageInfo.serializer(), info)
        val decoded = json.decodeFromString(MessageInfo.serializer(), jsonString)
        assertEquals("gpt-4", decoded.modelId)
        assertEquals(0.05, decoded.cost!!)
        assertNotNull(decoded.tokens)
        assertEquals(100, decoded.tokens!!.input)
        assertEquals(50, decoded.tokens!!.output)
    }

    // --- AppInfo / Providers / Mode tests ---
    @Test
    fun testAppInfoRoundTrip() {
        val appInfo = AppInfo(
            git = true,
            hostname = "my-server",
            path = AppPath(config = "/cfg", cwd = "/proj", data = "/data", root = "/root", state = "/state"),
            time = AppTime(initialized = 1700000000),
        )
        val jsonString = json.encodeToString(AppInfo.serializer(), appInfo)
        val decoded = json.decodeFromString(AppInfo.serializer(), jsonString)
        assertEquals(true, decoded.git)
        assertEquals("my-server", decoded.hostname)
        assertEquals("/proj", decoded.path.cwd)
        assertEquals(1700000000, decoded.time.initialized)
    }

    @Test
    fun testProvidersResponseRoundTrip() {
        val response = ProvidersResponse(
            default = mapOf("openai" to "gpt-4o"),
            providers = listOf(
                Provider(
                    id = "openai",
                    name = "OpenAI",
                    models = mapOf("gpt-4o" to ModelInfo(id = "gpt-4o", name = "GPT-4o")),
                ),
            ),
        )
        val jsonString = json.encodeToString(ProvidersResponse.serializer(), response)
        val decoded = json.decodeFromString(ProvidersResponse.serializer(), jsonString)
        assertEquals(1, decoded.providers.size)
        assertEquals("openai", decoded.providers[0].id)
        assertEquals("gpt-4o", decoded.default["openai"])
    }

    @Test
    fun testModeRoundTrip() {
        val mode = Mode(
            name = "code",
            tools = mapOf("file_read" to true, "bash" to true),
            model = ModeModel(modelId = "gpt-4", providerId = "openai"),
            description = "Code mode",
        )
        val jsonString = json.encodeToString(Mode.serializer(), mode)
        val decoded = json.decodeFromString(Mode.serializer(), jsonString)
        assertEquals("code", decoded.name)
        assertNotNull(decoded.model)
        assertEquals("gpt-4", decoded.model!!.modelId)
        assertEquals(true, decoded.tools["file_read"])
    }

    // --- ChatRequest tests ---
    @Test
    fun testChatRequestRoundTrip() {
        val request = ChatRequest(
            modelId = "gpt-4",
            providerId = "openai",
            parts = listOf(TextPartInput(text = "Hello")),
        )
        val jsonString = json.encodeToString(ChatRequest.serializer(), request)
        val decoded = json.decodeFromString(ChatRequest.serializer(), jsonString)
        assertEquals("gpt-4", decoded.modelId)
        assertEquals("openai", decoded.providerId)
        assertEquals(1, decoded.parts.size)
        assertEquals("Hello", decoded.parts[0].text)
        assertEquals("text", decoded.parts[0].type)
    }

    // --- ServerConfig tests ---
    @Test
    fun testServerConfigDefaults() {
        val config = ServerConfig(
            serverUrl = "http://localhost:54321",
            username = "opencode",
            password = "secret",
        )
        assertEquals("http://localhost:54321", config.serverUrl)
        assertEquals("opencode", config.username)
        assertEquals("secret", config.password)
        assertFalse(config.isConnected)
    }

    // --- Base64 encoding ---
    @Test
    fun testEncodeCredentials() {
        val encoded = OpenCodeApiClient.encodeCredentials("user", "pass")
        assertEquals("dXNlcjpwYXNz", encoded) // Base64 of "user:pass"
    }

    @Test
    fun testEncodeCredentialsEmptyPassword() {
        val encoded = OpenCodeApiClient.encodeCredentials("admin", "")
        assertEquals("YWRtaW46", encoded) // Base64 of "admin:"
    }

    // --- SessionTime ---
    @Test
    fun testSessionTimeDefaults() {
        val time = SessionTime()
        assertEquals(0, time.created)
        assertEquals(0, time.updated)
    }

    // --- MessageError ---
    @Test
    fun testMessageErrorRoundTrip() {
        val error = MessageError(name = "RateLimitError", data = "Too many requests")
        val jsonString = json.encodeToString(MessageError.serializer(), error)
        val decoded = json.decodeFromString(MessageError.serializer(), jsonString)
        assertEquals("RateLimitError", decoded.name)
        assertEquals("Too many requests", decoded.data)
    }

    // --- Default port consistency ---
    @Test
    fun testDefaultPortAndUrl() {
        assertEquals(54321, OpenCodeApiClient.DEFAULT_PORT)
        assertEquals("http://localhost:54321", OpenCodeApiClient.DEFAULT_URL)
    }
}