# Project State & Session Memory

## Status Saat Ini
- **Fase Aktif:** Fase 4: Full-Screen UX, Pure Black Surface, Desktop Mode Persistence & Network Speed Engine (v1.3.0)
- **Versi Terakhir:** v1.3.0 (versionCode: 10300)
- **Status CI Terakhir:** Siap Commit & Push v1.3.0 ke GitHub Actions
- **Remote Repo:** https://github.com/arbdevai/zenvora

## Fitur yang Selesai Tuntas (Vertical Slices)
- [x] Wawancara PRD & Penentuan Keputusan Arsitektur Engine (GeckoView + Native Rust Adblock)
- [x] Kontrak Dokumen Persisten `docs/PRD.md` dan `docs/STATE.md`
- [x] Inisiasi Struktur Proyek Android Compose (Single Activity, Material 3, True Dark OLED)
- [x] Konfigurasi Repositori Maven Mozilla & Dependensi GeckoView Omni di Gradle
- [x] GeckoRuntimeManager (Singleton) & BrowserSession (Progress, Navigation, Security, Content Blocking Delegate)
- [x] Native Rust Adblock Engine (`native/adblock_core/`) + JNI bridge `AdblockEngine` + Fallback Matcher
- [x] Persistensi Room Database (`HistoryDao`, `BookmarkDao`, `ZenvoraDatabase`) & Jetpack DataStore Preferences
- [x] Antarmuka Jetpack Compose Layar Penuh: `BrowserScreen` (`AnimatedContent` + `WindowInsets.systemBars`), `BrowserOmnibox` (Top bar + Inline Action Panel), `BrowserGeckoView`, `TabOverviewSheet` (Full-Screen), `SecurityDetailDialog` (Full-Screen), `HistoryBookmarkSheet` (Full-Screen), `SettingsSheet` (Full-Screen), dan `UpdateDialog` (Full-Screen dengan `SkeletonBox` shimmer)
- [x] In-App Updater System (`UpdateManager`): Dual-ABI Smart Selector (`arm64-v8a` & `armeabi-v7a`), streaming OkHttp chunk 64KB, isolasi `.part`, verifikasi 4-lapis `isApkValid`, dan pemicuan instalasi mandiri via `FileProvider`.
- [x] High-Speed Browser Download Manager (`BrowserDownloadManager`): Unduhan file langsung terisolasi dengan `ConnectionPool` 16-koneksi dan buffer I/O 256 KB.
- [x] Mode Situs Desktop Permanen Lintas Situs (`BrowserSession.kt`): Penguncian simultan `USER_AGENT_MODE_DESKTOP`, `VIEWPORT_MODE_DESKTOP`, `userAgentOverride` Linux x86_64, `reload(LOAD_FLAGS_BYPASS_CACHE)`, serta penulisan ulang otomatis subdomain mobile (`m.` / `.m.`) ke domain desktop (`www.`) di `onLoadRequest`.
- [x] Manajemen Kecepatan Internet Maksimal (`NetworkSpeedManager.kt`): Pemantauan kecepatan unduh/unggah real-time (`TrafficStats`), puncak kecepatan, total data sesi, pengukuran latensi ping (`ms`), DNS in-memory caching, speculative preconnect, dan pemilihan resolver DNS cepat (Cloudflare `1.1.1.1`, Google `8.8.8.8`, Quad9 `9.9.9.9`, System).

## Catatan Bug Terselesaikan & Workaround (Anti-Mengulang Masalah)
- *Catatan #01:* GeckoView membutuhkan registrasi repositori Maven Mozilla `https://maven.mozilla.org/maven2/` di `settings.gradle.kts`.
- *Catatan #02:* Pengikatan `GeckoSession` ke `GeckoView` dalam Jetpack Compose wajib diisolasi dalam `DisposableEffect` untuk melepas session (`releaseSession()`) saat composable keluar dari komposisi.
- *Catatan #03:* Room 2.6.1 di bawah KSP2 mengalami crash `unexpected jvm signature V` apabila fungsi `suspend` pada `@Dao` dengan `@Query("DELETE ...")` mengembalikan `Unit`. Solusi permanen: tambahkan `ksp.useKSP2=false` di `gradle.properties` dan kembalikan `: Int` pada query `DELETE`/`UPDATE`.
- *Catatan #04:* GeckoView v136+ `NavigationDelegate.onLocationChange` membutuhkan 4 parameter (`session`, `url`, `perms`, `hasUserGesture: Boolean`), `SecurityInformation` menggunakan properti `origin`, dan konstanta pencarian halaman berada langsung di `GeckoSession.FINDER_FIND_BACKWARDS`.
- *Catatan #05:* Folder `/scripts/` diisolasi ke dalam `.gitignore` dan dihapus dari git tracking (`git rm -r --cached scripts/`), sedangkan `.github/scripts/generate_release_notes.py` dipertahankan untuk CI.
- *Catatan #06:* **Root Cause Force Close v1.0.0 (GeckoRuntime Multi-Process Fork-Bomb)**: `ZenvoraApp.onCreate()` memanggil `GeckoRuntimeManager.getRuntime(this)` tanpa mengecek nama proses. Solusi permanen: cek `Application.getProcessName()` (skip jika mengandung `:`) di `ZenvoraApp.kt` dan `GeckoRuntimeManager.kt`.
- *Catatan #07:* Redesign UI/UX ke tata letak standar Google Chrome Android menggunakan palet warna **Electric Orange (`#FF6D00`) x True Dark OLED (`#000000`)**.
- *Catatan #08:* Diaktifkan `splits.abi` (`arm64-v8a` dan `armeabi-v7a`, `isUniversalApk = false`) untuk memangkas ukuran APK dari ~309 MB menjadi ~91 MB per arsitektur.
- *Catatan #09:* **Bug Blok Hitam Menutupi Layar Saat Keyboard Naik (v1.2.0 -> Fixed v1.3.0)**: Terjadi karena kombinasi `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)` (yang sudah mencakup `ime`) ditambah `.imePadding()` pada root `Column` di `BrowserScreen.kt` (menghasilkan `2x tinggi keyboard` padding hitam dari bawah), serta `BrowserGeckoView` (`SurfaceView` berlatar `Color.BLACK`) yang tetap dirender di belakang `ChromeNewTabPage` saat berada di `about:blank`. Solusi permanen: gunakan `WindowInsets.systemBars` pada `Scaffold`, pindahkan `.imePadding()` ke dalam `Column` scrollable `ChromeNewTabPage`, dan hanya render `BrowserGeckoView` ketika tab sedang tidak berada di `about:blank`.
- *Catatan #10:* **Bug Mode Desktop Reset Saat Pindah Situs (v1.2.0 -> Fixed v1.3.0)**: Mengubah `userAgentMode` saja tidak cukup karena viewport tetap mobile dan tautan `m.example.com` tetap mengarah ke situs mobile. Solusi permanen: kunci `USER_AGENT_MODE_DESKTOP` + `VIEWPORT_MODE_DESKTOP` + `userAgentOverride` di setiap `onLoadRequest` dan `onPageStart`, serta tulis ulang host `m.` / `mobile.` / `.m.` menjadi `www.` secara otomatis saat Mode Desktop aktif.
- *Catatan #11:* **Eliminasi Halaman Floating & Background Kecampur Oranye (v1.3.0)**: Mengatur `surfaceTint = Color.Transparent` di `Theme.kt`, menghapus semua `.background(ElectricOrange.copy(...))`, mengganti warna shimmer `SkeletonComponents.kt` ke netral `ShimmerBase`/`ShimmerHighlight`, dan mengubah seluruh `ModalBottomSheet` serta `BasicAlertDialog` menjadi halaman layar penuh (`AnimatedContent`).

## Tugas Aktif Selanjutnya (Next Actions)
1. Verifikasi sintaksis `python3 scripts/verify_kotlin_syntax.py .` (0 hard errors).
2. Commit dan push rilis `v1.3.0` ke `origin main` serta pantau workflow GitHub Actions hingga selesai.
