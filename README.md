# Teman Kuliah

Aplikasi Android untuk mahasiswa Indonesia: jadwal kuliah, mata kuliah, KRS/KHS (impor lewat OCR), tugas dan proyek, kalender akademik, pengingat/alarm layar penuh native, dan mode fokus belajar.

Semua data disimpan **lokal di perangkat** (Room/SQLite). Tidak ada server dan tidak ada akun cloud.

- Dokumen produk, aturan kerja, dan backlog: [`PRD.md`](PRD.md) (**baca bagian 1 dan 2 sebelum mengubah kode**)
- Paket: `com.temankuliah.app` · `minSdk 24` · `targetSdk 36` · `compileSdk 37`
- Stack: Kotlin 2.2.10, Jetpack Compose (Material 3 BOM 2026.09.00), Room, Glance, ML Kit Text Recognition (model bawaan)

## Fitur Utama & Modul Terbaru

1. **Pengingat & Alarm Layar Penuh Native (`com.temankuliah.app.alarm`):**
   - Alarm perkuliahan presisi menggunakan `AlarmManager.setAlarmClock()`.
   - `AlarmService` berjalan sebagai Foreground Service (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`) dengan audio pemanggil `res/raw/alarm_clock.mp3` dan fallback berlapis bawaan sistem (`TYPE_ALARM` -> `TYPE_RINGTONE` -> `TYPE_NOTIFICATION`).
   - Peningkatan volume alarm otomatis ke batas maksimal saat berdering dan pemulihan volume saat alarm dimatikan (*Stop*) atau ditunda (*Snooze* 5 menit).
   - Getaran berulang kontinu (*waveform*), akuisisi AudioFocus `AUDIOFOCUS_GAIN_TRANSIENT`, dan `WakeLock` terkelola.
   - Layar penuh interaktif (`AlarmActivity`) dengan tombol Stop dan Snooze yang aman terhadap gestur kembali (*back button*).

2. **Sistem Animasi & Mikro-Interaksi Terpadu (`ui/components/Motion.kt`):**
   - **Universal Press Feedback (`pressScale`):** Observasi pasif pointer (`PointerEventPass.Initial`) untuk feedback skala pegas (*bouncy spring* ke 0.97f) pada tombol, kartu `TkCard`, dan `Surface` tanpa memblokir klik/gestur.
   - **Staggered Entrance Stabil:** Penyimpanan status kemunculan via `rememberSaveable` untuk mencegah kedipan/re-animasi saat daftar `LazyColumn` digulir (*anti-scroll-flicker*), dengan pembatasan delay maksimal 5 item.
   - **Bilah Navigasi Dinamis (`TkBottomNav`):** Indikator aktif berbasis pegas dengan pemanjangan dinamis (3.5dp ke 12dp) dan pantulan skala ikon saat dipilih.
   - **Transisi Dialog Layar Penuh (`TkFullScreenDialog`):** Animasi masuk *fade-in* (180ms) + translasi vertikal pegas (24dp).
   - **Transisi Antartab Searah:** Transisi geser horizontal + *fade* M3 pada peralihan antartab Tugas Mandiri dan Proyek Kuliah.
   - **Dukungan Aksesibilitas Penuh:** Mematuhi pengaturan sistem *Reduced Motion* (`rememberReducedMotion()`).

3. **Tema Terang & Gelap Adaptif:**
   - Dukungan penuh mode Terang, Gelap, dan Ikuti Sistem di seluruh layar, bilah navigasi, kartu, dan komponen dengan rasio kontras tinggi.

## Prasyarat

- Android Studio terbaru dan JDK 17
- Android SDK platform 37

## Menjalankan secara lokal

1. Buka Android Studio, pilih **Open**, lalu pilih folder proyek ini.
2. Tunggu sinkronisasi Gradle selesai.
3. Jalankan varian `debug` di emulator atau perangkat.

Tidak ada API key atau file `.env` yang dibutuhkan. Build debug memakai debug keystore bawaan Android, sehingga clone baru langsung bisa dibangun.

```bash
./gradlew :app:assembleDebug
```

## Quality gate

Semua perintah ini harus lulus sebelum perubahan dianggap selesai (sama dengan CI di `.github/workflows/android-ci.yml`):

```bash
./gradlew :app:lintDebug            # warningsAsErrors, terhadap lint-baseline.xml
./gradlew :app:detekt               # terhadap detekt-baseline.xml
./gradlew :buildHealth              # analisis dependency
./gradlew :app:testDebugUnitTest    # unit test (JUnit + Robolectric, 426+ tests)
./gradlew :app:assembleDebug
```

Jangan menambah entri baru ke `detekt-baseline.xml` atau `lint-baseline.xml`.

## Build release

Build release memakai minify dan shrink resources. Keystore dibaca dari environment variable (jangan simpan keystore atau sandi di repo):

| Variabel | Keterangan |
|---|---|
| `KEYSTORE_PATH` | Path file keystore |
| `STORE_PASSWORD` | Sandi keystore |
| `KEY_ALIAS` | Alias kunci (default `upload`) |
| `KEY_PASSWORD` | Sandi kunci |

```bash
KEYSTORE_PATH=/path/ke/upload.jks STORE_PASSWORD=... KEY_PASSWORD=... \
  ./gradlew :app:assembleRelease
```

Tanpa `KEYSTORE_PATH`, build release tetap berjalan tetapi **tidak ditandatangani** (Gradle menampilkan peringatan).

Untuk debug keystore kustom, atur `DEBUG_KEYSTORE_PATH` (opsional: `DEBUG_STORE_PASSWORD`, `DEBUG_KEY_ALIAS`, `DEBUG_KEY_PASSWORD`) atau taruh `debug.keystore` di root proyek (sudah diabaikan oleh git).

## Arsitektur singkat

```
alarm/     layar & pengendali alarm native layar penuh (AlarmService, AlarmActivity, Scheduler, Receiver)
ui/        layar Compose + controller per fitur + sistem animasi Motion.kt
data/      Room, repository, OCR/dokumen, pengingat/notifikasi, cadangan lokal terenkripsi
domain/    aturan murni (jadwal, nilai/IPK, reminder, rekonsiliasi KRS/KHS)
widget/    widget "Hari ini" (Glance)
```

Satu modul `:app`. Skema Room diekspor ke `app/schemas/`; perubahan skema wajib menaikkan versi, menambah `Migration`, dan menjalankan `MigrationGuardTest`.
