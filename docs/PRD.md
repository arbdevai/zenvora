# Product Requirement Document (PRD)

## 1. Identitas Aplikasi
- **Nama Aplikasi:** Zenvora Browser
- **Package Name:** `com.zenvora.browser`
- **Min SDK / Target SDK:** 26 / 35 (Android 15 Ready)
- **Arsitektur:** Single-Activity, Clean Architecture (Data -> Domain -> UI), Jetpack Compose Material 3.

## 2. Nilai Jual Utama (Unique Value Proposition)
- **Engine Independen Non-Chromium:** Ditenagai oleh Mozilla GeckoView (Gecko rendering engine + SpiderMonkey JS engine), menghindari keterbatasan dan masalah rendering `android.webkit.WebView`.
- **Privasi & Ad-Blocking Tingkat Native:** Perlindungan privasi menyeluruh dengan engine pencocokan filter berbasis Rust (via JNI/UniFFI) yang terintegrasi langsung dengan Gecko Content Blocking API.
- **True Dark OLED Experience:** Antarmuka Jetpack Compose berbasis hitam murni (#000000) yang efisien daya untuk layar AMOLED.

## 3. Kebutuhan Teknis & Lapisan Sistem
- **Web Engine:** Mozilla GeckoView (`org.mozilla.geckoview:geckoview-omni`)
- **Native Layer:** Rust (Cargo-NDK + JNI/UniFFI) untuk parser aturan adblock (EasyList format) berkecepatan tinggi dengan memory safety.
- **Storage / Persistence:** 
  - Room Database: History, Bookmarks, Saved Tab Sessions.
  - Jetpack DataStore Preferences: Konfigurasi privasi, switch adblocker, pilihan default search engine.
- **Networking:** Gecko Network Stack untuk web browsing, OkHttp untuk in-app updater dan unduhan background.
- **In-App Updater:** Resumable chunk streaming (.part isolation) dengan verifikasi 4-lapis `isApkValid`.

## 4. Batasan & Desain Sistem (Anti-AI Slop)
- Bebas elemen dekorasi norak: Nol animasi pulse hijau, nol marquee, nol glow berlebihan.
- Nol emoji di seluruh antarmuka pengguna, tombol, dialog, notifikasi, dan pesan commit.
- Wajib menggunakan Vector Icons resmi Material Icons Extended.
- Wajib Skeleton Shimmer untuk status loading lokal.
- Scaffolding wajib: `Scaffold` + `contentWindowInsets` + `.padding(innerPadding)` + `.imePadding()`.

## 5. Daftar Fitur Utama (Vertical Slices)
- [x] **Slice 1: Scaffolding & CI/CD** - Struktur proyek Compose, toolchain verifikasi, in-app updater (.part streaming & isApkValid), dan GitHub Actions runner.
- [x] **Slice 2: GeckoView Compose Bridge** - `GeckoRuntimeManager` singleton, `BrowserSession` controller, dan rendering `AndroidView` di Compose dengan gesture & lifecycle terhubung penuh.
- [x] **Slice 3: Native Rust Adblock Engine** - Modul Rust NDK (`native/adblock_core`) untuk parsing dan matching URL/request network terhadap blocklist + fallback matcher.
- [x] **Slice 4: Browser UI & Omnibox** - Bottom address bar, progress bar non-obtrusive, dialog keamanan SSL/TLS, kontrol tab multi-session, mode desktop, dan find in page.
- [x] **Slice 5: Persistence & History Management** - Room Database (`HistoryDao`, `BookmarkDao`) untuk histori penelusuran, bookmark, dan DataStore Preferences.
