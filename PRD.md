# PRD — Teman Kuliah (Android)

**Versi:** 1.70 (1 November 2026 — Audit & Penyempurnaan Sistem Motion Stage 4 dan Modul Alarm Layar Penuh Native dengan Audio Kustom)  
**Basis Kode:** `Teman-Kuliah-V1` (`versionName 1.0.1`, `versionCode 2`)  
**Pemilik:** Xarverth  
**Target Platform:** Android 7.0+ (`minSdk 24`, `targetSdk 36`, `compileSdk 37`)

---

## 1. Ringkasan Produk

**Teman Kuliah** adalah aplikasi asisten perkuliahan untuk mahasiswa di Indonesia. Aplikasi memusatkan jadwal kuliah, tugas, proyek, kalender akademik, catatan KRS/KHS, dan alarm pengingat dalam satu wadah.

### Prinsip Utama
1. **Offline-First & Terisolasi:** Seluruh data tersimpan lokal di perangkat via Room Database (SQLite). Tidak ada server eksternal, akun cloud, maupun analitik pihak ketiga.
2. **Keamanan & Integritas Data:** Data terikat ketat pada `userId`. Skema database terjaga tanpa `fallbackToDestructiveMigration`. Proses *logout* membersihkan seluruh alarm dan status memori tanpa sisa.
3. **Standar Desain & Bahasa:** Antarmuka **100% Bahasa Indonesia baku**. Menggunakan **Mode Terang (*Light Theme*) M3** terpadu dengan tipografi Poppins, kartu sudut membulat, dan kontras tajam. **Tema Gelap** dapat dipilih (Terang / Gelap / Ikuti sistem di Profil > Tampilan) dan seluruh layarnya sudah terverifikasi dan lulus quality gate.
4. **Izin Terkendali:** Izin jaringan (`INTERNET`, `ACCESS_NETWORK_STATE`) dipertahankan sesuai keputusan pemilik untuk kebutuhan ekstensi masa depan, dengan model OCR ML Kit berjalan lokal (*bundled*).

---

## 2. Aturan Kerja AI Agent (Wajib)

1. **Pertahankan Fondasi Proyek:** Jangan buat ulang struktur direktori, paket (`com.temankuliah.app`), skema Room, atau palet warna dasar.
2. **Ubah Seminimal & Sepresisi Mungkin:** Modifikasi hanya berkas dan fungsi yang diminta.
3. **Database & Migrasi:** Setiap perubahan skema wajib menaikkan `DATABASE_SCHEMA_VERSION`, menambahkan fungsi `Migration`, menyimpan berkas skema di `app/schemas/`, dan lolos `MigrationGuardTest`.
4. **Uji Nyata:** Buat dan jalankan unit test untuk setiap logika bisnis baru atau perbaikan bug.
5. **Kualitas Kode:** Wajib lolos `warningsAsErrors` pada compiler dan lint tanpa menambah entri baseline detekt/lint.
6. **Navigasi Konsisten:** Setiap layar turunan wajib menyediakan tombol kembali ("←") pada top bar dan terhubung dengan `BackHandler`.

---

## 3. Arsitektur & Teknologi

- **Bahasa & UI:** Kotlin 2.2.10, Jetpack Compose (BOM 2026.09.00), Material Design 3.
- **Penyimpanan Lokal:** Room 2.8.4 (KSP) Skema v3, `SharedPreferences` privat untuk preferensi ringan (`AppStorage`).
- **Pola Arsitektur:** Single Activity (`MainActivity`), `TemanKuliahViewModel` berbasis Controller modular (`FeatureController`), StateFlow reaktif, serta pemisahan murni layer domain.
- **Enkripsi & Keamanan:** Sandi pengguna di-hash PBKDF2, verifikasi kunci layar perangkat untuk reset/pembuatan sandi, cadangan data terenkripsi AES-256-GCM (`BackupCrypto`).
- **Alarm & Notifikasi:** `AlarmManager` presisi (`setAlarmClock`), modul alarm native layar penuh (`AlarmService` + `AlarmActivity` + audio kustom `res/raw/alarm_clock.mp3`) untuk kuliah, dan notifikasi tepat waktu untuk tugas.
- **Widget:** Android Glance 1.1.1 ("Hari Ini") dengan pembaruan otomatis di setiap batas waktu agenda.
- **OCR:** Google ML Kit Text Recognition (*bundled model* offline).

```
app/src/main/java/com/temankuliah/app/
├── alarm/            # Layar & pengendali alarm native layar penuh (AlarmService, AlarmActivity, Scheduler, Receiver)
├── data/
│   ├── backup/       # Enkripsi & arsip cadangan lokal (.tkbk)
│   ├── files/        # Parser dokumen KRS/KHS & OCR ML Kit
│   ├── local/        # Room Database, Entitas, DAO, Migrasi
│   ├── notifications/# Receiver, scheduler, saluran notifikasi
│   └── repository/   # Repositori data per modul
├── domain/           # Logika murni (IPK, SKS, Jadwal, Throttle, dsb.)
├── ui/
│   ├── academic/     # Dasbor akademik, KRS, KHS, dialog impor
│   ├── auth/         # Masuk, daftar, kunci layar, lupa sandi
│   ├── calendar/     # Kalender agenda bulanan & mingguan
│   ├── components/   # Sistem animasi terpadu (Motion.kt) & komponen UI M3
│   ├── courses/      # Detail mata kuliah, transkrip nilai, cetak/PDF
│   ├── dashboard/    # Beranda & ringkasan hari ini
│   ├── profile/      # Pengaturan akun, tampilan, privasi
│   ├── schedule/     # Pengaturan jadwal kuliah & kelas online
│   ├── tasks/        # Manajemen tugas kuliah
│   └── projects/     # Pelacakan proyek & tahapan (milestones)
└── widget/           # Widget Glance "Hari Ini"
```

---

## 4. Status Modul & Fitur

| Modul | Cakupan Fitur | Status |
|---|---|---|
| **Otentikasi Lokal** | Masuk (Email/NIM + PBKDF2), Daftar, Mode Tamu, Kunci Layar untuk Buat/Reset Sandi, Pembatasan Gagal Masuk (*Throttle* per `userId`). | **Stabil** (Kode + Test) |
| **Beranda** | Ringkasan agenda harian, status kuliah berlangsung/berikutnya, pintasan cepat. | **Stabil** |
| **Kalender** | Tampilan bulanan/mingguan, penanda titik kegiatan perkuliahan, filter jadwal/tugas/ujian. | **Stabil** (Kode + Test) |
| **Tugas & Proyek** | Tugas mandiri, proyek kelompok, tenggat waktu, pembagian tahapan (*milestones*), status selesai. Transisi antartab searah M3. | **Stabil** (Kode + Test) |
| **Akademik** | Semester Aktif, KRS, KHS, **Target SKS Dinamis (default 144)**, Ringkasan Progres Kelulusan, Transkrip Nilai (Cetak/PDF). Berkas modular terpisah (<500 baris). | **Stabil** (Kode + Test) |
| **Impor Dokumen** | Pindai/Unggah berkas KRS/KHS, ekstraksi OCR, **dialog pratinjau dengan pemilih semester sasaran (1–8+)**, rekonsiliasi data per semester tanpa tumpang tindih. | **Stabil** (Kode + Test) |
| **Pengingat & Alarm** | Pemisahan jalur: **Kuliah** via modul alarm layar penuh native (`com.temankuliah.app.alarm`), Foreground Service (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`) dengan audio `res/raw/alarm_clock.mp3`, fallback bertingkat (TYPE_ALARM -> TYPE_RINGTONE -> TYPE_NOTIFICATION), boosting volume alarm maksimal, getaran berulang, WakeLock, dan layar penuh `AlarmActivity` (Stop/Snooze 5 menit). **Tugas** via notifikasi terjadwal dengan aksi Selesai/Tunda (tanpa alarm layar penuh). **Ujian, kegiatan, dan acara kalender** punya pengingat alarm opsional (saklar + 0–180 menit sebelum mulai) lewat jalur alarm layar penuh yang sama dengan kuliah; kolom `alarmEnabled`/`reminderMinutesBefore` di `academic_events` (skema v4, migrasi 3→4). | **Stabil** (Kode + Test) |
| **Tampilan & Tema** | **100% Bahasa Indonesia baku**, tipografi Poppins, tema Terang / Gelap / Ikuti sistem. Seluruh tahapan 0–5 selesai dan terverifikasi via test suite & build. | **Stabil** (Kode + Test) |
| **Isolasi Sesi (Logout)** | Pembersihan alarm latar belakang (`cancelAllAlarms`), reset dialog, reset navigasi ke Beranda, dan pengosongan memori kredensial saat logout. | **Stabil** |
| **Widget Hari Ini** | Widget Glance dengan pembaruan dinamis di jam batas mulai/selesai kuliah dan tenggat tugas. | **Stabil** (Kode + Test) |
| **Cadangan & Pulihkan** | Ekspor/impor arsip lokal terenkripsi AES-256-GCM dengan sandi pengguna. Dukungan impor ZIP versi lama. | **Stabil** (Kode + Test) |
| **Sistem Animasi & Motion** | Mikro-interaksi `pressScale` universal (passive pointer observation non-blocking), `staggeredEntrance` anti-flicker scroll via `rememberSaveable`, bilah navigasi `TkBottomNav` dengan indikator aktif pegas & icon bounce, transisi masuk `TkFullScreenDialog` (fade + slide-up 24dp), transisi antartab searah M3 Tugas vs Proyek, dan kepatuhan penuh `rememberReducedMotion()`. | **Stabil** (Kode + Test) |
| **Keandalan Alarm & OEM** | Edukasi optimasi baterai OEM (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`), rekalkulasi otomatis waktu & zona waktu via `ReminderRescheduleReceiver`. | **Stabil** (Kode + Test) |

---

## 5. Model Data Inti (Room Skema v3)

Setiap entitas memuat atribut `userId` sebagai pengikat kepemilikan data:
- `users`: Identitas mahasiswa, NIM/Email, hash sandi PBKDF2, tipe akun (`local` / `guest`).
- `courses`: Mata kuliah per semester (kode, nama, SKS, nilai huruf, bobot, status tempuh).
- `schedules`: Jadwal kuliah (hari, jam mulai/selesai, ruangan, mode offline/online, tautan kelas).
- `academic_documents`: Arsip berkas KRS/KHS per semester dengan data JSON hasil ekstraksi.
- `tasks`: Tugas kuliah dengan tanggal tenggat, prioritas, kategori, dan status selesai.
- `projects` & `project_milestones`: Pelacakan proyek besar dan tahapan pengerjaan.
- `academic_events`: Kegiatan agenda kalender (ujian, seminar, libur akademik).

---

## 6. Standar Mutu (Quality Gate)

Setiap siklus kerja wajib memenuhi:
1. `compile_applet` / `./gradlew :app:assembleDebug` sukses tanpa error.
2. `./gradlew :app:testDebugUnitTest` seluruh pengujian unit (logika akademik, alarm, auth, widget, motion) lulus.
3. Nol penambahan entri baru pada `lint-baseline.xml` dan `detekt-baseline.xml`.
4. Kode baru mematuhi aturan pembatalan coroutine (`rethrowIfCancellation`).

---

## 7. Rencana Perbaikan Tema Gelap

Seluruh perbaikan tema gelap telah diintegrasikan di basis kode dan diverifikasi melalui unit test serta proses build.

### 7.1. Dasar yang sudah ada (acuan implementasi)
- `ThemeMode` (Terang / Gelap / Ikuti sistem) disimpan di `SharedPreferences` kunci `theme_mode`; bawaan Terang. Dikelola `PreferencesController.themeMode`, dipilih di Profil > Tampilan.
- `TkPalette.isDark` (state Compose) memilih `LightTkColors` / `DarkTkColors` di `ui/theme/TkPalette.kt`. Semua konstanta di `Color.kt` dan `DashboardColors.kt` adalah properti `get()` yang membaca palet aktif, jadi pemakai lama ikut berganti tema.
- `tkAdaptive(light, dark)` dipakai untuk warna tetap yang butuh padanan gelap di satu layar.
- Acuan desain: `beranda-dark.png` (latar #0A101C, kartu #111A2C, garis #1F2D47, ubin #1A2A48, teks #F2F6FF / #8C9CBA, aksen #3B8BF5 / #5AAAFF).
- Beranda, bilah navigasi, halaman Tampilan, Kalender, Tugas, dan Proyek sudah mengikuti tema gelap.
- Isian pekat berteks putih (tombol hapus/ganti data, lencana IPS) memakai token `TemanKuliahRedFill` / `TemanKuliahGreenFill`.
- Warna kategori agenda (`AgendaKind`, `ActivityKindColors`) sudah punya padanan gelap.

### 7.2. Tahap Verifikasi

| Tahap | Cakupan | Berkas utama | Status |
|---|---|---|---|
| 0 | Verifikasi dasar: `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`, `compile_applet`, dan `buildHealth` lulus | seluruh modul | **Selesai**: Lulus 100% pada lingkungan build |
| 1 | Akademik: dasbor, KRS, KHS, Transkrip Nilai, dialog impor dan pratinjau dokumen, lencana nilai | `ui/academic/`, `ui/courses/` | Selesai |
| 2 | Jadwal Kuliah dan dialog tambah/ubah (jadwal, tugas, mata kuliah), pemilih tanggal dan jam | `ui/schedule/`, `ui/components/Tk*Picker*`, `TkFormSheet`, `TkFullScreenDialog` | Selesai |
| 3 | Profil dan seluruh sub-halaman, Notifikasi, kartu OEM, lembar pemilih semester | `ui/profile/`, `ui/notifications/`, `ui/components/TkSemesterPickerSheet.kt` | Selesai |
| 4 | Layar masuk/daftar (`AuthColors.kt`), splash, widget Glance, tata letak notifikasi dan alarm layar penuh | `ui/auth/`, `widget/`, `res/drawable`, `res/layout`, `alarm/` | Selesai |
| 5 | Pengujian kontras gelap, pratinjau malam, pembaruan PRD | `ThemeColorContrastTest`, `AdaptivePreviews.kt` | Selesai |

### 7.3. Keputusan dan Pengecualian Desain
- **Tombol biru gelap (#3B8BF5) berteks putih (±3,4:1):** dipertahankan sesuai desain `beranda-dark.png`, karena warna yang sama juga dipakai sebagai teks tautan (±5,1:1 di atas kartu).
- **`SystemBarStyle`:** `MainActivity.applyEdgeToEdge` memakai gaya eksplisit sesuai tema aplikasi (bukan tema sistem) dan dipanggil ulang saat tema berganti.
- **`gradeBadgeColors`:** memakai padanan gelap (`tkAdaptive`); nilai terang tidak berubah.
- **Widget, splash, dan XML notifikasi:** dipertahankan berwarna biru merek dengan teks putih atau pil pastel berinsiyal gelap, yang terbaca di kedua tema.
- **`AlarmActivity`:** membaca `theme_mode` dan memakai warna gelap sendiri (dibangun dari kode Jetpack Compose).
- **Latar di balik bilah navigasi bawah:** `Scaffold` luar di `TkAdaptiveNavigationSuiteScaffold` memakai `containerColor = BackgroundLight`, dan layout rail samping dibungkus `background(BackgroundLight)`.

---

## 8. Catatan Rilis & Penyempurnaan Terbaru

### 8.1. Modul Alarm Native Layar Penuh (`com.temankuliah.app.alarm`)
- **Mekanisme Penjadwalan:** `AlarmScheduler.schedule()` menggunakan `AlarmManager.setAlarmClock()` dengan `PendingIntent.FLAG_IMMUTABLE` agar alarm tetap aktif menembus mode Doze.
- **Foreground Service:** `AlarmService` mengeksekusi `startForeground()` secara instan dengan tipe `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
- **Reproduksi Audio:** Menggunakan `res/raw/alarm_clock.mp3` dengan fallback berlapis bawaan sistem (`TYPE_ALARM` -> `TYPE_RINGTONE` -> `TYPE_NOTIFICATION`), `isLooping = true`, dan `AudioAttributes` (`USAGE_ALARM`, `CONTENT_TYPE_SONIFICATION`).
- **Volume & Fokus Audio:** Memaksa volume `STREAM_ALARM` ke batas maksimal saat berdering dan mengembalikan ke volume semula saat dihentikan. Meminta `AUDIOFOCUS_GAIN_TRANSIENT`.
- **Keandalan & Daya:** `WakeLock` bertipe `PARTIAL_WAKE_LOCK` dengan batas aman 10 menit, getaran kontinu via `Vibrator`/`VibratorManager`, serta tombol *Stop* dan *Snooze 5 menit* pada antarmuka layar penuh `AlarmActivity`.

### 8.2. Penyempurnaan Sistem Animasi & Mikro-Interaksi (Motion Stage 4)
- **Universal Press Feedback (`pressScale`):** Menggunakan observasi pasif gesture (`PointerEventPass.Initial`) tanpa mengonsumsi event klik. Seluruh kartu `TkCard`, tombol header `TkHeaderIconButton`, dan elemen interaktif kini merespons sentuhan dengan fisika pegas (*bouncy spring* ke 0.97f).
- **Stabilitas Daftar (`staggeredEntrance`):** Menyimpan status kemunculan item melalui `rememberSaveable { mutableStateOf(false) }` sehingga elemen yang sudah tampil tidak berkedip atau teranimasi ulang saat daftar `LazyColumn` digulir (*anti-scroll-flicker*). Delay dibatasi maksimal 5 item.
- **Navigasi Dinamis (`TkBottomNav` & `TkNavRail`):** Indikator aktif bilah navigasi bawah bertransisi dinamis dari titik kecil (3.5dp) menjadi pil memanjang (12dp) dengan spring easing, disertai pantulan skala ikon (1.08f) saat tab dipilih.
- **Dialog Masuk Mulus (`TkFullScreenDialog`):** Transisi masuk modal dialog layar penuh menggabungkan efek *alpha fade-in* (180ms) dan translasi vertikal ke atas (24dp).
- **Transisi Antartab M3 (`TasksScreen`):** Pergantian antartab Tugas Mandiri dan Proyek Kuliah menggunakan `AnimatedContent` dengan transisi geser horizontal searah tujuan dan *fade*.
- **Aksesibilitas Terpadu:** Seluruh efek animasi mematuhi `rememberReducedMotion()` — jika mode hemat gerak aktif pada sistem operasi perangkat, transisi disederhanakan secara otomatis.

### 8.3. Status Quality Gate & Build
- `compile_applet`: **SUCCEEDED**
- `./gradlew :app:testDebugUnitTest`: **426+ unit tests PASS** (termasuk `AlarmModuleTest`, `AlarmReliabilityTest`, dan `MotionStage4UnitTest`).
- `./gradlew :buildHealth`: **BUILD SUCCESSFUL**
- `./gradlew :app:assembleDebug`: **BUILD SUCCESSFUL**
- Nol entri baru pada `lint-baseline.xml` dan `detekt-baseline.xml`.
