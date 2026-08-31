package com.workpulsetracker.agent.stats;

import com.workpulsetracker.agent.buffer.ActivityInterval;
import com.workpulsetracker.agent.storage.UserSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Характеризация текущего поведения агрегации: снимок, матрица, суточный таймлайн.
 * Все интервалы — в полностью завершённом марте 2024, поэтому границы периода детерминированы.
 */
class StatisticsServiceTest {

    private final List<ActivityInterval> intervals = new ArrayList<>();
    private UserSettings userSettings;
    private StatisticsService statisticsService;

    private static Instant at(int day, int hour, int minute) {
        return LocalDateTime.of(2024, 3, day, hour, minute).toInstant(ZoneOffset.UTC);
    }

    private void add(int day, int startHour, int startMinute, int endHour, int endMinute, String app, boolean idle) {
        intervals.add(new ActivityInterval(
                at(day, startHour, startMinute), at(day, endHour, endMinute), app, "title", idle));
    }

    @BeforeEach
    void setUp() {
        userSettings = new UserSettings();
        statisticsService = new StatisticsService(() -> intervals, () -> null, userSettings, ZoneOffset.UTC);
    }

    @Test
    void monthSnapshotAggregatesTrackedNonIdleTime() {
        add(4, 10, 0, 11, 0, "chrome", false);   // 3600
        add(5, 9, 0, 9, 30, "chrome", false);    // 1800
        add(4, 12, 0, 14, 0, "idea64", false);   // 7200
        add(4, 15, 0, 15, 30, "chrome", true);   // idle -> исключается
        userSettings.setApplicationTracked("slack", false);
        add(6, 8, 0, 9, 0, "slack", false);      // Track OFF -> исключается

        StatisticsSnapshot snapshot = statisticsService.buildSnapshotForAnchor(
                StatsPeriod.MONTH, LocalDate.of(2024, 3, 15));

        assertEquals(12600, snapshot.getTotalActiveSeconds());

        List<ApplicationUsageSummary> apps = snapshot.getApplicationUsageSummaries();
        assertEquals(2, apps.size());
        assertEquals("idea64", apps.get(0).getApplicationName());
        assertEquals(7200, apps.get(0).getDurationSeconds());
        assertEquals("chrome", apps.get(1).getApplicationName());
        assertEquals(5400, apps.get(1).getDurationSeconds());

        List<DailyUsageSummary> days = snapshot.getDailyUsageSummaries();
        assertEquals(LocalDate.of(2024, 3, 5), days.get(0).getDate());
        assertEquals(1800, days.get(0).getDurationSeconds());
        assertEquals(LocalDate.of(2024, 3, 4), days.get(1).getDate());
        assertEquals(10800, days.get(1).getDurationSeconds());
    }

    @Test
    void monthMatrixDistributesTimeIntoDayBuckets() {
        add(4, 10, 0, 11, 0, "chrome", false);
        add(5, 9, 0, 9, 30, "chrome", false);
        add(4, 12, 0, 14, 0, "idea64", false);

        ApplicationUsageMatrix matrix = statisticsService.buildApplicationUsageMatrixForAnchor(
                StatsPeriod.MONTH, LocalDate.of(2024, 3, 15));

        assertEquals(31, matrix.getPeriodBuckets().size());
        assertEquals(List.of("idea64", "chrome"), matrix.getApplicationNames());
        assertEquals(12600, matrix.getTotalActiveSeconds());
        assertEquals(7200, matrix.getApplicationTotalSeconds(0));
        assertEquals(5400, matrix.getApplicationTotalSeconds(1));
        // День 4 = индекс 3, день 5 = индекс 4.
        assertEquals(7200, matrix.getDurationSeconds(0, 3));
        assertEquals(3600, matrix.getDurationSeconds(1, 3));
        assertEquals(1800, matrix.getDurationSeconds(1, 4));
        assertEquals(10800, matrix.getBucketTotalSeconds(3));
    }

    @Test
    void intervalSpanningMidnightIsSplitBetweenDayBuckets() {
        intervals.add(new ActivityInterval(at(10, 23, 0), at(11, 1, 0), "idea64", "t", false));

        ApplicationUsageMatrix matrix = statisticsService.buildApplicationUsageMatrixForAnchor(
                StatsPeriod.MONTH, LocalDate.of(2024, 3, 15));

        assertEquals(3600, matrix.getDurationSeconds(0, 9));   // 23:00–24:00 день 10
        assertEquals(3600, matrix.getDurationSeconds(0, 10));  // 00:00–01:00 день 11
        assertEquals(7200, matrix.getApplicationTotalSeconds(0));
    }

    @Test
    void dayTimelineMergesAdjacentSameStateAndKeepsGaps() {
        add(4, 10, 0, 11, 0, "chrome", false);
        add(4, 12, 0, 14, 0, "idea64", false);
        add(4, 15, 0, 15, 30, "chrome", true);
        userSettings.setApplicationTracked("slack", false);
        add(4, 16, 0, 16, 15, "slack", false);

        List<DayActivityTimelineRow> rows = statisticsService.buildWeekActivityTimelines(LocalDate.of(2024, 3, 4));
        assertEquals(7, rows.size());

        DayActivityTimeline monday = rows.get(0).getDayActivityTimeline();
        List<DayActivityTimelineSegment> segments = monday.getSegments();
        assertEquals(4, segments.size());
        assertEquals(DayActivityState.ACTIVE, segments.get(0).getActivityState());
        assertEquals(DayActivityState.ACTIVE, segments.get(1).getActivityState());
        assertEquals(DayActivityState.IDLE, segments.get(2).getActivityState());
        assertEquals(DayActivityState.EXCLUDED, segments.get(3).getActivityState());
        assertEquals(LocalDateTime.of(2024, 3, 4, 10, 0), segments.get(0).getStartDateTime());
        assertEquals(LocalDateTime.of(2024, 3, 4, 11, 0), segments.get(0).getEndDateTime());

        // Остальные дни недели пустые.
        assertTrue(rows.get(1).getDayActivityTimeline().isEmpty());
    }

    @Test
    void backToBackSameStateIntervalsMergeIntoOneSegment() {
        add(4, 10, 0, 11, 0, "chrome", false);
        add(4, 11, 0, 12, 0, "idea64", false); // вплотную, оба ACTIVE -> один сегмент

        DayActivityTimeline monday = statisticsService.buildWeekActivityTimelines(LocalDate.of(2024, 3, 4))
                .get(0).getDayActivityTimeline();

        assertEquals(1, monday.getSegments().size());
        assertEquals(LocalDateTime.of(2024, 3, 4, 10, 0), monday.getSegments().get(0).getStartDateTime());
        assertEquals(LocalDateTime.of(2024, 3, 4, 12, 0), monday.getSegments().get(0).getEndDateTime());
    }
}
