package com.zenvora.browser.core.engine

import android.app.Application
import android.content.Context
import android.os.Build
import com.zenvora.browser.core.network.DnsProvider
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File

object GeckoRuntimeManager {
    private const val CONFIG_FILE_NAME = "gecko-config.yaml"
    private const val SHIELD_EXTENSION_URI = "resource://android/assets/addons/zenvora_shield/"
    private const val SHIELD_EXTENSION_ID = "zenvora-shield@zenvora.browser"

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

    fun writeGeckoConfigFile(
        context: Context,
        dnsProvider: DnsProvider = DnsProvider.CLOUDFLARE,
        isTurboEnabled: Boolean = true,
        videoCodecMode: VideoCodecMode = VideoCodecMode.AUTO_HARDWARE,
        isBackgroundAudioEnabled: Boolean = true
    ): File {
        val configFile = File(context.filesDir, CONFIG_FILE_NAME)
        val trrMode = if (dnsProvider == DnsProvider.SYSTEM) 0 else 3
        val trrUri = if (dnsProvider == DnsProvider.SYSTEM) {
            "https://1.1.1.1/dns-query"
        } else {
            dnsProvider.dohEndpoint
        }
        val bootstrapIp = if (dnsProvider == DnsProvider.SYSTEM) {
            "1.1.1.1"
        } else {
            dnsProvider.primaryIp
        }
        val maxConnections = if (isTurboEnabled) 900 else 256
        val maxPerServer = if (isTurboEnabled) 12 else 6

        val hwCaps = HardwareCodecDetector.detectCapabilities()
        val enableVp9 = hwCaps.shouldEnableVp9(videoCodecMode)
        val enableAv1 = hwCaps.shouldEnableAv1(videoCodecMode)
        val enableWhenInBackground = isBackgroundAudioEnabled

        val yamlContent = """
            prefs:
              network.trr.mode: $trrMode
              network.trr.uri: "$trrUri"
              network.trr.custom_uri: "$trrUri"
              network.trr.bootstrapAddress: "$bootstrapIp"
              network.trr.default_provider_uri: "$trrUri"
              network.trr.wait-for-portal: false
              network.trr.allow-rfc1918: true
              network.trr.useGET: true
              network.trr.confirmationNS: "skip"
              network.dns.disablePrefetch: false
              network.dns.disablePrefetchFromHTTPS: false
              network.dnsCacheEntries: 2048
              network.dnsCacheExpiration: 3600
              network.http.http3.enabled: true
              network.http.max-connections: $maxConnections
              network.http.max-persistent-connections-per-server: $maxPerServer
              network.http.pacing.requests.enabled: false
              network.predictor.enabled: true
              network.predictor.enable-prefetch: true
              security.OCSP.enabled: 0
              dom.disable_open_during_load: true
              full-screen-api.enabled: true
              full-screen-api.allow-trusted-requests-only: false
              full-screen-api.unprefix.enabled: true
              media.hardware-video-decoding.enabled: true
              media.hardware-video-decoding.force-enabled: true
              media.navigator.mediadatadecoder_vpx_enabled: $enableVp9
              media.mediasource.vp9.enabled: $enableVp9
              media.av1.enabled: $enableAv1
              media.ffvpx.enabled: $enableVp9
              media.suspend-bkgnd-video.enabled: false
              media.UtilityProcess.enabled: true
              dom.audiochannel.mediaControl: true
              media.geckoview.enable-When-In-Background: $enableWhenInBackground
              gfx.webrender.all: true
              layers.acceleration.force-enabled: true
              layout.frame_rate: 60
        """.trimIndent()

        configFile.writeText(yamlContent)
        return configFile
    }

    private fun createRuntime(context: Context): GeckoRuntime {
        val prefsFile = writeGeckoConfigFile(
            context = context,
            dnsProvider = DnsProvider.CLOUDFLARE,
            isTurboEnabled = true,
            videoCodecMode = VideoCodecMode.AUTO_HARDWARE,
            isBackgroundAudioEnabled = true
        )

        val settings = GeckoRuntimeSettings.Builder()
            .configFilePath(prefsFile.absolutePath)
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
                    .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                    .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                    .build()
            )
            .build()

        val createdRuntime = GeckoRuntime.create(context, settings)

        // Pasang Built-In Stealth Adblock & Anti-Adblock Defuser WebExtension
        try {
            createdRuntime.webExtensionController.ensureBuiltIn(
                SHIELD_EXTENSION_URI,
                SHIELD_EXTENSION_ID
            )
        } catch (_: Exception) {
            // Abaikan jika gagal memuat ekstensi pada mode terbatas
        }

        return createdRuntime
    }
}
