package com.zenvora.browser

import android.app.Application
import com.zenvora.browser.core.engine.GeckoRuntimeManager

class ZenvoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Warm-up GeckoRuntime process saat aplikasi dimulai
        GeckoRuntimeManager.getRuntime(this)
    }
}
