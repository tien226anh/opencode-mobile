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

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; explicitNulls = false }

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
            model = ChatRequestModel(
                modelId = "gpt-4",
                providerId = "openai",
            ),
            parts = listOf(TextPartInput(text = "Hello")),
        )
        val jsonString = json.encodeToString(ChatRequest.serializer(), request)
        val decoded = json.decodeFromString(ChatRequest.serializer(), jsonString)
        assertEquals("gpt-4", decoded.model?.modelId)
        assertEquals("openai", decoded.model?.providerId)
        assertEquals(1, decoded.parts.size)
        assertEquals("Hello", decoded.parts[0].text)
        assertEquals("text", decoded.parts[0].type)
    }

    @Test
    fun testTextPartInputSerializesWithTypeField() {
        val input = TextPartInput(text = "hi")
        val jsonString = json.encodeToString(TextPartInput.serializer(), input)
        // The server requires "type":"text" — verify it's actually in the JSON
        assertTrue(jsonString.contains("\"type\""), "Missing 'type' field in JSON: $jsonString")
        assertTrue(jsonString.contains("\"text\""), "Missing 'text' field in JSON: $jsonString")
        // Should be: {"type":"text","text":"hi"}
        assertTrue(
            jsonString.contains("\"type\":\"text\""),
            "Expected type=\"text\" but got: $jsonString"
        )
    }

    @Test
    fun testChatRequestJsonShapeMatchesServer() {
        val request = ChatRequest(
            model = ChatRequestModel(providerId = "ollama-cloud", modelId = "glm-5.1"),
            agent = "summary",
            parts = listOf(TextPartInput(text = "hi")),
        )
        val jsonString = json.encodeToString(ChatRequest.serializer(), request)
        // Verify the exact shape the server expects:
        // {"model":{"providerID":"ollama-cloud","modelID":"glm-5.1"},"agent":"summary","parts":[{"type":"text","text":"hi"}]}
        assertTrue(jsonString.contains("\"model\""), "Missing 'model' field: $jsonString")
        assertTrue(jsonString.contains("\"providerID\""), "Missing 'providerID' in model: $jsonString")
        assertTrue(jsonString.contains("\"modelID\""), "Missing 'modelID' in model: $jsonString")
        assertTrue(jsonString.contains("\"agent\""), "Missing 'agent' field: $jsonString")
        assertTrue(jsonString.contains("\"parts\""), "Missing 'parts' field: $jsonString")
        assertTrue(jsonString.contains("\"type\":\"text\""), "Part missing type discriminator: $jsonString")
    }

    @Test
    fun testChatRequestOmitsNullFields() {
        val request = ChatRequest(
            model = ChatRequestModel(providerId = "ollama-cloud", modelId = "rnj-1:8b"),
            agent = "code",
            parts = listOf(TextPartInput(text = "hi")),
            // messageId, system, tools, mode are all null — server rejects null values
        )
        val jsonString = json.encodeToString(ChatRequest.serializer(), request)
        // Null fields must be OMITTED, not sent as null
        assertFalse(jsonString.contains("\"messageID\":null"), "messageID should be omitted, not null: $jsonString")
        assertFalse(jsonString.contains("\"system\":null"), "system should be omitted, not null: $jsonString")
        assertFalse(jsonString.contains("\"tools\":null"), "tools should be omitted, not null: $jsonString")
        assertFalse(jsonString.contains("\"mode\":null"), "mode should be omitted, not null: $jsonString")
        // But type="text" default must still be present (encodeDefaults=true)
        assertTrue(jsonString.contains("\"type\":\"text\""), "type default must be present: $jsonString")
    }

    // --- ServerConfig tests ---
    @Test
    fun testServerConfigDefaults() {
        val config = ServerConfig(
            serverHost = "http://localhost",
            serverPort = "4096",
            username = "opencode",
            password = "secret",
        )
        assertEquals("http://localhost:4096", config.serverUrl)
        assertEquals("http://localhost", config.serverHost)
        assertEquals("4096", config.serverPort)
        assertEquals("opencode", config.username)
        assertEquals("secret", config.password)
        assertFalse(config.isConnected)
    }

    @Test
    fun testServerConfigUrlComposition() {
        // Host + port
        assertEquals("http://10.0.2.2:4096", ServerConfig(serverHost = "http://10.0.2.2", serverPort = "4096").serverUrl)
        // Host without port
        assertEquals("https://xxx.trycloudflare.com", ServerConfig(serverHost = "https://xxx.trycloudflare.com", serverPort = "").serverUrl)
        // Cloudflare with port
        assertEquals("https://xxx.trycloudflare.com:443", ServerConfig(serverHost = "https://xxx.trycloudflare.com", serverPort = "443").serverUrl)
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
        assertEquals(4096, OpenCodeApiClient.DEFAULT_PORT)
        assertEquals("http://localhost:4096", OpenCodeApiClient.DEFAULT_URL)
    }

    // --- New Part type tests ---
    @Test
    fun testReasoningPart() {
        val jsonString = """{"type": "reasoning", "id": "p-r1", "text": "Let me think about this step by step...", "time": {"start": 1000, "end": 2000}}"""
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertEquals("reasoning", decoded.type)
        assertEquals("p-r1", decoded.id)
        assertEquals("Let me think about this step by step...", decoded.text)
        assertNotNull(decoded.time)
        assertEquals(1000, decoded.time!!.start)
    }

    @Test
    fun testPatchPart() {
        val jsonString = """{"type": "patch", "id": "p-p1", "hash": "abc123", "files": ["src/Main.kt", "src/Utils.kt"]}"""
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertEquals("patch", decoded.type)
        assertEquals("abc123", decoded.hash)
        assertEquals(2, decoded.files!!.size)
        assertEquals("src/Main.kt", decoded.files!![0])
    }

    @Test
    fun testAgentPart() {
        val jsonString = """{"type": "agent", "id": "p-a1", "name": "plan"}"""
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertEquals("agent", decoded.type)
        assertEquals("plan", decoded.name)
    }

    @Test
    fun testRetryPart() {
        val jsonString = """{"type": "retry", "id": "p-rt1", "attempt": 3}"""
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertEquals("retry", decoded.type)
        assertEquals(3, decoded.attempt)
    }

    @Test
    fun testCompactionPart() {
        val jsonString = """{"type": "compaction", "id": "p-c1", "auto": true}"""
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertEquals("compaction", decoded.type)
        assertEquals(true, decoded.auto)
    }

    @Test
    fun testStepFinishWithReason() {
        val jsonString = """{"type": "step-finish", "id": "p-sf1", "reason": "completed", "cost": 0.05}"""
        val decoded = json.decodeFromString(Part.serializer(), jsonString)
        assertEquals("step-finish", decoded.type)
        assertEquals("completed", decoded.reason)
        assertEquals(0.05, decoded.cost!!)
    }
}