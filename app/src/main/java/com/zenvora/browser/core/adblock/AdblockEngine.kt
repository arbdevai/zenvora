package com.zenvora.browser.core.adblock

object AdblockEngine {
    private var isNativeLoaded = false

    private val fallbackBlockedDomains = setOf(
        "doubleclick.net",
        "google-analytics.com",
        "googlesyndication.com",
        "adservice.google.com",
        "adnxs.com",
        "advertising.com",
        "scorecardresearch.com",
        "criteo.com",
        "outbrain.com",
        "taboola.com",
        "facebook.com/tr",
        "amazon-adsystem.com",
        "rubiconproject.com",
        "pubmatic.com",
        "casalemedia.com",
        "openx.net",
        "quantserve.com",
        "chartbeat.com",
        "hotjar.com",
        "branch.io",
        "adjust.com",
        "appsflyer.com"
    )

    init {
        try {
            System.loadLibrary("adblock_core")
            isNativeLoaded = true
        } catch (_: UnsatisfiedLinkError) {
            isNativeLoaded = false
        }
    }

    fun isUrlBlocked(url: String): Boolean {
        if (isNativeLoaded) {
            try {
                return nativeShouldBlockUrl(url)
            } catch (_: Exception) {
                // Lanjut ke fallback
            }
        }

        val lowerUrl = url.lowercase()
        return fallbackBlockedDomains.any { domain ->
            lowerUrl.contains(domain)
        }
    }

    private external fun nativeShouldBlockUrl(url: String): Boolean
}
