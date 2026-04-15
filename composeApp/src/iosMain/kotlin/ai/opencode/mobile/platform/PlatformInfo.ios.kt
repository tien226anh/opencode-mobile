package ai.opencode.mobile.platform

actual class PlatformInfo actual constructor() {
    actual val name: String = "iOS"
    actual val isDarkModeSupported: Boolean = true
}

actual fun getPlatformInfo(): PlatformInfo = PlatformInfo()