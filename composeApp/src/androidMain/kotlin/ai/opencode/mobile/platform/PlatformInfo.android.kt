package ai.opencode.mobile.platform

actual class PlatformInfo actual constructor() {
    actual val name: String = "Android"
    actual val isDarkModeSupported: Boolean = true
}

actual fun getPlatformInfo(): PlatformInfo = PlatformInfo()

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000