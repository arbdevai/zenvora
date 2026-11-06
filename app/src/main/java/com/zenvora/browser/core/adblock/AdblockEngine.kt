package com.zenvora.browser.core.adblock

object AdblockEngine {
    private var isNativeLoaded = false

    private val fallbackBlockedDomains = setOf(
        "doubleclick.net",
        "google-analytics.com",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "googletagmanager.com",
        "googletagservices.com",
        "2mdn.net",
        "adnxs.com",
        "advertising.com",
        "scorecardresearch.com",
        "criteo.com",
        "criteo.net",
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
        "appsflyer.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "propellerclick.com",
        "adsterra.com",
        "adsterratech.com",
        "exoclick.com",
        "exosrv.com",
        "juicyads.com",
        "hilltopads.net",
        "clickadu.com",
        "trafficjunky.net",
        "trafficstars.com",
        "mgid.com",
        "revcontent.com",
        "bidvertiser.com",
        "adform.net",
        "smartadserver.com",
        "teads.tv",
        "moatads.com",
        "zedo.com",
        "histats.com",
        "statcounter.com",
        "clarity.ms",
        "mc.yandex.ru",
        "an.yandex.ru",
        "adrecover.com",
        "fundingchoicesmessages.google.com",
        "getadmiral.com",
        "bidgear.com",
        "galaksion.com",
        "pushground.com",
        "richpush.co",
        "evadav.com",
        "admaven.com",
        "adhitz.com",
        "adxpansion.com",
        "plugrush.com",
        "ero-advertising.com",
        "a-ads.com",
        "coinzilla.com"
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
