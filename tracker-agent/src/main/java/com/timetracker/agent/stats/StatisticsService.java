package com.timetracker.agent.stats;

import com.timetracker.agent.buffer.ActivityInterval;
import com.timetracker.agent.buffer.DataBuffer;
import com.timetracker.agent.storage.ActivityStore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Агрегация локальной статистики по сохранённым и текущим интервалам.
 */
public final class StatisticsService {

    private final ActivityStore activityStore;
    private final DataBuffer dataBuffer;
    private final ZoneId zoneId;

    public StatisticsService(ActivityStore activityStore, DataBuffer dataBuffer) {
        this(activityStore, dataBuffer, ZoneId.systemDefault());
    }

    public StatisticsService(ActivityStore activityStore, DataBuffer dataBuffer, ZoneId zoneId) {
        this.activityStore = activityStore;
        this.dataBuffer = dataBuffer;
        this.zoneId = zoneId;
    }

    public StatisticsSnapshot buildSnapshot(StatsPeriod statsPeriod) {
        InstantRange instantRange = resolveInstantRange(statsPeriod);
        List<ActivityInterval> relevantIntervals = collectActiveIntervals().stream()
                .map(activityInterval -> clipToRange(activityInterval, instantRange))
                .filter(Objects::nonNull)
                .filter(activityInterval -> activityInterval.getDurationSeconds() > 0)
                .collect(Collectors.toList());

        long totalActiveSeconds = relevantIntervals.stream()
                .mapToLong(ActivityInterval::getDurationSeconds)
                .sum();

        List<DailyUsageSummary> dailyUsageSummaries = relevantIntervals.stream()
                .collect(Collectors.groupingBy(
                        activityInterval -> toLocalDate(activityInterval.getStartInstant()),
                        Collectors.summingLong(ActivityInterval::getDurationSeconds)
                ))
                .entrySet()
                .stream()
                .map(entry -> new DailyUsageSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DailyUsageSummary::getDate).reversed())
                .collect(Collectors.toList());

        List<ApplicationUsageSummary> applicationUsageSummaries = relevantIntervals.stream()
                .collect(Collectors.groupingBy(
                        ActivityInterval::getApplicationName,
                        Collectors.summingLong(ActivityInterval::getDurationSeconds)
                ))
                .entrySet()
                .stream()
                .map(entry -> new ApplicationUsageSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ApplicationUsageSummary::getDurationSeconds).reversed())
                .collect(Collectors.toList());

        return new StatisticsSnapshot(
                statsPeriod,
                totalActiveSeconds,
                dailyUsageSummaries,
                applicationUsageSummaries
        );
    }

    /**
     * Разбивка по приложениям за сегодня (для главной панели).
     */
    public List<ApplicationUsageSummary> buildTodayApplicationUsage() {
        return buildSnapshot(StatsPeriod.DAY).getApplicationUsageSummaries();
    }

    public long buildTodayActiveSeconds() {
        return buildSnapshot(StatsPeriod.DAY).getTotalActiveSeconds();
    }

    private List<ActivityInterval> collectActiveIntervals() {
        List<ActivityInterval> activityIntervals = new ArrayList<>(activityStore.getAllIntervals());
        ActivityInterval currentActivityInterval = dataBuffer.getCurrentInterval();
        if (Objects.nonNull(currentActivityInterval) && !currentActivityInterval.isIdle()) {
            activityIntervals.add(currentActivityInterval);
        }
        return activityIntervals;
    }

    private InstantRange resolveInstantRange(StatsPeriod statsPeriod) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return switch (statsPeriod) {
            case DAY -> new InstantRange(
                    now.toLocalDate().atStartOfDay(zoneId).toInstant(),
                    now.toInstant()
            );
            case WEEK -> new InstantRange(
                    now.toLocalDate().minusDays(6).atStartOfDay(zoneId).toInstant(),
                    now.toInstant()
            );
            case MONTH -> new InstantRange(
                    now.toLocalDate().minusDays(29).atStartOfDay(zoneId).toInstant(),
                    now.toInstant()
            );
            case YEAR -> new InstantRange(
                    now.toLocalDate().minusDays(364).atStartOfDay(zoneId).toInstant(),
                    now.toInstant()
            );
            case ALL_TIME -> new InstantRange(Instant.EPOCH, now.toInstant());
        };
    }

    private ActivityInterval clipToRange(ActivityInterval activityInterval, InstantRange instantRange) {
        Instant startInstant = activityInterval.getStartInstant();
        Instant endInstant = Objects.nonNull(activityInterval.getEndInstant())
                ? activityInterval.getEndInstant()
                : Instant.now();

        if (endInstant.isBefore(instantRange.startInclusive())
                || !startInstant.isBefore(instantRange.endExclusive())) {
            return null;
        }

        Instant clippedStartInstant = startInstant.isBefore(instantRange.startInclusive())
                ? instantRange.startInclusive()
                : startInstant;
        Instant clippedEndInstant = endInstant.isAfter(instantRange.endExclusive())
                ? instantRange.endExclusive()
                : endInstant;

        if (!clippedStartInstant.isBefore(clippedEndInstant)) {
            return null;
        }

        return new ActivityInterval(
                clippedStartInstant,
                clippedEndInstant,
                activityInterval.getApplicationName(),
                activityInterval.getWindowTitle(),
                activityInterval.isIdle()
        );
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(zoneId).toLocalDate();
    }

    private record InstantRange(Instant startInclusive, Instant endExclusive) {
    }
}
