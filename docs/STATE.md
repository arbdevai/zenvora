# Project State & Session Memory

## Status Saat Ini
- **Fase Aktif:** Fase 3: Produksi & Pemeliharaan (v1.1.0 - Fix Force Close GeckoRuntime & Redesign Chrome UI Electric Orange x True Dark)
- **Versi Terakhir:** v1.1.0 (versionCode: 10100)
- **Status CI Terakhir:** Memicu CI Build v1.1.0 di GitHub Actions
- **Remote Repo:** https://github.com/arbdevai/zenvora

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
- [x] Mode Situs Desktop Dinamis (`GeckoSessionSettings.USER_AGENT_MODE_DESKTOP` / `MOBILE` toggle per-tab).
- [x] Pencarian Teks di Halaman (`FindInPageBar` + `geckoSession.finder` dengan navigasi match sebelumnya/berikutnya).
- [x] Mode Tab Samaran / Incognito Browsing: Isolasi sesi memori mandiri via `usePrivateMode(true)`, pencegahan penulisan cookie/cache ke disk, bypass total pencatatan histori Room DB, dan visualisasi stealth mask di Omnibox serta pemisah kategori tab di `TabOverviewSheet`.
- [x] Pre-flight Syntax Verification (28 file Kotlin, 8 file XML, 0 hard errors)

## Catatan Bug Terselesaikan & Workaround (Anti-Mengulang Masalah)
- *Catatan #01:* GeckoView membutuhkan registrasi repositori Maven Mozilla `https://maven.mozilla.org/maven2/` di `settings.gradle.kts`.
- *Catatan #02:* Pengikatan `GeckoSession` ke `GeckoView` dalam Jetpack Compose wajib diisolasi dalam `DisposableEffect` untuk melepas session (`releaseSession()`) saat composable keluar dari komposisi.
- *Catatan #03:* Room 2.6.1 di bawah KSP2 mengalami crash `unexpected jvm signature V` apabila fungsi `suspend` pada `@Dao` dengan `@Query("DELETE ...")` mengembalikan `Unit`. Solusi permanen: tambahkan `ksp.useKSP2=false` di `gradle.properties` dan kembalikan `: Int` pada query `DELETE`/`UPDATE`.
- *Catatan #04:* GeckoView v136+ `NavigationDelegate.onLocationChange` membutuhkan 4 parameter (`session`, `url`, `perms`, `hasUserGesture: Boolean`), `SecurityInformation` menggunakan properti `origin`, dan konstanta pencarian halaman berada langsung di `GeckoSession.FINDER_FIND_BACKWARDS`.
- *Catatan #05:* Folder `scripts/` diisolasi ke dalam `.gitignore` dan dihapus dari git tracking (`git rm -r --cached scripts/`) agar skrip internal chroot tidak bocor ke repositori publik.
- *Catatan #06:* **Root Cause Force Close v1.0.0 (GeckoRuntime Multi-Process Fork-Bomb)**: `ZenvoraApp.onCreate()` memanggil `GeckoRuntimeManager.getRuntime(this)` tanpa mengecek nama proses. Karena GeckoView meluncurkan child process (`com.zenvora.browser:tab0..tab39`, `:gpu`) menggunakan kelas `Application` yang sama, setiap proses anak ikut membuat instance `GeckoRuntime` baru secara rekursif hingga Android ActivityManager membunuh aplikasi utama dengan `SIGKILL (signal 9)` setelah ~5 detik. Solusi permanen: cek `Application.getProcessName()` (skip jika mengandung `:`) dan inisialisasi `GeckoRuntime` secara lazy hanya di proses utama (`BrowserViewModel`).
- *Catatan #07:* Redesign UI/UX ke tata letak standar Google Chrome Android (Top Omnibox Bar, tombol Home, Pill Address Bar, Tab Switcher Box `[1]`, Menu 3-Titik `⋮`, dan `ChromeNewTabPage` dengan Speed Dial 4x2) menggunakan palet warna **Electric Orange (`#FF6D00`) x True Dark OLED (`#000000`)**.

## Tugas Aktif Selanjutnya (Next Actions)
1. Seluruh 6 Vertical Slices dan pipeline CI/CD GitHub Actions telah tuntas dan menghasilkan artefak rilis (`app-release`).
2. Siap menerima pengembangan fitur lanjutan atau pembaruan versi berikutnya.
