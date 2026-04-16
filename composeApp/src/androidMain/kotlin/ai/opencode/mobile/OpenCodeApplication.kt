package ai.opencode.mobile

import android.app.Application
import android.content.SharedPreferences

class OpenCodeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("opencode_settings", MODE_PRIVATE)
    }

    companion object {
        lateinit var prefs: SharedPreferences
            private set
    }
}