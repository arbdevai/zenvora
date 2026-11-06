# Project State & Session Memory

## Status Saat Ini
- **Fase Aktif:** Fase 6: Zero-Glitch Offset Scroll, Hardware Back Navigation, Brave-Style Procedural Shield & Redmi 9C 20:9 Video Fit (v1.5.0)
- **Versi Terakhir:** v1.5.0 (versionCode: 10500)
- **Status CI Terakhir:** Siap Commit & Push v1.5.0 ke GitHub Actions
- **Remote Repo:** https://github.com/arbdevai/zenvora

## Fitur yang Selesai Tuntas (Vertical Slices)
- [x] Wawancara PRD & Penentuan Keputusan Arsitektur Engine (GeckoView + Native Rust Adblock)
- [x] Kontrak Dokumen Persisten `docs/PRD.md` dan `docs/STATE.md`
- [x] Inisiasi Struktur Proyek Android Compose (Single Activity, Material 3, True Dark OLED)
- [x] Konfigurasi Repositori Maven Mozilla & Dependensi GeckoView Omni di Gradle
- [x] GeckoRuntimeManager (Singleton) & BrowserSession (Progress, Navigation, Security, Content Blocking Delegate)
- [x] Native Rust Adblock Engine (`native/adblock_core/`) + JNI bridge `AdblockEngine` + Fallback Matcher
- [x] Persistensi Room Database (`HistoryDao`, `BookmarkDao`, `ZenvoraDatabase`) & Jetpack DataStore Preferences
- [x] Antarmuka Jetpack Compose Layar Penuh: `BrowserScreen` (`AnimatedContent` + `WindowInsets.systemBars`), `BrowserOmnibox` (Top bar + Menu 3-Titik autentik Chrome Android + Auto-Hide saat scroll ke bawah), `BrowserGeckoView`, `TabOverviewSheet` (Full-Screen), `SecurityDetailDialog` (Full-Screen), `HistoryBookmarkSheet` (Full-Screen), `SettingsSheet` (Full-Screen), dan `UpdateDialog` (Full-Screen dengan `SkeletonBox` shimmer)
- [x] In-App Updater System (`UpdateManager`): Dual-ABI Smart Selector (`arm64-v8a` & `armeabi-v7a`), streaming OkHttp chunk 64KB, isolasi `.part`, verifikasi 4-lapis `isApkValid`, dan pemicuan instalasi mandiri via `FileProvider`.
- [x] High-Speed Browser Download Manager (`BrowserDownloadManager`): Unduhan file langsung terisolasi dengan `ConnectionPool` 16-koneksi dan buffer I/O 256 KB.
- [x] Mode Situs Desktop Permanen Lintas Situs (`BrowserSession.kt`): Penguncian simultan `USER_AGENT_MODE_DESKTOP`, `VIEWPORT_MODE_DESKTOP`, `userAgentOverride` Linux x86_64, `reload(LOAD_FLAGS_BYPASS_CACHE)`, serta penulisan ulang otomatis subdomain mobile (`m.` / `.m.`) ke domain desktop (`www.`) di `onLoadRequest`.
- [x] Native Gecko TRR Strict DNS-over-HTTPS (`GeckoRuntimeManager.kt` & `NetworkSpeedManager.kt`): Injeksi `gecko-config.yaml` dengan `network.trr.mode: 3`, `network.trr.uri: "https://1.1.1.1/dns-query"`, dan `network.trr.bootstrapAddress: "1.1.1.1"` sehingga GeckoView mem-bypass total DNS bawaan HP/ISP.
- [x] True Immersive Video Streaming Mode (`BrowserSession.kt`, `BrowserScreen.kt`, `AndroidManifest.xml`): Implementasi `ContentDelegate.onFullScreen`, penyembunyian sistem bar (`WindowInsetsCompat.Type.systemBars()`), `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` untuk layar penuh tanpa potongan kamera depan, rotasi otomatis `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, dan `android:configChanges` agar video tidak me-reload saat rotasi.
- [x] 3-Layer Stealth Adblock & Anti-Adblock Defuser (`assets/addons/zenvora_shield/`, `lib.rs`, `AdblockEngine.kt`, `BrowserSession.kt`): Built-in GeckoView WebExtension untuk memblokir sub-resource XHR/script/iframe iklan, mengganti script umpan (`adsbygoogle.js` / `fuckadblock`) dengan stub palsu agar lolos deteksi anti-adblock, menyuntikkan `document_start` stealth defuser, auto-skip iklan video, memblokir pop-under `onNewSession`, dan menghubungkan `AdblockEngine.isUrlBlocked()` ke `onLoadRequest`.

## Catatan Bug Terselesaikan & Workaround (Anti-Mengulang Masalah)
- *Catatan #01 s/d #11:* Tercatat dari rilis v1.0.0 hingga v1.3.0.
- *Catatan #12:* **DNS di APK Tidak Berpengaruh Saat DNS HP Dimatikan (v1.3.0 -> Fixed v1.4.0)**: `GeckoView` menggunakan network stack C++ Mozilla Necko yang terpisah dari `OkHttp`. Solusi permanen: buat file `gecko-config.yaml` dengan `network.trr.mode: 3` (Strict DoH), `network.trr.uri: "https://1.1.1.1/dns-query"`, `network.trr.bootstrapAddress: "1.1.1.1"`, dan `security.OCSP.enabled: 0`, lalu muat melalui `GeckoRuntimeSettings.Builder().configFilePath(...)`.
- *Catatan #13:* **Video Streaming Fullscreen Berantakan di Layar HP Besar (v1.3.0 -> Fixed v1.4.0)**: `ContentDelegate.onFullScreen` belum diimplementasikan. Solusi permanen: hubungkan `onFullScreen` ke `BrowserTab.isFullScreen`, sembunyikan `BrowserOmnibox` dan seluruh `WindowInsets`, aktifkan `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` + `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`, serta tambahkan `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden|uiMode"` di `AndroidManifest.xml`.
- *Catatan #14:* **Adblock Lolos & Terdeteksi Anti-Adblock (v1.3.0 -> Fixed v1.4.0)**: Pasang Built-in GeckoView WebExtension (`resource://android/assets/addons/zenvora_shield/`) melalui `runtime.webExtensionController.ensureBuiltIn(...)` yang memfilter sub-resource (`webRequest.onBeforeRequest`), mengalihkan script umpan anti-adblock ke data URL stub sukses, menyuntikkan bait spoofing (`window.canRunAds = true`, `FuckAdBlock` stub), serta memblokir pop-under `onNewSession` dan `AdblockEngine.isUrlBlocked()` di `BrowserSession.kt`.
- *Catatan #15:* **Glitch Hitam di Bawah & Search Bar Muncul-Hilang Sendiri Saat Scroll (v1.4.0 -> Fixed v1.5.0)**: Mengubah tinggi `BrowserGeckoView` via `AnimatedVisibility` di atas `SurfaceView` menyebabkan realokasi buffer permukaan C++ dan koordinat sentuh lokal bergeser `57.dp` setiap kali bar bersembunyi (memicu loop osilasi `onScrollUp`). Solusi permanen: jaga tinggi `BrowserGeckoView` tetap 100% konstan (`Modifier.fillMaxSize()`), pasang `pointerInput` pada `Box` induk statis dengan debounce `420ms`, dan geser `BrowserOmnibox` serta `BrowserGeckoView` menggunakan `Modifier.offset { IntOffset(0, ...) }`.
- *Catatan #16:* **Search Bar Ganda & Tombol Kembali di Layar (v1.4.0 -> Fixed v1.5.0)**: Hapus search bar tengah pada `ChromeNewTabPage` (cukup gunakan `BrowserOmnibox` utama di atas) dan hapus seluruh ikon `ArrowBack` / tombol `"Kembali"` di semua halaman (`TabOverviewSheet`, `HistoryBookmarkSheet`, `SettingsSheet`, `SecurityDetailDialog`, `UpdateDialog`) karena navigasi kembali ditangani penuh oleh tombol kembali bawaan HP (`BackHandler`).
- *Catatan #17:* **Adblock Masih Bocor di Situs Streaming & Video Fullscreen Redmi 9C (v1.4.0 -> Fixed v1.5.0)**: Perkuat `zenvora_shield` dengan arsitektur Brave Shields (100+ ad/pop-under network, heuristic keyword judi/slot, blokir TLD pihak ketiga mencurigakan, blokir `window.open` & click-hijacking di `document_start`, pembersihan overlay transparan `z-index` tinggi di atas video) serta tambahkan kontrol rasio layar penuh video khusus layar 20:9 Redmi 9C (`Pas Layar 20:9`, `Zoom Penuh`, `Rasio 16:9`) dan pemicu langsung `"Layar Penuh Video (20:9)"` di menu 3-titik.

## Tugas Aktif Selanjutnya (Next Actions)
1. Verifikasi sintaksis `python3 /home/coder/.gemini/config/skills/android-compose-builder/scripts/verify_kotlin_syntax.py .` (0 hard errors).
2. Commit dan push rilis `v1.5.0` ke `origin main` serta pantau workflow GitHub Actions hingga selesai.
