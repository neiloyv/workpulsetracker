package com.workpulsetracker.agent.stats;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsCalendarTest {

    private static final ZoneId ZONE = ZoneOffset.UTC;
    // Среда, 2024-06-12.
    private static final Clock FIXED_CLOCK = Clock.fixed(LocalDate.of(2024, 6, 12)
            .atStartOfDay(ZONE).toInstant().plusSeconds(12 * 3600), ZONE);

    private final StatsCalendar calendar = new StatsCalendar(FIXED_CLOCK);

    @Test
    void normalizeAnchorSnapsToStartOfPeriod() {
        LocalDate anchor = LocalDate.of(2024, 6, 12); // среда
        assertEquals(LocalDate.of(2024, 6, 10), calendar.normalizeAnchorDate(StatsPeriod.WEEK, anchor));
        assertEquals(LocalDate.of(2024, 6, 1), calendar.normalizeAnchorDate(StatsPeriod.MONTH, anchor));
        assertEquals(LocalDate.of(2024, 1, 1), calendar.normalizeAnchorDate(StatsPeriod.YEAR, anchor));
        assertEquals(anchor, calendar.normalizeAnchorDate(StatsPeriod.DAY, anchor));
    }

    @Test
    void normalizeAnchorClampsFutureToToday() {
        LocalDate future = LocalDate.of(2025, 1, 1);
        assertEquals(LocalDate.of(2024, 6, 12), calendar.normalizeAnchorDate(StatsPeriod.DAY, future));
    }

    @Test
    void nullAnchorIsToday() {
        assertEquals(LocalDate.of(2024, 6, 12), calendar.normalizeAnchorDate(StatsPeriod.DAY, null));
    }

    @Test
    void shiftAnchorMovesByOnePeriod() {
        LocalDate weekAnchor = LocalDate.of(2024, 6, 10);
        assertEquals(LocalDate.of(2024, 6, 3), calendar.shiftAnchorDate(StatsPeriod.WEEK, weekAnchor, -1));
        assertEquals(LocalDate.of(2024, 5, 1), calendar.shiftAnchorDate(StatsPeriod.MONTH, LocalDate.of(2024, 6, 1), -1));
        assertEquals(LocalDate.of(2023, 1, 1), calendar.shiftAnchorDate(StatsPeriod.YEAR, LocalDate.of(2024, 1, 1), -1));
    }

    @Test
    void canNavigateToNextPeriodWhenPast() {
        assertTrue(calendar.canNavigateToNextPeriod(StatsPeriod.MONTH, LocalDate.of(2024, 4, 1)));
        assertTrue(calendar.canNavigateToNextPeriod(StatsPeriod.YEAR, LocalDate.of(2022, 1, 1)));
        // NB: на текущем периоде метод всё равно возвращает true (probe-дата клампится к сегодня);
        // это поведение оригинала — навигация вперёд на месте просто становится no-op в StatisticsPanel.
        assertTrue(calendar.canNavigateToNextPeriod(StatsPeriod.MONTH, LocalDate.of(2024, 6, 1)));
    }

    @Test
    void previousPeriodBoundedByEarliestActivity() {
        LocalDate earliest = LocalDate.of(2024, 5, 20);
        assertTrue(calendar.canNavigateToPreviousPeriod(StatsPeriod.MONTH, LocalDate.of(2024, 6, 1), earliest));
        assertFalse(calendar.canNavigateToPreviousPeriod(StatsPeriod.MONTH, LocalDate.of(2024, 5, 1), earliest));
        assertFalse(calendar.canNavigateToPreviousPeriod(StatsPeriod.MONTH, LocalDate.of(2024, 6, 1), null));
    }

    @Test
    void previousYearComparesByYearOfEarliestActivity() {
        assertTrue(calendar.canNavigateToPreviousPeriod(
                StatsPeriod.YEAR, LocalDate.of(2024, 1, 1), LocalDate.of(2023, 8, 1)));
        assertFalse(calendar.canNavigateToPreviousPeriod(
                StatsPeriod.YEAR, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 8, 1)));
    }

    @Test
    void listWeekDatesReturnsMondayToSunday() {
        var weekDates = calendar.listWeekDates(LocalDate.of(2024, 6, 12));
        assertEquals(7, weekDates.size());
        assertEquals(LocalDate.of(2024, 6, 10), weekDates.get(0));
        assertEquals(LocalDate.of(2024, 6, 16), weekDates.get(6));
    }

    @Test
    void weekIntersectsMonthBoundaries() {
        YearMonth june = YearMonth.of(2024, 6);
        assertTrue(calendar.weekIntersectsMonth(LocalDate.of(2024, 6, 10), june));
        assertTrue(calendar.weekIntersectsMonth(LocalDate.of(2024, 5, 27), june)); // неделя 27мая–2июн
        assertFalse(calendar.weekIntersectsMonth(LocalDate.of(2024, 5, 20), june)); // неделя 20–26 мая
    }

    @Test
    void weekNavigationInsideMonthStaysWithinBounds() {
        YearMonth may = YearMonth.of(2024, 5);
        LocalDate firstWeek = calendar.firstWeekMondayOfMonth(may); // 2024-04-29
        assertEquals(LocalDate.of(2024, 4, 29), firstWeek);
        assertFalse(calendar.canNavigateToPreviousWeekInMonth(may, firstWeek));
        assertTrue(calendar.canNavigateToNextWeekInMonth(may, firstWeek));
        assertEquals(LocalDate.of(2024, 5, 6), calendar.shiftWeekMondayInMonth(may, firstWeek, 1));
        // Не даёт уйти за пределы месяца.
        assertEquals(firstWeek, calendar.shiftWeekMondayInMonth(may, firstWeek, -1));
    }
}
