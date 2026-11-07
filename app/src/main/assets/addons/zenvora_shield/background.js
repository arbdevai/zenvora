"use strict";

// Daftar jaringan iklan global, regional, pop-under, dan iklan situs streaming (O(1) Set Lookup)
const BLOCKED_DOMAINS = new Set([
  "doubleclick.net",
  "googlesyndication.com",
  "googleadservices.com",
  "adservice.google.com",
  "google-analytics.com",
  "googletagservices.com",
  "2mdn.net",
  "adnxs.com",
  "advertising.com",
  "scorecardresearch.com",
  "criteo.com",
  "criteo.net",
  "outbrain.com",
  "taboola.com",
  "amazon-adsystem.com",
  "rubiconproject.com",
  "pubmatic.com",
  "casalemedia.com",
  "openx.net",
  "quantserve.com",
  "popads.net",
  "popcash.net",
  "propellerads.com",
  "propellerclick.com",
  "adsterra.com",
  "adsterratech.com",
  "highperformanceformat.com",
  "profitabledisplaynetwork.com",
  "effectivegatecpm.com",
  "topcreativeformat.com",
  "exoclick.com",
  "exosrv.com",
  "realsrv.com",
  "juicyads.com",
  "hilltopads.net",
  "clickadu.com",
  "clickaine.com",
  "trafficjunky.net",
  "trafficstars.com",
  "trafficfactory.biz",
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
  "adskeeper.co.uk",
  "adskeeper.com",
  "adnow.com",
  "adcash.com",
  "adblade.com",
  "adbooth.com",
  "adreactor.com",
  "adspirit.de",
  "adtelligent.com",
  "vidoomy.com",
  "viads.com",
  "spotxchange.com",
  "springserve.com",
  "onclasrv.com",
  "onclkds.com",
  "acscdn.com",
  "tsyndicate.com",
  "twinrdsrv.com",
  "magsrv.com"
]);

// Kata kunci spesifik untuk memblokir iklan slot/judi/pop-under tanpa merusak path halaman web modern
const AD_URL_KEYWORDS = [
  "/pagead/",
  "/adserver",
  "/adframe",
  "/adbanner",
  "/banner-iklan",
  "/pasang-iklan",
  "/wp-content/plugins/ad-inserter",
  "/wp-content/plugins/advanced-ads",
  "/wp-content/plugins/wp-quads",
  "/wp-content/uploads/iklan",
  "/popunder",
  "/pop_under",
  "slotgacor",
  "slot-gacor",
  "slot88",
  "slot77",
  "maxwin",
  "sbobet",
  "idnplay",
  "pkvgames",
  "joker123",
  "judol",
  "zeus88",
  "hoki88",
  "cuan88",
  "gacor88",
  "mahjongways",
  "kakekzeus",
  "situsgacor",
  "rtp-slot",
  "rtpslot"
];

const BAIT_SCRIPT_PATTERNS = [
  "adsbygoogle.js",
  "show_ads.js",
  "fuckadblock",
  "blockadblock",
  "adblock-detector",
  "adblockdetector",
  "deblocker"
];

const NEUTERED_AD_SCRIPT_DATA_URL =
  "data:application/javascript;base64," +
  btoa(
    "window.canRunAds=true;window.isAdBlockActive=false;window.google_ad_status=1;" +
    "window.adsbygoogle=window.adsbygoogle||[];window.adsbygoogle.loaded=true;" +
    "window.adsbygoogle.push=function(){return 0;};" +
    "window.googletag=window.googletag||{cmd:[],apiReady:true,pubadsReady:true," +
    "defineSlot:function(){return{addService:function(){return this;},setTargeting:function(){return this;}};}," +
    "enableServices:function(){},display:function(){}," +
    "pubads:function(){return{enableSingleRequest:function(){},collapseEmptyDivs:function(){},addEventListener:function(){},setTargeting:function(){return this;},refresh:function(){}};}};"
  );

const TRANSPARENT_PIXEL_DATA_URL =
  "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

// O(1) Domain Suffix Lookup (Sangat Cepat & Tidak Membebani CPU saat uBlock Origin Aktif)
function isDomainBlockedFast(hostname) {
  if (!hostname) return false;
  let current = hostname.toLowerCase();
  if (BLOCKED_DOMAINS.has(current)) return true;
  let dotIndex = current.indexOf(".");
  while (dotIndex !== -1) {
    current = current.substring(dotIndex + 1);
    if (BLOCKED_DOMAINS.has(current)) return true;
    dotIndex = current.indexOf(".");
  }
  return false;
}

function isBaitScript(urlLower) {
  for (let i = 0; i < BAIT_SCRIPT_PATTERNS.length; i++) {
    if (urlLower.indexOf(BAIT_SCRIPT_PATTERNS[i]) !== -1) {
      return true;
    }
  }
  return false;
}

function hasAdKeyword(urlLower) {
  for (let i = 0; i < AD_URL_KEYWORDS.length; i++) {
    if (urlLower.indexOf(AD_URL_KEYWORDS[i]) !== -1) {
      return true;
    }
  }
  return false;
}

// Hanya filter tipe sub-resource iklan (script, sub_frame, image, xmlhttprequest)
// Jangan pernah mencegat main_frame, stylesheet, font, atau media video agar loading web modern & streaming 100% kencang!
browser.webRequest.onBeforeRequest.addListener(
  function(details) {
    const url = details.url || "";
    if (!url.startsWith("http")) {
      return {};
    }

    const urlLower = url.toLowerCase();

    if (details.type === "script" && isBaitScript(urlLower)) {
      return { redirectUrl: NEUTERED_AD_SCRIPT_DATA_URL };
    }

    let reqHost = "";
    try {
      reqHost = new URL(url).hostname;
    } catch (e) {
      return {};
    }

    if (isDomainBlockedFast(reqHost) || hasAdKeyword(urlLower)) {
      if (details.type === "script") {
        return { redirectUrl: NEUTERED_AD_SCRIPT_DATA_URL };
      }
      if (details.type === "image") {
        return { redirectUrl: TRANSPARENT_PIXEL_DATA_URL };
      }
      if (details.type === "sub_frame") {
        return { redirectUrl: "about:blank" };
      }
      return { cancel: true };
    }

    return {};
  },
  {
    urls: ["<all_urls>"],
    types: ["script", "sub_frame", "image", "xmlhttprequest"]
  },
  ["blocking"]
);
