package ai.opencode.mobile.platform

actual class PlatformInfo actual constructor() {
    actual val name: String = "Android"
    actual val isDarkModeSupported: Boolean = true
}

actual fun getPlatformInfo(): PlatformInfo = PlatformInfo()

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000

actual fun normalizeServerUrl(url: String): String {
    var normalized = url.trim()

    // Auto-prepend http:// if no scheme is present
    if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
        normalized = "http://$normalized"
    }

    // On Android emulators, localhost/127.0.0.1 refers to the emulator itself,
    // not the host machine. Remap to 10.0.2.2 which is the emulator's gateway
    // to the host machine.
    normalized = normalized
        .replace("://localhost:", "://10.0.2.2:")
        .replace("://localhost/", "://10.0.2.2/")
        .replace("://localhost", "://10.0.2.2")     // end of string — must be last
        .replace("://127.0.0.1:", "://10.0.2.2:")
        .replace("://127.0.0.1/", "://10.0.2.2/")
        .replace("://127.0.0.1", "://10.0.2.2")    // end of string

    return normalized
}