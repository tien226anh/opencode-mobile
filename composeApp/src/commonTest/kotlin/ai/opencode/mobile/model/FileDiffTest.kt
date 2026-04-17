package ai.opencode.mobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileDiffTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun testFileDiffDeserialization() {
        val jsonString = """
        {
            "file": "src/Main.kt",
            "before": "fun main() {}",
            "after": "fun main() { println() }",
            "additions": 1,
            "deletions": 0
        }
        """.trimIndent()

        val diff = json.decodeFromString<FileDiff>(jsonString)
        assertEquals("src/Main.kt", diff.file)
        assertEquals(1, diff.additions)
        assertEquals(0, diff.deletions)
    }

    @Test
    fun testFileDiffDefaultValues() {
        val jsonString = """{ "file": "test.txt" }"""
        val diff = json.decodeFromString<FileDiff>(jsonString)
        assertEquals("test.txt", diff.file)
        assertEquals("", diff.before)
        assertEquals("", diff.after)
        assertEquals(0, diff.additions)
        assertEquals(0, diff.deletions)
    }

    @Test
    fun testSessionDiffResponseDeserialization() {
        val jsonString = """
        {
            "files": [
                { "file": "a.txt", "before": "", "after": "hello", "additions": 1, "deletions": 0 },
                { "file": "b.txt", "before": "world", "after": "", "additions": 0, "deletions": 1 }
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<SessionDiffResponse>(jsonString)
        assertEquals(2, response.files.size)
        assertEquals("a.txt", response.files[0].file)
        assertEquals("b.txt", response.files[1].file)
    }

    @Test
    fun testSessionDiffResponseEmpty() {
        val jsonString = """{ "files": [] }"""
        val response = json.decodeFromString<SessionDiffResponse>(jsonString)
        assertTrue(response.files.isEmpty())
    }
}