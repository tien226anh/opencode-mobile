package ai.opencode.mobile.platform

expect class PlatformInfo() {
    val name: String
    val isDarkModeSupported: Boolean
}

expect fun getPlatformInfo(): PlatformInfo

/** Returns current time in seconds since epoch (cross-platform) */
expect fun currentTimeSeconds(): Long

/**
 * Normalizes a server URL for the current platform.
 *
 * On Android emulators, `localhost` and `127.0.0.1` refer to the emulator itself,
 * not the host machine. This function remaps them to `10.0.2.2` (the emulator's
 * special IP for the host) so the app can reach servers running on the developer's
 * machine.
 *
 * On physical devices and iOS, the URL is returned unchanged — users must use their
 * computer's LAN IP (e.g. `192.168.x.x`) to connect.
 */
expect fun normalizeServerUrl(url: String): String