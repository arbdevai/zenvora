"use strict";

(function() {
  // 1. Brave-Style Scriptlet Injection:
  // - Anti-Adblock Defuser
  // - YouTube & Web Background Play Unlocker (Page Visibility Spoofer)
  // - Pop-Under & Click-Hijack Blocker
  // - Clean Video Fullscreen Controller (Tanpa Merusak UI/Elemen YouTube atau Membuat Blank Saat Exit)
  const defuserCode = `
    (function() {
      try {
        // A. Anti-Adblock Bait Spoofing
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

        // B. YouTube & Streaming Background Play Unlocker (Page Visibility API Spoofer)
        // Mencegah YouTube mem-pause video otomatis saat pindah aplikasi atau layar HP dimatikan
        Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'webkitHidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
        Object.defineProperty(document, 'webkitVisibilityState', { get: () => 'visible', configurable: true });

        document.hasFocus = function() { return true; };

        // C. Blokir total window.open pop-under otomatis pada situs streaming
        window.open = function() {
          return null;
        };

        // D. Cegah event visibilitychange (agar YouTube tidak pause di background) & cegah click-hijacking pop-under
        const origAddEventListener = EventTarget.prototype.addEventListener;
        EventTarget.prototype.addEventListener = function(type, listener, options) {
          if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || type === 'mozvisibilitychange') {
            return;
          }
          if ((this === window || this === document || this === document.body || this === document.documentElement) &&
              (type === 'click' || type === 'mousedown' || type === 'touchstart' || type === 'pointerdown' || type === 'mouseup')) {
            const listenerStr = listener ? listener.toString() : '';
            if (listenerStr.includes('window.open') ||
                listenerStr.includes('popunder') ||
                listenerStr.includes('createElement("a")') ||
                listenerStr.includes("createElement('a')") ||
                listenerStr.includes('_blank') ||
                listenerStr.includes('location.href')) {
              return;
            }
          }
          return origAddEventListener.call(this, type, listener, options);
        };

        // E. Hanya bantu tombol fullscreen player pihak ketiga jika BUKAN YouTube dan API fullscreen gagal dipicu
        const isYouTubeHost = location.hostname.includes('youtube.com') || location.hostname.includes('youtu.be');
        if (!isYouTubeHost) {
          const origReqFs = Element.prototype.requestFullscreen || Element.prototype.webkitRequestFullscreen || Element.prototype.mozRequestFullScreen;
          document.addEventListener('click', function(e) {
            try {
              const target = e.target ? e.target.closest('.jw-icon-fullscreen, .vjs-fullscreen-control, .plyr__control[data-plyr="fullscreen"], [aria-label*="Fullscreen"], [title*="Fullscreen"]') : null;
              if (!target || !origReqFs) return;
              setTimeout(function() {
                if (!document.fullscreenElement && !document.webkitFullscreenElement && !document.mozFullScreenElement) {
                  const playerContainer = target.closest('.jwplayer, .video-js, .plyr, [class*="player"]');
                  const videoEl = playerContainer ? playerContainer.querySelector('video') : document.querySelector('video');
                  const fsTarget = playerContainer || videoEl;
                  if (fsTarget) {
                    origReqFs.call(fsTarget).catch(function() {});
                  }
                }
              }, 150);
            } catch (_) {}
          }, true);
        }
      } catch (_) {}
    })();
  `;

  try {
    const script = document.createElement("script");
    script.textContent = defuserCode;
    (document.head || document.documentElement).appendChild(script);
    script.remove();
  } catch (_) {}

  // 2. Suntikkan Cosmetic CSS (WordPress Ad Plugins & Indo-Streaming Banners, Tanpa Mengganggu Layout Video / UI YouTube)
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
      div[class*="floating_ads"],
      div[class*="popup-ad"],
      div[class*="banner-ad"],
      div[class*="banner_ad"],
      div[class*="iklan"],
      div[id*="iklan"],
      div[class*="blox-kln"],
      div[class*="pasang-iklan"],
      div[class*="ads-banner"],
      div[class*="ad-banner"],
      div[class*="code-block-"],
      div[class*="ai-viewports"],
      div[class*="quads-location"],
      div[id^="quads-ad"],
      div[id*="popunder"],
      a[href*="slot"],
      a[href*="gacor"],
      a[href*="togel"],
      a[href*="maxwin"],
      a[href*="sbobet"],
      a[href*="pragmatic"],
      a[href*="mahjong"],
      a[href*="judol"],
      a[href*="heylink.me"],
      a[href*="adsterra"],
      a[href*="propeller"],
      a[href*="exoclick"],
      img[src*="slot"],
      img[src*="gacor"],
      img[src*="maxwin"],
      img[src*="togel"],
      img[src*="banner-iklan"],
      img[src*="/iklan"],
      .ytp-ad-overlay-container,
      .ytp-ad-message-container,
      ytd-promoted-sparkles-web-renderer,
      ytd-display-ad-renderer,
      ytm-promoted-sparkles-web-renderer,
      ytm-companion-ad-renderer {
        display: none !important;
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

      /* Hanya berlaku saat elemen <video> langsung yang diputar fullscreen tanpa kontainer player */
      video:fullscreen,
      video:-moz-full-screen {
        width: 100vw !important;
        height: 100vh !important;
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

  // 3. Pemindai DOM Dinamis (Brave-Style Procedural Filter untuk WordPress & Indo-Streaming)
  const AD_HREF_PATTERNS = [
    "slot", "gacor", "maxwin", "togel", "sbobet", "pragmatic", "idnplay", "pkv",
    "judol", "bet88", "zeus88", "hoki88", "cuan88", "mahjong", "rtp", "popads",
    "propeller", "adsterra", "exoclick", "juicyads", "clickadu", "hilltopads",
    "mgid.com", "doubleclick.net", "googlesyndication.com", "heylink.me",
    "rebrand.ly", "cutt.ly", "bit.ly/", "s.id/", "linktr.ee/", "/pasang-iklan"
  ];

  function cleanDomAndEnhanceVideo() {
    try {
      // Hentikan pemindaian berat saat sedang memutar video fullscreen agar CPU/GPU 100% fokus ke 60fps video
      if (document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement) {
        return;
      }

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
          frame.setAttribute("allow", (allowAttr ? allowAttr + "; " : "") + "fullscreen *; autoplay *; encrypted-media *; picture-in-picture *");
        }
        const frameSrc = (frame.getAttribute("src") || "").toLowerCase();
        for (let j = 0; j < AD_HREF_PATTERNS.length; j++) {
          if (frameSrc.includes(AD_HREF_PATTERNS[j])) {
            frame.style.setProperty("display", "none", "important");
            break;
          }
        }
      }

      // B. Sembunyikan banner WordPress (gambar .gif/.webp dalam <a target="_blank">) & overlay klik transparan
      const elements = document.querySelectorAll("a[href], div[onclick], div[style*='z-index'], img[src*='.gif']");
      for (let i = 0; i < elements.length; i++) {
        const el = elements[i];

        if (el.tagName === "IMG") {
          const src = (el.getAttribute("src") || el.getAttribute("data-src") || "").toLowerCase();
          const parentLink = el.closest("a[href]");
          if (parentLink) {
            const linkHref = (parentLink.getAttribute("href") || "").toLowerCase();
            const isExternal = linkHref.startsWith("http") && !linkHref.includes(location.hostname);
            if (isExternal || src.includes("banner") || src.includes("iklan") || src.includes("slot") || src.includes("gacor")) {
              parentLink.style.setProperty("display", "none", "important");
              el.style.setProperty("display", "none", "important");
              continue;
            }
          }
        }

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

        // Deteksi click-jacking overlay transparan di atas pemutar video (z-index tinggi tanpa teks)
        const styleAttr = (el.getAttribute("style") || "").toLowerCase();
        if (
          el.tagName === "DIV" &&
          (styleAttr.includes("z-index: 999") || styleAttr.includes("z-index:999") || styleAttr.includes("z-index: 2147483647")) &&
          (styleAttr.includes("position: absolute") || styleAttr.includes("position:absolute") || styleAttr.includes("position: fixed") || styleAttr.includes("position:fixed")) &&
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

  setInterval(cleanDomAndEnhanceVideo, 1400);
})();
