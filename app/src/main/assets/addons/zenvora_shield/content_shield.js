"use strict";

(function() {
  // 1. Brave-Style Scriptlet Injection: Anti-Adblock Defuser + Pop-Under & Click-Hijack Blocker
  const defuserCode = `
    (function() {
      try {
        Object.defineProperty(window, 'canRunAds', { get: () => true, set: () => {}, configurable: true });
        Object.defineProperty(window, 'isAdBlockActive', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'adblock', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'adBlockEnabled', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'google_ad_status', { get: () => 1, set: () => {}, configurable: true });
        Object.defineProperty(window, 'google_jobrunner', { get: () => ({}), set: () => {}, configurable: true });

        window.adsbygoogle = window.adsbygoogle || [];
        window.adsbygoogle.loaded = true;
        window.adsbygoogle.push = function() { return 0; };

        const FakeDetector = function() {};
        FakeDetector.prototype.check = function() { return false; };
        FakeDetector.prototype.emitEvent = function() { return this; };
        FakeDetector.prototype.clearEvent = function() {};
        FakeDetector.prototype.on = function(detected, fn) {
          if (!detected && typeof fn === 'function') {
            try { fn(); } catch (_) {}
          }
          return this;
        };
        FakeDetector.prototype.onDetected = function() { return this; };
        FakeDetector.prototype.onNotDetected = function(fn) {
          if (typeof fn === 'function') {
            try { fn(); } catch (_) {}
          }
          return this;
        };
        const fakeInstance = new FakeDetector();
        Object.defineProperty(window, 'FuckAdBlock', { get: () => FakeDetector, set: () => {}, configurable: true });
        Object.defineProperty(window, 'BlockAdBlock', { get: () => FakeDetector, set: () => {}, configurable: true });
        Object.defineProperty(window, 'fuckAdBlock', { get: () => fakeInstance, set: () => {}, configurable: true });
        Object.defineProperty(window, 'blockAdBlock', { get: () => fakeInstance, set: () => {}, configurable: true });

        // Blokir total window.open pop-under otomatis pada situs streaming
        window.open = function() {
          return null;
        };

        // Cegah script pop-under menangkap event click pada document/window untuk membuka iklan
        const origAddEventListener = EventTarget.prototype.addEventListener;
        EventTarget.prototype.addEventListener = function(type, listener, options) {
          if ((this === window || this === document || this === document.body || this === document.documentElement) &&
              (type === 'click' || type === 'mousedown' || type === 'touchstart' || type === ' pointerdown')) {
            const listenerStr = listener ? listener.toString() : '';
            if (listenerStr.includes('window.open') ||
                listenerStr.includes('popunder') ||
                listenerStr.includes('createElement("a")') ||
                listenerStr.includes("createElement('a')") ||
                listenerStr.includes('_blank')) {
              return;
            }
          }
          return origAddEventListener.call(this, type, listener, options);
        };
      } catch (_) {}
    })();
  `;

  try {
    const script = document.createElement("script");
    script.textContent = defuserCode;
    (document.head || document.documentElement).appendChild(script);
    script.remove();
  } catch (_) {}

  // 2. Suntikkan Cosmetic CSS + Aturan Fullscreen Video Universal
  const injectCosmeticStyle = () => {
    if (document.getElementById("zenvora-stealth-css")) return;
    const style = document.createElement("style");
    style.id = "zenvora-stealth-css";
    style.textContent = `
      iframe[src*="doubleclick.net"],
      iframe[src*="googlesyndication.com"],
      iframe[src*="adnxs.com"],
      iframe[src*="exoclick.com"],
      iframe[src*="exosrv.com"],
      iframe[src*="adsterra"],
      iframe[src*="propeller"],
      iframe[src*="juicyads"],
      iframe[src*="popads"],
      iframe[src*="mgid.com"],
      iframe[src*="taboola.com"],
      iframe[src*="outbrain.com"],
      iframe[src*="hilltopads"],
      iframe[src*="clickadu"],
      ins.adsbygoogle,
      div[id^="div-gpt-ad"],
      div[id^="google_ads_iframe"],
      div[class*="floating-ads"],
      div[class*="popup-ad"],
      div[class*="banner-ad"],
      div[class*="iklan"],
      div[id*="iklan"],
      div[id*="popunder"],
      a[href*="slot"],
      a[href*="gacor"],
      a[href*="togel"],
      a[href*="maxwin"],
      a[href*="sbobet"],
      a[href*="pragmatic"],
      a[href*="adsterra"],
      a[href*="propeller"],
      a[href*="exoclick"],
      .ytp-ad-overlay-container,
      .ytp-ad-message-container,
      ytd-promoted-sparkles-web-renderer,
      ytd-display-ad-renderer,
      ytm-promoted-sparkles-web-renderer,
      ytm-companion-ad-renderer {
        visibility: hidden !important;
        opacity: 0 !important;
        pointer-events: none !important;
        max-height: 0px !important;
        min-height: 0px !important;
        height: 0px !important;
        margin: 0px !important;
        padding: 0px !important;
        overflow: hidden !important;
      }

      /* Kompatibilitas penuh untuk semua pemutar video di web streaming saat Fullscreen */
      video:fullscreen,
      video:-moz-full-screen,
      *:fullscreen video,
      *:-moz-full-screen video {
        width: 100vw !important;
        height: 100vh !important;
        max-width: 100vw !important;
        max-height: 100vh !important;
        margin: 0 !important;
        padding: 0 !important;
        background: #000 !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", injectCosmeticStyle, { once: true });
  } else {
    injectCosmeticStyle();
  }

  // 3. Pemindai DOM Dinamis (Brave-Style Procedural Filter) + Universal Streaming Player Fixer
  const AD_HREF_PATTERNS = [
    "slot", "gacor", "maxwin", "togel", "sbobet", "pragmatic", "idnplay", "pkv",
    "judol", "bet88", "zeus88", "hoki88", "cuan88", "popads", "propeller",
    "adsterra", "exoclick", "juicyads", "clickadu", "hilltopads", "mgid.com",
    "doubleclick.net", "googlesyndication.com", "bit.ly/slot", "rebrand.ly/slot",
    "cutt.ly/", "tinyurl.com/slot"
  ];

  function cleanDomAndEnhanceVideo() {
    try {
      // A. Pastikan semua iframe pemutar video mengizinkan fullscreen
      const iframes = document.querySelectorAll("iframe");
      for (let i = 0; i < iframes.length; i++) {
        const frame = iframes[i];
        if (!frame.hasAttribute("allowfullscreen")) {
          frame.setAttribute("allowfullscreen", "true");
          frame.setAttribute("webkitallowfullscreen", "true");
          frame.setAttribute("mozallowfullscreen", "true");
        }
        const allowAttr = frame.getAttribute("allow") || "";
        if (!allowAttr.includes("fullscreen")) {
          frame.setAttribute("allow", (allowAttr ? allowAttr + "; " : "") + "fullscreen; autoplay; encrypted-media; picture-in-picture");
        }
      }

      // B. Sembunyikan banner/tautan iklan & judi serta overlay transparan di atas video player
      const links = document.querySelectorAll("a[href], div[onclick], div[style*='z-index']");
      for (let i = 0; i < links.length; i++) {
        const el = links[i];
        const href = (el.getAttribute("href") || el.getAttribute("onclick") || "").toLowerCase();

        let isAdLink = false;
        for (let j = 0; j < AD_HREF_PATTERNS.length; j++) {
          if (href.includes(AD_HREF_PATTERNS[j])) {
            isAdLink = true;
            break;
          }
        }

        if (isAdLink) {
          el.style.setProperty("display", "none", "important");
          el.style.setProperty("pointer-events", "none", "important");
          continue;
        }

        // Deteksi click-jacking overlay transparan di atas pemutar video (z-index > 999 tanpa teks/kontrol)
        const styleAttr = (el.getAttribute("style") || "").toLowerCase();
        if (
          el.tagName === "DIV" &&
          (styleAttr.includes("z-index: 999") || styleAttr.includes("z-index:999") || styleAttr.includes("z-index: 2147483647")) &&
          (styleAttr.includes("position: absolute") || styleAttr.includes("position:absolute") || styleAttr.includes("position: fixed")) &&
          el.children.length === 0 &&
          (el.textContent || "").trim() === ""
        ) {
          el.style.setProperty("display", "none", "important");
          el.style.setProperty("pointer-events", "none", "important");
        }
      }

      // C. Auto-Skip Video Ads (YouTube & HTML5 Pre-roll Ads)
      const skipButtons = document.querySelectorAll(
        ".ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .videoAdUiSkipButton, [class*='skip-ad'], [id*='skip-ad']"
      );
      for (let i = 0; i < skipButtons.length; i++) {
        const btn = skipButtons[i];
        if (btn && typeof btn.click === "function") {
          btn.click();
        }
      }

      const adShowingVideo = document.querySelector(".ad-showing video, .ad-interrupting video");
      if (adShowingVideo && !isNaN(adShowingVideo.duration) && adShowingVideo.duration > 0) {
        adShowingVideo.currentTime = adShowingVideo.duration;
        adShowingVideo.muted = true;
      }
    } catch (_) {}
  }

  setInterval(cleanDomAndEnhanceVideo, 900);
})();
