package com.temankuliah.app.domain

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pemeriksa konsistensi angka hasil OCR untuk tabel KRS dan KHS. Semua fungsi murni (tanpa Android) agar
 * mudah diuji.
 *
 * Dua kaidah yang dipakai pada setiap BARIS (jadi aman walau foto hanya memuat sebagian tabel):
 *  - KRS: SKS = Teori + Praktek.
 *  - KHS: Mutu = SKS x Bobot, dan Bobot harus sama dengan bobot nilai huruf.
 *
 * Angka hanya dikoreksi bila hasilnya memenuhi kaidah di atas dan koreksinya tunggal; bila tidak pasti,
 * nilai dibiarkan seperti terbaca dan tidak ditebak.
 */
object AcademicReconciler {
    /** Toleransi pembulatan dokumen: mutu dicetak 2 desimal. */
    private const val MUTU_TOLERANCE = 0.051

    /**
     * Angka yang sering tertukar saat OCR membaca cetakan tabel kecil (0/O, 1/I/l, 2/Z, 3/E, 4/A, 5/S, 6/G, 7/T, 8/B, 9/g).
     * Dipakai hanya untuk mencari SATU koreksi yang membuat kaidah baris terpenuhi.
     */
    private val CONFUSABLE: Map<Int, List<Int>> = mapOf(
        0 to listOf(6, 8, 9),
        1 to listOf(7, 4),
        2 to listOf(7, 3),
        3 to listOf(8, 2, 5),
        4 to listOf(9, 1),
        5 to listOf(6, 3, 8),
        6 to listOf(5, 0, 8),
        7 to listOf(1, 2),
        8 to listOf(3, 0, 6),
        9 to listOf(4, 0)
    )

    // ---------------------------------------------------------------------------------------
    // Pembacaan angka
    // ---------------------------------------------------------------------------------------

    private val GRADE_OCR_PLUS = Regex("^[A-E]\\s?[T\u2020]$")
    private val DIGIT_LIKE = Regex("^[0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg–—-]{1,2}$")

    /** Token satu/dua karakter yang dapat berupa angka SKS atau salah baca OCR (O/0, I/1, Z/2, S/5, dan lainnya). */
    fun isDigitLike(token: String): Boolean = DIGIT_LIKE.matches(token.trim())

    /** Konversi glyph OCR ambigu menjadi digit menurut konteks kolom numerik; null bila bukan token angka 1-2 digit. */
    fun ocrInt(token: String): Int? {
        val t = token.trim()
        if (!DIGIT_LIKE.matches(t)) return null
        val mapped = t.map {
            when (it) {
                'O', 'o', 'Q', 'q', 'D', 'd' -> '0'
                'I', 'i', 'l', '|', '!' -> '1'
                'Z', 'z' -> '2'
                'S', 's' -> '5'
                'G' -> '6'
                'g' -> '9'
                'B', 'b' -> '8'
                'A', 'a' -> '4'
                'T', 't' -> '7'
                'E', 'e' -> '3'
                '–', '—', '-' -> '0'
                else -> it
            }
        }.joinToString("")
        return mapped.toIntOrNull()
    }

    private val DECIMAL_LIKE = Regex("^[0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg]{1,3}[.,][0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg]{1,2}$")

    /** "4,00", "12.OO", "3.5" -> angka desimal; null bila bukan angka desimal bertitik/berkoma. */
    fun parseDecimal(token: String): Double? {
        val t = token.trim()
        if (!DECIMAL_LIKE.matches(t)) return null
        if (t.none(Char::isDigit)) return null
        val mapped = t.map {
            when (it) {
                'O', 'o', 'Q', 'q', 'D', 'd' -> '0'
                'I', 'i', 'l', '|', '!' -> '1'
                'Z', 'z' -> '2'
                'S', 's' -> '5'
                'G' -> '6'
                'g' -> '9'
                'B', 'b' -> '8'
                'A', 'a' -> '4'
                'T', 't' -> '7'
                'E', 'e' -> '3'
                ',' -> '.'
                else -> it
            }
        }.joinToString("")
        return mapped.toDoubleOrNull()
    }

    // ---------------------------------------------------------------------------------------
    // KRS: SKS = Teori + Praktek
    // ---------------------------------------------------------------------------------------

    data class KrsCredits(val theory: Int, val practice: Int, val sks: Int, val repaired: Boolean, val consistent: Boolean)

    /**
     * Cocokkan Teori, Praktek, dan SKS tercetak ([printed], null bila tabel tidak punya kolom SKS).
     * Bila Teori + Praktek != SKS, cari TEPAT SATU koreksi satu angka (Teori, Praktek, atau SKS) yang
     * memenuhi kaidah. Jika tidak ada atau lebih dari satu, SKS memakai Teori + Praktek dan [consistent] = false.
     */
    fun repairKrsCredits(theory: Int, practice: Int, printed: Int?): KrsCredits {
        if (printed == null || theory + practice == printed) {
            return KrsCredits(theory, practice, theory + practice, repaired = false, consistent = true)
        }
        val fixes = linkedSetOf<Triple<Int, Int, Int>>()
        CONFUSABLE[theory].orEmpty().forEach { if (it + practice == printed) fixes += Triple(it, practice, printed) }
        CONFUSABLE[practice].orEmpty().forEach { if (theory + it == printed) fixes += Triple(theory, it, printed) }
        CONFUSABLE[printed].orEmpty().forEach { if (theory + practice == it) fixes += Triple(theory, practice, it) }
        fixes.removeAll { it.first !in 0..MAX_SKS_PER_COURSE || it.second !in 0..MAX_SKS_PER_COURSE }
        if (fixes.size == 1) {
            val (t, p, s) = fixes.first()
            return KrsCredits(t, p, s, repaired = true, consistent = true)
        }
        return KrsCredits(theory, practice, theory + practice, repaired = false, consistent = false)
    }

    /** Hasil memilih kolom angka pada baris KRS tanpa pemisah kolom. */
    data class KrsRun(
        /** Jumlah token di awal yang menjadi nama mata kuliah. */
        val nameTokenCount: Int,
        /** Jumlah token angka yang dipakai (2 = Teori, Praktek; 3 = Teori, Praktek, SKS). */
        val numberTokenCount: Int,
        val credits: KrsCredits
    )

    /**
     * Pilih kolom Teori/Praktek(/SKS) pada [tokens] (teks di antara kode dan hari, sudah dipecah per kata).
     *
     * Nama mata kuliah boleh berakhir angka ("Pemrograman Web 2", "Fisika 1"), jadi angka PERTAMA tidak boleh
     * dianggap kolom Teori. Kolom angka adalah deretan token angka TERAKHIR (sebelum nama dosen); dari deretan itu
     * dipilih jendela yang memenuhi Teori + Praktek = SKS, lalu jendela dua angka terakhir.
     */
    fun pickKrsNumberRun(tokens: List<String>, sksColumn: Boolean? = null): KrsRun? {
        var end = -1
        var start = -1
        var i = tokens.size - 1
        while (i >= 0) {
            if (isDigitLike(tokens[i])) {
                val runEnd = i
                while (i >= 0 && isDigitLike(tokens[i])) i--
                val runStart = i + 1
                if (runEnd - runStart + 1 >= 2) { end = runEnd; start = runStart; break }
            } else i--
        }
        if (start < 0) return null
        val values = (start..end).map { ocrInt(tokens[it]) ?: return null }

        // Jendela tiga angka (Teori, Praktek, SKS) hanya dipakai bila tabel tidak dipastikan tanpa kolom SKS.
        // Tanpa petunjuk kolom, hanya yang sudah memenuhi Teori + Praktek = SKS yang diterima; koreksi satu angka
        // baru dilakukan bila dokumen jelas punya kolom SKS.
        if (values.size >= 3 && sksColumn != false) {
            val w = values.takeLast(3)
            val credits = repairKrsCredits(w[0], w[1], w[2])
            if (credits.consistent && (sksColumn == true || !credits.repaired)) {
                return KrsRun(nameTokenCount = end - 2, numberTokenCount = 3, credits = credits)
            }
        }
        val w2 = values.takeLast(2)
        val credits = repairKrsCredits(w2[0], w2[1], null)
        return KrsRun(nameTokenCount = end - 1, numberTokenCount = 2, credits = credits)
    }

    // ---------------------------------------------------------------------------------------
    // KHS: Mutu = SKS x Bobot
    // ---------------------------------------------------------------------------------------

    data class KhsTail(
        /** Jumlah token di awal (setelah kode) yang menjadi nama mata kuliah. */
        val nameTokenCount: Int,
        val sks: Int,
        val grade: String,
        val bobot: Double,
        val mutu: Double,
        /** SKS atau nilai huruf dikoreksi dari Bobot dan Mutu yang tercetak. */
        val repaired: Boolean
    )

    /**
     * Baca ekor baris KHS berpola `... SKS NILAI BOBOT MUTU` dari belakang. Ekor diterima hanya bila
     * SKS x Bobot = Mutu terpenuhi (apa adanya, atau setelah SKS dikoreksi dari Mutu / Bobot) sehingga baris
     * berformat lain tidak tersentuh. Nilai huruf diambil dari Bobot bila terbaca salah atau hilang.
     *
     * [tokens] harus sudah tanpa nomor urut dan kode mata kuliah.
     */
    fun parseKhsTail(tokens: List<String>): KhsTail? {
        val n = tokens.size
        if (n < 5) return null
        val mutu = parseDecimal(tokens[n - 1]) ?: return null
        val bobot = parseDecimal(tokens[n - 2]) ?: return null
        if (bobot !in 0.0..4.0 || mutu < 0.0) return null
        val gradeToken = tokens[n - 3].trim()
        val sksToken = tokens[n - 4].trim()
        if (gradeToken.length > 2 || !sksToken.all { it.isLetterOrDigit() || it == '|' } || sksToken.length > 2) return null

        // Token SKS harus berupa angka (atau salah baca angka seperti "O"/"l"); selain itu bagian dari nama.
        val sksRead = ocrInt(sksToken) ?: return null
        fun consistent(s: Int) = s in 1..MAX_SKS_PER_COURSE && abs(s * bobot - mutu) <= MUTU_TOLERANCE

        val derived = if (bobot > 0.0) (mutu / bobot).roundToInt() else null
        val sks: Int
        var repaired = false
        when {
            consistent(sksRead) -> sks = sksRead
            derived != null && consistent(derived) -> { sks = derived; repaired = true }
            else -> return null
        }

        val normalizedRead = Grades.normalize(gradeToken).replace('\u2013', '-').replace('\u2014', '-').let {
            // "BT"/"B†" adalah salah baca "B+" yang sangat umum.
            if (GRADE_OCR_PLUS.matches(it)) it.first() + "+" else it
        }
        val gradeFromRead = normalizedRead.takeIf { Grades.isGraded(it) }
        val bobotLetters = Grades.lettersForPoint(bobot)
        val grade = when {
            gradeFromRead != null && abs(Grades.pointOf(gradeFromRead) - bobot) < 0.011 -> gradeFromRead
            bobotLetters.isNotEmpty() -> {
                repaired = true
                // Bobot kembar (3.5 = AB/B+): pilih huruf yang awalnya sama dengan yang terbaca.
                bobotLetters.firstOrNull { gradeToken.isNotEmpty() && it.first().equals(gradeToken.first(), ignoreCase = true) }
                    ?: bobotLetters.first()
            }
            gradeFromRead != null -> gradeFromRead
            else -> return null
        }
        return KhsTail(nameTokenCount = n - 4, sks = sks, grade = grade, bobot = bobot, mutu = mutu, repaired = repaired)
    }

    // ---------------------------------------------------------------------------------------
    // Ukuran gambar untuk OCR
    // ---------------------------------------------------------------------------------------

    /** ML Kit menganjurkan karakter minimal 16 x 16 piksel (di atas 24 piksel tidak menambah akurasi). */
    const val MIN_CHAR_PX = 16

    /**
     * Faktor perbesaran sebelum OCR kedua bila median tinggi teks [medianTextHeightPx] di bawah anjuran.
     * 1f berarti tidak perlu. Hasil dibatasi 3x dan sisi terpanjang gambar tidak melewati [maxEdge].
     */
    fun ocrUpscaleFactor(medianTextHeightPx: Int, longestEdgePx: Int, maxEdge: Int = 4000): Float {
        if (medianTextHeightPx <= 0 || longestEdgePx <= 0 || medianTextHeightPx >= MIN_CHAR_PX) return 1f
        val wanted = ceil(MIN_CHAR_PX * 1.15 / medianTextHeightPx).toFloat()
        val limit = maxEdge.toFloat() / longestEdgePx
        val factor = min(min(wanted, 3f), limit)
        return if (factor >= 1.2f) factor else 1f
    }

    /** Median dari [values]; 0 bila kosong. */
    fun median(values: List<Int>): Int = if (values.isEmpty()) 0 else values.sorted()[values.size / 2]

}
