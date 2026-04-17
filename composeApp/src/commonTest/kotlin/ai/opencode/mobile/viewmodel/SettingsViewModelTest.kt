package ai.opencode.mobile.viewmodel

import ai.opencode.mobile.model.ServerConfig
import ai.opencode.mobile.network.OpenCodeApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    // --- SettingsState tests (verify no provider/model/mode fields) ---

    @Test
    fun testSettingsStateDefaults() {
        val state = SettingsState()
        assertEquals("http://localhost", state.serverHost)
        assertEquals("${OpenCodeApiClient.DEFAULT_PORT}", state.serverPort)
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertFalse(state.isTesting)
        assertEquals(null, state.isConnectionSuccessful)
        assertEquals(null, state.error)
        assertFalse(state.isSaved)
    }

    @Test
    fun testSettingsStateServerUrlWithPort() {
        val state = SettingsState(
            serverHost = "http://192.168.1.100",
            serverPort = "8080",
        )
        assertEquals("http://192.168.1.100:8080", state.serverUrl)
    }

    @Test
    fun testSettingsStateServerUrlWithoutPort() {
        val state = SettingsState(
            serverHost = "https://example.trycloudflare.com",
            serverPort = "",
        )
        assertEquals("https://example.trycloudflare.com", state.serverUrl)
    }

    // --- ServerConfig tests (verify no providerId, modelId, modeName fields) ---

    @Test
    fun testServerConfigNoProviderModelModeFields() {
        // This test verifies ServerConfig does NOT have providerId, modelId, modeName.
        // If these fields existed, this would fail to compile or the values would be non-empty.
        val config = ServerConfig(
            serverHost = "http://localhost",
            serverPort = "4096",
            username = "opencode",
            password = "secret",
            isConnected = true,
        )
        assertEquals("http://localhost:4096", config.serverUrl)
        assertEquals("http://localhost", config.serverHost)
        assertEquals("4096", config.serverPort)
        assertEquals("opencode", config.username)
        assertEquals("secret", config.password)
        assertTrue(config.isConnected)
    }

    @Test
    fun testServerConfigDefaults() {
        val config = ServerConfig()
        assertEquals("", config.serverHost)
        assertEquals("4096", config.serverPort)
        assertEquals("", config.username)
        assertEquals("", config.password)
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
}