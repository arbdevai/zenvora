use jni::objects::{JClass, JString};
use jni::sys::jboolean;
use jni::JNIEnv;

static BLOCKED_PATTERNS: &[&str] = &[
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
    "appsflyer.com",
    "/ads/",
    "/ad-banner/",
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
