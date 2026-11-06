"use strict";

// Daftar komprehensif jaringan iklan global, regional, pop-under, dan iklan situs streaming (Gaya Brave Shields)
const BLOCKED_DOMAINS = [
  "doubleclick.net",
  "googlesyndication.com",
  "googleadservices.com",
  "adservice.google.com",
  "google-analytics.com",
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
  "highperformanceformat.com",
  "profitabledisplaynetwork.com",
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
  "clarity.ms",
  "mc.yandex.ru",
  "an.yandex.ru",
  "adrecover.com",
  "fundingchoicesmessages.google.com",
  "getadmiral.com",
  "consensu.org",
  "onetrust.com",
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
  "adthrive.com",
  "mediavine.com",
  "monumetric.com",
  "playwire.com",
  "vidoomy.com",
  "viads.com",
  "spotxchange.com",
  "springserve.com",
  "improvedigital.com",
  "indexexchange.com",
  "33across.com",
  "sovrn.com",
  "lijit.com",
  "sharethrough.com",
  "triplelift.com",
  "gumgum.com",
  "kargo.com",
  "nativo.com",
  "zemanta.com",
  "connatix.com",
  "primis.tech",
  "aniview.com",
  "brid.tv",
  "jwpltx.com"
];

// Kata kunci heuristik untuk memblokir iklan slot/judi/banner/pop-under pihak ketiga di situs streaming Indonesia
const THIRD_PARTY_AD_KEYWORDS = [
  "/pagead/",
  "/adserver",
  "/adframe",
  "/adbanner",
  "/ads/",
  "/adv/",
  "/banner/",
  "/banners/",
  "/popunder",
  "/pop_under",
  "/pop.js",
  "/vast.xml",
  "/vmap.xml",
  "/preroll",
  "slotgacor",
  "slot-gacor",
  "slot88",
  "maxwin",
  "togel",
  "sbobet",
  "idnplay",
  "pkvgames",
  "pragmatic",
  "joker123",
  "judol",
  "zeus88",
  "hoki88",
  "cuan88",
  "gacor88",
  "bola88",
  "poker88"
];

// TLD yang sangat sering dipakai oleh jaringan iklan pop-under rotasi otomatis
const SUSPICIOUS_THIRD_PARTY_TLDS = [
  ".icu",
  ".cfd",
  ".sbs",
  ".buzz",
  ".loan",
  ".men",
  ".date",
  ".click"
];

// Pola URL script anti-adblock / bait yang dinetralkan dengan stub 200 OK
const BAIT_SCRIPT_PATTERNS = [
  "adsbygoogle.js",
  "show_ads.js",
  "fuckadblock",
  "blockadblock",
  "adblock-detector",
  "adblockdetector",
  "ads-beacon.js",
  "prebid",
  "admiral"
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

function getRootDomain(hostname) {
  if (!hostname) return "";
  const parts = hostname.toLowerCase().split(".");
  if (parts.length <= 2) return hostname.toLowerCase();
  return parts.slice(-2).join(".");
}

function isDomainBlocked(hostname) {
  if (!hostname) return false;
  const lower = hostname.toLowerCase();
  for (let i = 0; i < BLOCKED_DOMAINS.length; i++) {
    const d = BLOCKED_DOMAINS[i];
    if (lower === d || lower.endsWith("." + d)) {
      return true;
    }
  }
  return false;
}

function isBaitScript(url) {
  const lower = url.toLowerCase();
  for (let i = 0; i < BAIT_SCRIPT_PATTERNS.length; i++) {
    if (lower.indexOf(BAIT_SCRIPT_PATTERNS[i]) !== -1) {
      return true;
    }
  }
  return false;
}

function hasAdKeyword(url) {
  const lower = url.toLowerCase();
  for (let i = 0; i < THIRD_PARTY_AD_KEYWORDS.length; i++) {
    if (lower.indexOf(THIRD_PARTY_AD_KEYWORDS[i]) !== -1) {
      return true;
    }
  }
  return false;
}

function hasSuspiciousTld(hostname) {
  const lower = hostname.toLowerCase();
  for (let i = 0; i < SUSPICIOUS_THIRD_PARTY_TLDS.length; i++) {
    if (lower.endsWith(SUSPICIOUS_THIRD_PARTY_TLDS[i])) {
      return true;
    }
  }
  return false;
}

browser.webRequest.onBeforeRequest.addListener(
  function(details) {
    const url = details.url || "";
    if (!url.startsWith("http")) {
      return {};
    }

    // 1. Jika script umpan anti-adblock, arahkan ke stub sukses agar tidak terdeteksi
    if (details.type === "script" && isBaitScript(url)) {
      return { redirectUrl: NEUTERED_AD_SCRIPT_DATA_URL };
    }

    let reqHost = "";
    let docHost = "";
    try {
      reqHost = new URL(url).hostname;
      if (details.originUrl || details.documentUrl) {
        docHost = new URL(details.originUrl || details.documentUrl).hostname;
      }
    } catch (e) {
      return {};
    }

    const isThirdParty = docHost && getRootDomain(reqHost) !== getRootDomain(docHost);

    // 2. Blokir navigasi main_frame hanya jika mengarah ke domain iklan / pop-under
    if (details.type === "main_frame") {
      if (isDomainBlocked(reqHost)) {
        return { cancel: true };
      }
      return {};
    }

    // 3. Blokir sub-resource dari domain iklan, kata kunci iklan/judi, atau script/iframe pihak ketiga dari TLD mencurigakan
    if (
      isDomainBlocked(reqHost) ||
      hasAdKeyword(url) ||
      (isThirdParty && hasSuspiciousTld(reqHost) && (details.type === "script" || details.type === "sub_frame" || details.type === "xmlhttprequest"))
    ) {
      if (details.type === "script") {
        return { redirectUrl: NEUTERED_AD_SCRIPT_DATA_URL };
      }
      if (details.type === "image" || details.type === "imageset") {
        return { redirectUrl: TRANSPARENT_PIXEL_DATA_URL };
      }
      if (details.type === "sub_frame") {
        return { redirectUrl: "about:blank" };
      }
      return { cancel: true };
    }

    return {};
  },
  { urls: ["<all_urls>"] },
  ["blocking"]
);
