package com.temankuliah.app.data.files

import com.temankuliah.app.data.files.AcademicDocumentProcessor.AcademicRow
import com.temankuliah.app.data.files.AcademicDocumentProcessor.gradeLetterToPoint
import com.temankuliah.app.data.files.AcademicNameText.hasLecturerHint
import com.temankuliah.app.data.files.AcademicNameText.hasLetters
import com.temankuliah.app.data.files.AcademicNameText.hasLeadingCourseCode
import com.temankuliah.app.data.files.AcademicNameText.normalizeCourseName
import com.temankuliah.app.data.files.AcademicNameText.normalizeLecturerName
import com.temankuliah.app.data.files.AcademicNameText.splitCode
import com.temankuliah.app.data.files.AcademicNameText.splitPipeDelimited
import com.temankuliah.app.data.files.AcademicScheduleText.addMinutes
import com.temankuliah.app.data.files.AcademicScheduleText.canonicalDay
import com.temankuliah.app.data.files.AcademicScheduleText.extractSchedule
import com.temankuliah.app.data.files.AcademicScheduleText.parseLamaMinutes
import com.temankuliah.app.data.files.AcademicScheduleText.parseScheduleParts
import com.temankuliah.app.data.files.AcademicTextPatterns.DAY_NAMES
import com.temankuliah.app.data.files.AcademicTextPatterns.DAY_REGEX
import com.temankuliah.app.data.files.AcademicTextPatterns.KRS_TOTAL
import com.temankuliah.app.data.files.AcademicTextPatterns.SINGLE_TIME_ANYWHERE
import com.temankuliah.app.data.files.AcademicTextPatterns.SKS_SUFFIX
import com.temankuliah.app.data.files.AcademicTextPatterns.TIME_RANGE
import com.temankuliah.app.data.files.AcademicTextPatterns.WHITESPACE
import com.temankuliah.app.data.files.KhsRowParser.gradeFromNumbers
import com.temankuliah.app.data.files.KhsRowParser.normalizeGradeToken
import com.temankuliah.app.data.files.KhsRowParser.parseKhsByTail
import com.temankuliah.app.data.files.KhsRowParser.parseKhsFreeText
import com.temankuliah.app.data.files.KrsRowParser.detectKrsSksColumn
import com.temankuliah.app.data.files.KrsRowParser.krsContinuation
import com.temankuliah.app.data.files.KrsRowParser.krsDigit
import com.temankuliah.app.data.files.KrsRowParser.ocrDigits
import com.temankuliah.app.data.files.KrsRowParser.parseKrsColumns
import com.temankuliah.app.data.files.KrsRowParser.parseKrsFreeText
import com.temankuliah.app.domain.Grades
import com.temankuliah.app.domain.MAX_SKS_PER_COURSE
import java.util.Locale

/** Pengurai baris tabel KRS/KHS: memilih jalur baca (berpembatas, KRS, KHS) untuk tiap baris teks. */
internal object AcademicRowParser {
    /**
     * Membaca baris mata kuliah dari teks hasil OCR, PDF, atau teks yang disalin.
     * - KRS ([isKrs] = true): kode, nama mata kuliah, dosen, dan jam kuliah.
     * - KHS: kode, nama mata kuliah, SKS, nilai (bobot dan mutu dihitung dari nilai).
     * Bagian yang tidak cocok dengan kolom tabel dibiarkan kosong.
     */
    fun findAcademicRows(text: String, isKrs: Boolean = false): List<AcademicRow> {
        val sksColumn = if (isKrs) detectKrsSksColumn(text) else null
        val rows = mutableListOf<AcademicRow>()
        val seenKeys = mutableSetOf<String>()
        var currentDay = ""
        // Sambungan sel yang terpotong ke baris berikutnya (nama MK / dosen panjang) pada KRS.
        var continuationOpen = false
        var continuationCount = 0
        var afterTotal = false

        text.lineSequence().forEach { raw ->
            val trimmed = raw.replace('\u00A0', ' ').replace(WIB_REGEX, " ").trim()
            if (trimmed.length !in 3..220) { continuationOpen = false; return@forEach }
            val collapsed = trimmed.replace(WHITESPACE, " ")
            val lower = collapsed.lowercase(Locale.ROOT)

            if (isKrs && KRS_TOTAL.containsMatchIn(collapsed)) {
                afterTotal = true
                continuationOpen = false
                return@forEach
            }

            // Baris ringkasan nilai/SKS bukan baris mata kuliah, termasuk ketika OCR memisahkan
            // label "Kode kosong" dan "Total" dari angka pada kolom-kolomnya.
            if (SUMMARY_ROW.matches(collapsed)) {
                continuationOpen = false
                return@forEach
            }
            // Baris pemisah hari ("Senin") pada KRS yang dikelompokkan per hari.
            if (DAY_ONLY.matches(collapsed)) {
                currentDay = canonicalDay(collapsed)
                continuationOpen = false
                return@forEach
            }
            if (isHeaderLine(lower)) { continuationOpen = false; return@forEach }
            // Setelah baris Jumlah SKS hanya tersisa tanda tangan/keterangan; lewati kecuali ada kode MK.
            if (isKrs && afterTotal && !hasLeadingCourseCode(collapsed)) return@forEach

            // Sel yang terpotong ke baris berikutnya dicek lebih dulu: baris sambungan tidak memuat angka
            // (kode MK selalu berangka), jadi tidak mungkin berupa mata kuliah baru.
            if (isKrs && continuationOpen && !afterTotal && continuationCount < 2 && rows.isNotEmpty()) {
                val merged = krsContinuation(rows.last(), trimmed)
                if (merged != null) {
                    rows[rows.lastIndex] = merged
                    continuationCount++
                    return@forEach
                }
            }

            var detected: AcademicRow? = null
            // KRS dengan kolom lengkap dicoba menggunakan baris mentah agar TAB pemisah kolom
            // tetap tersedia; versi teks datar tetap didukung sebagai fallback di parser.
            if (isKrs) detected = parseKrsColumns(trimmed, sksColumn)
            // KHS berpola "... SKS NILAI BOBOT MUTU": dibaca dari belakang dan dicocokkan dengan SKS x Bobot = Mutu.
            if (!isKrs) detected = parseKhsByTail(collapsed)
            if (detected == null) {
                val cells = splitCells(trimmed, isKrs)
                if (cells != null) detected = parseDelimited(cells, isKrs)
            }
            if (detected == null) {
                detected = if (isKrs) parseKrsFreeText(collapsed) else parseKhsFreeText(collapsed)
            }
            if (detected == null) {
                continuationOpen = false
                return@forEach
            }

            detected?.let { row ->
                val withDay = if (isKrs && row.schedule.isNotBlank() && !DAY_REGEX.containsMatchIn(row.schedule) && currentDay.isNotBlank()) {
                    row.copy(schedule = "$currentDay ${row.schedule}")
                } else row
                val key = "${withDay.code}|${withDay.courseName}|${withDay.credits}|${withDay.schedule}".lowercase(Locale.ROOT)
                if (withDay.courseName.length >= 3 && seenKeys.add(key)) {
                    val point = gradeLetterToPoint(withDay.grade)
                    // Hari/jam dari teks jadwal bila baris belum memuatnya (jalur baca lama).
                    val parts = if (isKrs && withDay.start.isBlank()) parseScheduleParts(withDay.schedule) else null
                    rows.add(
                        withDay.copy(
                            no = rows.size + 1,
                            gradePoint = point,
                            qualityPoints = withDay.credits * point,
                            day = parts?.day ?: withDay.day,
                            start = parts?.start ?: withDay.start,
                            end = parts?.end ?: withDay.end,
                            lama = parts?.lamaMinutes ?: withDay.lama
                        )
                    )
                    continuationOpen = isKrs
                    continuationCount = 0
                } else {
                    continuationOpen = false
                }
            }
        }
        return rows.take(100)
    }

    /** Pisahkan baris menjadi sel bila memakai TAB, |, ;, atau koma; null bila bukan baris bertabel. */
    private fun splitCells(line: String, isKrs: Boolean): List<String>? {
        val parts = when {
            line.contains('\t') -> line.split('\t')
            line.contains('|') -> splitPipeDelimited(line) ?: return null
            line.contains(';') -> line.split(';')
            else -> {
                val commas = line.count { it == ',' }
                val hasDecimalComma = DECIMAL_COMMA.containsMatchIn(line)
                if (commas >= (if (isKrs) 3 else 2) && !hasDecimalComma) line.split(',') else return null
            }
        }
        val cells = parts.map { it.trim().removeSurrounding("\"").trim() }.filter { it.isNotBlank() }
        return cells.takeIf { it.size >= 2 }
    }

    private fun parseDelimited(rawCells: List<String>, isKrs: Boolean): AcademicRow? {
        var cells = rawCells
        if (cells.size >= 3 && ROW_NUMBER.matches(cells.first())) cells = cells.drop(1)

        var code = ""
        var name = ""
        var lecturer = ""
        var credits = 0
        var grade = ""
        var dayOnly = ""
        var timeText = ""
        val leftovers = mutableListOf<String>()
        val decimals = mutableListOf<Double>()

        val sksNumbers = mutableListOf<Int>()
        val krsTimes = mutableListOf<String>()
        var lamaMinutes = 0
        for (original in cells) {
            var cell = original
            val upper = cell.uppercase(Locale.ROOT)

            if (isKrs) {
                // Kolom MULAI / SELESAI berdiri sendiri (mis. "19.00") dan kolom LAMA ("2 Jam 30 Menit").
                if (ONLY_CLOCKS.matches(cell.trim())) {
                    SINGLE_TIME_ANYWHERE.findAll(cell).forEach {
                        krsTimes += String.format(Locale.US, "%02d:%s", it.groupValues[1].toInt(), it.groupValues[2])
                    }
                    continue
                }
                if (LAMA_ONLY.matches(cell.trim())) {
                    lamaMinutes = parseLamaMinutes(cell)
                    continue
                }
            }
            if (TIME_RANGE.containsMatchIn(cell)) {
                if (timeText.isBlank()) timeText = extractSchedule(cell).first
                continue
            }
            if (DAY_ONLY.matches(cell)) {
                if (dayOnly.isBlank()) dayOnly = canonicalDay(cell)
                continue
            }
            if (DECIMAL.matches(cell)) { // bobot / mutu: dihitung ulang dari nilai, dicatat untuk memulihkan nilai yang gagal terbaca
                cell.replace(',', '.').toDoubleOrNull()?.let { decimals += it }
                continue
            }

            val numberText = cell.replace(SKS_SUFFIX, "").trim()
            if (!isKrs && grade.isBlank()) {
                val repairedGrade = normalizeGradeToken(upper)
                if (repairedGrade in Grades.VALID && repairedGrade != "-" && (upper.trim() != "8" || credits > 0)) {
                    grade = repairedGrade
                    continue
                }
            }
            val number = ocrDigits(numberText).toIntOrNull()
            if (number != null) {
                val limit = MAX_SKS_PER_COURSE
                if (number in 0..limit) sksNumbers += number
                if (credits == 0 && number in 1..limit) credits = number
                continue
            }
            if (isKrs) {
                val digitsInCell = Regex("\\b([0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg\u2013—-]{1,2})\\b").findAll(numberText)
                    .map { krsDigit(it.value) }
                    .filter { it in 0..MAX_SKS_PER_COURSE }
                    .toList()
                if (digitsInCell.size in 2..3) {
                    sksNumbers += digitsInCell
                    continue
                }
            }
            if (code.isBlank()) {
                val (maybeCode, rest) = splitCode(cell)
                if (maybeCode.isNotBlank() && rest.isBlank()) {
                    code = maybeCode
                    continue
                }
                if (name.isBlank() && maybeCode.isNotBlank() && hasLetters(rest, 3)) {
                    code = maybeCode
                    cell = rest
                }
            }
            if (isKrs && lecturer.isBlank() && name.isNotBlank() && hasLecturerHint(cell)) {
                lecturer = cell.trim(' ', ',', ';')
                continue
            }
            if (hasLetters(cell, 3) && !DECIMAL.matches(cell)) {
                if (name.isBlank()) name = cell.trim(' ', '-', ':')
                else leftovers += cell
            }
        }

        var theory = 0
        var practice = 0
        if (isKrs && sksNumbers.isNotEmpty()) {
            when (sksNumbers.size) {
                1 -> {
                    theory = sksNumbers[0]
                    practice = 0
                    credits = sksNumbers[0]
                }
                2 -> {
                    theory = sksNumbers[0]
                    practice = sksNumbers[1]
                    credits = theory + practice
                }
                else -> {
                    theory = sksNumbers[0]
                    practice = sksNumbers[1]
                    val sum = theory + practice
                    credits = if (sum > 0) sum else sksNumbers[2]
                }
            }
        }

        if (isKrs && lecturer.isBlank()) {
            // Kolom setelah nama mata kuliah yang berupa nama orang (tanpa angka) dianggap dosen.
            lecturer = leftovers.firstOrNull { it.none(Char::isDigit) && it.count(Char::isLetter) >= 4 }.orEmpty().trim(' ', ',', ';')
        }
        if (!hasLetters(name, 3)) return null
        // Kolom Nilai kadang tidak terbaca OCR padahal Bobot/Mutu terbaca: pulihkan dari angka di dokumen.
        if (!isKrs && grade.isBlank()) grade = gradeFromNumbers(credits, decimals)

        if (isKrs && timeText.isBlank() && krsTimes.isNotEmpty()) {
            val first = krsTimes[0]
            val second = krsTimes.getOrNull(1) ?: if (lamaMinutes in 30..600) addMinutes(first, lamaMinutes) else ""
            timeText = if (second.isNotBlank()) "$first-$second" else first
        }

        val schedule = when {
            timeText.isNotBlank() && !DAY_REGEX.containsMatchIn(timeText) && dayOnly.isNotBlank() -> "$dayOnly $timeText"
            timeText.isNotBlank() -> timeText
            else -> dayOnly
        }

        return if (isKrs) {
            val totalSks = if (credits > 0) credits else (theory + practice)
            if (code.isBlank() && schedule.isBlank() && lecturer.isBlank() && totalSks == 0) null
            else AcademicRow(
                courseName = normalizeCourseName(name),
                credits = totalSks,
                grade = "-",
                code = code,
                lecturer = normalizeLecturerName(lecturer),
                schedule = schedule,
                theory = theory,
                practice = practice,
                day = dayOnly,
                lama = lamaMinutes
            )
        } else {
            val hasGrade = grade.isNotBlank()
            if (credits == 0 && !hasGrade) null
            else AcademicRow(courseName = normalizeCourseName(name), credits = credits, grade = if (hasGrade) grade else "-", code = code)
        }
    }

    private fun isHeaderLine(lower: String): Boolean {
        if (HEADER_PHRASES.any(lower::contains) || HEADER_WORDS.containsMatchIn(lower)) return true
        // Baris yang memuat nama hari beserta jam (Kamis ... 08:00) adalah data, bukan judul kolom.
        if (DAY_REGEX.containsMatchIn(lower) && SINGLE_TIME_ANYWHERE.containsMatchIn(lower)) return false
        return HEADER_TOKENS.findAll(lower).map { it.value }.toSet().size >= 2
    }

    private val HEADER_PHRASES = listOf(
        "mata kuliah", "jumlah sks", "total sks", "nilai huruf", "semester", "program studi", "fakultas",
        "universitas", "kartu hasil", "kartu rencana", "tanda tangan", "dosen pembimbing", "indeks prestasi"
    )
    // Whole words only: a plain substring check on "ips" wrongly dropped "Skripsi".
    private val HEADER_WORDS = Regex("\\b(?:ipk|ips)\\b")
    // Judul kolom: dua kata atau lebih dalam satu baris menandakan baris judul tabel.
    private val HEADER_TOKENS = Regex("\\b(?:kode|dosen|jam|hari|sks|nilai|bobot|mutu|ruang|kelas|teori|praktek|praktik|lama|mulai|sel)\\b")

    private val DAY_ONLY = Regex("^(?:$DAY_NAMES)\\s*[,:]?$", RegexOption.IGNORE_CASE)

    private val ONLY_CLOCKS = Regex("^(?:(?:[01]?\\d|2[0-3])[.:][0-5]\\d\\s*)+$")
    private val LAMA_ONLY = Regex("(?i)^(?:\\d{1,2}\\s*jam(?:\\s*\\d{1,2}\\s*menit)?|\\d{1,3}\\s*menit)$")

    private val SUMMARY_ROW = Regex("(?i)^(?:\\d{1,3}[.)]?\\s*)?(?:total|jumlah|jml|sub\\s*total|kode\\s+kosong)(?:\\s+(?:sks|mutu|nilai|beban\\s+studi))?(?:\\s*[:=]?\\s*(?:[-\\d.,]+(?:\\s+[-\\d.,]+)*))?$")

    private val WIB_REGEX = Regex("\\bW(?:IB|ITA|IT)\\b")

    private val ROW_NUMBER = Regex("^\\d{1,3}[.)]?$")

    private val DECIMAL = Regex("^\\d+[.,]\\d+$")
    private val DECIMAL_COMMA = Regex("(?<![\\d.])\\d{1,2},\\d{2}(?![\\d.])")
}
