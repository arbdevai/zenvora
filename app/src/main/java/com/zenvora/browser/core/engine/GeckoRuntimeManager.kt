package com.zenvora.browser.core.engine

import android.app.Application
import android.content.Context
import android.os.Build
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

object GeckoRuntimeManager {
    @Volatile
    private var runtime: GeckoRuntime? = null

    fun getRuntime(context: Context): GeckoRuntime {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = Application.getProcessName()
            check(!processName.contains(":")) {
                "GeckoRuntime hanya boleh diinisialisasi di proses utama, bukan di child process ($processName)"
            }
        }
        return runtime ?: synchronized(this) {
            runtime ?: createRuntime(context.applicationContext).also {
                runtime = it
            }
        }
    }

    private fun createRuntime(context: Context): GeckoRuntime {
        val settings = GeckoRuntimeSettings.Builder()
            .aboutConfigEnabled(false)
            .automaticFontSizeAdjustment(true)
            .consoleOutput(false)
            .contentBlocking(
                ContentBlocking.Settings.Builder()
                    .antiTracking(
                        ContentBlocking.AntiTracking.DEFAULT or
                            ContentBlocking.AntiTracking.STRICT or
                            ContentBlocking.AntiTracking.FINGERPRINTING or
                            ContentBlocking.AntiTracking.CRYPTOMINING
                    )
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                    .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                    .build()
            )
            .build()

        return GeckoRuntime.create(context, settings)
    }
}
