# Product Requirement Document (PRD)

## 1. Identitas Aplikasi
- **Nama Aplikasi:** Zenvora
- **Package Name:** `com.zenvora.browser`
- **Min SDK / Target SDK:** 26 / 36 (Android 15+ Ready)
- **Arsitektur:** Single-Activity, Clean Architecture (Data -> Domain -> UI), Jetpack Compose Material 3.

## 2. Nilai Jual Utama (Unique Value Proposition)
- **Engine Independen Non-Chromium:** Ditenagai oleh Mozilla GeckoView (Gecko rendering engine + SpiderMonkey JS engine), menghindari keterbatasan dan masalah rendering `android.webkit.WebView`.
- **Privasi & Ad-Blocking Tingkat Native:** Perlindungan privasi menyeluruh dengan engine pencocokan filter berbasis Rust (via JNI/UniFFI) yang terintegrasi langsung dengan Gecko Content Blocking API.
- **True Dark OLED x Electric Orange Experience:** Antarmuka Jetpack Compose berbasis hitam murni (`#000000`) dan permukaan abu gelap netral (`#0A0A0A` / `#141414` / `#1A1A1A`) tanpa permukaan kecampur oranye (`surfaceTint = Color.Transparent`), dipadukan dengan aksen tegas Electric Orange (`#FF6D00`).
- **Manajemen Kecepatan Internet Maksimal (`NetworkSpeedManager`):** Telemetri throughput real-time (`TrafficStats`), akselerasi DNS-over-HTTPS / Fast DNS (`1.1.1.1`, `8.8.8.8`, `9.9.9.9`) dengan in-memory DNS cache, speculative preconnect 16-jalur paralel, dan streaming unduhan ber-buffer 256 KB.

## 3. Kebutuhan Teknis & Lapisan Sistem
- **Web Engine:** Mozilla GeckoView (`org.mozilla.geckoview:geckoview-omni`)
- **Native Layer:** Rust (Cargo-NDK + JNI) untuk parser aturan adblock (EasyList format) berkecepatan tinggi dengan memory safety.
- **Storage / Persistence:** 
  - Room Database: History, Bookmarks, Saved Tab Sessions.
  - Jetpack DataStore Preferences: Konfigurasi privasi, switch adblocker, pilihan default search engine, mode akselerasi jaringan Turbo, dan profil resolver DNS.
- **Networking:** Gecko Network Stack untuk web browsing, `NetworkSpeedManager` + OkHttp (`ConnectionPool` 16 idle connections, 256 KB streaming buffer) untuk akselerasi DNS, preconnect, in-app updater, dan unduhan background.
- **In-App Updater:** Dual-ABI Smart Updater (`arm64-v8a` & `armeabi-v7a`) dengan resumable chunk streaming (`.part` isolation) dan verifikasi 4-lapis `isApkValid`.

## 4. Batasan & Desain Sistem (Anti-AI Slop & Zero Floating UI)
- Bebas elemen dekorasi norak: Nol animasi pulse hijau, nol marquee, nol glow berlebihan.
- Nol emoji di seluruh antarmuka pengguna, tombol, halaman, notifikasi, changelog, dan pesan commit.
- **Nol Halaman Mengambang (Zero Floating Pages):** Dilarang menggunakan `ModalBottomSheet`, `BasicAlertDialog`, atau `DropdownMenu` mengambang. Seluruh sub-halaman (Pengelola Tab, Histori & Bookmark, Setelan, Keamanan SSL, dan Pembaruan Sistem) wajib dirender sebagai Halaman Layar Penuh (*Full-Screen Page*) dengan transisi `AnimatedContent` dan tombol kembali (`BackHandler`).
- **Nol Background Kecampur Oranye:** Dilarang menggunakan `ElectricOrange.copy(alpha = ...)` sebagai warna background atau membiarkan `surfaceTint` Material 3 mencampurkan warna oranye ke permukaan gelap.
- Wajib menggunakan Vector Icons resmi Material Icons Extended.
- Wajib Skeleton Shimmer (`SkeletonBox`) untuk status tunggu/pemeriksaan data.
- Scaffolding terstruktur: `Scaffold(contentWindowInsets = WindowInsets.systemBars)` + `.padding(innerPadding)` dengan `.imePadding()` pada kontainer form scrollable dan isolasi `SurfaceView` `GeckoView` saat berada di beranda `about:blank`.

## 5. Daftar Fitur Utama (Vertical Slices)
- [x] **Slice 1: Scaffolding & CI/CD** - Struktur proyek Compose, toolchain verifikasi, in-app updater (`.part` streaming & `isApkValid`), dan GitHub Actions runner.
- [x] **Slice 2: GeckoView Compose Bridge** - `GeckoRuntimeManager` singleton (terisolasi dari child process), `BrowserSession` controller, dan rendering `AndroidView` di Compose.
- [x] **Slice 3: Native Rust Adblock Engine** - Modul Rust NDK (`native/adblock_core`) untuk parsing dan matching URL/request network terhadap blocklist + fallback matcher.
- [x] **Slice 4: Chrome-Style Top Omnibox & Navigation** - Top address bar ala Chrome, progress bar Electric Orange, kontrol tab multi-session, mode desktop, dan find in page.
- [x] **Slice 5: Persistence & History Management** - Room Database (`HistoryDao`, `BookmarkDao`) untuk histori penelusuran, bookmark, dan DataStore Preferences.
- [x] **Slice 6: Incognito Private Browsing** - Isolasi sesi GeckoView mandiri via `usePrivateMode(true)`, anti-leaking cache/cookie disk, tab filter, dan zero history recording.
- [x] **Slice 7: Full-Screen Chrome UX, Pure Zenvora Identity & Zero Orange-Tinted Surface (v1.3.0)** - Identitas murni `"Zenvora"`, konversi seluruh sheet/dialog menjadi halaman layar penuh (`AnimatedContent`), penghapusan total background kecampur oranye (`surfaceTint = Color.Transparent`), perbaikan permanen Mode Desktop lintas situs (`USER_AGENT_MODE_DESKTOP` + `VIEWPORT_MODE_DESKTOP` + `userAgentOverride` + rewrite `m.` ke `www.`), dan perbaikan bug blok hitam saat keyboard muncul.
- [x] **Slice 8: Network Speed & Bandwidth Optimization Engine (v1.3.0)** - `NetworkSpeedManager` dengan pemantauan kecepatan unduh/unggah real-time (`TrafficStats`), pengukuran ping latensi (`ms`), pemilihan resolver DNS cepat (Cloudflare `1.1.1.1`, Google `8.8.8.8`, Quad9 `9.9.9.9`), in-memory DNS cache, speculative preconnect, dan peningkatan buffer unduhan ke 256 KB.
- [x] **Slice 9: Native Gecko TRR Strict DoH, Auto-Hiding Toolbar & True Immersive Video Streaming (v1.4.0)** - Injeksi `gecko-config.yaml` (`network.trr.mode: 3` Strict DoH + `bootstrapAddress: 1.1.1.1` yang 100% mem-bypass DNS HP/ISP), Menu 3-Titik autentik Chrome Android, Top Omnibox otomatis sembunyi saat scroll ke bawah, dan mode Video Fullscreen Immersive (`onFullScreen` + `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` + `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`).
- [x] **Slice 10: 3-Layer Stealth Adblock & Anti-Adblock Defuser Engine (v1.4.0)** - Built-in GeckoView WebExtension (`assets/addons/zenvora_shield/`) dengan `webRequest.onBeforeRequest` sub-resource blocker, redirect script umpan (`adsbygoogle.js` / `fuckadblock`) ke stub sukses agar tidak terdeteksi anti-adblock, `document_start` bait spoofing, auto-skip video pre-roll ads, pemblokiran pop-under `onNewSession`, serta integrasi penuh JNI Rust `AdblockEngine`.
- [x] **Slice 11: Zero-Glitch Offset Scroll, Hardware-Only Back Navigation, Brave-Style Procedural Shield & Redmi 9C 20:9 Video Fit (v1.5.0)** - Perbaikan permanen glitch hitam bawah saat scroll dengan offset translasi tanpa mengubah ukuran `SurfaceView` `GeckoView`, penghapusan search bar ganda di beranda dan seluruh tombol kembali di layar (100% mengandalkan tombol kembali bawaan HP / `BackHandler`), upgrade Adblock setara Brave Shields (100+ ad/pop-under network + heuristic keyword judi/slot + blokir TLD mencurigakan + procedural DOM overlay remover + blokir `window.open` & click-hijacking), serta pengatur rasio layar penuh video khusus layar 20:9 Redmi 9C (`Pas Layar 20:9`, `Zoom Penuh`, `Rasio 16:9`) yang kompatibel dengan semua pemutar video web.
- [x] **Slice 12: Dynamic Hardware Video Codec Detector, Background Play Unlocker, Universal Fullscreen Engine Bridge & WordPress Ad Shield (v1.6.0)** - `HardwareCodecDetector` berbasis `MediaCodecList` yang mendeteksi dekoder hardware fisik SoC (`H.264`, `HEVC`, `VP9`, `AV1`) secara dinamis tanpa hardcode agar video 720p/1080p berjalan mulus 60fps sesuai kemampuan HP, pemutaran latar belakang YouTube/web (`Page Visibility API` spoofer + `media.geckoview.enable-When-In-Background`), jembatan fullscreen universal untuk player web custom (`JWPlayer`, `Video.js`, `Plyr`), pemindahan kontrol rasio fullscreen ke tengah atas (`Alignment.TopCenter`), serta filter iklan khusus situs WordPress/streaming modern.
- [x] **Slice 13: Clean YouTube Fullscreen UI, Centered Cutout Viewport, Blank-Screen Exit Recovery, 3-Dot Auto Update Detector & Persistent Tab Restoration (v1.7.0)** - Perbaikan UI/elemen YouTube saat fullscreen (menghapus CSS `:fullscreen video` yang menutupi kontrol YouTube), perbaikan posisi video miring ke kanan saat landscape (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER` + default `STANDAR_16_9` `1.0f` dengan `TransformOrigin.Center`), pemulihan otomatis layar blank saat keluar fullscreen (`requestLayout()` + `invalidate()` bertingkat pada `GeckoView`), penghapusan menu fullscreen Redmi 9C dari menu 3-titik dan penambahan menu Update dengan deteksi otomatis di latar belakang (`checkForUpdatesSilently` + badge titik oranye), serta penyimpanan otomatis seluruh tab aktif ke `DataStore` (`savedTabsJson`) agar tab tidak hilang saat aplikasi di-update.
