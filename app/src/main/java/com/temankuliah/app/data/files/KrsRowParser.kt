package com.temankuliah.app.data.files

import com.temankuliah.app.data.files.AcademicDocumentProcessor.AcademicRow
import com.temankuliah.app.data.files.AcademicDocumentProcessor.KrsTotals
import com.temankuliah.app.data.files.AcademicNameText.hasLecturerHint
import com.temankuliah.app.data.files.AcademicNameText.hasLetters
import com.temankuliah.app.data.files.AcademicNameText.normalizeCourseCode
import com.temankuliah.app.data.files.AcademicNameText.normalizeCourseName
import com.temankuliah.app.data.files.AcademicNameText.normalizeLecturerName
import com.temankuliah.app.data.files.AcademicNameText.normalizeOcrDigits
import com.temankuliah.app.data.files.AcademicNameText.splitCode
import com.temankuliah.app.data.files.AcademicNameText.splitNameAndLecturer
import com.temankuliah.app.data.files.AcademicNameText.splitPipeDelimited
import com.temankuliah.app.data.files.AcademicNameText.stripLeadingIndex
import com.temankuliah.app.data.files.AcademicScheduleText.buildSchedule
import com.temankuliah.app.data.files.AcademicScheduleText.canonicalDay
import com.temankuliah.app.data.files.AcademicScheduleText.extractSchedule
import com.temankuliah.app.data.files.AcademicScheduleText.readKrsTime
import com.temankuliah.app.data.files.AcademicTextPatterns.DAY_NAMES
import com.temankuliah.app.data.files.AcademicTextPatterns.DAY_REGEX
import com.temankuliah.app.data.files.AcademicTextPatterns.DEGREE_SUFFIX
import com.temankuliah.app.data.files.AcademicTextPatterns.KRS_TOTAL
import com.temankuliah.app.data.files.AcademicTextPatterns.SKS_SUFFIX
import com.temankuliah.app.data.files.AcademicTextPatterns.WHITESPACE
import com.temankuliah.app.domain.AcademicReconciler
import com.temankuliah.app.domain.MAX_SKS_PER_COURSE
import java.util.Locale

/** Pembacaan baris KRS (jadwal, SKS teori/praktek, dosen) dari teks hasil OCR atau berkas teks. */
internal object KrsRowParser {
    /**
     * Koreksi karakter OCR hanya saat token sudah diketahui sebagai kolom numerik.
     * Kode mata kuliah/nama tidak boleh melewati fungsi ini.
     */
    internal fun ocrDigits(token: String): String = normalizeOcrDigits(token)

    internal fun krsDigit(token: String): Int = ocrDigits(token.trim()).toIntOrNull() ?: 0

    internal fun parseKrsFreeText(line: String): AcademicRow? {
        val (schedule, afterSchedule) = extractSchedule(line)
        var text = stripLeadingIndex(afterSchedule).trim(' ', ',', ';', '-', ':')

        var theory = 0
        var practice = 0
        val tpMatch = Regex("""\[(?:T|Teori):(\d+),\s*(?:P|Praktik|Praktek):(\d+)\]""", RegexOption.IGNORE_CASE).find(text)
            ?: Regex("""(?i)(\d+)\s*(?:teori|t)\s*[,+&/]\s*(\d+)\s*(?:praktik|praktek|p)""").find(text)
        if (tpMatch != null) {
            theory = tpMatch.groupValues[1].toIntOrNull() ?: 0
            practice = tpMatch.groupValues[2].toIntOrNull() ?: 0
            text = text.replace(tpMatch.value, " ").replace(WHITESPACE, " ").trim()
        }

        var credits = if (theory + practice > 0) theory + practice else 0
        if (credits == 0) {
            SKS_INLINE.find(text)?.let { match ->
                val value = krsDigit(match.groupValues[1])
                if (value in 1..MAX_SKS_PER_COURSE) credits = value
                text = text.replaceFirst(match.value, " ").replace(WHITESPACE, " ").trim()
            }
        }
        if (credits == 0) {
            TRAILING_NUMBER.matchEntire(text)?.let { match ->
                val value = krsDigit(match.groupValues[2])
                if (value in 1..MAX_SKS_PER_COURSE && hasLetters(match.groupValues[1], 3)) {
                    credits = value
                    text = match.groupValues[1].trim()
                }
            }
        }
        if (theory == 0 && practice == 0 && credits > 0) {
            theory = credits
        }

        val (code, rest) = splitCode(text)
        val (name, lecturer) = splitNameAndLecturer(rest)
        if (!hasLetters(name, 3)) return null
        val totalSks = if (credits > 0) credits else (theory + practice)
        if (code.isBlank() && schedule.isBlank() && lecturer.isBlank() && totalSks == 0) return null
        return AcademicRow(
            courseName = normalizeCourseName(name),
            credits = totalSks,
            grade = "-",
            code = code,
            lecturer = normalizeLecturerName(lecturer),
            schedule = schedule,
            theory = theory,
            practice = practice
        )
    }

    /**
     * Baca satu baris KRS berkolom: [No] KODE NAMA-MATA-KULIAH TEORI PRAKTEK DOSEN HARI LAMA MULAI SEL.
     * Baris hanya dikenali bila dua angka TEORI/PRAKTEK dan nama hari ditemukan, sehingga format KRS
     * lain tetap dibaca oleh jalur lama. SEL dilengkapi dari MULAI + LAMA (dan sebaliknya) bila salah satunya
     * tidak terbaca; nilai yang tidak bisa dipastikan dibiarkan kosong.
     */
    data class KrsExtractedMiddle(
        val courseName: String,
        val theory: Int,
        val practice: Int,
        val credits: Int,
        val lecturer: String
    )

    private fun calculateKrsCredits(courseName: String, numbers: List<Int>): Triple<Int, Int, Int> {
        val isPractice = courseName.contains("Praktik", ignoreCase = true) || courseName.contains("Praktek", ignoreCase = true)
        return when (numbers.size) {
            1 -> {
                val num = numbers[0]
                if (isPractice) Triple(0, num, num) else Triple(num, 0, num)
            }
            2 -> {
                val t = numbers[0]
                val p = numbers[1]
                Triple(t, p, t + p)
            }
            else -> {
                // Tiga kolom: Teori, Praktek, SKS. Kaidah Teori + Praktek = SKS dipakai untuk membetulkan satu angka
                // yang salah baca; bila tidak ada koreksi tunggal, SKS tetap Teori + Praktek.
                val fixed = AcademicReconciler.repairKrsCredits(numbers[0], numbers[1], numbers[2])
                val sum = fixed.theory + fixed.practice
                Triple(fixed.theory, fixed.practice, if (sum > 0) fixed.sks else numbers[2])
            }
        }
    }

    private fun extractKrsNameSksLecturer(middleText: String, sksColumn: Boolean? = null): KrsExtractedMiddle? {
        val clean = middleText.trim(' ', '\t', '|', '-', ':', ',')
        if (clean.isBlank()) return null

        // 1. Coba bila ada TAB atau PIPE (|) pemisah kolom
        if (clean.contains('\t') || clean.count { it == '|' } >= 2 || Regex("\\s\\|\\s").containsMatchIn(clean)) {
            val cells = if (clean.contains('\t')) {
                clean.split('\t').map { it.trim().trim('|', ' ') }.filter { it.isNotBlank() }
            } else {
                splitPipeDelimited(clean, allowNonCodeFirst = true).orEmpty()
            }
            val sksCellIndices = mutableListOf<Int>()
            val foundNumbers = mutableListOf<Int>()

            cells.forEachIndexed { idx, cell ->
                val singleNum = krsDigit(cell.replace(SKS_SUFFIX, "").trim())
                val isSingle = Regex("^([0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg\u2013—-]{1,2}|-)$").matches(cell.replace(SKS_SUFFIX, "").trim())
                if (isSingle && singleNum in 0..MAX_SKS_PER_COURSE) {
                    sksCellIndices.add(idx)
                    foundNumbers.add(singleNum)
                } else {
                    val multi = Regex("\\b([0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg\u2013—-]{1,2})\\b").findAll(cell)
                        .map { krsDigit(it.value) }
                        .filter { it in 0..MAX_SKS_PER_COURSE }
                        .toList()
                    if (multi.size in 2..3) {
                        sksCellIndices.add(idx)
                        foundNumbers.addAll(multi)
                    }
                }
            }

            if (sksCellIndices.isNotEmpty() && foundNumbers.isNotEmpty()) {
                val firstSksIdx = sksCellIndices.first()
                val lastSksIdx = sksCellIndices.last()
                val nameParts = cells.take(firstSksIdx)
                val lecturerParts = cells.drop(lastSksIdx + 1)

                val name = nameParts.joinToString(" ").trim(' ', '|', '-', ':', ',')
                val lecturer = lecturerParts.joinToString(" ").trim(' ', '|', '-', ':', ',')

                val (theory, practice, credits) = calculateKrsCredits(name, foundNumbers)
                if (hasLetters(name, 2)) {
                    return KrsExtractedMiddle(name, theory, practice, credits, lecturer)
                }
            }
        }

        // 2. Tanpa pemisah kolom: kolom Teori/Praktek(/SKS) adalah deret angka TERAKHIR sebelum nama dosen.
        // Angka di dalam nama ("Pemrograman Web 2", "Fisika 1") tidak boleh terbaca sebagai kolom Teori.
        val normalizedSpaces = clean.replace(WHITESPACE, " ")
        val tokens = normalizedSpaces.split(' ').filter { it.isNotBlank() }
        AcademicReconciler.pickKrsNumberRun(tokens, sksColumn)?.let { run ->
            val rawName = tokens.take(run.nameTokenCount).joinToString(" ").trim(' ', '|', '-', ':', ',')
            val lecturer = tokens.drop(run.nameTokenCount + run.numberTokenCount).joinToString(" ").trim(' ', '|', '-', ':', ',')
            if (hasLetters(rawName, 2)) {
                return KrsExtractedMiddle(rawName, run.credits.theory, run.credits.practice, run.credits.sks, lecturer)
            }
        }

        // 3. Fallback bila hanya ada satu angka SKS
        val singleNumMatch = Regex("""^(.*?)\s+([0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg]{1,2})(?:\s*SKS)?(?:\s+(.*))?$""", RegexOption.IGNORE_CASE)
            .matchEntire(normalizedSpaces)
        if (singleNumMatch != null) {
            val rawName = singleNumMatch.groupValues[1].trim(' ', '|', '-', ':', ',')
            val num = krsDigit(singleNumMatch.groupValues[2])
            val lecturer = singleNumMatch.groupValues[3].trim(' ', '|', '-', ':', ',')
            val (theory, practice, credits) = calculateKrsCredits(rawName, listOf(num))
            if (hasLetters(rawName, 2)) {
                return KrsExtractedMiddle(rawName, theory, practice, credits, lecturer)
            }
        }

        return null
    }

    internal fun parseKrsColumns(line: String, sksColumn: Boolean? = null): AcademicRow? {
        val trimmed = line.trim()
        val dayMatch = DAY_REGEX.find(trimmed)
        if (dayMatch != null) {
            val day = canonicalDay(dayMatch.value)
            val beforeDay = trimmed.substring(0, dayMatch.range.first).trim()
            val afterDay = trimmed.substring(dayMatch.range.last + 1).trim()

            val (code, betweenCodeAndDay) = splitCode(beforeDay)
            if (code.isNotBlank()) {

                val parsed = extractKrsNameSksLecturer(betweenCodeAndDay, sksColumn)
                if (parsed != null && hasLetters(parsed.courseName, 2)) {
                    val krsTime = readKrsTime(afterDay)
                    val start = krsTime.start
                    val end = krsTime.end
                    val lama = krsTime.lama

                    return AcademicRow(
                        courseName = normalizeCourseName(parsed.courseName),
                        credits = parsed.credits,
                        grade = "-",
                        code = code,
                        lecturer = normalizeLecturerName(parsed.lecturer),
                        schedule = buildSchedule(day, start, end),
                        theory = parsed.theory,
                        practice = parsed.practice,
                        day = day,
                        lama = lama,
                        start = start,
                        end = end
                    )
                }
            }
        }

        // Pada PDF/OCR bertabel, pertahankan kolom aslinya
        if (line.contains('\t')) {
            val cells = line.split('\t').map(String::trim)
            val codeIndex = cells.indexOfFirst { cell ->
                val (candidate, rest) = splitCode(cell)
                candidate.isNotBlank() && rest.isBlank()
            }
            if (codeIndex >= 0) {
                val code = splitCode(cells[codeIndex]).first
                val name = cells.getOrNull(codeIndex + 1).orEmpty().trim(' ', '-', ':', ',')
                fun parseColumnSks(value: String?): Int? {
                    val token = ocrDigits(value.orEmpty().trim())
                    if (token == "-" || token == "\u2013") return 0
                    val number = Regex("(?i)^(\\d{1,2})(?:\\s*sks)?$").matchEntire(token)
                        ?.groupValues?.get(1)?.toIntOrNull() ?: return null
                    return number.takeIf { it in 0..MAX_SKS_PER_COURSE }
                }
                val theory = parseColumnSks(cells.getOrNull(codeIndex + 2))
                val practice = parseColumnSks(cells.getOrNull(codeIndex + 3))
                val trailing = cells.drop((codeIndex + 4).coerceAtMost(cells.size))
                val dayCell = trailing.firstOrNull { DAY_REGEX.containsMatchIn(it) }
                if (hasLetters(name, 3) && theory != null && practice != null && dayCell != null) {
                    val day = DAY_REGEX.find(dayCell)?.value?.let(::canonicalDay).orEmpty()
                    val lecturer = trailing.takeWhile { !DAY_REGEX.containsMatchIn(it) }
                        .joinToString(" ").trim(' ', ',', ';')
                    val rest = trailing.dropWhile { !DAY_REGEX.containsMatchIn(it) }.joinToString(" ")
                    val krsTime = readKrsTime(rest)
                    val start = krsTime.start
                    val end = krsTime.end
                    val lama = krsTime.lama
                    return AcademicRow(
                        courseName = normalizeCourseName(name),
                        credits = theory + practice,
                        grade = "-",
                        code = code,
                        lecturer = normalizeLecturerName(lecturer),
                        schedule = buildSchedule(day, start, end),
                        theory = theory,
                        practice = practice,
                        day = day,
                        lama = lama,
                        start = start,
                        end = end
                    )
                }
            }
        }

        // Fallback untuk teks PDF/hasil salin yang tidak memiliki TAB.
        val match = KRS_COLUMNS.matchEntire(line.replace(WHITESPACE, " ")) ?: return null
        val code = normalizeCourseCode(match.groupValues[1]) ?: return null
        val name = match.groupValues[2].trim(' ', '-', ':', ',')
        if (!hasLetters(name, 3)) return null
        val theory = krsDigit(match.groupValues[3])
        val practice = krsDigit(match.groupValues[4])
        val lecturer = match.groupValues[5].trim(' ', ',', ';')
        val day = canonicalDay(match.groupValues[6])
        val rest = match.groupValues[7]

        val krsTime = readKrsTime(rest)
        val start = krsTime.start
        val end = krsTime.end
        val lama = krsTime.lama

        return AcademicRow(
            courseName = normalizeCourseName(name),
            credits = theory + practice,
            grade = "-",
            code = code,
            lecturer = normalizeLecturerName(lecturer),
            schedule = buildSchedule(day, start, end),
            theory = theory,
            practice = practice,
            day = day,
            lama = lama,
            start = start,
            end = end
        )
    }

    /**
     * Sel yang terpotong ke baris berikutnya (nama mata kuliah atau dosen yang panjang) digabungkan
     * ke baris KRS sebelumnya. Hanya baris pendek tanpa angka dan maksimal dua sel yang dianggap sambungan.
     */
    internal fun krsContinuation(previous: AcademicRow, rawLine: String): AcademicRow? {
        val cells = rawLine.split('\t').map { it.trim() }.filter { it.isNotBlank() }
        if (cells.isEmpty() || cells.size > 2) return null
        if (cells.any { it.length > 60 || it.any(Char::isDigit) || !hasLetters(it, 2) }) return null

        fun join(a: String, b: String) = if (a.isBlank()) b else "$a $b"
        // Koma di ujung nama dosen sudah terpangkas saat dibaca; pulihkan sebelum gelar ("Khusna, S.Kom.").
        fun joinLecturer(a: String, b: String) = when {
            a.isBlank() -> b
            DEGREE_AT_START.containsMatchIn(b) -> "$a, $b"
            else -> "$a $b"
        }
        var name = previous.courseName
        var lecturer = previous.lecturer
        if (cells.size == 2) {
            name = join(name, cells[0])
            lecturer = joinLecturer(lecturer, cells[1])
        } else {
            val cell = cells[0]
            val lecturerUnfinished = previous.lecturer.isNotBlank() && !DEGREE_SUFFIX.containsMatchIn(previous.lecturer)
            if (hasLecturerHint(cell) || lecturerUnfinished) lecturer = joinLecturer(lecturer, cell) else name = join(name, cell)
        }
        return previous.copy(courseName = normalizeCourseName(name), lecturer = normalizeLecturerName(lecturer))
    }

    private val KRS_HEADER_WITH_SKS = Regex("(?:praktek|praktik)\\W+(?:sks|jml|jumlah)")

    /**
     * Apakah tabel KRS punya kolom SKS sendiri di samping TEORI dan PRAKTEK? Dibaca dari judul kolom
     * ("TEORI PRAKTEK SKS ..." berarti ada; "TEORI PRAKTEK DOSEN ..." berarti tidak). Baris "Jumlah SKS" tidak
     * dipakai karena bisa memuat tiga angka (total Teori, Praktek, SKS) walau barisnya tanpa kolom SKS.
     * Null bila judul kolom tidak terbaca.
     */
    internal fun detectKrsSksColumn(text: String): Boolean? {
        for (raw in text.lineSequence()) {
            val lower = raw.lowercase(Locale.ROOT)
            if (!lower.contains("teori") || !(lower.contains("praktek") || lower.contains("praktik"))) continue
            if (KRS_HEADER_WITH_SKS.containsMatchIn(lower)) return true
            if (lower.contains("dosen") || lower.contains("hari")) return false
        }
        return null
    }

    /**
     * Baca baris "Jumlah SKS" di dasar tabel KRS. Satu angka = total SKS; dua angka = total teori dan
     * praktek (SKS = jumlahnya); tiga angka atau lebih = teori, praktek, lalu total SKS.
     */
    fun findKrsTotals(text: String): KrsTotals? {
        for (raw in text.lineSequence()) {
            val line = raw.replace('\u00A0', ' ')
            val match = KRS_TOTAL.find(line) ?: continue
            val numbers = TOTAL_NUMBER.findAll(line.substring(match.range.last + 1))
                .mapNotNull { it.value.toIntOrNull() }.filter { it in 0..60 }.toList()
            if (numbers.isEmpty()) continue
            return when (numbers.size) {
                1 -> KrsTotals(null, null, numbers[0])
                2 -> KrsTotals(numbers[0], numbers[1], numbers[0] + numbers[1])
                else -> KrsTotals(numbers[0], numbers[1], numbers[2])
            }
        }
        return null
    }

    private val TOTAL_NUMBER = Regex("(?<![\\d.,])\\d{1,3}(?![\\d.,])")

    // No? KODE NAMA TEORI PRAKTEK [DOSEN] HARI sisa(LAMA MULAI SEL). Dosen harus diawali huruf agar angka SKS tidak terbawa ke nama dosen.
    private val KRS_COLUMNS = Regex(
        "^(?:\\d{1,3}[.)]?\\s+)?([A-Za-z0-9|!]{1,6}-?[A-Za-z0-9|!]{2,5}[A-Za-z]?)\\s+(.+?)\\s+([0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg\\u2013—-])\\s+([0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg\\u2013—-])\\s+" +
            "(?:(\\p{L}.*?)\\s+)?((?i:$DAY_NAMES))\\b\\s*(.*)$"
    )

    private val SKS_INLINE = Regex("(?i)\\b([0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg]{1,2})\\s*SKS\\b")
    private val TRAILING_NUMBER = Regex("^(.*\\S)\\s+([0-9OoQqDdIiIl|!ZzSsGgBbAaTtEeGg]{1,2})$")

    private val DEGREE_AT_START = Regex("^(?:[SM]\\.[A-Za-z]{1,5}\\b|Ph\\.?D\\b)")
}
