# Zenvora Gesture Engine — Product Requirements Document (PRD)

## 1. Ringkasan Proyek
- **Nama Aplikasi:** Zenvora
- **Package ID:** `com.zenvora.gesture`
- **Target Platform:** Android 8.0+ (minSdk 26, compileSdk/targetSdk 36)
- **Arsitektur:** Kotlin + Jetpack Compose (Material 3 True Neutral OLED Dark) + Room Database + AccessibilityService Gesture Engine + WindowManager Floating Overlay

## 2. Tujuan Utama
Membangun aplikasi utilitas otomasi sentuhan & gulir layar (Auto-Scroll, Auto-Tap, dan Macro Sequence) yang modular, presisi, dan dapat dikembangkan jangka panjang—bukan sekadar MVP—dengan fokus pada:
1. **Interval Fleksibel:** Pengaturan jeda waktu bebas dalam satuan milidetik, detik, atau menit (misalnya 30 detik, 1 menit, 500 ms) lengkap dengan hitung mundur langsung.
2. **Humanized Gesture Engine:** Variasi koordinat acak (jitter radius), variasi interval acak (variance), durasi sentuhan natural, dan jalur swipe kurva Bezier kuadratik.
3. **Multi-Profil Persisten (Room):** Menyimpan banyak preset skenario secara terpisah agar mudah dipakai ulang atau di-upgrade.
4. **Floating Overlay HUD:** Kontrol melayang (Play/Pause, Countdown, Penempatan Titik Target, Minimize) yang stabil di berbagai OEM Android.

## 3. Spesifikasi Fitur Inti
### A. Engine Eksekusi (`core/engine`)
- Menggunakan `AccessibilityService.dispatchGesture` dengan `GestureResultCallback` berbasis coroutine.
- **Mode Profil:**
  - `AUTO_SCROLL`: Gulir otomatis ke arah `UP`, `DOWN`, `LEFT`, atau `RIGHT` dengan persentase jarak layar dan durasi yang dapat diatur.
  - `SINGLE_TAP`: Ketukan berulang pada 1 titik koordinat ternormalisasi `(0.0..1.0)`.
  - `MACRO_SEQUENCE`: Urutan multi-langkah (`TAP`, `SWIPE`, `WAIT`) dengan jeda antar-siklus yang dapat dikonfigurasi.
- **Humanized Randomization:**
  - `humanizedJitterPx`: Offset koordinat acak berbatas radius piksel.
  - `intervalVarianceMs`: Variasi acak `[-variance, +variance]` di sekitar interval utama.
  - `useBezierCurve`: Kurva Bezier kuadratik dengan titik kontrol tegak lurus acak untuk meniru lengkungan ibu jari.

### B. Penyimpanan Data (`data/local`)
- **Room Database (`ZenvoraDatabase`):**
  - `GestureProfileEntity`: Profil otomasi lengkap (mode, interval, variance, jitter, kurva Bezier, arah/jarak scroll, batas ulang).
  - `GestureStepEntity`: Langkah koordinat ternormalisasi `(x, y, endX, endY)` yang berelasi `CASCADE` ke profil.
- **DataStore (`PreferencesRepository`):** Status overlay terakhir dan preferensi tampilan.

### C. Antarmuka & Tema (`ui/`)
- **True Neutral OLED Dark:** Latar `#000000` (OLED Black), surface `#121212`, surfaceVariant `#1E1E1E`, aksen netral kontras tinggi, nol emoji dekoratif, nol kartu transparan berwarna.
- **Layar Utama:** Status izin (Accessibility, Overlay, Notifikasi), kontrol cepat profil aktif, dan daftar preset.
- **Editor Profil:** Pemilih interval fleksibel (chip cepat 5s/15s/30s/60s + input kustom ms/detik/menit), pengaturan humanisasi, serta editor langkah.

## 4. Pipeline Build
- Seluruh kompilasi APK Release & Debug dijalankan melalui **GitHub Actions** (`ubuntu-latest`) dengan verifikasi sintaksis Kotlin/XML lokal sebelum commit/push.
