# Project State & Session Memory

## Status Saat Ini
- **Fase Aktif:** Fase 1: Fondasi Arsitektur Selesai (GeckoView + Compose + Rust Adblock + Room DB)
- **Versi Terakhir:** v1.0.0 (versionCode: 10000)
- **Status CI Terakhir:** Siap Push ke GitHub Remote

## Fitur yang Selesai Tuntas (Vertical Slices)
- [x] Wawancara PRD & Penentuan Keputusan Arsitektur Engine (GeckoView + Native Rust Adblock)
- [x] Kontrak Dokumen Persisten `docs/PRD.md` dan `docs/STATE.md`
- [x] Inisiasi Struktur Proyek Android Compose (Single Activity, Material 3, True Dark OLED)
- [x] Konfigurasi Repositori Maven Mozilla & Dependensi GeckoView Omni di Gradle
- [x] GeckoRuntimeManager (Singleton) & BrowserSession (Progress, Navigation, Security, Content Blocking Delegate)
- [x] Native Rust Adblock Engine (`native/adblock_core/`) + JNI bridge `AdblockEngine` + Fallback Matcher
- [x] Persistensi Room Database (`HistoryDao`, `BookmarkDao`, `ZenvoraDatabase`) & Jetpack DataStore Preferences
- [x] Antarmuka Jetpack Compose: `BrowserScreen` (Scaffold + safeDrawing + imePadding), `BrowserOmnibox` (Bottom bar, SSL badge, shield counter, tabs badge), `BrowserGeckoView`, `TabOverviewSheet`, `SecurityDetailDialog`, `HistoryBookmarkSheet`
- [x] In-App Updater System (`UpdateManager`): Streaming OkHttp chunk 64KB, isolasi `.part`, verifikasi 4-lapis `isApkValid`, dan pemicuan instalasi mandiri via `FileProvider`.
- [x] Background Browser Download Manager (`BrowserDownloadManager`): Unduhan file langsung terisolasi dengan pelacakan progres.
- [x] Settings & Privacy Screen (`SettingsSheet` & `UpdateDialog`): Pengaturan mesin pencari (DuckDuckGo, Startpage, Brave, Google), toggle adblocker, strict privacy, dan tombol cek pembaruan sistem.
- [x] Pre-flight Syntax Verification (27 file Kotlin, 8 file XML, 0 hard errors)

## Catatan Bug Terselesaikan & Workaround (Anti-Mengulang Masalah)
- *Catatan #01:* GeckoView membutuhkan registrasi repositori Maven Mozilla `https://maven.mozilla.org/maven2/` di `settings.gradle.kts`.
- *Catatan #02:* Pengikatan `GeckoSession` ke `GeckoView` dalam Jetpack Compose wajib diisolasi dalam `DisposableEffect` untuk melepas session (`releaseSession()`) saat composable keluar dari komposisi.

## Tugas Aktif Selanjutnya (Next Actions)
1. Hubungkan repository remote git pengguna (GitHub) jika sudah disiapkan.
2. Setup GitHub Secrets untuk keystore release otomatis (`python3 scripts/setup_release_keystore.py`).
3. Push commit ke remote branch `main` untuk memicu build APK release di GitHub Actions.
