package ai.opencode.mobile.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
            "time": {
                "created": 1712345678
            }
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
        assertEquals("ls -la", permission.metadataCommand)
        assertNull(permission.metadataFile)
        assertEquals(1712345678L, permission.timeCreated)
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
            "time": {
                "created": 1712345678
            }
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
            "time": {
                "created": 1712345678
            }
        }
        """.trimIndent()

        val permission = json.decodeFromString<Permission>(jsonString)

        assertEquals("perm-789", permission.id)
        assertEquals("/path/to/file.kt", permission.metadataFile)
        assertNull(permission.metadataCommand)
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
    fun testPermissionDeserialization_withStringPattern() {
        val jsonString = """
        {
            "id": "perm-pattern",
            "type": "file-read",
            "pattern": "src/**/*.kt",
            "sessionID": "sess-pat",
            "messageID": "msg-pat",
            "title": "Read file matching pattern",
            "time": {
                "created": 1712345678
            }
        }
        """.trimIndent()

        val permission = json.decodeFromString<Permission>(jsonString)
        assertNotNull(permission.pattern)
        assertEquals("src/**/*.kt", permission.pattern!!.jsonPrimitive.content)
    }

    @Test
    fun testPermissionDeserialization_withArrayPattern() {
        val jsonString = """
        {
            "id": "perm-arr",
            "type": "file-read",
            "pattern": ["src/**/*.kt", "test/**/*.kt"],
            "sessionID": "sess-arr",
            "messageID": "msg-arr",
            "title": "Read files",
            "time": {
                "created": 1712345678
            }
        }
        """.trimIndent()

        val permission = json.decodeFromString<Permission>(jsonString)
        assertNotNull(permission.pattern)
        // Pattern is a JsonArray — verify it deserializes without error
        assertTrue(permission.pattern!!.jsonArray.size == 2)
    }
}