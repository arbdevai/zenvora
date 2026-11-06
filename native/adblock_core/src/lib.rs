use jni::objects::{JClass, JString};
use jni::sys::jboolean;
use jni::JNIEnv;

static BLOCKED_PATTERNS: &[&str] = &[
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
    "coinzilla.com",
    "/ads/",
    "/ad-banner/",
    "/adserver/",
    "/popunder",
    "tracker.",
    "telemetry.",
];

#[no_mangle]
pub extern "system" fn Java_com_zenvora_browser_core_adblock_AdblockEngine_nativeShouldBlockUrl(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jboolean {
    let url_str: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    let lower_url = url_str.to_lowercase();
    for pattern in BLOCKED_PATTERNS {
        if lower_url.contains(pattern) {
            return 1;
        }
    }

    0
}
