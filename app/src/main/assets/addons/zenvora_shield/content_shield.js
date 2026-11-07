"use strict";

(function() {
  // 1. Brave-Style Scriptlet Injection:
  // - Anti-Adblock Defuser
  // - YouTube & Web Background Play Unlocker (Page Visibility Spoofer)
  // - Pop-Under & Click-Hijack Blocker
  // - Zero-Loop Fullscreen Controller & Landscape Resize Sync (Memperbaiki JWPlayer/SUPJAV.COM mengecil di tengah-atas)
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
        Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'webkitHidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
        Object.defineProperty(document, 'webkitVisibilityState', { get: () => 'visible', configurable: true });

        document.hasFocus = function() { return true; };

        // C. Blokir total window.open pop-under otomatis pada situs streaming
        window.open = function() {
          return null;
        };

        // D. Cegah event visibilitychange & cegah click-hijacking pop-under
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
                listenerStr.includes("createElement('a')")) {
              return;
            }
          }
          return origAddEventListener.call(this, type, listener, options);
        };

        // E. Sinkronisasi Resize Bertingkat Saat Masuk/Keluar Fullscreen agar JWPlayer/Plyr/Video.js Menghitung Ulang Ukuran Landscape
        let lastFullscreenExitMs = 0;
        const triggerPlayerResizeSync = function() {
          const isNowFs = !!(document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement);
          if (!isNowFs) {
            lastFullscreenExitMs = Date.now();
          }
          [80, 220, 450, 850].forEach(function(delayMs) {
            setTimeout(function() {
              try {
                window.dispatchEvent(new Event('resize'));
              } catch (_) {}
            }, delayMs);
          });
        };

        origAddEventListener.call(document, 'fullscreenchange', triggerPlayerResizeSync, true);
        origAddEventListener.call(document, 'webkitfullscreenchange', triggerPlayerResizeSync, true);
        origAddEventListener.call(document, 'mozfullscreenchange', triggerPlayerResizeSync, true);
        origAddEventListener.call(window, 'orientationchange', triggerPlayerResizeSync, true);

        // F. Jembatan Fullscreen Bebas-Looping:
        // Hanya bantu masuk fullscreen jika saat tombol diklik kondisi BELUM fullscreen dan bukan klik tombol Exit Fullscreen!
        const isYouTubeHost = location.hostname.includes('youtube.com') || location.hostname.includes('youtu.be');
        if (!isYouTubeHost) {
          const origReqFs = Element.prototype.requestFullscreen || Element.prototype.webkitRequestFullscreen || Element.prototype.mozRequestFullScreen;
          origAddEventListener.call(document, 'click', function(e) {
            try {
              // 1. Jika SAAT KLIK sudah berada di mode fullscreen, berarti user ingin KELUAR — JANGAN PERNAH panggil requestFullscreen lagi!
              const isAlreadyFs = !!(document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement);
              if (isAlreadyFs) {
                lastFullscreenExitMs = Date.now();
                return;
              }
              if (Date.now() - lastFullscreenExitMs < 1500) {
                return;
              }

              const target = e.target ? e.target.closest('.jw-icon-fullscreen, .vjs-fullscreen-control, .plyr__control[data-plyr="fullscreen"]') : null;
              if (!target || !origReqFs) return;

              // 2. Cek apakah kontainer player sedang dalam status class fullscreen (JWPlayer / Video.js / Plyr)
              const playerContainer = target.closest('.jwplayer, .video-js, .plyr, [class*="player"]');
              if (playerContainer && (
                  playerContainer.classList.contains('jw-flag-fullscreen') ||
                  playerContainer.classList.contains('vjs-fullscreen') ||
                  playerContainer.classList.contains('plyr--fullscreen-active')
              )) {
                lastFullscreenExitMs = Date.now();
                return;
              }

              setTimeout(function() {
                // Pastikan user tidak baru saja keluar dari fullscreen dalam 1.5 detik terakhir
                if (Date.now() - lastFullscreenExitMs < 1500) return;
                if (!document.fullscreenElement && !document.webkitFullscreenElement && !document.mozFullScreenElement) {
                  const videoEl = playerContainer ? playerContainer.querySelector('video') : document.querySelector('video');
                  const fsTarget = playerContainer || videoEl;
                  if (fsTarget) {
                    origReqFs.call(fsTarget).catch(function() {});
                  }
                }
              }, 160);
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

  // 2. Integrasi Khusus Toko Ekstensi (addons.mozilla.org -> Zenvora Add-ons Store)
  const isAmoStore = location.hostname.includes("addons.mozilla.org");
  if (isAmoStore) {
    const resolveAmoSlug = () => {
      const match = location.pathname.match(/\/addon\/([^/?#]+)/i);
      return match && match[1] ? match[1] : null;
    };

    const transformMozillaStoreForZenvora = () => {
      try {
        if (!document.getElementById("zenvora-amo-style")) {
          const amoStyle = document.createElement("style");
          amoStyle.id = "zenvora-amo-style";
          amoStyle.textContent = `
            .GetFirefoxButton,
            .Notice-firefoxRequired,
            .WrongPlatformWarning,
            .FirefoxPromoBanner,
            [class*="FirefoxPromo"],
            [class*="WrongPlatformWarning"] {
              display: none !important;
            }
            a.AMInstallButton-button,
            button.AMInstallButton-button,
            .InstallButtonWrapper a {
              background-color: #FF6D00 !important;
              background-image: none !important;
              color: #000000 !important;
              font-weight: 800 !important;
              border-radius: 10px !important;
              border: none !important;
              pointer-events: auto !important;
              opacity: 1 !important;
              cursor: pointer !important;
              box-shadow: 0 2px 10px rgba(255, 109, 0, 0.35) !important;
            }
          `;
          (document.head || document.documentElement).appendChild(amoStyle);
        }

        const slug = resolveAmoSlug();
        const fallbackXpiUrl = slug
          ? "https://addons.mozilla.org/firefox/downloads/latest/" + slug + "/latest.xpi"
          : null;

        const buttons = document.querySelectorAll(
          "a.AMInstallButton-button, button.AMInstallButton-button, .InstallButtonWrapper a, a.Button--action, a[href*='.xpi']"
        );
        for (let i = 0; i < buttons.length; i++) {
          const btn = buttons[i];
          btn.removeAttribute("disabled");
          btn.classList.remove("AMInstallButton-button--disabled", "Button--disabled");

          const currentHref = btn.getAttribute("href") || "";
          if (fallbackXpiUrl && (!currentHref || currentHref.includes("mozilla.org/firefox/download") || currentHref === "#")) {
            btn.setAttribute("href", fallbackXpiUrl);
          }

          const txt = (btn.textContent || "").trim();
          if (
            txt.includes("Firefox") ||
            txt.includes("Tambahkan") ||
            txt.includes("Add to") ||
            txt.includes("Download file") ||
            txt.includes("Unduh berkas")
          ) {
            btn.textContent = "Pasang ke Zenvora";
          }
        }

        const headerTitle = document.querySelector(".Header-title, h1.HeroRecommendation-title");
        if (headerTitle && headerTitle.textContent && headerTitle.textContent.includes("Firefox")) {
          headerTitle.textContent = headerTitle.textContent.replace(/Firefox/g, "Zenvora");
        }
      } catch (_) {}
    };

    document.addEventListener(
      "click",
      function(e) {
        try {
          const targetBtn = e.target
            ? e.target.closest(
                "a.AMInstallButton-button, button.AMInstallButton-button, .InstallButtonWrapper a, a[href*='.xpi'], a[href*='/firefox/downloads/']"
              )
            : null;
          if (!targetBtn) return;

          let xpiHref = targetBtn.getAttribute("href") || "";
          const slug = resolveAmoSlug();
          if ((!xpiHref.includes(".xpi") && !xpiHref.includes("/firefox/downloads/")) && slug) {
            xpiHref = "https://addons.mozilla.org/firefox/downloads/latest/" + slug + "/latest.xpi";
          }

          if (xpiHref && (xpiHref.includes(".xpi") || xpiHref.includes("/firefox/downloads/"))) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
            targetBtn.textContent = "Memasang ke Zenvora...";
            const absoluteXpi = xpiHref.startsWith("http")
              ? xpiHref
              : new URL(xpiHref, location.origin).href;
            window.location.assign(absoluteXpi);
          }
        } catch (_) {}
      },
      true
    );

    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", transformMozillaStoreForZenvora, { once: true });
    } else {
      transformMozillaStoreForZenvora();
    }
    setInterval(transformMozillaStoreForZenvora, 500);
  }

  // 3. Suntikkan Cosmetic CSS Presisi (Tanpa Merusak Struktur Layout Web Modern & Memperbaiki Fullscreen JWPlayer/Plyr/Video.js)
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
      div[class*="blox-kln"],
      div[class*="pasang-iklan"],
      div[class*="quads-location"],
      div[id^="quads-ad"],
      a[href*="slotgacor"],
      a[href*="slot-gacor"],
      a[href*="slot88"],
      a[href*="maxwin"],
      a[href*="sbobet"],
      a[href*="judol"],
      a[href*="heylink.me"],
      a[href*="adsterra"],
      a[href*="propeller"],
      a[href*="exoclick"],
      img[src*="banner-iklan"],
      img[src*="/wp-content/uploads/iklan"],
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

      /* Pastikan Video & Pemutar JWPlayer / Plyr / Video.js (Seperti SUPJAV.COM) Mengisi Penuh Layar 100vw x 100vh Saat Fullscreen */
      video:fullscreen,
      video:-moz-full-screen {
        width: 100vw !important;
        height: 100vh !important;
        max-width: 100vw !important;
        max-height: 100vh !important;
        object-fit: contain !important;
        background: #000 !important;
      }

      .jwplayer.jw-flag-fullscreen,
      .jwplayer:fullscreen,
      .jwplayer:-moz-full-screen,
      .video-js.vjs-fullscreen,
      .video-js:fullscreen,
      .video-js:-moz-full-screen,
      .plyr.plyr--fullscreen-active,
      .plyr:fullscreen,
      .plyr:-moz-full-screen {
        position: fixed !important;
        inset: 0 !important;
        width: 100vw !important;
        height: 100vh !important;
        max-width: 100vw !important;
        max-height: 100vh !important;
        margin: 0 !important;
        padding: 0 !important;
        background: #000 !important;
        z-index: 2147483647 !important;
      }

      .jwplayer.jw-flag-fullscreen .jw-aspect,
      .jwplayer:fullscreen .jw-aspect,
      .jwplayer:-moz-full-screen .jw-aspect {
        display: none !important;
        padding-top: 0 !important;
      }

      .jwplayer.jw-flag-fullscreen .jw-wrapper,
      .jwplayer:fullscreen .jw-wrapper,
      .jwplayer:-moz-full-screen .jw-wrapper,
      .jwplayer.jw-flag-fullscreen .jw-media,
      .jwplayer:fullscreen .jw-media,
      .jwplayer:-moz-full-screen .jw-media,
      .jwplayer.jw-flag-fullscreen video,
      .jwplayer:fullscreen video,
      .jwplayer:-moz-full-screen video,
      .video-js.vjs-fullscreen video,
      .video-js:fullscreen video,
      .video-js:-moz-full-screen video,
      .plyr.plyr--fullscreen-active video,
      .plyr:fullscreen video,
      .plyr:-moz-full-screen video {
        position: absolute !important;
        inset: 0 !important;
        width: 100% !important;
        height: 100% !important;
        max-width: 100% !important;
        max-height: 100% !important;
        object-fit: contain !important;
        transform: none !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", injectCosmeticStyle, { once: true });
  } else {
    injectCosmeticStyle();
  }

  // 4. Pemindai Ringan & Aman (Tidak Menghapus div/gambar Web Modern & Tidak Membebani CPU Saat uBlock Origin Aktif)
  const STRICT_AD_HREF_PATTERNS = [
    "slotgacor", "slot-gacor", "slot88", "maxwin", "sbobet", "idnplay", "pkvgames",
    "judol", "zeus88", "hoki88", "cuan88", "popads.net", "propellerads",
    "adsterra", "exoclick", "juicyads", "clickadu", "hilltopads", "heylink.me",
    "/pasang-iklan"
  ];

  function enhanceVideoFramesAndCleanStrictAds() {
    try {
      if (document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement) {
        return;
      }

      // Pastikan semua iframe pemutar video mengizinkan fullscreen penuh
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
      }

      // Hanya sembunyikan tautan yang 100% cocok dengan pola iklan judi/pop-under eksplisit (aman untuk semua web modern)
      const adLinks = document.querySelectorAll("a[href]");
      for (let i = 0; i < adLinks.length; i++) {
        const el = adLinks[i];
        const href = (el.getAttribute("href") || "").toLowerCase();
        if (!href) continue;
        for (let j = 0; j < STRICT_AD_HREF_PATTERNS.length; j++) {
          if (href.includes(STRICT_AD_HREF_PATTERNS[j])) {
            el.style.setProperty("display", "none", "important");
            break;
          }
        }
      }

      // Auto-Skip Video Ads di YouTube
      if (location.hostname.includes("youtube.com")) {
        const skipButtons = document.querySelectorAll(
          ".ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button"
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
      }
    } catch (_) {}
  }

  // Jalankan hanya 3 kali saat halaman dimuat (bukan loop abadi) agar CPU hemat dan tidak bentrok dengan uBlock Origin
  setTimeout(enhanceVideoFramesAndCleanStrictAds, 700);
  setTimeout(enhanceVideoFramesAndCleanStrictAds, 2200);
  setTimeout(enhanceVideoFramesAndCleanStrictAds, 4500);
})();
