package com.temankuliah.app

import com.temankuliah.app.data.local.TaskEntity
import com.temankuliah.app.domain.TaskStatus
import com.temankuliah.app.ui.calendar.AgendaKind
import com.temankuliah.app.ui.calendar.agendaBadgeLabel
import com.temankuliah.app.ui.calendar.agendaCountLabel
import com.temankuliah.app.ui.calendar.agendaKindOf
import com.temankuliah.app.ui.calendar.weekRangeLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class CalendarStage3UnitTest {

    @Test
    fun testWeeklyNavigationCalculations() {
        // Given Wednesday Oct 21, 2026
        val wednesday = LocalDate.of(2026, 10, 21)
        val startOfWeek = wednesday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)

        assertEquals(DayOfWeek.MONDAY, startOfWeek.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 19), startOfWeek)
        assertEquals(DayOfWeek.SUNDAY, endOfWeek.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 25), endOfWeek)

        val label = weekRangeLabel(startOfWeek, endOfWeek)
        assertEquals("19 – 25 Okt 2026", label)
    }

    @Test
    fun testWeeklyNavigationAcrossMonths() {
        val wednesday = LocalDate.of(2026, 9, 30)
        val startOfWeek = wednesday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)

        assertEquals(LocalDate.of(2026, 9, 28), startOfWeek)
        assertEquals(LocalDate.of(2026, 10, 4), endOfWeek)

        val label = weekRangeLabel(startOfWeek, endOfWeek)
        assertEquals("28 Sep – 4 Okt 2026", label)
    }

    @Test
    fun testWeeklyNavigationAcrossYears() {
        val date = LocalDate.of(2026, 12, 31)
        val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)

        assertEquals(LocalDate.of(2026, 12, 28), startOfWeek)
        assertEquals(LocalDate.of(2027, 1, 3), endOfWeek)

        val label = weekRangeLabel(startOfWeek, endOfWeek)
        assertEquals("28 Des 2026 – 3 Jan 2027", label)
    }

    @Test
    fun testAgendaKindAndBadgeResolution() {
        assertEquals(AgendaKind.Kuliah, agendaKindOf("Kuliah"))
        assertEquals(AgendaKind.Tugas, agendaKindOf("Tugas"))
        assertEquals(AgendaKind.Ujian, agendaKindOf("Ujian"))
        assertEquals(AgendaKind.Ujian, agendaKindOf("Kegiatan Akademik"))
        assertEquals(AgendaKind.Ujian, agendaKindOf("Seminar"))

        assertEquals("Kuliah", agendaBadgeLabel("Kuliah"))
        assertEquals("Tugas", agendaBadgeLabel("Tugas"))
        assertEquals("Ujian", agendaBadgeLabel("Ujian"))
        assertEquals("Kegiatan", agendaBadgeLabel("Kegiatan Akademik"))
        assertEquals("Kegiatan", agendaBadgeLabel("Kegiatan Kampus"))
    }

    @Test
    fun testAgendaCountText() {
        assertEquals("3 agenda hari ini", agendaCountLabel(3, isToday = true))
        assertEquals("3 agenda", agendaCountLabel(3, isToday = false))
        assertEquals("0 agenda hari ini", agendaCountLabel(0, isToday = true))
    }

    @Test
    fun testDashboardDynamicTaskSegregation() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val tasks = listOf(
            TaskEntity(id = 1, userId = 1, semester = 1, title = "Tugas Lewat", dueAtMillis = startOfToday - 3600_000L, status = TaskStatus.BELUM),
            TaskEntity(id = 2, userId = 1, semester = 1, title = "Tugas Hari Ini", dueAtMillis = startOfToday + 3600_000L, status = TaskStatus.BELUM),
            TaskEntity(id = 3, userId = 1, semester = 1, title = "Tugas Besok", dueAtMillis = endOfToday + 3600_000L, status = TaskStatus.BELUM),
            TaskEntity(id = 4, userId = 1, semester = 1, title = "Tugas Selesai", dueAtMillis = startOfToday + 3600_000L, status = TaskStatus.SELESAI)
        )

        val open = tasks.filter { it.status != TaskStatus.SELESAI }
        val dueTodayOrOverdue = open.filter { it.dueAtMillis < endOfToday }
        val later = open.filter { it.dueAtMillis >= endOfToday }

        assertEquals(2, dueTodayOrOverdue.size)
        assertEquals(1, later.size)
        assertEquals("Tugas Besok", later.first().title)
    }
}
