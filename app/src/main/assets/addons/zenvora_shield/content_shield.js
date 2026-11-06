"use strict";

(function() {
  // 1. Suntikkan Stealth Anti-Adblock Defuser ke page context pada document_start
  const defuserCode = `
    (function() {
      try {
        Object.defineProperty(window, 'canRunAds', { get: () => true, set: () => {}, configurable: true });
        Object.defineProperty(window, 'isAdBlockActive', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'adblock', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'adBlockEnabled', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'google_ad_status', { get: () => 1, set: () => {}, configurable: true });

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

        // Blokir window.open pop-under otomatis pada situs streaming
        const originalOpen = window.open;
        window.open = function(url, target, features) {
          if (!url || typeof url !== 'string') return null;
          const lower = url.toLowerCase();
          if (
            lower.includes('popads') ||
            lower.includes('propeller') ||
            lower.includes('adsterra') ||
            lower.includes('exoclick') ||
            lower.includes('juicyads') ||
            lower.includes('clickadu') ||
            lower.includes('hilltopads') ||
            lower.includes('doubleclick') ||
            lower.includes('googlesyndication') ||
            lower === 'about:blank'
          ) {
            return null;
          }
          return originalOpen ? originalOpen.call(window, url, target, features) : null;
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

  // 2. Suntikkan Cosmetic CSS yang menyembunyikan iframe/overlay iklan tanpa memicu detektor offsetHeight === 0 pada bait .adsbox
  const injectCosmeticStyle = () => {
    if (document.getElementById("zenvora-stealth-css")) return;
    const style = document.createElement("style");
    style.id = "zenvora-stealth-css";
    style.textContent = `
      iframe[src*="doubleclick.net"],
      iframe[src*="googlesyndication.com"],
      iframe[src*="adnxs.com"],
      iframe[src*="exoclick.com"],
      iframe[src*="adsterra"],
      iframe[src*="propeller"],
      iframe[src*="juicyads"],
      iframe[src*="popads"],
      iframe[src*="mgid.com"],
      iframe[src*="taboola.com"],
      iframe[src*="outbrain.com"],
      ins.adsbygoogle,
      div[id^="div-gpt-ad"],
      div[id^="google_ads_iframe"],
      div[class*="floating-ads"],
      div[class*="popup-ad"],
      div[id*="popunder"],
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
        overflow: hidden !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", injectCosmeticStyle, { once: true });
  } else {
    injectCosmeticStyle();
  }

  // 3. Auto-Skip Video Ads (YouTube & HTML5 Pre-roll Ads)
  setInterval(() => {
    try {
      const skipButtons = document.querySelectorAll(
        ".ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .videoAdUiSkipButton, [class*='skip-ad']"
      );
      skipButtons.forEach((btn) => {
        if (btn && typeof btn.click === "function") {
          btn.click();
        }
      });

      const adShowingVideo = document.querySelector(".ad-showing video, .ad-interrupting video");
      if (adShowingVideo && !isNaN(adShowingVideo.duration) && adShowingVideo.duration > 0) {
        adShowingVideo.currentTime = adShowingVideo.duration;
        adShowingVideo.muted = true;
      }
    } catch (_) {}
  }, 800);
})();
