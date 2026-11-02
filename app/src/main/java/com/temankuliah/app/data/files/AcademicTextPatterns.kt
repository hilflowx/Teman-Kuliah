package com.temankuliah.app.data.files

/** Pola regex yang dipakai bersama oleh beberapa pengurai KRS/KHS. */
internal object AcademicTextPatterns {
    internal const val DAY_NAMES = "senin|selasa|rabu|kamis|jumat|jum'at|jum’at|sabtu|minggu|ahad"
    internal val DAY_REGEX = Regex("\\b(?:$DAY_NAMES)\\b", RegexOption.IGNORE_CASE)

    internal val TIME_RANGE = Regex(
        "(?<![\\d:.])([01]?\\d|2[0-3])[.:]([0-5]\\d)\\s*(?:-|–|—|s/d|s\\.d\\.?|sd|sampai|to)\\s*([01]?\\d|2[0-3])[.:]([0-5]\\d)(?!\\d)",
        RegexOption.IGNORE_CASE
    )

    internal val SINGLE_TIME_ANYWHERE = Regex("(?<![\\d:.])([01]?\\d|2[0-3])[.:]([0-5]\\d)(?![\\d:.])")

    internal val KRS_TOTAL = Regex("(?i)\\b(?:jumlah|jml|total)\\s*(?:sks|beban\\s*studi|(?:sks\\s*)?(?:teori|praktik|praktek))\\b[\\s:=]*")

    internal val WHITESPACE = Regex("\\s+")

    /**
     * Validasi pasca-OCR kode mata kuliah. Format umum yang didukung:
     * IF101, CS201, A02111, TIF-201, dan MKU 101.
     * Huruf harus menjadi prefiks dan angka harus menjadi bagian numerik; nama mata kuliah tidak
     * dianggap kode hanya karena di dalamnya terdapat angka.
     */
    internal val COURSE_CODE_VALIDATION_REGEX = Regex(
        "^(?:[A-Z]{1,6}-?\\d{2,5}[A-Z]?|[A-Z]{2,6} \\d{2,5}[A-Z]?)$"
    )

    internal val SKS_SUFFIX = Regex("(?i)\\s*sks$")

    internal val DEGREE_SUFFIX = Regex("\\b[SM]\\.[A-Za-z]{1,5}\\b|\\bPh\\.?D\\b")
}
