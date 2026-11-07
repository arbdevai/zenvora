"use strict";

(function() {
  // 1. Injeksi Inti Zenvora (Ringan, 100% Murni Tanpa Merusak Layout Web Modern):
  // - Anti-Adblock Bait Spoofing
  // - YouTube & Web Background Play Unlocker
  // - Detektor Stream Video (.m3u8 / .mp4 / .mpd) Lintas-Iframe untuk Zenvora Native ExoPlayer
  // - Pengatur Rasio Video (object-fit: contain / cover / fill) Tanpa Men-Zoom Tombol Kontrol Player
  const coreScript = `
    (function() {
      try {
        // A. Anti-Adblock Bait Spoofing
        Object.defineProperty(window, 'canRunAds', { get: () => true, set: () => {}, configurable: true });
        Object.defineProperty(window, 'isAdBlockActive', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'adblock', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'adBlockEnabled', { get: () => false, set: () => {}, configurable: true });
        Object.defineProperty(window, 'google_ad_status', { get: () => 1, set: () => {}, configurable: true });

        window.adsbygoogle = window.adsbygoogle || [];
        window.adsbygoogle.loaded = true;
        window.adsbygoogle.push = function() { return 0; };

        // B. YouTube & Streaming Background Play Unlocker
        Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'webkitHidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
        Object.defineProperty(document, 'webkitVisibilityState', { get: () => 'visible', configurable: true });
        document.hasFocus = function() { return true; };

        // C. Blokir pop-under window.open otomatis
        window.open = function() { return null; };

        const origAddEventListener = EventTarget.prototype.addEventListener;
        EventTarget.prototype.addEventListener = function(type, listener, options) {
          if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || type === 'mozvisibilitychange') {
            return;
          }
          return origAddEventListener.call(this, type, listener, options);
        };

        // D. Sinkronisasi Ukuran Player Saat Layar Berputar ke Landscape Fullscreen
        const syncResizeOnFullscreen = function() {
          [100, 280, 550, 950].forEach(function(delayMs) {
            setTimeout(function() {
              try {
                window.dispatchEvent(new Event('resize'));
              } catch (_) {}
            }, delayMs);
          });
        };
        origAddEventListener.call(document, 'fullscreenchange', syncResizeOnFullscreen, true);
        origAddEventListener.call(document, 'webkitfullscreenchange', syncResizeOnFullscreen, true);
        origAddEventListener.call(document, 'mozfullscreenchange', syncResizeOnFullscreen, true);
        origAddEventListener.call(window, 'orientationchange', syncResizeOnFullscreen, true);

        // E. Pengatur Rasio Video (object-fit) Lintas-Iframe Tanpa Men-Zoom Kontrol Player
        let currentObjectFit = 'contain';
        const applyObjectFitToVideos = function(fitMode) {
          currentObjectFit = fitMode || 'contain';
          const videos = document.querySelectorAll('video');
          for (let i = 0; i < videos.length; i++) {
            videos[i].style.setProperty('object-fit', currentObjectFit, 'important');
          }
          const iframes = document.querySelectorAll('iframe');
          for (let j = 0; j < iframes.length; j++) {
            try {
              if (iframes[j].contentWindow) {
                iframes[j].contentWindow.postMessage({ zenvoraObjectFit: currentObjectFit }, '*');
              }
            } catch (_) {}
          }
        };

        origAddEventListener.call(window, 'message', function(e) {
          try {
            if (e && e.data && typeof e.data === 'object' && e.data.zenvoraObjectFit) {
              applyObjectFitToVideos(e.data.zenvoraObjectFit);
            }
          } catch (_) {}
        }, false);

        // F. Detektor Aliran Video Otomatis (.m3u8 / .mp4 / .mpd) untuk Zenvora Native Player (ExoPlayer)
        const isYouTube = location.hostname.includes('youtube.com') || location.hostname.includes('youtu.be');
        let lastReportedStream = '';

        const reportDetectedStream = function(rawUrl) {
          try {
            if (isYouTube || !rawUrl || typeof rawUrl !== 'string') return;
            if (rawUrl.startsWith('blob:') || rawUrl.startsWith('data:')) return;
            const absUrl = rawUrl.startsWith('http') ? rawUrl : new URL(rawUrl, location.href).href;
            const lower = absUrl.toLowerCase();
            // Abaikan segmen pendek .ts atau gambar/iklan
            if (lower.includes('.ts?') || lower.endsWith('.ts') || lower.includes('doubleclick') || lower.includes('googlesyndication')) {
              return;
            }
            const isStreamManifestOrVideo =
              lower.includes('.m3u8') ||
              lower.includes('.mpd') ||
              lower.includes('.mp4') ||
              lower.includes('/hls/') ||
              lower.includes('master.txt') ||
              lower.includes('index.txt');

            if (!isStreamManifestOrVideo) return;
            if (absUrl === lastReportedStream) return;
            lastReportedStream = absUrl;

            const bridgeUrl = 'https://zenvora.stream.detect/?src=' +
              encodeURIComponent(absUrl) +
              '&ref=' + encodeURIComponent(location.href);
            const pingFrame = document.createElement('iframe');
            pingFrame.style.cssText = 'display:none !important;width:0 !important;height:0 !important;border:0 !important;';
            pingFrame.src = bridgeUrl;
            (document.body || document.documentElement).appendChild(pingFrame);
            setTimeout(function() {
              try { pingFrame.remove(); } catch (_) {}
            }, 250);
          } catch (_) {}
        };

        // Tangkap manifest .m3u8 / .mp4 yang diminta lewat XHR atau Fetch oleh JWPlayer / HLS.js / Plyr
        const origXhrOpen = XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open = function(method, url) {
          try {
            if (typeof url === 'string') {
              reportDetectedStream(url);
            }
          } catch (_) {}
          return origXhrOpen.apply(this, arguments);
        };

        if (window.fetch) {
          const origFetch = window.fetch;
          window.fetch = function(input) {
            try {
              const urlStr = typeof input === 'string' ? input : (input && input.url ? input.url : '');
              if (urlStr) {
                reportDetectedStream(urlStr);
              }
            } catch (_) {}
            return origFetch.apply(this, arguments);
          };
        }

        // Tangkap juga sumber langsung dari elemen <video> saat mulai diputar
        const inspectVideoElement = function(e) {
          try {
            const v = e.target;
            if (v && v.tagName === 'VIDEO') {
              if (currentObjectFit !== 'contain') {
                v.style.setProperty('object-fit', currentObjectFit, 'important');
              }
              const src = v.currentSrc || v.src || '';
              if (src && src.startsWith('http')) {
                reportDetectedStream(src);
              } else {
                const sourceEl = v.querySelector('source[src]');
                if (sourceEl) {
                  reportDetectedStream(sourceEl.getAttribute('src') || '');
                }
              }
            }
          } catch (_) {}
        };

        origAddEventListener.call(document, 'loadedmetadata', inspectVideoElement, true);
        origAddEventListener.call(document, 'play', inspectVideoElement, true);
        origAddEventListener.call(document, 'playing', inspectVideoElement, true);
      } catch (_) {}
    })();
  `;

  try {
    const script = document.createElement("script");
    script.textContent = coreScript;
    (document.head || document.documentElement).appendChild(script);
    script.remove();
  } catch (_) {}

  // 2. CSS Minimalis Khusus Elemen <video> Saat Fullscreen (Tidak Mengubah Layout Web Sama Sekali)
  const injectMinimalFullscreenVideoFix = () => {
    if (document.getElementById("zenvora-video-fs-style")) return;
    const style = document.createElement("style");
    style.id = "zenvora-video-fs-style";
    style.textContent = `
      :fullscreen video,
      :-moz-full-screen video,
      :-webkit-full-screen video,
      .jwplayer.jw-flag-fullscreen video,
      .jwplayer.jw-flag-fullscreen .jw-media {
        width: 100% !important;
        height: 100% !important;
        max-width: 100vw !important;
        max-height: 100vh !important;
      }
      .jwplayer.jw-flag-fullscreen .jw-aspect {
        padding-top: 0 !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", injectMinimalFullscreenVideoFix, { once: true });
  } else {
    injectMinimalFullscreenVideoFix();
  }

  // 3. Integrasi Toko Ekstensi (addons.mozilla.org -> Zenvora Add-ons Store)
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
})();
