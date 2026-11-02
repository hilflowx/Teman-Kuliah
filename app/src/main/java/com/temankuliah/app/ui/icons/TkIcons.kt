package com.temankuliah.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * Set ikon TemanKuliah: glif solid dengan sudut membulat (mengikuti ikon pada desain referensi),
 * digambar pada kanvas 24x24. Bentuk benda memakai isian solid dengan detail berupa lubang
 * (even-odd) supaya tetap terbaca saat diwarnai satu warna lewat tint. Tanda aksi sederhana
 * (tambah, tutup, centang, panah) memakai garis tebal 2,8 dengan ujung bulat.
 *
 * Nama properti sengaja sama dengan ikon Material yang dipakai layar, sehingga pemanggilan
 * Icons.Default.Nama tetap berlaku dan hanya impornya yang mengarah ke berkas ini.
 */
private class Part(val d: String, val stroke: Float = 0f, val evenOdd: Boolean = false)

private fun tkIcon(name: String, vararg parts: Part): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        parts.forEach { part ->
            val nodes = PathParser().parsePathString(part.d).toNodes()
            if (part.stroke > 0f) {
                addPath(
                    pathData = nodes,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = part.stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            } else {
                addPath(
                    pathData = nodes,
                    pathFillType = if (part.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                    fill = SolidColor(Color.Black)
                )
            }
        }
    }.build()

object TkIcons {
    val Add: ImageVector by lazy {
        tkIcon(
            "Add",
            Part("M12 5.5V18.5M5.5 12H18.5", stroke = 2.8f)
        )
    }
    val Close: ImageVector by lazy {
        tkIcon(
            "Close",
            Part("M6.5 6.5L17.5 17.5M17.5 6.5L6.5 17.5", stroke = 2.8f)
        )
    }
    val Clear: ImageVector by lazy {
        Close
    }
    val Check: ImageVector by lazy {
        tkIcon(
            "Check",
            Part("M5.5 12.8L10 17.2L18.5 7.4", stroke = 2.8f)
        )
    }
    val ChevronRight: ImageVector by lazy {
        tkIcon(
            "ChevronRight",
            Part("M9.5 5.5L16 12L9.5 18.5", stroke = 2.8f)
        )
    }
    val ChevronLeft: ImageVector by lazy {
        tkIcon(
            "ChevronLeft",
            Part("M14.5 5.5L8 12L14.5 18.5", stroke = 2.8f)
        )
    }
    val ChevronDown: ImageVector by lazy {
        tkIcon(
            "ChevronDown",
            Part("M5.5 9.5L12 16L18.5 9.5", stroke = 2.8f)
        )
    }
    val ArrowBack: ImageVector by lazy {
        tkIcon(
            "ArrowBack",
            Part("M19 12H5.5M11.5 5.5L5 12L11.5 18.5", stroke = 2.8f)
        )
    }
    val Search: ImageVector by lazy {
        tkIcon(
            "Search",
            Part("M4.3 10.5A6.2 6.2 0 1 0 16.7 10.5A6.2 6.2 0 1 0 4.3 10.5Z", stroke = 2.6f),
            Part("M15.4 15.4L20 20", stroke = 2.8f)
        )
    }
    val Edit: ImageVector by lazy {
        tkIcon(
            "Edit",
            Part("M15.3 4.2a2.2 2.2 0 0 1 3.1 0l1.4 1.4a2.2 2.2 0 0 1 0 3.1L9.4 19.1 4 20.3l1.2-5.4L15.3 4.2Z"),
            Part("M12.8 20.4H20", stroke = 2.2f)
        )
    }
    val Delete: ImageVector by lazy {
        tkIcon(
            "Delete",
            Part("M5.7 6H18.3A1.2 1.2 0 0 1 19.5 7.2V7.2A1.2 1.2 0 0 1 18.3 8.4H5.7A1.2 1.2 0 0 1 4.5 7.2V7.2A1.2 1.2 0 0 1 5.7 6ZM10.1 3H13.9A1.1 1.1 0 0 1 15 4.1V4.5A1.1 1.1 0 0 1 13.9 5.6H10.1A1.1 1.1 0 0 1 9 4.5V4.1A1.1 1.1 0 0 1 10.1 3Z"),
            Part("M6.2 9.8h11.6l-.8 9.3a2.1 2.1 0 0 1-2.1 1.9H9.1A2.1 2.1 0 0 1 7 19.1L6.2 9.8ZM10.4 12H10.4A0.8 0.8 0 0 1 11.2 12.8V16.8A0.8 0.8 0 0 1 10.4 17.6H10.4A0.8 0.8 0 0 1 9.6 16.8V12.8A0.8 0.8 0 0 1 10.4 12ZM13.6 12H13.6A0.8 0.8 0 0 1 14.4 12.8V16.8A0.8 0.8 0 0 1 13.6 17.6H13.6A0.8 0.8 0 0 1 12.8 16.8V12.8A0.8 0.8 0 0 1 13.6 12Z", evenOdd = true)
        )
    }
    val DeleteOutline: ImageVector by lazy {
        tkIcon(
            "DeleteOutline",
            Part("M6.6 9.6l.8 9.5a1.9 1.9 0 0 0 1.9 1.7h5.4a1.9 1.9 0 0 0 1.9-1.7l.8-9.5", stroke = 2.3f),
            Part("M4.6 6.6h14.8M9.4 6.4V4.9a1 1 0 0 1 1-1h3.2a1 1 0 0 1 1 1v1.5M10.2 11.6v5M13.8 11.6v5", stroke = 2.3f)
        )
    }
    val Person: ImageVector by lazy {
        tkIcon(
            "Person",
            Part("M7.9 8A4.1 4.1 0 1 0 16.1 8A4.1 4.1 0 1 0 7.9 8Z"),
            Part("M4.4 20.1c0-4.1 3.4-6.6 7.6-6.6 4.2 0 7.6 2.5 7.6 6.6a1.4 1.4 0 0 1-1.4 1.4H5.8a1.4 1.4 0 0 1-1.4-1.4Z")
        )
    }
    val School: ImageVector by lazy {
        tkIcon(
            "School",
            Part("M12 3.4L1.9 8.8 12 14.2l10.1-5.4L12 3.4Z"),
            Part("M5.9 12.4v4.3c0 1.7 2.7 3.3 6.1 3.3s6.1-1.6 6.1-3.3v-4.3L12 15.7l-6.1-3.3Z"),
            Part("M21 9.4v6.3", stroke = 1.9f)
        )
    }
    val MenuBook: ImageVector by lazy {
        tkIcon(
            "MenuBook",
            Part("M2.7 5.7c2.9-.8 5.7-.5 8.3 1.1v12.4c-2.6-1.6-5.4-1.9-8.3-1.1a.9.9 0 0 1-1.1-.9V6.6a.9.9 0 0 1 1.1-.9Z"),
            Part("M21.3 5.7c-2.9-.8-5.7-.5-8.3 1.1v12.4c2.6-1.6 5.4-1.9 8.3-1.1a.9.9 0 0 0 1.1-.9V6.6a.9.9 0 0 0-1.1-.9Z")
        )
    }
    val CalendarMonth: ImageVector by lazy {
        tkIcon(
            "CalendarMonth",
            Part("M7.2 4.6H16.8A4.2 4.2 0 0 1 21 8.8V16.8A4.2 4.2 0 0 1 16.8 21H7.2A4.2 4.2 0 0 1 3 16.8V8.8A4.2 4.2 0 0 1 7.2 4.6ZM7.1 10.2H16.9A1.7 1.7 0 0 1 18.6 11.9V16.9A1.7 1.7 0 0 1 16.9 18.6H7.1A1.7 1.7 0 0 1 5.4 16.9V11.9A1.7 1.7 0 0 1 7.1 10.2ZM7.6 13.4A1.1 1.1 0 1 0 9.8 13.4A1.1 1.1 0 1 0 7.6 13.4ZM10.9 13.4A1.1 1.1 0 1 0 13.1 13.4A1.1 1.1 0 1 0 10.9 13.4ZM14.2 13.4A1.1 1.1 0 1 0 16.4 13.4A1.1 1.1 0 1 0 14.2 13.4ZM7.6 16.2A1.1 1.1 0 1 0 9.8 16.2A1.1 1.1 0 1 0 7.6 16.2ZM10.9 16.2A1.1 1.1 0 1 0 13.1 16.2A1.1 1.1 0 1 0 10.9 16.2Z", evenOdd = true),
            Part("M8 2.9v3.3M16 2.9v3.3", stroke = 2.4f)
        )
    }
    val CalendarToday: ImageVector by lazy {
        tkIcon(
            "CalendarToday",
            Part("M7.2 4.6H16.8A4.2 4.2 0 0 1 21 8.8V16.8A4.2 4.2 0 0 1 16.8 21H7.2A4.2 4.2 0 0 1 3 16.8V8.8A4.2 4.2 0 0 1 7.2 4.6ZM7.1 10.2H16.9A1.7 1.7 0 0 1 18.6 11.9V16.9A1.7 1.7 0 0 1 16.9 18.6H7.1A1.7 1.7 0 0 1 5.4 16.9V11.9A1.7 1.7 0 0 1 7.1 10.2ZM9.7 12.4H11.3A1.1 1.1 0 0 1 12.4 13.5V15.1A1.1 1.1 0 0 1 11.3 16.2H9.7A1.1 1.1 0 0 1 8.6 15.1V13.5A1.1 1.1 0 0 1 9.7 12.4Z", evenOdd = true),
            Part("M8 2.9v3.3M16 2.9v3.3", stroke = 2.4f)
        )
    }
    val Event: ImageVector by lazy {
        tkIcon(
            "Event",
            Part("M7.2 4.6H16.8A4.2 4.2 0 0 1 21 8.8V16.8A4.2 4.2 0 0 1 16.8 21H7.2A4.2 4.2 0 0 1 3 16.8V8.8A4.2 4.2 0 0 1 7.2 4.6ZM7.1 10.2H16.9A1.7 1.7 0 0 1 18.6 11.9V16.9A1.7 1.7 0 0 1 16.9 18.6H7.1A1.7 1.7 0 0 1 5.4 16.9V11.9A1.7 1.7 0 0 1 7.1 10.2ZM9.9 14.4A2.1 2.1 0 1 0 14.1 14.4A2.1 2.1 0 1 0 9.9 14.4Z", evenOdd = true),
            Part("M8 2.9v3.3M16 2.9v3.3", stroke = 2.4f)
        )
    }
    val EventAvailable: ImageVector by lazy {
        tkIcon(
            "EventAvailable",
            Part("M7.2 4.6H16.8A4.2 4.2 0 0 1 21 8.8V16.8A4.2 4.2 0 0 1 16.8 21H7.2A4.2 4.2 0 0 1 3 16.8V8.8A4.2 4.2 0 0 1 7.2 4.6ZM7.1 10.2H16.9A1.7 1.7 0 0 1 18.6 11.9V16.9A1.7 1.7 0 0 1 16.9 18.6H7.1A1.7 1.7 0 0 1 5.4 16.9V11.9A1.7 1.7 0 0 1 7.1 10.2Z", evenOdd = true),
            Part("M8 2.9v3.3M16 2.9v3.3", stroke = 2.4f),
            Part("M9 14.4l2.2 2.2L15.4 12.2", stroke = 2.2f)
        )
    }
    val Schedule: ImageVector by lazy {
        tkIcon(
            "Schedule",
            Part("M12 2.4a9.6 9.6 0 1 0 0 19.2 9.6 9.6 0 0 0 0-19.2ZM11 7.2a1 1 0 0 1 2 0v3.8h3.4a1 1 0 0 1 0 2H12a1 1 0 0 1-1-1V7.2Z", evenOdd = true)
        )
    }
    val Notifications: ImageVector by lazy {
        tkIcon(
            "Notifications",
            Part("M12 2.8a1.4 1.4 0 0 1 1.4 1.4v.5c2.9.7 4.8 3.2 4.8 6.2v3.6l1.6 2.2a1 1 0 0 1-.8 1.6H5a1 1 0 0 1-.8-1.6l1.6-2.2v-3.6c0-3 1.9-5.5 4.8-6.2v-.5A1.4 1.4 0 0 1 12 2.8Z"),
            Part("M9.6 19.6h4.8a2.4 2.4 0 0 1-4.8 0Z")
        )
    }
    val Lock: ImageVector by lazy {
        tkIcon(
            "Lock",
            Part("M7.8 10.4H16.2A3.2 3.2 0 0 1 19.4 13.6V18A3.2 3.2 0 0 1 16.2 21.2H7.8A3.2 3.2 0 0 1 4.6 18V13.6A3.2 3.2 0 0 1 7.8 10.4ZM12 12.8a1.8 1.8 0 0 1 1 3.3v1.9a1 1 0 0 1-2 0v-1.9a1.8 1.8 0 0 1 1-3.3Z", evenOdd = true),
            Part("M7.9 10.4V7.9a4.1 4.1 0 0 1 8.2 0v2.5", stroke = 2.4f)
        )
    }
    val Security: ImageVector by lazy {
        tkIcon(
            "Security",
            Part("M12 2.6l7.6 2.8v6c0 4.6-3.1 8.4-7.6 10-4.5-1.6-7.6-5.4-7.6-10v-6L12 2.6ZM7.9 12.1L9.3 10.7 11.1 12.5 14.8 8.8 16.2 10.2 11.1 15.3Z", evenOdd = true)
        )
    }
    val Info: ImageVector by lazy {
        tkIcon(
            "Info",
            Part("M12 2.4a9.6 9.6 0 1 0 0 19.2 9.6 9.6 0 0 0 0-19.2ZM10.65 7.9A1.35 1.35 0 1 0 13.35 7.9A1.35 1.35 0 1 0 10.65 7.9ZM12 10.6H12A1.1 1.1 0 0 1 13.1 11.7V15.9A1.1 1.1 0 0 1 12 17H12A1.1 1.1 0 0 1 10.9 15.9V11.7A1.1 1.1 0 0 1 12 10.6Z", evenOdd = true)
        )
    }
    val ErrorOutline: ImageVector by lazy {
        tkIcon(
            "ErrorOutline",
            Part("M2.4 12A9.6 9.6 0 1 0 21.6 12A9.6 9.6 0 1 0 2.4 12ZM4.6 12A7.4 7.4 0 1 0 19.4 12A7.4 7.4 0 1 0 4.6 12Z", evenOdd = true),
            Part("M12 7H12A1.1 1.1 0 0 1 13.1 8.1V12.1A1.1 1.1 0 0 1 12 13.2H12A1.1 1.1 0 0 1 10.9 12.1V8.1A1.1 1.1 0 0 1 12 7ZM10.7 16.3A1.3 1.3 0 1 0 13.3 16.3A1.3 1.3 0 1 0 10.7 16.3Z")
        )
    }
    val Warning: ImageVector by lazy {
        tkIcon(
            "Warning",
            Part("M12 4.2L21.2 20H2.8Z", stroke = 2.2f),
            Part("M12 10v4.2", stroke = 2.4f),
            Part("M12 17.2v0.1", stroke = 2.6f)
        )
    }
    val Assignment: ImageVector by lazy {
        tkIcon(
            "Assignment",
            Part("M7.7 4.6H16.3A3.2 3.2 0 0 1 19.5 7.8V18.4A3.2 3.2 0 0 1 16.3 21.6H7.7A3.2 3.2 0 0 1 4.5 18.4V7.8A3.2 3.2 0 0 1 7.7 4.6ZM8.95 10.4H15.05A0.95 0.95 0 0 1 16 11.35V11.35A0.95 0.95 0 0 1 15.05 12.3H8.95A0.95 0.95 0 0 1 8 11.35V11.35A0.95 0.95 0 0 1 8.95 10.4ZM8.95 14.2H12.65A0.95 0.95 0 0 1 13.6 15.15V15.15A0.95 0.95 0 0 1 12.65 16.1H8.95A0.95 0.95 0 0 1 8 15.15V15.15A0.95 0.95 0 0 1 8.95 14.2Z", evenOdd = true),
            Part("M10 2.6H14A1.8 1.8 0 0 1 15.8 4.4V5.2A1.8 1.8 0 0 1 14 7H10A1.8 1.8 0 0 1 8.2 5.2V4.4A1.8 1.8 0 0 1 10 2.6ZM11.1 4.8A0.9 0.9 0 1 0 12.9 4.8A0.9 0.9 0 1 0 11.1 4.8Z", evenOdd = true)
        )
    }
    val AssignmentLate: ImageVector by lazy {
        tkIcon(
            "AssignmentLate",
            Part("M7.7 4.6H16.3A3.2 3.2 0 0 1 19.5 7.8V18.4A3.2 3.2 0 0 1 16.3 21.6H7.7A3.2 3.2 0 0 1 4.5 18.4V7.8A3.2 3.2 0 0 1 7.7 4.6ZM12 9.4H12A1.1 1.1 0 0 1 13.1 10.5V13.5A1.1 1.1 0 0 1 12 14.6H12A1.1 1.1 0 0 1 10.9 13.5V10.5A1.1 1.1 0 0 1 12 9.4ZM10.75 17.4A1.25 1.25 0 1 0 13.25 17.4A1.25 1.25 0 1 0 10.75 17.4Z", evenOdd = true),
            Part("M10 2.6H14A1.8 1.8 0 0 1 15.8 4.4V5.2A1.8 1.8 0 0 1 14 7H10A1.8 1.8 0 0 1 8.2 5.2V4.4A1.8 1.8 0 0 1 10 2.6ZM11.1 4.8A0.9 0.9 0 1 0 12.9 4.8A0.9 0.9 0 1 0 11.1 4.8Z", evenOdd = true)
        )
    }
    val Description: ImageVector by lazy {
        tkIcon(
            "Description",
            Part("M6.8 2.8h6.6l5.8 5.8v10.6a2.4 2.4 0 0 1-2.4 2.4H6.8a2.4 2.4 0 0 1-2.4-2.4V5.2a2.4 2.4 0 0 1 2.4-2.4ZM8.95 12H15.05A0.95 0.95 0 0 1 16 12.95V12.95A0.95 0.95 0 0 1 15.05 13.9H8.95A0.95 0.95 0 0 1 8 12.95V12.95A0.95 0.95 0 0 1 8.95 12ZM8.95 15.6H12.65A0.95 0.95 0 0 1 13.6 16.55V16.55A0.95 0.95 0 0 1 12.65 17.5H8.95A0.95 0.95 0 0 1 8 16.55V16.55A0.95 0.95 0 0 1 8.95 15.6Z", evenOdd = true)
        )
    }
    /** Printer bergaris (stroke) untuk tombol header transkrip: lebih ringan dan jelas dibanding glif solid. */
    val PrintOutline: ImageVector by lazy {
        tkIcon(
            "PrintOutline",
            Part("M7 8.5V4h10v4.5", stroke = 2f),
            Part("M7 17H5.5A2.5 2.5 0 0 1 3 14.5v-3A2.5 2.5 0 0 1 5.5 9h13a2.5 2.5 0 0 1 2.5 2.5v3a2.5 2.5 0 0 1-2.5 2.5H17", stroke = 2f),
            Part("M7 14h10v6.5H7Z", stroke = 2f)
        )
    }
    /** Panah unduh ke dalam baki, untuk tombol simpan PDF. */
    val SaveAlt: ImageVector by lazy {
        tkIcon(
            "SaveAlt",
            Part("M12 4v10.5M7.5 10.2l4.5 4.5 4.5-4.5", stroke = 2.2f),
            Part("M4.5 15.5v1.8A2.2 2.2 0 0 0 6.7 19.5h10.6a2.2 2.2 0 0 0 2.2-2.2v-1.8", stroke = 2.2f)
        )
    }
    val UploadFile: ImageVector by lazy {
        tkIcon(
            "UploadFile",
            Part("M6.8 2.8h6.6l5.8 5.8v10.6a2.4 2.4 0 0 1-2.4 2.4H6.8a2.4 2.4 0 0 1-2.4-2.4V5.2a2.4 2.4 0 0 1 2.4-2.4ZM12 10.2l3.4 3.4h-2.4v3.6h-2v-3.6H8.6L12 10.2Z", evenOdd = true)
        )
    }
    val Upload: ImageVector by lazy {
        tkIcon(
            "Upload",
            Part("M12 16V5.5M7.5 9.8L12 5.3l4.5 4.5M5 19.5h14", stroke = 2.4f)
        )
    }
    val Backup: ImageVector by lazy {
        tkIcon(
            "Backup",
            Part("M7 18.6a4.3 4.3 0 0 1-.5-8.5 5.8 5.8 0 0 1 11.1 1.2 3.7 3.7 0 0 1-.6 7.3H7ZM12 10.3l3.2 3.2h-2.2v3.1h-2v-3.1H8.8L12 10.3Z", evenOdd = true)
        )
    }
    val Restore: ImageVector by lazy {
        tkIcon(
            "Restore",
            Part("M4.4 12.2a7.8 7.8 0 1 0 2.4-5.6", stroke = 2.4f),
            Part("M2.6 3.8v5.6h5.6L2.6 3.8Z"),
            Part("M12 8v4.3l3 1.9", stroke = 2.2f)
        )
    }
    val Settings: ImageVector by lazy {
        tkIcon(
            "Settings",
            Part("M19.2 10.27L21.61 10.71L21.61 13.29L19.2 13.73L18.31 15.87L19.71 17.88L17.88 19.71L15.87 18.31L13.73 19.2L13.29 21.61L10.71 21.61L10.27 19.2L8.13 18.31L6.12 19.71L4.29 17.88L5.69 15.87L4.8 13.73L2.39 13.29L2.39 10.71L4.8 10.27L5.69 8.13L4.29 6.12L6.12 4.29L8.13 5.69L10.27 4.8L10.71 2.39L13.29 2.39L13.73 4.8L15.87 5.69L17.88 4.29L19.71 6.12L18.31 8.13ZM8.9 12A3.1 3.1 0 1 0 15.1 12A3.1 3.1 0 1 0 8.9 12Z", evenOdd = true)
        )
    }
    val Palette: ImageVector by lazy {
        tkIcon(
            "Palette",
            Part("M12 2.6a9.4 9.4 0 1 0 0 18.8c1.3 0 2.1-1 2.1-2 0-.6-.3-1.1-.6-1.5-.4-.5-.6-.9-.6-1.5 0-1.1.9-2 2-2h2.4a3.4 3.4 0 0 0 3.4-3.4C20.7 6.3 16.9 2.6 12 2.6ZM5.9 11.6A1.4 1.4 0 1 0 8.7 11.6A1.4 1.4 0 1 0 5.9 11.6ZM8.2 7.3A1.4 1.4 0 1 0 11 7.3A1.4 1.4 0 1 0 8.2 7.3ZM13.2 7.1A1.4 1.4 0 1 0 16 7.1A1.4 1.4 0 1 0 13.2 7.1ZM16.2 10.4A1.3 1.3 0 1 0 18.8 10.4A1.3 1.3 0 1 0 16.2 10.4Z", evenOdd = true)
        )
    }
    val Tune: ImageVector by lazy {
        tkIcon(
            "Tune",
            Part("M4 7h8.4M17.6 7H20M4 12h2.4M11.6 12H20M4 17h8.9M18.1 17H20", stroke = 2.4f),
            Part("M12.7 7A2.3 2.3 0 1 0 17.3 7A2.3 2.3 0 1 0 12.7 7ZM6.7 12A2.3 2.3 0 1 0 11.3 12A2.3 2.3 0 1 0 6.7 12ZM13.2 17A2.3 2.3 0 1 0 17.8 17A2.3 2.3 0 1 0 13.2 17Z")
        )
    }
    val Timeline: ImageVector by lazy {
        tkIcon(
            "Timeline",
            Part("M3.8 17.2l4.9-5.2 3.8 3.3 7.8-8.3", stroke = 2.5f),
            Part("M1.9 17.2A1.9 1.9 0 1 0 5.7 17.2A1.9 1.9 0 1 0 1.9 17.2ZM6.8 12A1.9 1.9 0 1 0 10.6 12A1.9 1.9 0 1 0 6.8 12ZM10.6 15.3A1.9 1.9 0 1 0 14.4 15.3A1.9 1.9 0 1 0 10.6 15.3ZM18.4 7A1.9 1.9 0 1 0 22.2 7A1.9 1.9 0 1 0 18.4 7Z")
        )
    }
    val ViewKanban: ImageVector by lazy {
        tkIcon(
            "ViewKanban",
            Part("M6.6 3.8H17.4A3.6 3.6 0 0 1 21 7.4V16.6A3.6 3.6 0 0 1 17.4 20.2H6.6A3.6 3.6 0 0 1 3 16.6V7.4A3.6 3.6 0 0 1 6.6 3.8ZM6.6 6.4H7.6A1.1 1.1 0 0 1 8.7 7.5V13.3A1.1 1.1 0 0 1 7.6 14.4H6.6A1.1 1.1 0 0 1 5.5 13.3V7.5A1.1 1.1 0 0 1 6.6 6.4ZM11.5 6.4H12.5A1.1 1.1 0 0 1 13.6 7.5V16.5A1.1 1.1 0 0 1 12.5 17.6H11.5A1.1 1.1 0 0 1 10.4 16.5V7.5A1.1 1.1 0 0 1 11.5 6.4ZM16.4 6.4H17.4A1.1 1.1 0 0 1 18.5 7.5V10.9A1.1 1.1 0 0 1 17.4 12H16.4A1.1 1.1 0 0 1 15.3 10.9V7.5A1.1 1.1 0 0 1 16.4 6.4Z", evenOdd = true)
        )
    }
    val Dashboard: ImageVector by lazy {
        tkIcon(
            "Dashboard",
            Part("M5.8 3.5H8.4A2.3 2.3 0 0 1 10.7 5.8V10.4A2.3 2.3 0 0 1 8.4 12.7H5.8A2.3 2.3 0 0 1 3.5 10.4V5.8A2.3 2.3 0 0 1 5.8 3.5ZM15.6 3.5H18.2A2.3 2.3 0 0 1 20.5 5.8V6.8A2.3 2.3 0 0 1 18.2 9.1H15.6A2.3 2.3 0 0 1 13.3 6.8V5.8A2.3 2.3 0 0 1 15.6 3.5ZM5.8 15.3H8.4A2.3 2.3 0 0 1 10.7 17.6V18.2A2.3 2.3 0 0 1 8.4 20.5H5.8A2.3 2.3 0 0 1 3.5 18.2V17.6A2.3 2.3 0 0 1 5.8 15.3ZM15.6 11.7H18.2A2.3 2.3 0 0 1 20.5 14V18.2A2.3 2.3 0 0 1 18.2 20.5H15.6A2.3 2.3 0 0 1 13.3 18.2V14A2.3 2.3 0 0 1 15.6 11.7Z")
        )
    }
    val CameraAlt: ImageVector by lazy {
        tkIcon(
            "CameraAlt",
            Part("M6.2 6.8H17.8A3.4 3.4 0 0 1 21.2 10.2V16.6A3.4 3.4 0 0 1 17.8 20H6.2A3.4 3.4 0 0 1 2.8 16.6V10.2A3.4 3.4 0 0 1 6.2 6.8ZM7.9 13.4A4.1 4.1 0 1 0 16.1 13.4A4.1 4.1 0 1 0 7.9 13.4ZM9.8 13.4A2.2 2.2 0 1 0 14.2 13.4A2.2 2.2 0 1 0 9.8 13.4Z", evenOdd = true),
            Part("M9.7 4H14.3A1.5 1.5 0 0 1 15.8 5.5V6.3A1.5 1.5 0 0 1 14.3 7.8H9.7A1.5 1.5 0 0 1 8.2 6.3V5.5A1.5 1.5 0 0 1 9.7 4Z")
        )
    }
    val PhotoLibrary: ImageVector by lazy {
        tkIcon(
            "PhotoLibrary",
            Part("M6.4 4.4H17.6A3.4 3.4 0 0 1 21 7.8V16.2A3.4 3.4 0 0 1 17.6 19.6H6.4A3.4 3.4 0 0 1 3 16.2V7.8A3.4 3.4 0 0 1 6.4 4.4ZM7 6.9H17A1.5 1.5 0 0 1 18.5 8.4V15.6A1.5 1.5 0 0 1 17 17.1H7A1.5 1.5 0 0 1 5.5 15.6V8.4A1.5 1.5 0 0 1 7 6.9ZM5.5 17.1V15.2l3.4-3.8 3.2 3.1 2.6-2.8 3.8 4.1v1.3H5.5ZM7.5 9.6A1.5 1.5 0 1 0 10.5 9.6A1.5 1.5 0 1 0 7.5 9.6Z", evenOdd = true)
        )
    }
    val Badge: ImageVector by lazy {
        tkIcon(
            "Badge",
            Part("M6.4 5H17.6A3.4 3.4 0 0 1 21 8.4V15.6A3.4 3.4 0 0 1 17.6 19H6.4A3.4 3.4 0 0 1 3 15.6V8.4A3.4 3.4 0 0 1 6.4 5ZM6.7 10.7A1.9 1.9 0 1 0 10.5 10.7A1.9 1.9 0 1 0 6.7 10.7ZM5.8 16.4c.3-1.8 1.5-2.6 2.8-2.6s2.5.8 2.8 2.6ZM13.85 9.8H17.15A0.85 0.85 0 0 1 18 10.65V10.65A0.85 0.85 0 0 1 17.15 11.5H13.85A0.85 0.85 0 0 1 13 10.65V10.65A0.85 0.85 0 0 1 13.85 9.8ZM13.85 13.2H15.75A0.85 0.85 0 0 1 16.6 14.05V14.05A0.85 0.85 0 0 1 15.75 14.9H13.85A0.85 0.85 0 0 1 13 14.05V14.05A0.85 0.85 0 0 1 13.85 13.2Z", evenOdd = true)
        )
    }
    val TextFields: ImageVector by lazy {
        tkIcon(
            "TextFields",
            Part("M4.2 5H14.4A1.2 1.2 0 0 1 15.6 6.2V6.6A1.2 1.2 0 0 1 14.4 7.8H4.2A1.2 1.2 0 0 1 3 6.6V6.2A1.2 1.2 0 0 1 4.2 5ZM8.4 5H8.8A1.2 1.2 0 0 1 10 6.2V17.2A1.2 1.2 0 0 1 8.8 18.4H8.4A1.2 1.2 0 0 1 7.2 17.2V6.2A1.2 1.2 0 0 1 8.4 5ZM15.4 11H19.8A1.2 1.2 0 0 1 21 12.2V12.3A1.2 1.2 0 0 1 19.8 13.5H15.4A1.2 1.2 0 0 1 14.2 12.3V12.2A1.2 1.2 0 0 1 15.4 11ZM17.6 11H17.6A1.2 1.2 0 0 1 18.8 12.2V17.2A1.2 1.2 0 0 1 17.6 18.4H17.6A1.2 1.2 0 0 1 16.4 17.2V12.2A1.2 1.2 0 0 1 17.6 11Z")
        )
    }
    val Calculate: ImageVector by lazy {
        tkIcon(
            "Calculate",
            Part("M7.6 3H16.4A3.6 3.6 0 0 1 20 6.6V17.4A3.6 3.6 0 0 1 16.4 21H7.6A3.6 3.6 0 0 1 4 17.4V6.6A3.6 3.6 0 0 1 7.6 3ZM7.7 5.6H16.3A1.1 1.1 0 0 1 17.4 6.7V8.1A1.1 1.1 0 0 1 16.3 9.2H7.7A1.1 1.1 0 0 1 6.6 8.1V6.7A1.1 1.1 0 0 1 7.7 5.6ZM7.6 11.4H8.4A0.8 0.8 0 0 1 9.2 12.2V13A0.8 0.8 0 0 1 8.4 13.8H7.6A0.8 0.8 0 0 1 6.8 13V12.2A0.8 0.8 0 0 1 7.6 11.4ZM11.6 11.4H12.4A0.8 0.8 0 0 1 13.2 12.2V13A0.8 0.8 0 0 1 12.4 13.8H11.6A0.8 0.8 0 0 1 10.8 13V12.2A0.8 0.8 0 0 1 11.6 11.4ZM15.6 11.4H16.4A0.8 0.8 0 0 1 17.2 12.2V13A0.8 0.8 0 0 1 16.4 13.8H15.6A0.8 0.8 0 0 1 14.8 13V12.2A0.8 0.8 0 0 1 15.6 11.4ZM7.6 15.6H8.4A0.8 0.8 0 0 1 9.2 16.4V17.2A0.8 0.8 0 0 1 8.4 18H7.6A0.8 0.8 0 0 1 6.8 17.2V16.4A0.8 0.8 0 0 1 7.6 15.6ZM11.6 15.6H12.4A0.8 0.8 0 0 1 13.2 16.4V17.2A0.8 0.8 0 0 1 12.4 18H11.6A0.8 0.8 0 0 1 10.8 17.2V16.4A0.8 0.8 0 0 1 11.6 15.6ZM15.6 15.6H16.4A0.8 0.8 0 0 1 17.2 16.4V17.2A0.8 0.8 0 0 1 16.4 18H15.6A0.8 0.8 0 0 1 14.8 17.2V16.4A0.8 0.8 0 0 1 15.6 15.6Z", evenOdd = true)
        )
    }
    val Logout: ImageVector by lazy {
        tkIcon(
            "Logout",
            Part("M10 4H6.4A2.4 2.4 0 0 0 4 6.4v11.2A2.4 2.4 0 0 0 6.4 20H10", stroke = 2.4f),
            Part("M9.5 12H20M16 7.8l4.2 4.2-4.2 4.2", stroke = 2.4f)
        )
    }
    val LocationOn: ImageVector by lazy {
        tkIcon(
            "LocationOn",
            Part("M12 2.4a7.6 7.6 0 0 1 7.6 7.6c0 5.3-5.6 10.2-6.9 11.3a1.1 1.1 0 0 1-1.4 0C10 20.2 4.4 15.3 4.4 10A7.6 7.6 0 0 1 12 2.4ZM9.2 10A2.8 2.8 0 1 0 14.8 10A2.8 2.8 0 1 0 9.2 10Z", evenOdd = true)
        )
    }
    val RadioButtonUnchecked: ImageVector by lazy {
        tkIcon(
            "RadioButtonUnchecked",
            Part("M3.6 12A8.4 8.4 0 1 0 20.4 12A8.4 8.4 0 1 0 3.6 12Z", stroke = 2.4f)
        )
    }
    val CheckCircle: ImageVector by lazy {
        tkIcon(
            "CheckCircle",
            Part("M2.4 12A9.6 9.6 0 1 0 21.6 12A9.6 9.6 0 1 0 2.4 12ZM7.4 12.3l1.5-1.5 2.2 2.2 4.3-4.4 1.5 1.5-5.8 5.9-3.7-3.7Z", evenOdd = true)
        )
    }
    val ArrowDownward: ImageVector by lazy {
        tkIcon(
            "ArrowDownward",
            Part("M12 4.5v14M6 13.5l6 6 6-6", stroke = 2.6f)
        )
    }
    val Apartment: ImageVector by lazy {
        tkIcon(
            "Apartment",
            Part("M4 3h16a1 1 0 0 1 1 1v17H3V4a1 1 0 0 1 1-1Z", stroke = 2.2f),
            Part("M7 7h2v2H7V7ZM11 7h2v2h-2V7ZM15 7h2v2h-2V7ZM7 11h2v2H7v-2ZM11 11h2v2h-2v-2ZM15 11h2v2h-2v-2ZM10 21v-4h4v4h-4Z")
        )
    }
    val Laptop: ImageVector by lazy {
        tkIcon(
            "Laptop",
            Part("M4 6h16a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z", stroke = 2.2f),
            Part("M1 18.5h22", stroke = 2.6f)
        )
    }
    val Videocam: ImageVector by lazy {
        tkIcon(
            "Videocam",
            Part("M3 6.5h11a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z"),
            Part("M16 10l5-3.5v11L16 14v-4Z")
        )
    }
    val OpenInNew: ImageVector by lazy {
        tkIcon(
            "OpenInNew",
            Part("M19 19H5V5h7", stroke = 2.2f),
            Part("M14 5h5v5", stroke = 2.2f),
            Part("M10 14L19 5", stroke = 2.2f)
        )
    }
    val MoreHoriz: ImageVector by lazy {
        tkIcon(
            "MoreHoriz",
            Part("M6 12A1.5 1.5 0 1 0 6 12.01M12 12A1.5 1.5 0 1 0 12 12.01M18 12A1.5 1.5 0 1 0 18 12.01", stroke = 3.2f)
        )
    }
    val Email: ImageVector by lazy {
        tkIcon(
            "Email",
            Part("M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z")
        )
    }
    val Visibility: ImageVector by lazy {
        tkIcon(
            "Visibility",
            Part("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z")
        )
    }
    val VisibilityOff: ImageVector by lazy {
        tkIcon(
            "VisibilityOff",
            Part("M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.44-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z")
        )
    }
    val Timer: ImageVector by lazy {
        tkIcon(
            "Timer",
            Part("M15 1H9v2h6V1zm-4 13h2V8h-2v6zm8.03-6.61l1.42-1.42c-.43-.51-.9-.99-1.41-1.41l-1.42 1.42C16.07 4.74 14.12 4 12 4c-4.97 0-9 4.03-9 9s4.02 9 9 9 9-4.03 9-9c0-2.12-.74-4.07-1.97-5.61zM12 20c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z")
        )
    }
    val FactCheck: ImageVector by lazy {
        tkIcon(
            "FactCheck",
            Part("M20 3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 14H5v-2h6v2zm0-4H5v-2h6v2zm0-4H5V7h6v2zm4.5 9L12 14.5l1.41-1.41 2.09 2.09 4.59-4.59L21.5 12l-6 6z")
        )
    }
}

val Icons.Filled.Add: ImageVector get() = TkIcons.Add
val Icons.Filled.Close: ImageVector get() = TkIcons.Close
val Icons.Filled.Clear: ImageVector get() = TkIcons.Clear
val Icons.Filled.Check: ImageVector get() = TkIcons.Check
val Icons.Filled.ChevronRight: ImageVector get() = TkIcons.ChevronRight
val Icons.Filled.ChevronLeft: ImageVector get() = TkIcons.ChevronLeft
val Icons.Filled.ArrowBack: ImageVector get() = TkIcons.ArrowBack
val Icons.Filled.Search: ImageVector get() = TkIcons.Search
val Icons.Filled.Edit: ImageVector get() = TkIcons.Edit
val Icons.Filled.Delete: ImageVector get() = TkIcons.Delete
val Icons.Filled.DeleteOutline: ImageVector get() = TkIcons.DeleteOutline
val Icons.Filled.Person: ImageVector get() = TkIcons.Person
val Icons.Filled.School: ImageVector get() = TkIcons.School
val Icons.Filled.MenuBook: ImageVector get() = TkIcons.MenuBook
val Icons.Filled.CalendarMonth: ImageVector get() = TkIcons.CalendarMonth
val Icons.Filled.CalendarToday: ImageVector get() = TkIcons.CalendarToday
val Icons.Filled.Event: ImageVector get() = TkIcons.Event
val Icons.Filled.EventAvailable: ImageVector get() = TkIcons.EventAvailable
val Icons.Filled.Schedule: ImageVector get() = TkIcons.Schedule
val Icons.Filled.Notifications: ImageVector get() = TkIcons.Notifications
val Icons.Filled.Lock: ImageVector get() = TkIcons.Lock
val Icons.Filled.Security: ImageVector get() = TkIcons.Security
val Icons.Filled.Info: ImageVector get() = TkIcons.Info
val Icons.Filled.ErrorOutline: ImageVector get() = TkIcons.ErrorOutline
val Icons.Filled.Warning: ImageVector get() = TkIcons.Warning
val Icons.Filled.Assignment: ImageVector get() = TkIcons.Assignment
val Icons.Filled.AssignmentLate: ImageVector get() = TkIcons.AssignmentLate
val Icons.Filled.Description: ImageVector get() = TkIcons.Description
val Icons.Filled.PrintOutline: ImageVector get() = TkIcons.PrintOutline
val Icons.Filled.SaveAlt: ImageVector get() = TkIcons.SaveAlt
val Icons.Filled.UploadFile: ImageVector get() = TkIcons.UploadFile
val Icons.Filled.Upload: ImageVector get() = TkIcons.Upload
val Icons.Filled.Backup: ImageVector get() = TkIcons.Backup
val Icons.Filled.Restore: ImageVector get() = TkIcons.Restore
val Icons.Filled.Settings: ImageVector get() = TkIcons.Settings
val Icons.Filled.Palette: ImageVector get() = TkIcons.Palette
val Icons.Filled.Tune: ImageVector get() = TkIcons.Tune
val Icons.Filled.Timeline: ImageVector get() = TkIcons.Timeline
val Icons.Filled.ViewKanban: ImageVector get() = TkIcons.ViewKanban
val Icons.Filled.Dashboard: ImageVector get() = TkIcons.Dashboard
val Icons.Filled.CameraAlt: ImageVector get() = TkIcons.CameraAlt
val Icons.Filled.PhotoLibrary: ImageVector get() = TkIcons.PhotoLibrary
val Icons.Filled.Badge: ImageVector get() = TkIcons.Badge
val Icons.Filled.TextFields: ImageVector get() = TkIcons.TextFields
val Icons.Filled.Calculate: ImageVector get() = TkIcons.Calculate
val Icons.Filled.Logout: ImageVector get() = TkIcons.Logout
val Icons.Filled.LocationOn: ImageVector get() = TkIcons.LocationOn
val Icons.Filled.RadioButtonUnchecked: ImageVector get() = TkIcons.RadioButtonUnchecked
val Icons.Filled.CheckCircle: ImageVector get() = TkIcons.CheckCircle
val Icons.Filled.ChevronDown: ImageVector get() = TkIcons.ChevronDown
val Icons.Filled.KeyboardArrowDown: ImageVector get() = TkIcons.ChevronDown
val Icons.Filled.ArrowDownward: ImageVector get() = TkIcons.ArrowDownward
val Icons.Filled.Apartment: ImageVector get() = TkIcons.Apartment
val Icons.Filled.Laptop: ImageVector get() = TkIcons.Laptop
val Icons.Filled.Videocam: ImageVector get() = TkIcons.Videocam
val Icons.Filled.OpenInNew: ImageVector get() = TkIcons.OpenInNew
val Icons.Filled.MoreHoriz: ImageVector get() = TkIcons.MoreHoriz
val Icons.Filled.Email: ImageVector get() = TkIcons.Email
val Icons.Filled.Visibility: ImageVector get() = TkIcons.Visibility
val Icons.Filled.VisibilityOff: ImageVector get() = TkIcons.VisibilityOff
val Icons.Filled.Timer: ImageVector get() = TkIcons.Timer
val Icons.Filled.FactCheck: ImageVector get() = TkIcons.FactCheck
