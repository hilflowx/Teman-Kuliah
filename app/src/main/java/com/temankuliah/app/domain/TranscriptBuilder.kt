package com.temankuliah.app.domain

import com.temankuliah.app.data.local.CourseEntity
import java.util.Locale

/** Identitas mahasiswa pada kepala transkrip; bagian yang kosong tidak dicetak. */
data class TranscriptStudent(
    val fullName: String,
    val nim: String,
    val studyProgram: String,
    val faculty: String,
    val university: String
)

data class TranscriptCourse(
    val no: Int,
    val code: String,
    val name: String,
    val credits: Int,
    val grade: String,
    val gradePoint: Double,
    val qualityPoints: Double
)

/** Satu semester pada transkrip beserta IPS dan IPK kumulatif sampai semester tersebut. */
data class TranscriptSemester(
    val semester: Int,
    val courses: List<TranscriptCourse>,
    val totalCredits: Int,
    val totalQualityPoints: Double,
    val ips: Double,
    val cumulativeCredits: Int,
    val cumulativeGpa: Double
)

data class Transcript(
    val student: TranscriptStudent,
    val semesters: List<TranscriptSemester>,
    val totalCredits: Int,
    val totalQualityPoints: Double,
    val gpa: Double,
    val predicate: String
) {
    val isEmpty: Boolean get() = semesters.isEmpty()
}

/**
 * Menyusun transkrip dari semester paling awal sampai paling akhir yang sudah memiliki nilai.
 * Hanya mata kuliah bernilai (bukan "-") yang masuk; semester tanpa nilai dilewati. Angka IPS dan IPK
 * memakai [GpaCalculator] agar sama persis dengan yang tampil di layar Transkrip Nilai dan KHS.
 */
object TranscriptBuilder {
    fun build(student: TranscriptStudent, courses: List<CourseEntity>): Transcript {
        val graded = courses.filter { it.credits > 0 && GpaCalculator.isGraded(it.gradeLetter) }
        val bySemester = graded.groupBy { it.semester }.toSortedMap()
        val semesters = bySemester.map { (semester, list) ->
            val sorted = list.sortedWith(
                compareBy<CourseEntity>({ it.code.lowercase(Locale.ROOT) }, { it.name.lowercase(Locale.ROOT) })
            )
            val totals = GpaCalculator.semesterTotals(sorted.map { it.credits to it.gradeLetter })
            val upToHere = GpaCalculator.calculateSummary(graded.filter { it.semester <= semester })
            TranscriptSemester(
                semester = semester,
                courses = sorted.mapIndexed { index, course ->
                    val grade = course.gradeLetter.trim().uppercase(Locale.ROOT)
                    TranscriptCourse(
                        no = index + 1,
                        code = course.code,
                        name = course.name,
                        credits = course.credits,
                        grade = grade,
                        gradePoint = GpaCalculator.gradeLetterToPoint(grade),
                        qualityPoints = GpaCalculator.qualityPoints(course.credits, grade)
                    )
                },
                totalCredits = totals.gradedCredits,
                totalQualityPoints = totals.totalQualityPoints,
                ips = totals.ips,
                cumulativeCredits = upToHere.totalCredits,
                cumulativeGpa = upToHere.cumulativeGpa
            )
        }
        val overall = GpaCalculator.calculateSummary(graded)
        return Transcript(
            student = student,
            semesters = semesters,
            totalCredits = overall.totalCredits,
            totalQualityPoints = semesters.sumOf { it.totalQualityPoints },
            gpa = overall.cumulativeGpa,
            predicate = overall.honorsPredicate
        )
    }
}

/** HTML transkrip siap cetak (A4). Semua teks pengguna di-escape; tidak ada skrip maupun sumber luar. */
object TranscriptHtml {
    fun render(transcript: Transcript, printedOn: String): String {
        val s = transcript.student
        val identity = listOf(
            "Nama" to s.fullName,
            "NIM" to s.nim,
            "Program Studi" to s.studyProgram,
            "Fakultas" to s.faculty,
            "Perguruan Tinggi" to s.university
        ).filter { it.second.isNotBlank() }

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html lang=\"id\"><head><meta charset=\"utf-8\">")
        sb.append("<meta name=\"color-scheme\" content=\"light\"><title>Transkrip Nilai</title><style>")
        sb.append(CSS)
        sb.append("</style></head><body>")
        sb.append("<h1>Transkrip Nilai</h1>")
        if (identity.isNotEmpty()) {
            sb.append("<table class=\"id\">")
            identity.forEach { (label, value) ->
                sb.append("<tr><td>").append(esc(label)).append("</td><td>:</td><td>").append(esc(value)).append("</td></tr>")
            }
            sb.append("</table>")
        }
        transcript.semesters.forEach { sem ->
            sb.append("<section><h2>Semester ").append(sem.semester).append("</h2>")
            sb.append("<table class=\"grid\"><thead><tr>")
            sb.append("<th class=\"n\">No</th><th>Kode</th><th>Mata Kuliah</th><th class=\"n\">SKS</th><th class=\"n\">Nilai</th><th class=\"n\">Bobot</th><th class=\"n\">Mutu</th>")
            sb.append("</tr></thead><tbody>")
            sem.courses.forEach { c ->
                sb.append("<tr><td class=\"n\">").append(c.no).append("</td><td>").append(esc(c.code)).append("</td><td>")
                    .append(esc(c.name)).append("</td><td class=\"n\">").append(c.credits).append("</td><td class=\"n\">")
                    .append(esc(c.grade)).append("</td><td class=\"n\">").append(num(c.gradePoint))
                    .append("</td><td class=\"n\">").append(num(c.qualityPoints)).append("</td></tr>")
            }
            sb.append("<tr class=\"sum\"><td colspan=\"3\">Jumlah Semester ").append(sem.semester).append("</td><td class=\"n\">")
                .append(sem.totalCredits).append("</td><td></td><td></td><td class=\"n\">")
                .append(num(sem.totalQualityPoints)).append("</td></tr>")
            sb.append("</tbody></table>")
            sb.append("<p class=\"idx\">IPS: <b>").append(num(sem.ips)).append("</b> &nbsp;·&nbsp; IPK sampai semester ini: <b>")
                .append(num(sem.cumulativeGpa)).append("</b> (").append(sem.cumulativeCredits).append(" SKS)</p>")
            sb.append("</section>")
        }
        sb.append("<section class=\"total\"><h2>Ringkasan Akhir</h2><table class=\"id\">")
        sb.append("<tr><td>Total SKS</td><td>:</td><td>").append(transcript.totalCredits).append("</td></tr>")
        sb.append("<tr><td>Total Mutu</td><td>:</td><td>").append(num(transcript.totalQualityPoints)).append("</td></tr>")
        sb.append("<tr><td>IPK</td><td>:</td><td><b>").append(num(transcript.gpa)).append("</b></td></tr>")
        sb.append("<tr><td>Predikat</td><td>:</td><td>").append(esc(transcript.predicate)).append("</td></tr>")
        sb.append("</table></section>")
        sb.append("<p class=\"note\">Dicetak dari aplikasi TemanKuliah pada ").append(esc(printedOn))
            .append(". Dokumen ini dibuat dari data yang tersimpan di aplikasi dan bukan transkrip resmi perguruan tinggi.</p>")
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun num(value: Double): String = String.format(Locale.US, "%.2f", value)

    internal fun esc(text: String): String = buildString(text.length) {
        text.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }

    private const val CSS = "@page{size:A4;margin:16mm}" +
        "body{font-family:sans-serif;font-size:11pt;color:#111;margin:0}" +
        "h1{font-size:18pt;text-align:center;margin:0 0 12pt}" +
        "h2{font-size:12pt;margin:14pt 0 6pt}" +
        "table{border-collapse:collapse;width:100%}" +
        "table.id{width:auto;margin-bottom:6pt}table.id td{padding:1pt 6pt 1pt 0;vertical-align:top}" +
        "table.grid th,table.grid td{border:1px solid #444;padding:3pt 5pt;font-size:10pt;text-align:left}" +
        "table.grid th{background:#e8edf5}.n{text-align:center!important;white-space:nowrap}" +
        "thead{display:table-header-group}tr{page-break-inside:avoid}section{page-break-inside:auto}" +
        "tr.sum td{font-weight:bold;background:#f4f6fa}.idx{margin:4pt 0 0;font-size:10pt}" +
        ".total{page-break-inside:avoid}.note{margin-top:18pt;font-size:8.5pt;color:#555}"
}
