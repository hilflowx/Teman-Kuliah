package com.temankuliah.app.domain

import java.util.Locale

/**
 * Sumber tunggal nilai huruf dan bobotnya. Dipakai GpaCalculator, CourseRepository, pembaca dokumen
 * (OCR), validasi restore cadangan, dan semua pilihan nilai di UI. Jangan menulis daftar nilai di tempat lain.
 *
 * Nilai yang didukung hanya yang punya bobot di [POINTS] ditambah [UNGRADED] ("belum bernilai").
 * Kode seperti BL/TL/T/K/P (belum lengkap, tunda, dst.) SENGAJA tidak didukung: IPK memberinya bobot 0,
 * UI tidak bisa memilih atau mengeditnya, dan repository menolaknya, sehingga cadangan yang memuatnya
 * tidak akan bisa dipakai lagi setelah dipulihkan. Cadangan seperti itu ditolak dengan pesan jelas.
 */
object Grades {
    const val UNGRADED = "-"

    /** Huruf -> bobot (skala 4). Urutan = urutan tampil di UI; untuk bobot kembar, huruf pertama adalah huruf kanonik. */
    val POINTS: Map<String, Double> = linkedMapOf(
        "A" to 4.0,
        "A-" to 3.75,
        "AB" to 3.5,
        "B+" to 3.5,
        "B" to 3.0,
        "B-" to 2.75,
        "BC" to 2.5,
        "C+" to 2.5,
        "C" to 2.0,
        "D" to 1.0,
        "E" to 0.0
    )

    /** Pilihan nilai di UI, dari yang tertinggi, diakhiri [UNGRADED]. */
    val OPTIONS: List<String> = POINTS.keys.toList() + UNGRADED

    /** Semua nilai yang sah disimpan: nilai bernilai dan [UNGRADED]. */
    val VALID: Set<String> = OPTIONS.toSet()

    /** Menormalkan masukan pengguna/dokumen: buang spasi tepi dan jadikan huruf besar. */
    fun normalize(letter: String): String = letter.trim().uppercase(Locale.ROOT)

    /** [letter] harus sudah dinormalkan ([normalize]). */
    fun isValid(letter: String): Boolean = letter in VALID

    fun isGraded(letter: String): Boolean = POINTS.containsKey(normalize(letter))

    /** Bobot nilai, atau 0.0 untuk nilai kosong/tidak dikenal. */
    fun pointOf(letter: String): Double = POINTS[normalize(letter)] ?: 0.0

    /**
     * Huruf kanonik untuk [point] (toleransi pembulatan dokumen), atau null. Bobot 3.5 menghasilkan "AB" dan
     * 2.5 menghasilkan "BC" (huruf pertama di [POINTS]); B+/C+ memiliki bobot sama, jadi IPK tidak terpengaruh.
     */
    fun letterForPoint(point: Double): String? = lettersForPoint(point).firstOrNull()

    /** Semua huruf yang berbobot [point], mis. 3.5 -> [AB, B+]. */
    fun lettersForPoint(point: Double): List<String> =
        POINTS.entries.filter { kotlin.math.abs(it.value - point) < 0.011 }.map { it.key }
}
