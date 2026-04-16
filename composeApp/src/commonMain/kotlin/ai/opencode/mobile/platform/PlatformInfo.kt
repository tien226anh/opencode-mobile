package ai.opencode.mobile.platform

expect class PlatformInfo() {
    val name: String
    val isDarkModeSupported: Boolean
}

expect fun getPlatformInfo(): PlatformInfo

/** Returns current time in seconds since epoch (cross-platform) */
expect fun currentTimeSeconds(): Long