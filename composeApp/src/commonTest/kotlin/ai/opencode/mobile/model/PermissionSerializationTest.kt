package ai.opencode.mobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PermissionSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun testPermissionDeserialization_fullObject() {
        val jsonString = """
        {
            "id": "perm-123",
            "type": "bash",
            "pattern": null,
            "sessionID": "sess-abc",
            "messageID": "msg-def",
            "callID": "call-ghi",
            "title": "Run bash command",
            "metadata": {
                "command": "ls -la",
                "file": null,
                "args": null
            },
            "time": 1712345678
        }
        """.trimIndent()

        val permission = json.decodeFromString<Permission>(jsonString)

        assertEquals("perm-123", permission.id)
        assertEquals("bash", permission.type)
        assertNull(permission.pattern)
        assertEquals("sess-abc", permission.sessionId)
        assertEquals("msg-def", permission.messageId)
        assertEquals("call-ghi", permission.callId)
        assertEquals("Run bash command", permission.title)
        assertEquals("ls -la", permission.metadata?.command)
        assertNull(permission.metadata?.file)
        assertEquals(1712345678, permission.time)
    }

    @Test
    fun testPermissionDeserialization_minimalObject() {
        val jsonString = """
        {
            "id": "perm-456",
            "type": "file-edit",
            "sessionID": "sess-xyz",
            "messageID": "msg-abc",
            "title": "Edit file",
            "time": 1712345678
        }
        """.trimIndent()

        val permission = json.decodeFromString<Permission>(jsonString)

        assertEquals("perm-456", permission.id)
        assertEquals("file-edit", permission.type)
        assertNull(permission.pattern)
        assertEquals("sess-xyz", permission.sessionId)
        assertEquals("msg-abc", permission.messageId)
        assertNull(permission.callId)
        assertEquals("Edit file", permission.title)
        assertNull(permission.metadata)
    }

    @Test
    fun testPermissionDeserialization_withFileMetadata() {
        val jsonString = """
        {
            "id": "perm-789",
            "type": "file-write",
            "sessionID": "sess-456",
            "messageID": "msg-789",
            "title": "Write file",
            "metadata": {
                "file": "/path/to/file.kt",
                "args": "{\"content\": \"hello\"}"
            },
            "time": 1712345678
        }
        """.trimIndent()

        val permission = json.decodeFromString<Permission>(jsonString)

        assertEquals("perm-789", permission.id)
        assertEquals("/path/to/file.kt", permission.metadata?.file)
        assertEquals("{\"content\": \"hello\"}", permission.metadata?.args)
        assertNull(permission.metadata?.command)
    }

    @Test
    fun testPermissionResponseSerialization_allow() {
        val response = PermissionResponse(response = "allow")
        val jsonString = json.encodeToString(PermissionResponse.serializer(), response)

        assertTrue(jsonString.contains("\"response\""))
        assertTrue(jsonString.contains("\"allow\""))
    }

    @Test
    fun testPermissionResponseSerialization_deny() {
        val response = PermissionResponse(response = "deny")
        val jsonString = json.encodeToString(PermissionResponse.serializer(), response)

        assertTrue(jsonString.contains("\"deny\""))
    }

    @Test
    fun testPermissionDeserialization_withPattern() {
        val jsonString = """
        {
            "id": "perm-pattern",
            "type": "file-read",
            "pattern": "src/**/*.kt",
            "sessionID": "sess-pat",
            "messageID": "msg-pat",
            "title": "Read file matching pattern",
            "time": 1712345678
        }
        """.trimIndent()

        val permission = json.decodeFromString<Permission>(jsonString)

        assertEquals("src/**/*.kt", permission.pattern)
    }
}