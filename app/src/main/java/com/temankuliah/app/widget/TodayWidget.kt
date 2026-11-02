package com.temankuliah.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.temankuliah.app.MainActivity
import com.temankuliah.app.R
import com.temankuliah.app.data.local.ScheduleEntity
import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.data.local.TemanKuliahDatabase
import com.temankuliah.app.domain.TaskStatus
import com.temankuliah.app.ui.components.IndonesianLocale
import com.temankuliah.app.ui.components.formatWeekdayDate
import com.temankuliah.app.domain.rethrowIfCancellation
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Data widget: diambil langsung dari database untuk pengguna yang sedang masuk. */
object TodayWidgetData {
    enum class State { SIGNED_OUT, READY, ERROR }

    data class Row(val title: String, val detail: String)

    /** Ringkasan satu baris widget: [badge] adalah label di kanan, [overdue] mewarnainya merah. */
    data class Highlight(val title: String, val detail: String, val badge: String, val overdue: Boolean = false)

    /** [classes]/[tasks] sudah dibatasi untuk ditampilkan; [moreClasses]/[moreTasks] = jumlah yang tidak muat ("+N lainnya"). */
    data class Snapshot(
        val state: State,
        val classes: List<Row> = emptyList(),
        val tasks: List<Row> = emptyList(),
        val moreClasses: Int = 0,
        val moreTasks: Int = 0,
        /** Kuliah berikutnya (atau yang sedang berlangsung) hari ini; null bila tidak ada. */
        val nextClass: Highlight? = null,
        /** True bila hari ini ada kuliah tetapi semuanya sudah lewat. */
        val classesFinished: Boolean = false,
        /** Tugas paling mendesak: tenggat hari ini/besok, atau yang sudah terlambat. */
        val urgentTask: Highlight? = null
    )

    /** Baris yang ditampilkan dan jumlah baris yang tersembunyi. */
    data class Limited(val rows: List<Row>, val more: Int)

    internal const val MAX_CLASS_ROWS = 3
    internal const val MAX_TASK_ROWS = 3

    /** Tugas terlambat dibatasi agar tidak menutupi tugas hari ini; sisanya masuk "+N lainnya". */
    internal const val MAX_OVERDUE_ROWS = 2

    private val clock = DateTimeFormatter.ofPattern("HH:mm")
    internal var databaseProvider: (Context) -> TemanKuliahDatabase = { TemanKuliahDatabase.getDatabase(it) }

    internal fun limitClasses(rows: List<Row>): Limited = Limited(rows.take(MAX_CLASS_ROWS), (rows.size - MAX_CLASS_ROWS).coerceAtLeast(0))

    /**
     * Tugas belum selesai yang jatuh tempo hari ini atau sudah terlambat. Urutan tampil: tugas hari ini lebih dulu
     * (paling dekat dulu), lalu tugas terlambat sebelum hari ini (paling baru terlambat dulu, maksimal
     * [MAX_OVERDUE_ROWS] dan hanya di slot yang tersisa dari [MAX_TASK_ROWS]).
     */
    internal fun arrangeTasks(tasks: List<TaskEntity>, now: Long, zone: ZoneId): Limited {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val open = tasks.filter { it.status != TaskStatus.SELESAI && it.dueAtMillis < endOfToday }
        val dueToday = open.filter { it.dueAtMillis >= startOfToday }.sortedBy { it.dueAtMillis }
        val overdue = open.filter { it.dueAtMillis < startOfToday }.sortedByDescending { it.dueAtMillis }
        val shownToday = dueToday.take(MAX_TASK_ROWS)
        val shownOverdue = overdue.take(minOf(MAX_OVERDUE_ROWS, MAX_TASK_ROWS - shownToday.size))
        val shown = shownToday + shownOverdue
        val rows = shown.map { task ->
            val time = Instant.ofEpochMilli(task.dueAtMillis).atZone(zone).toLocalTime().format(clock)
            Row(task.title, if (task.dueAtMillis < now) "Terlambat · $time" else "Tenggat $time")
        }
        return Limited(rows, open.size - shown.size)
    }

    private val dotClock = DateTimeFormatter.ofPattern("HH.mm")
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", IndonesianLocale)

    /** "08:00" -> "08.00" (format jam Indonesia di widget). */
    private fun dotted(time: String) = time.replace(':', '.')

    /**
     * Kuliah berikutnya dari jadwal hari ini yang belum ditandai selesai: yang masih berlangsung atau belum mulai.
     * Mengembalikan pasangan (ringkasan, semuaSudahLewat).
     */
    internal fun pickNextClass(todays: List<ScheduleEntity>, nowTime: LocalTime): Pair<Highlight?, Boolean> {
        val sorted = todays.sortedBy { it.startTime }
        val next = sorted.firstOrNull { schedule ->
            val end = runCatching { LocalTime.parse(schedule.endTime) }.getOrNull()
            end == null || end.isAfter(nowTime)
        } ?: return null to sorted.isNotEmpty()
        val start = runCatching { LocalTime.parse(next.startTime) }.getOrNull()
        val room = if (next.room.isBlank()) "Ruang belum diatur" else "Ruang ${next.room}"
        return Highlight(
            title = next.courseName,
            detail = "${dotted(next.startTime)}–${dotted(next.endTime)} · $room",
            badge = if (start != null && !start.isAfter(nowTime)) "Berlangsung" else "Berikutnya"
        ) to false
    }

    /**
     * Tugas belum selesai yang paling mendesak: tenggat hari ini atau besok (paling dekat dulu); bila tidak ada,
     * tugas terlambat yang paling baru.
     */
    internal fun pickUrgentTask(tasks: List<TaskEntity>, now: Long, zone: ZoneId): Highlight? {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfTomorrow = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val open = tasks.filter { it.status != TaskStatus.SELESAI }
        val task = open.filter { it.dueAtMillis in startOfToday until endOfTomorrow }.minByOrNull { it.dueAtMillis }
            ?: open.filter { it.dueAtMillis < startOfToday }.maxByOrNull { it.dueAtMillis }
            ?: return null
        val due = Instant.ofEpochMilli(task.dueAtMillis).atZone(zone)
        val time = due.toLocalTime().format(dotClock)
        val overdue = task.dueAtMillis < now
        val detail = when {
            overdue && due.toLocalDate() == today -> "Terlambat · $time"
            overdue -> "Terlambat sejak ${due.toLocalDate().format(dayMonth)}"
            due.toLocalDate() == today -> "Tenggat hari ini, $time"
            else -> "Tenggat besok, $time"
        }
        return Highlight(task.title, detail, if (overdue) "Terlambat" else "Mendesak", overdue)
    }

    /**
     * Waktu terdekat setelah [now] saat isi widget berubah: jam mulai atau selesai kuliah hari ini yang belum
     * ditandai selesai, atau tenggat tugas yang belum selesai. Null bila tidak ada batas waktu tersisa
     * (pergantian hari ditangani oleh DATE_CHANGED).
     */
    internal fun nextBoundaryMillis(
        todaysSchedules: List<ScheduleEntity>,
        tasks: List<TaskEntity>,
        now: Long,
        zone: ZoneId
    ): Long? {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val classTimes = todaysSchedules.flatMap { listOf(it.startTime, it.endTime) }.mapNotNull { text ->
            val time = runCatching { LocalTime.parse(text) }.getOrNull() ?: return@mapNotNull null
            today.atTime(time).atZone(zone).toInstant().toEpochMilli()
        }
        val taskTimes = tasks.filter { it.status != TaskStatus.SELESAI }.map { it.dueAtMillis }
        return (classTimes + taskTimes).filter { it > now && it < endOfToday }.minOrNull()
    }

    /** Batas waktu berikutnya untuk pengguna aktif; null bila tidak ada pengguna atau tidak ada batas. */
    suspend fun loadNextBoundary(context: Context): Long? = withContext(Dispatchers.IO) {
        try {
            val db = databaseProvider(context)
            val user = db.userDao().getActiveUser() ?: return@withContext null
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            val todayIso = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()
            val todays = db.scheduleDao().getAllSchedulesByUserOnce(user.id)
                .filter { it.scheduledDate == todayIso && !it.isCompleted }
            nextBoundaryMillis(todays, db.taskDao().getAllByUserOnce(user.id), now, zone)
        } catch (error: Exception) {
            rethrowIfCancellation(error)
            null
        }
    }

    suspend fun load(context: Context): Snapshot = withContext(Dispatchers.IO) {
        runCatching {
            val db = databaseProvider(context)
            val user = db.userDao().getActiveUser() ?: return@withContext Snapshot(State.SIGNED_OUT)
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            val todayIso = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()
            val todaysSchedules = db.scheduleDao().getAllSchedulesByUserOnce(user.id)
                .filter { it.scheduledDate == todayIso && !it.isCompleted }
                .sortedBy { it.startTime }
            val classes = limitClasses(
                todaysSchedules.map { Row("${it.startTime} · ${it.courseName}", it.room.ifBlank { "Ruang belum diatur" }) }
            )
            val allTasks = db.taskDao().getAllByUserOnce(user.id)
            val tasks = arrangeTasks(allTasks, now, zone)
            val (nextClass, classesFinished) = pickNextClass(todaysSchedules, Instant.ofEpochMilli(now).atZone(zone).toLocalTime())
            Snapshot(
                state = State.READY,
                classes = classes.rows,
                tasks = tasks.rows,
                moreClasses = classes.more,
                moreTasks = tasks.more,
                nextClass = nextClass,
                classesFinished = classesFinished,
                urgentTask = pickUrgentTask(allTasks, now, zone)
            )
        }.getOrElse { error ->
            rethrowIfCancellation(error)
            Snapshot(State.ERROR)
        }
    }
}

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = TodayWidgetData.load(context)
        val openIntent = Intent(context, MainActivity::class.java)
        provideContent { TodayContent(snapshot, openIntent) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    /** Pergantian hari, jam, atau zona waktu mengubah isi "Hari ini"; perbarui tanpa menunggu pembaruan berkala. */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in REFRESH_ACTIONS) TodayWidgetUpdater.refreshAsync(context)
    }

    private companion object {
        val REFRESH_ACTIONS = setOf(
            TodayWidgetUpdater.ACTION_BOUNDARY,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}

/** Memperbarui widget di luar siklus Glance, dipanggil saat isi database berubah. */
object TodayWidgetUpdater {
    const val ACTION_BOUNDARY = "com.temankuliah.app.widget.ACTION_BOUNDARY"
    private const val BOUNDARY_REQUEST_CODE = 7101
    private const val BOUNDARY_SLACK_MS = 1_000L
    private const val DEBOUNCE_MS = 500L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = AtomicBoolean(false)

    /** Banyak invalidasi beruntun digabung jadi satu pembaruan setelah [DEBOUNCE_MS]. */
    fun refreshAsync(context: Context) {
        val app = context.applicationContext
        if (!pending.compareAndSet(false, true)) return
        scope.launch {
            delay(DEBOUNCE_MS)
            pending.set(false)
            try {
                TodayWidget().updateAll(app)
                scheduleNextBoundary(app, TodayWidgetData.loadNextBoundary(app))
            } catch (error: Exception) {
                rethrowIfCancellation(error)
            }
        }
    }

    /**
     * Memasang satu pembaruan di batas waktu berikutnya (mulai/selesai kuliah, tenggat tugas) agar label
     * "Berlangsung", "Berikutnya", dan "Terlambat" berganti tepat waktu. Memakai alarm tidak presisi
     * sehingga tidak butuh izin tambahan; keterlambatan beberapa menit bisa terjadi saat mode hemat daya.
     */
    internal fun scheduleNextBoundary(context: Context, atMillis: Long?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TodayWidgetReceiver::class.java).setAction(ACTION_BOUNDARY)
        val pendingIntent = PendingIntent.getBroadcast(
            context, BOUNDARY_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (atMillis == null) {
            alarmManager.cancel(pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, atMillis + BOUNDARY_SLACK_MS, pendingIntent)
        }
    }
}

private val White = ColorProvider(Color.White)
private val WhiteSoft = ColorProvider(Color.White.copy(alpha = 0.85f))
private val UrgentText = ColorProvider(Color(0xFF7A3E00))
private val OverdueText = ColorProvider(Color(0xFF8A1C12))

private val HeaderTitleStyle = TextStyle(color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
private val DatePillStyle = TextStyle(color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
private val RowTitleStyle = TextStyle(color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
private val RowDetailStyle = TextStyle(color = WhiteSoft, fontSize = 11.sp)
private val BadgeStyle = TextStyle(color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

@Composable
private fun TodayContent(snapshot: TodayWidgetData.Snapshot, openIntent: Intent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(10.dp)
            .clickable(actionStartActivity(openIntent))
    ) {
        Header()
        Spacer(GlanceModifier.height(6.dp))
        when (snapshot.state) {
            TodayWidgetData.State.SIGNED_OUT -> MessageRow("Masuk ke TemanKuliah untuk melihat jadwal.")
            TodayWidgetData.State.ERROR -> MessageRow("Data belum dapat dimuat. Ketuk untuk membuka aplikasi.")
            TodayWidgetData.State.READY -> {
                SummaryRow(
                    icon = R.drawable.ic_widget_book,
                    heading = "Kuliah",
                    highlight = snapshot.nextClass,
                    emptyText = if (snapshot.classesFinished) "Kuliah hari ini sudah selesai" else "Tidak ada kuliah hari ini"
                )
                Spacer(GlanceModifier.height(6.dp))
                SummaryRow(
                    icon = if (snapshot.urgentTask == null) R.drawable.ic_widget_task_check else R.drawable.ic_widget_task_list,
                    heading = "Tugas",
                    highlight = snapshot.urgentTask,
                    emptyText = "Tidak ada tugas mendesak"
                )
            }
        }
    }
}

/** Ikon kalender, judul "Hari ini", dan kapsul tanggal (mis. "Min, 25 Okt") di kanan. */
@Composable
private fun Header() {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier.size(26.dp).background(ImageProvider(R.drawable.widget_header_icon_bg)),
            contentAlignment = Alignment.Center
        ) {
            Image(ImageProvider(R.drawable.ic_widget_calendar), contentDescription = null, modifier = GlanceModifier.size(16.dp))
        }
        Spacer(GlanceModifier.width(8.dp))
        Text("Hari ini", style = HeaderTitleStyle, maxLines = 1)
        Spacer(GlanceModifier.defaultWeight())
        Box(
            modifier = GlanceModifier
                .background(ImageProvider(R.drawable.widget_pill_bg))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(formatWeekdayDate(System.currentTimeMillis()), style = DatePillStyle, maxLines = 1)
        }
    }
}

/**
 * Satu baris ringkasan. Ada data: judul + rincian dan badge di kanan. Kosong: teks [emptyText] dan tanda centang.
 */
@Composable
private fun SummaryRow(icon: Int, heading: String, highlight: TodayWidgetData.Highlight?, emptyText: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_row_bg))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.size(28.dp).background(ImageProvider(R.drawable.widget_icon_tile)),
            contentAlignment = Alignment.Center
        ) {
            Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(17.dp))
        }
        Spacer(GlanceModifier.width(9.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(highlight?.title ?: heading, style = RowTitleStyle, maxLines = 1)
            Text(highlight?.detail ?: emptyText, style = RowDetailStyle, maxLines = 1)
        }
        Spacer(GlanceModifier.width(6.dp))
        if (highlight == null) {
            Box(
                modifier = GlanceModifier.size(22.dp).background(ImageProvider(R.drawable.widget_check_circle)),
                contentAlignment = Alignment.Center
            ) {
                Image(ImageProvider(R.drawable.ic_widget_check), contentDescription = "Selesai", modifier = GlanceModifier.size(12.dp))
            }
        } else {
            Badge(highlight)
        }
    }
}

/** "Berikutnya"/"Berlangsung" berupa kapsul putih transparan; "Mendesak"/"Terlambat" berkapsul oranye/merah dengan titik. */
@Composable
private fun Badge(highlight: TodayWidgetData.Highlight) {
    val tinted = highlight.badge == "Mendesak" || highlight.overdue
    if (!tinted) {
        Box(
            modifier = GlanceModifier
                .background(ImageProvider(R.drawable.widget_pill_bg))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(highlight.badge, style = BadgeStyle, maxLines = 1)
        }
        return
    }
    val textColor = if (highlight.overdue) OverdueText else UrgentText
    Row(
        modifier = GlanceModifier
            .background(ImageProvider(if (highlight.overdue) R.drawable.widget_pill_overdue else R.drawable.widget_pill_urgent))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(6.dp)
                .background(ImageProvider(if (highlight.overdue) R.drawable.widget_dot_overdue else R.drawable.widget_dot_urgent)),
            content = {}
        )
        Spacer(GlanceModifier.width(5.dp))
        Text(highlight.badge, style = BadgeStyle.copy(color = textColor), maxLines = 1)
    }
}

@Composable
private fun MessageRow(text: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_row_bg))
            .padding(12.dp)
    ) {
        Text(text, style = RowDetailStyle.copy(color = White))
    }
}
