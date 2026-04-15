package ai.opencode.mobile.platform

expect class PlatformInfo() {
    val name: String
    val isDarkModeSupported: Boolean
}

expect fun getPlatformInfo(): PlatformInfo