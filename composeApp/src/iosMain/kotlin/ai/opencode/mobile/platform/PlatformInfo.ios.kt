package ai.opencode.mobile.platform

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual class PlatformInfo actual constructor() {
    actual val name: String = "iOS"
    actual val isDarkModeSupported: Boolean = true
}

actual fun getPlatformInfo(): PlatformInfo = PlatformInfo()

actual fun currentTimeSeconds(): Long = NSDate().timeIntervalSince1970.toLong()

/** On iOS, localhost/127.0.0.1 works fine for the simulator (maps to host). */
actual fun normalizeServerUrl(url: String): String {
    val normalized = url.trim()
    // Auto-prepend http:// if no scheme is present
    return if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
        "http://$normalized"
    } else {
        normalized
    }
}