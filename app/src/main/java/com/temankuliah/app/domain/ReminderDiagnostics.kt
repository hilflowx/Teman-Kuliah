package com.temankuliah.app.domain

/** Hasil satu pemeriksaan pada tes pengingat. */
enum class CheckStatus { OK, INFO, WARNING, FAILED }

data class ReminderCheck(val title: String, val status: CheckStatus, val detail: String)

/** Status satu saluran notifikasi Android; [importance] memakai nilai NotificationManager.IMPORTANCE_*. */
data class ChannelState(val label: String, val importance: Int, val hasSound: Boolean)

/**
 * Potret kondisi perangkat saat tes. Semua nilai dibaca dari sistem Android, tidak ditebak,
 * dan dipisahkan dari aturan penilaian di bawah supaya aturannya bisa diuji tanpa perangkat.
 */
data class ReminderEnvironment(
    val notificationsEnabled: Boolean,
    val exactAlarmAllowed: Boolean,
    val soundPreferred: Boolean,
    val vibrationPreferred: Boolean,
    /** Kosong di Android 7.x (belum ada saluran notifikasi). */
    val channels: List<ChannelState>,
    val doNotDisturbActive: Boolean,
    val ringerNormal: Boolean,
    val notificationVolume: Int
)

private const val IMPORTANCE_NONE = 0
private const val IMPORTANCE_HIGH = 4
private const val MILLIS_PER_SECOND = 1000.0

/** Selisih terbesar antara waktu alarm yang diminta dan waktu terpicunya yang masih dianggap tepat waktu. */
const val ALARM_ON_TIME_TOLERANCE_MS = 5_000L

/**
 * Menilai kondisi perangkat untuk tes pengingat. [includeAlarm] menambahkan pemeriksaan izin alarm presisi
 * (relevan untuk tes alarm, tidak untuk tes notifikasi langsung).
 */
fun evaluateReminderEnvironment(env: ReminderEnvironment, includeAlarm: Boolean): List<ReminderCheck> {
    val checks = mutableListOf<ReminderCheck>()

    checks += if (env.notificationsEnabled) {
        ReminderCheck("Izin notifikasi", CheckStatus.OK, "Notifikasi TemanKuliah diizinkan.")
    } else {
        ReminderCheck("Izin notifikasi", CheckStatus.FAILED, "Notifikasi TemanKuliah belum diizinkan, jadi pengingat tidak akan tampil.")
    }

    if (env.channels.isNotEmpty()) {
        val blocked = env.channels.filter { it.importance <= IMPORTANCE_NONE }
        val noBanner = env.channels.filter { it.importance in (IMPORTANCE_NONE + 1) until IMPORTANCE_HIGH }
        checks += when {
            blocked.isNotEmpty() -> ReminderCheck(
                "Saluran notifikasi", CheckStatus.FAILED,
                "Saluran ${blocked.joinToString { it.label }} dimatikan di pengaturan Android."
            )
            noBanner.isNotEmpty() -> ReminderCheck(
                "Saluran notifikasi", CheckStatus.WARNING,
                "Saluran ${noBanner.joinToString { it.label }} tidak memakai banner (kepentingan diturunkan di pengaturan Android)."
            )
            else -> ReminderCheck("Saluran notifikasi", CheckStatus.OK, "Saluran ${env.channels.joinToString { it.label }} aktif dengan banner.")
        }
    }

    checks += soundCheck(env)
    checks += vibrationCheck(env)

    if (includeAlarm) {
        checks += if (env.exactAlarmAllowed) {
            ReminderCheck("Alarm presisi", CheckStatus.OK, "Alarm presisi diizinkan.")
        } else {
            ReminderCheck("Alarm presisi", CheckStatus.WARNING, "Alarm presisi belum diizinkan. Pengingat tetap berjalan, tetapi bisa terlambat.")
        }
    }
    return checks
}

private fun soundCheck(env: ReminderEnvironment): ReminderCheck {
    if (!env.soundPreferred) {
        return ReminderCheck("Bunyi", CheckStatus.INFO, "Bunyi dimatikan di preferensi, jadi pengingat tampil tanpa suara.")
    }
    val problems = mutableListOf<String>()
    val silentChannels = env.channels.filter { it.importance > IMPORTANCE_NONE && !it.hasSound }
    if (silentChannels.isNotEmpty()) problems += "Saluran ${silentChannels.joinToString { it.label }} disenyapkan di pengaturan Android."
    if (env.doNotDisturbActive) problems += "Mode Jangan Ganggu aktif dan bisa membungkam bunyi."
    if (!env.ringerNormal) problems += "Ponsel sedang dalam mode getar atau senyap."
    if (env.notificationVolume <= 0) problems += "Volume notifikasi 0."
    return if (problems.isEmpty()) {
        ReminderCheck(
            "Bunyi", CheckStatus.OK,
            "Pengaturan perangkat mengizinkan bunyi. Aplikasi tidak bisa mendengar sendiri, jadi pastikan kamu mendengarnya."
        )
    } else {
        ReminderCheck("Bunyi", CheckStatus.WARNING, problems.joinToString(" "))
    }
}

private fun vibrationCheck(env: ReminderEnvironment): ReminderCheck {
    if (!env.vibrationPreferred) {
        return ReminderCheck("Getaran", CheckStatus.INFO, "Getaran dimatikan di preferensi.")
    }
    return if (env.doNotDisturbActive) {
        ReminderCheck("Getaran", CheckStatus.WARNING, "Mode Jangan Ganggu aktif dan bisa menahan getaran.")
    } else {
        ReminderCheck("Getaran", CheckStatus.OK, "Pengaturan perangkat mengizinkan getaran.")
    }
}

/**
 * Menilai pengiriman notifikasi uji. [posted] = Android menerima panggilan notify tanpa galat;
 * [visible] = notifikasi terbaca di daftar notifikasi aktif (null bila tidak dapat diperiksa).
 */
fun evaluateNotificationDelivery(permitted: Boolean, posted: Boolean, visible: Boolean?): ReminderCheck = when {
    !permitted -> ReminderCheck("Notifikasi uji", CheckStatus.FAILED, "Tidak dikirim karena notifikasi TemanKuliah belum diizinkan.")
    !posted -> ReminderCheck("Notifikasi uji", CheckStatus.FAILED, "Android menolak notifikasi uji.")
    visible == true -> ReminderCheck("Notifikasi uji", CheckStatus.OK, "Notifikasi uji tampil di daftar notifikasi aktif.")
    visible == false -> ReminderCheck(
        "Notifikasi uji", CheckStatus.FAILED,
        "Notifikasi dikirim, tetapi tidak muncul di daftar notifikasi aktif. Kemungkinan diblokir oleh sistem."
    )
    else -> ReminderCheck("Notifikasi uji", CheckStatus.WARNING, "Notifikasi dikirim, tetapi statusnya tidak dapat diperiksa di perangkat ini.")
}

/**
 * Menilai tes alarm. [firedAtMillis] null berarti alarm tidak terpicu sampai batas tunggu [waitedSeconds] detik.
 * [exactRequested] = alarm dipasang sebagai alarm presisi.
 */
fun evaluateAlarmFire(triggerAtMillis: Long, firedAtMillis: Long?, exactRequested: Boolean, waitedSeconds: Int): ReminderCheck {
    if (firedAtMillis == null) {
        val hint = if (exactRequested) {
            "Periksa hemat baterai atau pembatasan aplikasi latar belakang di pengaturan perangkat."
        } else {
            "Alarm presisi belum diizinkan, sehingga Android boleh menunda alarm jauh lebih lama."
        }
        return ReminderCheck("Alarm terpicu", CheckStatus.FAILED, "Alarm tidak terpicu dalam $waitedSeconds detik. $hint")
    }
    val lateMillis = (firedAtMillis - triggerAtMillis).coerceAtLeast(0L)
    val lateSeconds = lateMillis / MILLIS_PER_SECOND
    val lateText = "%.1f".format(java.util.Locale.US, lateSeconds).replace('.', ',')
    return if (exactRequested && lateMillis <= ALARM_ON_TIME_TOLERANCE_MS) {
        ReminderCheck("Alarm terpicu", CheckStatus.OK, "Alarm terpicu tepat waktu (selisih $lateText detik).")
    } else if (exactRequested) {
        ReminderCheck("Alarm terpicu", CheckStatus.WARNING, "Alarm terpicu, tetapi terlambat $lateText detik. Hemat baterai bisa menunda alarm.")
    } else {
        ReminderCheck("Alarm terpicu", CheckStatus.WARNING, "Alarm terpicu dengan selisih $lateText detik. Tanpa izin alarm presisi, waktunya tidak terjamin.")
    }
}

/** Status terburuk dari kumpulan pemeriksaan; INFO tidak memengaruhi hasil. */
fun reminderTestVerdict(checks: List<ReminderCheck>): CheckStatus = when {
    checks.any { it.status == CheckStatus.FAILED } -> CheckStatus.FAILED
    checks.any { it.status == CheckStatus.WARNING } -> CheckStatus.WARNING
    else -> CheckStatus.OK
}
