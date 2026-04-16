package ai.opencode.mobile.platform

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual class PlatformInfo actual constructor() {
    actual val name: String = "iOS"
    actual val isDarkModeSupported: Boolean = true
}

actual fun getPlatformInfo(): PlatformInfo = PlatformInfo()

actual fun currentTimeSeconds(): Long = NSDate().timeIntervalSince1970.toLong()