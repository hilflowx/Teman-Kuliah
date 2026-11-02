package com.temankuliah.app.data.files

import com.temankuliah.app.data.files.AcademicTextPatterns.COURSE_CODE_VALIDATION_REGEX
import com.temankuliah.app.data.files.AcademicTextPatterns.DEGREE_SUFFIX
import java.text.Normalizer

/** Pembantu teks untuk kode, nama mata kuliah, dan nama dosen pada baris KRS/KHS. */
internal object AcademicNameText {
    internal fun hasLetters(text: String, min: Int): Boolean = text.count(Char::isLetter) >= min

    /** Normalisasi OCR untuk token yang konteksnya sudah dipastikan numerik (SKS, teori, praktik). */
    internal fun normalizeOcrDigits(token: String): String = buildString(token.trim().length) {
        token.trim().forEach { char ->
            append(
                when (char) {
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
                    else -> char
                }
            )
        }
    }

    internal fun stripLeadingIndex(text: String): String {
        val trimmed = text.trim()
        trimmed.replaceFirst(LEADING_INDEX, "").let { stripped ->
            if (stripped != trimmed) return stripped.trim()
        }
        return trimmed
    }

    internal fun normalizeLecturerName(name: String): String {
        var clean = name.trim(' ', ',', ';', '-', ':', '\t')
        clean = clean.replace(Regex("\\bSoikin\\b", RegexOption.IGNORE_CASE), "Solikin")
        clean = clean.replace(Regex("\\bSolkin\\b", RegexOption.IGNORE_CASE), "Solikin")
        clean = clean.replace(Regex("\\bNaial\\b", RegexOption.IGNORE_CASE), "Nailal")
        clean = clean.replace(Regex("\\bNaill\\b", RegexOption.IGNORE_CASE), "Nailal")
        clean = clean.replace(Regex(",([A-Za-z])"), ", $1")
        clean = clean.replace(Regex("\\s+"), " ").trim()
        return clean
    }

    /**
     * Memulihkan karakter yang lazim tertukar OCR hanya di tengah kata, bukan di sembarang posisi.
     * Contoh: "Ku1iah" -> "Kuliah", "Pr0graman" -> "Programan", dan "S1STEM" -> "SISTEM".
     * Huruf/angka di luar kata (mis. nomor mata kuliah atau "Web 2") tidak diubah di tahap ini.
     */
    internal fun normalizeCourseName(name: String): String {
        val normalized = Normalizer.normalize(stripLeadingIndex(name), Normalizer.Form.NFKC)
        val chars = normalized.toCharArray()
        for (index in chars.indices) {
            val previousIsLetter = index > 0 && chars[index - 1].isLetter()
            val nextIsLetter = index < chars.lastIndex && chars[index + 1].isLetter()
            if (!previousIsLetter && !nextIsLetter) continue

            // Jangan mengubah singkatan/istilah pendek seperti "3D". Koreksi hanya token yang
            // memiliki setidaknya tiga huruf (termasuk bila karakter OCR berada di awal/akhir kata).
            var tokenStart = index
            while (tokenStart > 0 && chars[tokenStart - 1].isLetterOrDigit()) tokenStart--
            var tokenEnd = index
            while (tokenEnd < chars.lastIndex && chars[tokenEnd + 1].isLetterOrDigit()) tokenEnd++
            if ((tokenStart..tokenEnd).count { chars[it].isLetter() } < 3) continue

            val upperContext = previousIsLetter && nextIsLetter &&
                chars[index - 1].isUpperCase() && chars[index + 1].isUpperCase()
            val previousIsVowel = previousIsLetter && chars[index - 1].lowercaseChar() in VOWELS
            val replacement = when (chars[index]) {
                '1', '|', '!' -> when {
                    upperContext -> 'I'
                    previousIsVowel -> 'l'
                    else -> 'i'
                }
                '0' -> if (upperContext) 'O' else 'o'
                '5' -> if (upperContext) 'S' else 's'
                '8' -> if (upperContext) 'B' else 'b'
                '2' -> if (upperContext) 'Z' else 'z'
                '6' -> if (upperContext) 'G' else 'g'
                '4' -> if (upperContext) 'A' else 'a'
                '7' -> if (upperContext) 'T' else 't'
                '3' -> if (upperContext) 'E' else 'e'
                '9' -> if (upperContext) 'G' else 'g'
                // I/O kapital di tengah kata campuran sering merupakan salah baca l/1 atau 0.
                'I' -> if (upperContext || !previousIsLetter || !nextIsLetter) 'I' else 'l'
                'O' -> if (upperContext || !previousIsLetter || !nextIsLetter) 'O' else 'o'
                else -> null
            }
            if (replacement != null) chars[index] = replacement
        }
        return String(chars).replace(Regex("\\s+"), " ").trim(' ', ',', ';', '-', ':', '\t')
    }

    /** Validasi bentuk kode setelah karakter ambigu diperbaiki sesuai posisi huruf/angkanya. */
    internal fun normalizeCourseCode(raw: String): String? {
        val candidate = raw.trim().trim(',', ';', ':', '.', '|')
        if (candidate.isBlank()) return null

        val separator = when {
            candidate.contains('-') -> "-"
            candidate.contains(' ') -> " "
            else -> ""
        }
        val compact = candidate.replace("-", "").replace(Regex("\\s+"), "")
        if (compact.any { !it.isLetterOrDigit() && it !in "|!" }) return null

        val possibilities = mutableListOf<CodeCandidate>()
        for (suffixLength in 0..1) {
            val bodyLength = compact.length - suffixLength
            if (bodyLength <= 0) continue
            val suffixRaw = if (suffixLength == 1) compact.last() else null
            // Huruf-huruf ini sering merupakan angka yang salah dikenali OCR; prioritaskan sebagai digit.
            if (suffixRaw != null && (!suffixRaw.isLetter() || suffixRaw.uppercaseChar() in AMBIGUOUS_SUFFIX_GLYPHS)) continue
            for (digitLength in 2..5) {
                val prefixLength = bodyLength - digitLength
                if (prefixLength !in 1..6) continue
                val rawPrefix = compact.substring(0, prefixLength)
                val rawDigits = compact.substring(prefixLength, bodyLength)
                if (rawDigits.none { it.isDigit() || it == '|' || it == '!' }) continue
                val prefix = normalizeCodeLetters(rawPrefix) ?: continue
                val digits = normalizeCodeDigits(rawDigits) ?: continue
                val suffix = suffixRaw?.uppercaseChar()?.toString().orEmpty()
                val code = prefix + separator + digits + suffix
                if (!COURSE_CODE_VALIDATION_REGEX.matches(code)) continue
                val edits = countCodeEdits(rawPrefix, prefix, letters = true) +
                    countCodeEdits(rawDigits, digits, letters = false)
                possibilities += CodeCandidate(
                    code = code,
                    edits = edits,
                    digitLength = digitLength,
                    prefixLength = prefixLength,
                    hasSuffix = suffixLength == 1
                )
            }
        }

        // Utamakan pembacaan yang memerlukan koreksi paling sedikit; pada seri, tiga digit adalah
        // bentuk paling umum (mis. IF101 / TIF101). Ini mencegah IF101 bergeser menjadi IFI01.
        return possibilities.minWithOrNull(
            compareBy<CodeCandidate> { it.edits }
                .thenBy { if (it.digitLength == 3) 0 else 1 }
                .thenBy { if (it.prefixLength == 3) 0 else kotlin.math.abs(it.prefixLength - 3) }
                .thenBy { if (it.hasSuffix) 0 else 1 }
        )?.code
    }

    /**
     * Memisahkan kode hanya dari awal teks dan hanya jika hasil koreksinya lolos regex validasi.
     * Format kode dengan spasi (mis. "MKU 101") hanya diterima bila prefiks sumbernya huruf kapital,
     * sehingga "Fisika 101" tidak salah dianggap sebagai kode.
     */
    internal fun splitCode(text: String): Pair<String, String> {
        val clean = stripLeadingIndex(text)
        if (clean.isBlank()) return "" to clean

        val firstTokenMatch = FIRST_TOKEN.find(clean) ?: return "" to clean
        val firstToken = firstTokenMatch.value.trim(',', ';', ':', '.')
        normalizeCourseCode(firstToken)?.let { code ->
            val consumed = firstTokenMatch.range.last + 1
            return code to clean.substring(consumed).trim(' ', '-', ':', ',')
        }

        // OCR kadang menghilangkan spasi batas kolom: "IF101Algoritma". Cari batas kode
        // terpendek yang valid agar huruf pertama nama tidak terserap sebagai suffix kode.
        for (boundary in 3 until firstToken.length) {
            val rawCode = firstToken.substring(0, boundary).trimEnd('-', ':', ',')
            val rawName = firstToken.substring(boundary).trimStart('-', ':', ',')
            // Tanpa bukti digit/pemisah OCR, kata biasa seperti "Fisika" dapat keliru dibaca F15.
            if (rawCode.none { it.isDigit() || it == '|' || it == '!' }) continue
            if (rawName.firstOrNull()?.isLetter() != true || !hasLetters(rawName, 3)) continue
            val code = normalizeCourseCode(rawCode) ?: continue
            val remainder = clean.substring(firstTokenMatch.range.last + 1).trim(' ', '-', ':', ',')
            return code to (rawName + if (remainder.isBlank()) "" else " $remainder")
        }

        val separatorEnd = firstTokenMatch.range.last + 1
        val remainder = clean.substring(separatorEnd)
        val secondMatch = SECOND_TOKEN.find(remainder) ?: return "" to clean
        val secondToken = secondMatch.value.trim(',', ';', ':', '.')
        if (!firstToken.matches(UPPERCASE_CODE_PREFIX)) return "" to clean
        val combined = normalizeCourseCode("$firstToken $secondToken") ?: return "" to clean
        val consumed = separatorEnd + secondMatch.range.last + 1
        return combined to clean.substring(consumed).trim(' ', '-', ':', ',')
    }

    internal fun hasLeadingCourseCode(text: String): Boolean = splitCode(text).first.isNotBlank()

    /**
     * Pisahkan pipe sebagai kolom hanya jika bentuk baris memang tabel. Pipe tunggal yang menyatu
     * dengan token kode (contoh "C|S201") tetap dipertahankan untuk validasi OCR.
     */
    internal fun splitPipeDelimited(line: String, allowNonCodeFirst: Boolean = false): List<String>? {
        if (!line.contains('|')) return null
        val pieces = line.split('|')
        if (pieces.size < 2) return null

        val spacedPipe = Regex("\\s\\|\\s").containsMatchIn(line)
        val first = pieces.first().trim()
        val firstIsRowNumber = first.matches(ROW_NUMBER_TOKEN)
        val codeIndex = if (firstIsRowNumber) 1 else 0
        val codeCell = pieces.getOrNull(codeIndex).orEmpty().trim()
        val codeCellIsValid = splitCode(codeCell).let { it.first.isNotBlank() && it.second.isBlank() }
        val mergedLeadingCode = pieces.getOrNull(codeIndex + 1)?.let { next ->
            normalizeCourseCode(codeCell + "|" + next.trim()) != null
        } == true
        val isTable = spacedPipe || (pieces.size >= 3 && (firstIsRowNumber || codeCellIsValid || mergedLeadingCode || allowNonCodeFirst))
        if (!isTable) return null

        val cells = if (!codeCellIsValid && mergedLeadingCode) {
            pieces.take(codeIndex) + (codeCell + "|" + pieces[codeIndex + 1].trim()) + pieces.drop(codeIndex + 2)
        } else {
            pieces
        }
        return cells.map { it.trim().removeSurrounding("\"").trim() }.filter { it.isNotBlank() }
    }

    internal fun extractCodeAndName(rawTitle: String): Pair<String, String> {
        val (code, rest) = splitCode(rawTitle)
        return if (code.isNotBlank() && rest.isNotBlank()) code to rest else "" to stripLeadingIndex(rawTitle)
    }

    internal fun hasLecturerHint(text: String): Boolean =
        TITLE_PREFIX.containsMatchIn(text) || DEGREE_SUFFIX.containsMatchIn(text)

    /** Pisahkan "Nama Mata Kuliah Dr. Nama Dosen, M.Kom." menjadi nama mata kuliah dan dosen. */
    internal fun splitNameAndLecturer(text: String): Pair<String, String> {
        val title = TITLE_PREFIX.find(text)
        if (title != null) {
            val index = title.range.first
            return text.substring(0, index).trim(' ', '-', ':', ',', ';') to text.substring(index).trim(' ', ',', ';')
        }
        val degree = DEGREE_SUFFIX.find(text)
        if (degree != null) {
            val before = text.substring(0, degree.range.first).trimEnd(' ', ',')
            val words = before.split(' ').filter { it.isNotBlank() }
            val take = minOf(3, words.size - 1).coerceAtLeast(0)
            if (take > 0) {
                val lecturerStart = before.length - words.takeLast(take).joinToString(" ").length
                return text.substring(0, lecturerStart).trim(' ', '-', ':', ',', ';') to text.substring(lecturerStart).trim(' ', ',', ';')
            }
        }
        return text.trim(' ', '-', ':', ',', ';') to ""
    }

    private fun normalizeCodeLetters(value: String): String? {
        val mapped = buildString(value.length) {
            value.forEach { char ->
                append(
                    when (char) {
                        '0' -> 'O'; '1', 'l', 'I', '|', '!' -> 'I'
                        '2' -> 'Z'; '3' -> 'E'; '5' -> 'S'; '6' -> 'G'; '8' -> 'B'; '4' -> 'A'; '7' -> 'T'; '9' -> 'G'
                        else -> char.uppercaseChar()
                    }
                )
            }
        }
        return mapped.takeIf { it.matches(Regex("[A-Z]{1,6}")) }
    }

    private fun normalizeCodeDigits(value: String): String? {
        val mapped = buildString(value.length) {
            value.forEach { char ->
                append(
                    when (char) {
                        'O', 'o', 'Q', 'q', 'D', 'd' -> '0'
                        'I', 'i', 'l', '|', '!' -> '1'
                        'Z', 'z' -> '2'; 'S', 's' -> '5'; 'G' -> '6'; 'g' -> '9'; 'B', 'b' -> '8'
                        'A', 'a' -> '4'; 'T', 't' -> '7'; 'E', 'e' -> '3'
                        else -> char
                    }
                )
            }
        }
        return mapped.takeIf { it.matches(Regex("\\d{2,5}")) }
    }

    private fun countCodeEdits(raw: String, normalized: String, letters: Boolean): Int =
        raw.zip(normalized).count { (before, after) ->
            if (letters) before.uppercaseChar() != after else before != after
        }

    private data class CodeCandidate(
        val code: String,
        val edits: Int,
        val digitLength: Int,
        val prefixLength: Int,
        val hasSuffix: Boolean
    )

    private val VOWELS = setOf('a', 'i', 'u', 'e', 'o')
    private val AMBIGUOUS_SUFFIX_GLYPHS = setOf('I', 'L', 'O', 'Q', 'D', 'S', 'Z', 'G', 'B', 'T')
    private val FIRST_TOKEN = Regex("^[^\\s]+")
    private val SECOND_TOKEN = Regex("^\\s+([^\\s]+)")
    private val UPPERCASE_CODE_PREFIX = Regex("[A-Z]{1,6}")
    private val ROW_NUMBER_TOKEN = Regex("\\d{1,3}[.)]?")
    private val LEADING_INDEX = Regex("^\\d{1,3}[.)]\\s*|^\\d{1,3}\\s+(?=[A-Za-z]{1,6}[- ]?\\d)")
    private val TITLE_PREFIX = Regex(
        "(?<![A-Za-z.])(?:(?:Prof|Dr|Drs|Dra|Ir|Ns|apt|H|Hj)\\.|(?:Prof|Dr|Drs|Dra|Ir)(?=\\s+[A-Z]))",
        RegexOption.IGNORE_CASE
    )
}
