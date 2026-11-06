"use strict";

// Daftar domain iklan, tracker, pop-under, dan jaringan iklan situs streaming modern
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
  "SourcePoint",
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
  "coinzilla.com"
];

// Pola URL script anti-adblock / bait yang harus dinetralkan dengan stub palsu (bukan cancel)
// agar <script onload> sukses dan situs tidak mendeteksi adblock
const BAIT_SCRIPT_PATTERNS = [
  "adsbygoogle.js",
  "show_ads.js",
  "fuckadblock",
  "blockadblock",
  "adblock-detector",
  "adblockdetector",
  "ads-beacon.js",
  "prebid"
];

const URL_PATH_PATTERNS = [
  "/pagead/",
  "/adserver/",
  "/adframe.",
  "/adbanner/",
  "/popunder",
  "/pop_under",
  "/vast.xml",
  "/vmap.xml",
  "/preroll"
];

// Stub JS palsu dalam base64 yang menyimulasikan objek iklan berhasil dimuat
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

function isDomainBlocked(hostname) {
  if (!hostname) return false;
  const lower = hostname.toLowerCase();
  for (let i = 0; i < BLOCKED_DOMAINS.length; i++) {
    const d = BLOCKED_DOMAINS[i].toLowerCase();
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

function isBlockedPath(url) {
  const lower = url.toLowerCase();
  for (let i = 0; i < URL_PATH_PATTERNS.length; i++) {
    if (lower.indexOf(URL_PATH_PATTERNS[i]) !== -1) {
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

    // 1. Jika script umpan anti-adblock (seperti adsbygoogle.js), arahkan ke stub sukses
    if (details.type === "script" && isBaitScript(url)) {
      return { redirectUrl: NEUTERED_AD_SCRIPT_DATA_URL };
    }

    let hostname = "";
    try {
      hostname = new URL(url).hostname;
    } catch (e) {
      return {};
    }

    // 2. Blokir navigasi main_frame hanya jika mengarah ke jaringan pop-under/redirect iklan
    if (details.type === "main_frame") {
      if (isDomainBlocked(hostname)) {
        return { cancel: true };
      }
      return {};
    }

    // 3. Untuk sub-resource (script, xhr, iframe, image, media, beacon), blokir jika cocok
    if (isDomainBlocked(hostname) || isBlockedPath(url)) {
      if (details.type === "script") {
        return { redirectUrl: NEUTERED_AD_SCRIPT_DATA_URL };
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
