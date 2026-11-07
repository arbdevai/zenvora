"use strict";

// 1. Jembatan Komando Internal Zenvora (Bebas CSP 100% Lintas-Tab & Lintas-Iframe)
// Menangkap sinyal dari commandSession (https://zenvora.cmd.local/exec?cmd=...) dan meneruskannya ke seluruh frame via browser.tabs.sendMessage
browser.webRequest.onBeforeRequest.addListener(
  function(details) {
    try {
      const parsed = new URL(details.url);
      const cmd = parsed.searchParams.get("cmd") || "";
      const mode = parsed.searchParams.get("mode") || "";
      const user = parsed.searchParams.get("user") || "";
      const pass = parsed.searchParams.get("pass") || "";

      if (cmd) {
        browser.tabs.query({}).then(function(tabs) {
          for (let i = 0; i < tabs.length; i++) {
            const tabId = tabs[i].id;
            if (typeof tabId === "number") {
              browser.tabs.sendMessage(tabId, {
                zenvoraCmd: cmd,
                mode: mode,
                user: user,
                pass: pass
              }).catch(function() {});
            }
          }
        }).catch(function() {});
      }
    } catch (_) {}
    return { cancel: true };
  },
  {
    urls: ["https://zenvora.cmd.local/*"]
  },
  ["blocking"]
);

// 2. Daftar Jaringan Iklan Global, Regional, Pop-Under, Push & Tracker (O(1) Set Lookup)
const BLOCKED_DOMAINS = new Set([
  "doubleclick.net",
  "googlesyndication.com",
  "googleadservices.com",
  "adservice.google.com",
  "google-analytics.com",
  "googletagservices.com",
  "pagead2.googlesyndication.com",
  "fundingchoicesmessages.google.com",
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
  "ads-twitter.com",
  "ads.yahoo.com",
  "ads.linkedin.com",
  "adroll.com",
  "sharethrough.com",
  "triplelift.com",
  "indexexchange.com",
  "contextweb.com",
  "lijit.com",
  "sovrn.com",
  "gumgum.com",
  "kargo.com",
  "yieldmo.com",
  "33across.com",
  "conversantmedia.com",
  "mediavine.com",
  "adthrive.com",
  "ezoic.net",
  "freestar.io",
  "snigelweb.com",
  "playwire.com",
  "venatusmedia.com",
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
  "magsrv.com",
  "rollerads.com",
  "pushengage.com",
  "notix.io",
  "propush.me",
  "monetag.com",
  "alwingulla.com",
  "weeghoof.com",
  "glugreez.com",
  "auglupti.com",
  "whoursie.com",
  "groleegni.net",
  "zytpirwai.net",
  "psaughun.com",
  "dooloust.net",
  "heylink.me"
]);

// TLD yang 99% dipakai oleh jaringan script pop-under pihak ketiga (Adsterra / Monetag rotating domains)
const SUSPICIOUS_THIRD_PARTY_AD_TLDS = [
  ".icu",
  ".cfd",
  ".sbs",
  ".buzz",
  ".loan",
  ".men",
  ".hair",
  ".skin",
  ".rest"
];

// Kata kunci spesifik untuk memblokir iklan slot/judi/pop-under tanpa merusak path halaman web modern
const AD_URL_KEYWORDS = [
  "/pagead/",
  "/adserver",
  "/adframe",
  "/adbanner",
  "/banner-iklan",
  "/iklan-banner",
  "/pasang-iklan",
  "/space-iklan",
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
  "pragmaticplay",
  "pgsoft",
  "judol",
  "zeus88",
  "hoki88",
  "cuan88",
  "gacor88",
  "rajacuan",
  "mahjongways",
  "kakekzeus",
  "situsgacor",
  "garudagacor",
  "dewaslot",
  "rajaslot",
  "ligaciputra",
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

// O(1) Domain Suffix Lookup (Sangat Cepat & Tidak Membebani CPU)
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

function isSuspiciousThirdPartyAdScript(reqHost, originUrl, resourceType) {
  if (resourceType !== "script" && resourceType !== "sub_frame") return false;
  if (!reqHost) return false;
  const hostLower = reqHost.toLowerCase();
  let matchedTld = false;
  for (let i = 0; i < SUSPICIOUS_THIRD_PARTY_AD_TLDS.length; i++) {
    if (hostLower.endsWith(SUSPICIOUS_THIRD_PARTY_AD_TLDS[i])) {
      matchedTld = true;
      break;
    }
  }
  if (!matchedTld) return false;
  if (!originUrl) return true;
  try {
    const docHost = new URL(originUrl).hostname.toLowerCase();
    return docHost !== hostLower && !hostLower.endsWith("." + docHost);
  } catch (_) {
    return true;
  }
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
    if (!url.startsWith("http") || url.startsWith("https://zenvora.")) {
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

    const originUrl = details.originUrl || details.documentUrl || "";

    if (
      isDomainBlockedFast(reqHost) ||
      hasAdKeyword(urlLower) ||
      isSuspiciousThirdPartyAdScript(reqHost, originUrl, details.type)
    ) {
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
