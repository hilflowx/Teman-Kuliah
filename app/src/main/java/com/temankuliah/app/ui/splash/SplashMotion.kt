package com.temankuliah.app.ui.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.unit.dp

/**
 * Semua angka waktu, jeda, easing, dan ukuran gerak splash ada di sini supaya mudah disetel.
 * Satuan waktu: milidetik, dihitung sejak splash pertama kali tampil (t = 0).
 *
 * Linimasa masuk (sesuai spesifikasi):
 *  0 - 400   latar fade in (lingkaran dekoratif drift terus-menerus)
 *  150 - 750 ikon scale 0.6 -> 1.0 (spring, overshoot kecil) + fade in
 *  300 - 900 dua halo mengembang + fade in (stagger 120), lalu berdenyut
 *  500 - 900 judul naik 16dp + fade in; subjudul menyusul 150 ms kemudian
 *  700 - 1000 bar loading dan teks status fade in
 *  700 - 2200 bar terisi 0 -> 100% (ease-in-out, 1,5 detik), tertahan di 90% bila data belum siap
 */
internal object SplashMotion {

    // ---- Durasi tampil ---------------------------------------------------------------------
    /** Splash tidak boleh hilang sebelum waktu ini (mulai keluar paling cepat di sini). */
    const val MIN_VISIBLE_MS = 1_800L

    /** Batas atas: setelah ini splash keluar walau data belum siap (tujuan: layar muat bawaan aplikasi). */
    const val MAX_VISIBLE_MS = 3_000L

    // ---- Latar & lingkaran dekoratif -------------------------------------------------------
    const val BG_FADE_DELAY_MS = 0L
    const val BG_FADE_MS = 400

    /** Satu siklus drift (kiri-kanan-kembali) per lingkaran; sengaja berbeda supaya tidak sinkron. */
    const val DRIFT_PERIOD_TOP_RIGHT_MS = 14_000
    const val DRIFT_PERIOD_LEFT_MS = 17_000
    const val DRIFT_PERIOD_BOTTOM_MS = 20_000
    val DRIFT_AMPLITUDE = 14.dp

    // ---- Ikon ------------------------------------------------------------------------------
    const val ICON_DELAY_MS = 0L
    const val ICON_FADE_MS = 360
    const val ICON_SCALE_FROM = 0.6f

    /** Spring ikon: damping sedang (~0.6) menghasilkan overshoot sekitar 4%, mengendap kira-kira 600 ms. */
    const val ICON_SPRING_DAMPING = 0.6f
    const val ICON_SPRING_STIFFNESS = 120f

    // ---- Halo ------------------------------------------------------------------------------
    const val HALO_INNER_DELAY_MS = 300L
    const val HALO_STAGGER_MS = 120L
    const val HALO_OUTER_DELAY_MS = HALO_INNER_DELAY_MS + HALO_STAGGER_MS
    const val HALO_EXPAND_MS = 480

    /** Denyut halo: satu siklus penuh (1.0 -> 1.04 -> 1.0) = 2 detik. */
    const val HALO_PULSE_CYCLE_MS = 2_000
    const val HALO_PULSE_SCALE = 1.04f

    // ---- Judul & subjudul ------------------------------------------------------------------
    const val TITLE_DELAY_MS = 500L
    const val TITLE_MS = 400
    const val SUBTITLE_STAGGER_MS = 150L
    val TITLE_RISE = 16.dp

    // ---- Bar loading & teks status ---------------------------------------------------------
    const val FOOTER_DELAY_MS = 700L
    const val FOOTER_FADE_MS = 300
    const val BAR_FILL_DELAY_MS = 700L
    const val BAR_FILL_MS = 1_500

    /** Bila data belum siap saat bar mencapai nilai ini, bar berhenti di sini. */
    const val BAR_HOLD_FRACTION = 0.9f

    /** Waktu menyelesaikan 90% -> 100% setelah data akhirnya siap. */
    const val BAR_FINISH_MS = 280

    // ---- Transisi keluar (total sekitar 350 ms) --------------------------------------------
    const val EXIT_TEXT_MS = 200
    const val EXIT_ICON_MS = 220
    const val EXIT_ICON_SCALE_TO = 0.9f
    const val EXIT_REVEAL_DELAY_MS = 130L
    const val EXIT_REVEAL_MS = 220
    const val EXIT_TOTAL_MS = EXIT_REVEAL_DELAY_MS + EXIT_REVEAL_MS

    // ---- Mode "reduce motion": hanya fade sederhana ----------------------------------------
    const val REDUCED_FADE_MS = 250

    // ---- Easing ----------------------------------------------------------------------------
    /** Ease-in-out standar CSS (0.42, 0, 0.58, 1) untuk pengisian bar. */
    val EaseInOutStandard: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

    /** Fade/slide masuk: melambat di akhir. */
    val EaseEnter: Easing = LinearOutSlowInEasing

    /** Fade keluar: mempercepat di akhir. */
    val EaseExit: Easing = FastOutLinearInEasing

    /** Denyut dan drift: lembut di kedua ujung. */
    val EaseBreathing: Easing = EaseInOutStandard
}

/**
 * Aturan waktu tampil splash sebagai fungsi murni (mudah diuji tanpa UI).
 */
internal object SplashGate {

    /**
     * Waktu tunggu tambahan (ms) sebelum transisi keluar boleh mulai, dihitung dari [elapsedMs]
     * saat semua syarat lain (data siap, bar penuh) sudah terpenuhi. 0 bila minimal sudah terlewati.
     */
    fun remainingMinimumMs(elapsedMs: Long): Long =
        (SplashMotion.MIN_VISIBLE_MS - elapsedMs).coerceAtLeast(0L)

    /** Sisa waktu sampai batas atas; dipakai sebagai timeout menunggu data. */
    fun remainingMaximumMs(elapsedMs: Long): Long =
        (SplashMotion.MAX_VISIBLE_MS - elapsedMs).coerceAtLeast(0L)

    /** Apakah bar harus berhenti di [SplashMotion.BAR_HOLD_FRACTION] karena data belum siap. */
    fun shouldHoldBar(progress: Float, dataReady: Boolean): Boolean =
        !dataReady && progress >= SplashMotion.BAR_HOLD_FRACTION
}
