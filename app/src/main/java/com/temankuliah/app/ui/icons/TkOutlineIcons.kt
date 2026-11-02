package com.temankuliah.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * Ikon garis (outline) bergaya desain Beranda: kanvas 24x24, garis 1,8 dengan ujung bulat.
 * Dipakai bilah navigasi, tombol notifikasi, dan ikon tugas. Warna diberikan lewat tint.
 */
private fun outlineIcon(name: String, vararg paths: String, strokeWidth: Float = 1.8f): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

object TkOutlineIcons {
    val Bell: ImageVector by lazy {
        outlineIcon(
            "BellOutline",
            "M6 10a6 6 0 0 1 12 0c0 4.2 1.4 5.6 2 6.5H4c.6-.9 2-2.3 2-6.5Z",
            "M9.9 19.2a2.3 2.3 0 0 0 4.2 0"
        )
    }
    val Calendar: ImageVector by lazy {
        outlineIcon(
            "CalendarOutline",
            "M6.2 4.8h11.6a2.7 2.7 0 0 1 2.7 2.7v10.3a2.7 2.7 0 0 1-2.7 2.7H6.2a2.7 2.7 0 0 1-2.7-2.7V7.5a2.7 2.7 0 0 1 2.7-2.7Z",
            "M3.5 10.2h17",
            "M8 2.8v3.6",
            "M16 2.8v3.6"
        )
    }
    val Clipboard: ImageVector by lazy {
        outlineIcon(
            "ClipboardOutline",
            "M8.4 5H7.2a2.7 2.7 0 0 0-2.7 2.7v10.6A2.7 2.7 0 0 0 7.2 21h9.6a2.7 2.7 0 0 0 2.7-2.7V7.7A2.7 2.7 0 0 0 16.8 5h-1.2",
            "M10 3h4a1.3 1.3 0 0 1 1.3 1.3v1.4A1.3 1.3 0 0 1 14 7h-4a1.3 1.3 0 0 1-1.3-1.3V4.3A1.3 1.3 0 0 1 10 3Z",
            "M8.6 12h6.8",
            "M8.6 16h6.8"
        )
    }
    val ListLines: ImageVector by lazy {
        outlineIcon(
            "ListLinesOutline",
            "M9 6.5h11",
            "M9 12h11",
            "M9 17.5h11",
            "M4.2 6.5h.01",
            "M4.2 12h.01",
            "M4.2 17.5h.01"
        )
    }
    val Kanban: ImageVector by lazy {
        outlineIcon(
            "KanbanOutline",
            "M6.2 3.5h11.6a2.7 2.7 0 0 1 2.7 2.7v11.6a2.7 2.7 0 0 1-2.7 2.7H6.2a2.7 2.7 0 0 1-2.7-2.7V6.2a2.7 2.7 0 0 1 2.7-2.7Z",
            "M9 7.8v8",
            "M15 7.8v4.6"
        )
    }
    val Flag: ImageVector by lazy {
        outlineIcon(
            "FlagOutline",
            "M5.5 21V4",
            "M5.5 5h11.5l-2.4 3.7 2.4 3.7H5.5"
        )
    }
    val BookOpen: ImageVector by lazy {
        outlineIcon(
            "BookOpenOutline",
            "M12 7C10.4 5.4 8 4.8 3.5 4.8v12.9c4.5 0 6.9.6 8.5 2.2 1.6-1.6 4-2.2 8.5-2.2V4.8C16 4.8 13.6 5.4 12 7Z",
            "M12 7v12.9"
        )
    }
    val Book: ImageVector by lazy {
        outlineIcon(
            "BookOutline",
            "M6.5 4h11A2.5 2.5 0 0 1 20 6.5v13A1.5 1.5 0 0 1 18.5 21H6.5A2.5 2.5 0 0 1 4 18.5v-12A2.5 2.5 0 0 1 6.5 4Z",
            "M4 17.5h14.5"
        )
    }
    val School: ImageVector by lazy {
        outlineIcon(
            "SchoolOutline",
            "M12 3L1 9l11 6 9-4.91V17h2V9L12 3z",
            "M5 13.18v4L12 21l7-3.82v-4"
        )
    }
    val Person: ImageVector by lazy {
        outlineIcon(
            "PersonOutline",
            "M12 3.5a4.2 4.2 0 1 1 0 8.4 4.2 4.2 0 0 1 0-8.4Z",
            "M4.5 20.5c0-3.6 3.1-6 7.5-6s7.5 2.4 7.5 6"
        )
    }

    val IdCard: ImageVector by lazy {
        outlineIcon(
            "IdCardOutline",
            "M5.5 5h13A2.5 2.5 0 0 1 21 7.5v9a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 16.5v-9A2.5 2.5 0 0 1 5.5 5Z",
            "M8.5 9.3a1.6 1.6 0 1 1 0 3.2 1.6 1.6 0 0 1 0-3.2Z",
            "M6 16c.3-1.3 1.3-2 2.5-2s2.2.7 2.5 2",
            "M14 10h3.2",
            "M14 14h3.2"
        )
    }
    val ShieldCheck: ImageVector by lazy {
        outlineIcon(
            "ShieldCheckOutline",
            "M12 3l7 2.6v5.6c0 4.4-2.9 8-7 9.8-4.1-1.8-7-5.4-7-9.8V5.6L12 3Z",
            "M8.8 12.2l2.3 2.3 4.2-4.4"
        )
    }
    val InfoCircle: ImageVector by lazy {
        outlineIcon(
            "InfoCircleOutline",
            "M12 3.5a8.5 8.5 0 1 1 0 17 8.5 8.5 0 0 1 0-17Z",
            "M12 11v5.2",
            "M12 7.8h.01"
        )
    }
    val Logout: ImageVector by lazy {
        outlineIcon(
            "LogoutOutline",
            "M10 4.5H6.5a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2H10",
            "M9.5 12h10.2",
            "M16 8.3l3.7 3.7-3.7 3.7"
        )
    }
    val Palette: ImageVector by lazy {
        outlineIcon(
            "PaletteOutline",
            "M12 3.5c-4.7 0-8.5 3.6-8.5 8.2 0 4.4 3.5 7.8 7.7 7.8 1.2 0 1.9-.8 1.9-1.8 0-.5-.2-.9-.5-1.3-.3-.4-.5-.8-.5-1.3 0-.9.7-1.6 1.6-1.6H17c2 0 3.5-1.4 3.5-3.3 0-3.2-3.6-6.7-8.5-6.7Z",
            "M7.8 11.3h.01",
            "M10.3 7.9h.01",
            "M14.3 7.9h.01"
        )
    }
    val Camera: ImageVector by lazy {
        outlineIcon(
            "CameraOutline",
            "M4.5 8h2.2l1.2-2h8.2l1.2 2h2.2a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-15a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1Z",
            "M12 10.3a3 3 0 1 1 0 6 3 3 0 0 1 0-6Z"
        )
    }

    val Phone: ImageVector by lazy {
        outlineIcon(
            "PhoneOutline",
            "M9 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z",
            "M11 18h2"
        )
    }
    val Lock: ImageVector by lazy {
        outlineIcon(
            "LockOutline",
            "M6.5 10.5h11A1.5 1.5 0 0 1 19 12v7a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 19v-7a1.5 1.5 0 0 1 1.5-1.5Z",
            "M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"
        )
    }
    val Mail: ImageVector by lazy {
        outlineIcon(
            "MailOutline",
            "M5.5 5h13A2.5 2.5 0 0 1 21 7.5v9a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 16.5v-9A2.5 2.5 0 0 1 5.5 5Z",
            "M3.6 7.9 12 13.4l8.4-5.5"
        )
    }
    val Eye: ImageVector by lazy {
        outlineIcon(
            "EyeOutline",
            "M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z",
            "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"
        )
    }
    val EyeOff: ImageVector by lazy {
        outlineIcon(
            "EyeOffOutline",
            "M9.9 5.7A9.6 9.6 0 0 1 12 5.5c6 0 9.5 6.5 9.5 6.5a16 16 0 0 1-2.7 3.5",
            "M6.5 7C3.7 8.8 2.5 12 2.5 12S6 18.5 12 18.5c1.6 0 3-.4 4.2-1",
            "M9.9 9.9a3 3 0 0 0 4.2 4.2",
            "M4 4l16 16"
        )
    }
    val ArrowRight: ImageVector by lazy {
        outlineIcon(
            "ArrowRightOutline",
            "M5 12h14",
            "M13 6l6 6-6 6",
            strokeWidth = 2.2f
        )
    }
    val CloudUpload: ImageVector by lazy {
        outlineIcon(
            "CloudUploadOutline",
            "M7 18.5a4 4 0 0 1-.6-7.95A5.5 5.5 0 0 1 17 9.2a4.7 4.7 0 0 1 .5 9.3H7Z",
            "M12 16v-5.5",
            "M9.8 12.6l2.2-2.2 2.2 2.2"
        )
    }

    val Hash: ImageVector by lazy {
        outlineIcon("HashOutline", "M9.5 4L8 20", "M16 4l-1.5 16", "M4.5 9h15", "M4 15h15")
    }
    val GraduationCap: ImageVector by lazy {
        outlineIcon(
            "GraduationCapOutline",
            "M12 4.5L2.8 9.2 12 13.9l9.2-4.7L12 4.5Z",
            "M6.2 11.7v4.3c0 1.3 2.6 2.7 5.8 2.7s5.8-1.4 5.8-2.7v-4.3",
            "M21.2 9.2v5.3"
        )
    }
    val Building: ImageVector by lazy {
        outlineIcon(
            "BuildingOutline",
            "M5 20.5V8.2L12 4l7 4.2v12.3",
            "M3.5 20.5h17",
            "M9.5 20.5V16h5v4.5",
            "M12 10.8h.01"
        )
    }
    val MapPin: ImageVector by lazy {
        outlineIcon(
            "MapPinOutline",
            "M12 21c-3.6-3.9-6-7-6-10.2a6 6 0 0 1 12 0C18 14 15.6 17.1 12 21Z",
            "M12 12.2a2.2 2.2 0 1 0 0-4.4 2.2 2.2 0 0 0 0 4.4Z"
        )
    }
    val Check: ImageVector by lazy {
        outlineIcon("CheckOutline", "M5 12.5l4.5 4.5L19 7.5", strokeWidth = 2.4f)
    }
    val ChevronLeft: ImageVector by lazy {
        outlineIcon("ChevronLeftOutline", "M14.5 5.5L8 12l6.5 6.5", strokeWidth = 2.2f)
    }
    val ChevronDown: ImageVector by lazy {
        outlineIcon("ChevronDownOutline", "M5.5 9.5L12 16l6.5-6.5", strokeWidth = 2.2f)
    }
    val Pencil: ImageVector by lazy {
        outlineIcon(
            "PencilOutline",
            "M4.5 19.5l.8-3.6L16.4 4.8a2 2 0 0 1 2.8 0 2 2 0 0 1 0 2.8L8.1 18.7l-3.6.8Z",
            "M14.5 6.7l2.8 2.8"
        )
    }

    val Volume: ImageVector by lazy {
        outlineIcon(
            "VolumeOutline",
            "M4.5 9.8h3l4.2-3.6v11.6l-4.2-3.6h-3Z",
            "M15 9.2a4 4 0 0 1 0 5.6",
            "M17.5 6.8a7.4 7.4 0 0 1 0 10.4"
        )
    }
    val Vibrate: ImageVector by lazy {
        outlineIcon(
            "VibrateOutline",
            "M9.5 4.5h5A1.5 1.5 0 0 1 16 6v12a1.5 1.5 0 0 1-1.5 1.5h-5A1.5 1.5 0 0 1 8 18V6a1.5 1.5 0 0 1 1.5-1.5Z",
            "M5 9v6",
            "M2.8 10.8v2.4",
            "M19 9v6",
            "M21.2 10.8v2.4"
        )
    }
    val AlarmClock: ImageVector by lazy {
        outlineIcon(
            "AlarmClockOutline",
            "M12 7.5a6.5 6.5 0 1 1 0 13 6.5 6.5 0 0 1 0-13Z",
            "M12 10.5v3.2l2 1.3",
            "M4 6.5l2.6-2.2",
            "M20 6.5l-2.6-2.2"
        )
    }

    val History: ImageVector by lazy {
        outlineIcon(
            "HistoryOutline",
            "M4 12a8 8 0 1 0 2.6-5.9",
            "M4 4.5v4h4",
            "M12 8v4.2l2.8 1.6"
        )
    }

    /**
     * Ikon Beranda: empat kotak membulat berisi. Kotak kiri-atas dan kanan-bawah memakai [primary],
     * dua lainnya memakai [secondary], seperti pada desain.
     */
    fun homeGrid(primary: Color, secondary: Color): ImageVector =
        ImageVector.Builder(
            name = "HomeGrid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            fun square(x: Float, y: Float, color: Color) {
                val size = 8.2f
                val r = 2.4f
                val inner = size - 2 * r
                val d = "M${x + r} ${y}h${inner}a$r $r 0 0 1 $r $r" +
                    "v${inner}a$r $r 0 0 1 -$r $r" +
                    "h-${inner}a$r $r 0 0 1 -$r -$r" +
                    "v-${inner}a$r $r 0 0 1 $r -${r}Z"
                addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(color))
            }
            square(3f, 3f, primary)
            square(12.8f, 3f, secondary)
            square(3f, 12.8f, secondary)
            square(12.8f, 12.8f, primary)
        }.build()
}
