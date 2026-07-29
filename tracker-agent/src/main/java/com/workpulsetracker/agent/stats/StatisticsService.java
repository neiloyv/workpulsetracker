package com.workpulsetracker.agent.stats;

import com.workpulsetracker.agent.buffer.ActivityInterval;
import com.workpulsetracker.agent.buffer.DataBuffer;
import com.workpulsetracker.agent.storage.ActivityStore;
import com.workpulsetracker.common.i18n.UserLocaleContext;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        return buildSnapshot(statsPeriod, null, null);
    }

    public StatisticsSnapshot buildSnapshot(
            StatsPeriod statsPeriod,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
        InstantRange instantRange = resolveInstantRange(statsPeriod, rangeStartDate, rangeEndDate);
        List<ActivityInterval> relevantIntervals = collectClippedActiveIntervals(instantRange);

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

        List<ApplicationUsageSummary> applicationUsageSummaries = buildApplicationUsageSummaries(relevantIntervals);

        return new StatisticsSnapshot(
                statsPeriod,
                totalActiveSeconds,
                dailyUsageSummaries,
                applicationUsageSummaries
        );
    }

    /**
     * Матрица: приложения × колонки периода (дни / недели / месяцы / годы).
     */
    public ApplicationUsageMatrix buildApplicationUsageMatrix(StatsPeriod statsPeriod) {
        return buildApplicationUsageMatrix(statsPeriod, null, null);
    }

    public ApplicationUsageMatrix buildApplicationUsageMatrix(
            StatsPeriod statsPeriod,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
        List<PeriodBucket> periodBuckets = buildPeriodBuckets(statsPeriod, rangeStartDate, rangeEndDate);
        if (periodBuckets.isEmpty()) {
            return ApplicationUsageMatrix.empty(statsPeriod, periodBuckets);
        }

        InstantRange instantRange = new InstantRange(
                periodBuckets.get(0).getStartInclusive(),
                periodBuckets.get(periodBuckets.size() - 1).getEndExclusive()
        );
        List<ActivityInterval> relevantIntervals = collectClippedActiveIntervals(instantRange);
        List<ApplicationUsageSummary> applicationUsageSummaries = buildApplicationUsageSummaries(relevantIntervals);
        if (applicationUsageSummaries.isEmpty()) {
            return ApplicationUsageMatrix.empty(statsPeriod, periodBuckets);
        }

        List<String> applicationNames = applicationUsageSummaries.stream()
                .map(ApplicationUsageSummary::getApplicationName)
                .collect(Collectors.toList());
        Map<String, Integer> applicationIndexByName = IntStream.range(0, applicationNames.size())
                .boxed()
                .collect(Collectors.toMap(applicationNames::get, applicationIndex -> applicationIndex, (left, right) -> left, LinkedHashMap::new));

        long[][] durationSecondsByApplicationAndBucket = new long[applicationNames.size()][periodBuckets.size()];
        long[] applicationTotalSeconds = new long[applicationNames.size()];
        long totalActiveSeconds = 0L;

        for (int bucketIndex = 0; bucketIndex < periodBuckets.size(); bucketIndex++) {
            PeriodBucket periodBucket = periodBuckets.get(bucketIndex);
            InstantRange bucketRange = new InstantRange(
                    periodBucket.getStartInclusive(),
                    periodBucket.getEndExclusive()
            );
            for (ActivityInterval activityInterval : relevantIntervals) {
                ActivityInterval clippedActivityInterval = clipToRange(activityInterval, bucketRange);
                if (Objects.isNull(clippedActivityInterval)) {
                    continue;
                }
                Integer applicationIndex = applicationIndexByName.get(clippedActivityInterval.getApplicationName());
                if (Objects.isNull(applicationIndex)) {
                    continue;
                }
                long durationSeconds = clippedActivityInterval.getDurationSeconds();
                durationSecondsByApplicationAndBucket[applicationIndex][bucketIndex] += durationSeconds;
                applicationTotalSeconds[applicationIndex] += durationSeconds;
                totalActiveSeconds += durationSeconds;
            }
        }

        return new ApplicationUsageMatrix(
                statsPeriod,
                periodBuckets,
                applicationNames,
                durationSecondsByApplicationAndBucket,
                applicationTotalSeconds,
                totalActiveSeconds
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

    /**
     * Таймлайн активных (non-idle) интервалов за сегодня для главной панели.
     */
    public DayActivityTimeline buildTodayActivityTimeline() {
        InstantRange instantRange = resolveInstantRange(StatsPeriod.DAY, null, null);
        LocalDateTime rangeStartDateTime = LocalDateTime.ofInstant(instantRange.startInclusive(), zoneId);
        LocalDateTime rangeEndDateTime = LocalDateTime.ofInstant(instantRange.endExclusive(), zoneId);

        List<ActivityInterval> clippedIntervals = collectClippedActiveIntervals(instantRange).stream()
                .sorted(Comparator.comparing(ActivityInterval::getStartInstant))
                .collect(Collectors.toList());
        if (clippedIntervals.isEmpty()) {
            return DayActivityTimeline.empty(rangeStartDateTime, rangeEndDateTime);
        }

        List<DayActivityTimelineSegment> timelineSegments = new ArrayList<>();
        ActivityInterval mergeCandidate = clippedIntervals.get(0);
        for (int intervalIndex = 1; intervalIndex < clippedIntervals.size(); intervalIndex++) {
            ActivityInterval nextInterval = clippedIntervals.get(intervalIndex);
            if (canMergeTimelineIntervals(mergeCandidate, nextInterval)) {
                mergeCandidate = new ActivityInterval(
                        mergeCandidate.getStartInstant(),
                        nextInterval.getEndInstant(),
                        mergeCandidate.getApplicationName(),
                        mergeCandidate.getWindowTitle(),
                        false
                );
            } else {
                timelineSegments.add(toTimelineSegment(mergeCandidate));
                mergeCandidate = nextInterval;
            }
        }
        timelineSegments.add(toTimelineSegment(mergeCandidate));

        LocalDateTime activityRangeStartDateTime = timelineSegments.get(0).getStartDateTime();
        LocalDateTime activityRangeEndDateTime = timelineSegments.stream()
                .map(DayActivityTimelineSegment::getEndDateTime)
                .max(LocalDateTime::compareTo)
                .orElse(activityRangeStartDateTime);
        if (!activityRangeStartDateTime.isBefore(activityRangeEndDateTime)) {
            activityRangeEndDateTime = activityRangeStartDateTime.plusMinutes(1L);
        }
        return new DayActivityTimeline(activityRangeStartDateTime, activityRangeEndDateTime, timelineSegments);
    }

    private DayActivityTimelineSegment toTimelineSegment(ActivityInterval activityInterval) {
        Instant endInstant = Objects.nonNull(activityInterval.getEndInstant())
                ? activityInterval.getEndInstant()
                : Instant.now();
        return new DayActivityTimelineSegment(
                activityInterval.getApplicationName(),
                LocalDateTime.ofInstant(activityInterval.getStartInstant(), zoneId),
                LocalDateTime.ofInstant(endInstant, zoneId)
        );
    }

    private static boolean canMergeTimelineIntervals(
            ActivityInterval leftInterval,
            ActivityInterval rightInterval
    ) {
        if (!Objects.equals(leftInterval.getApplicationName(), rightInterval.getApplicationName())) {
            return false;
        }
        Instant leftEndInstant = Objects.nonNull(leftInterval.getEndInstant())
                ? leftInterval.getEndInstant()
                : Instant.now();
        return !rightInterval.getStartInstant().isAfter(leftEndInstant.plusSeconds(1L));
    }

    private List<ApplicationUsageSummary> buildApplicationUsageSummaries(List<ActivityInterval> activityIntervals) {
        return activityIntervals.stream()
                .collect(Collectors.groupingBy(
                        ActivityInterval::getApplicationName,
                        Collectors.summingLong(ActivityInterval::getDurationSeconds)
                ))
                .entrySet()
                .stream()
                .map(entry -> new ApplicationUsageSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ApplicationUsageSummary::getDurationSeconds).reversed())
                .collect(Collectors.toList());
    }

    private List<ActivityInterval> collectClippedActiveIntervals(InstantRange instantRange) {
        return collectActiveIntervals().stream()
                .map(activityInterval -> clipToRange(activityInterval, instantRange))
                .filter(Objects::nonNull)
                .filter(activityInterval -> activityInterval.getDurationSeconds() > 0)
                .collect(Collectors.toList());
    }

    private List<ActivityInterval> collectActiveIntervals() {
        List<ActivityInterval> activityIntervals = new ArrayList<>(activityStore.getAllIntervals());
        ActivityInterval currentActivityInterval = dataBuffer.getCurrentInterval();
        if (Objects.nonNull(currentActivityInterval) && !currentActivityInterval.isIdle()) {
            activityIntervals.add(currentActivityInterval);
        }
        return activityIntervals;
    }

    private List<PeriodBucket> buildPeriodBuckets(
            StatsPeriod statsPeriod,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();
        Locale locale = UserLocaleContext.getLanguage().toLocale();

        return switch (statsPeriod) {
            case DAY -> List.of(new PeriodBucket(
                    today.format(DateTimeFormatter.ofPattern("dd.MM", locale)),
                    today.atStartOfDay(zoneId).toInstant(),
                    now.toInstant()
            ));
            case WEEK -> buildWeekDayBuckets(today, now, locale);
            case MONTH -> buildMonthWeekBuckets(today, now, locale);
            case YEAR -> buildYearMonthBuckets(today, now, locale);
            case ALL_TIME -> buildAllTimeYearBuckets(now);
            case CUSTOM -> buildCustomDayBuckets(rangeStartDate, rangeEndDate, today, now, locale);
        };
    }

    private List<PeriodBucket> buildCustomDayBuckets(
            LocalDate rangeStartDate,
            LocalDate rangeEndDate,
            LocalDate today,
            ZonedDateTime now,
            Locale locale
    ) {
        if (Objects.isNull(rangeStartDate) || Objects.isNull(rangeEndDate)) {
            return List.of();
        }
        LocalDate normalizedStartDate = rangeStartDate.isAfter(rangeEndDate) ? rangeEndDate : rangeStartDate;
        LocalDate normalizedEndDate = rangeStartDate.isAfter(rangeEndDate) ? rangeStartDate : rangeEndDate;
        if (normalizedEndDate.isAfter(today)) {
            normalizedEndDate = today;
        }
        if (normalizedStartDate.isAfter(normalizedEndDate)) {
            return List.of();
        }

        List<PeriodBucket> periodBuckets = new ArrayList<>();
        LocalDate bucketDate = normalizedStartDate;
        while (!bucketDate.isAfter(normalizedEndDate)) {
            Instant startInclusive = bucketDate.atStartOfDay(zoneId).toInstant();
            Instant endExclusive = bucketDate.equals(today)
                    ? now.toInstant()
                    : bucketDate.plusDays(1).atStartOfDay(zoneId).toInstant();
            String label = bucketDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale)
                    + " "
                    + bucketDate.format(DateTimeFormatter.ofPattern("dd.MM", locale));
            periodBuckets.add(new PeriodBucket(label, startInclusive, endExclusive));
            bucketDate = bucketDate.plusDays(1);
        }
        return periodBuckets;
    }

    private List<PeriodBucket> buildWeekDayBuckets(LocalDate today, ZonedDateTime now, Locale locale) {
        LocalDate weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return IntStream.range(0, 7)
                .mapToObj(dayOffset -> {
                    LocalDate bucketDate = weekStartDate.plusDays(dayOffset);
                    Instant startInclusive = bucketDate.atStartOfDay(zoneId).toInstant();
                    Instant endExclusive = bucketDate.equals(today)
                            ? now.toInstant()
                            : bucketDate.plusDays(1).atStartOfDay(zoneId).toInstant();
                    String label = bucketDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale)
                            + " "
                            + bucketDate.getDayOfMonth();
                    return new PeriodBucket(label, startInclusive, endExclusive);
                })
                .collect(Collectors.toList());
    }

    private List<PeriodBucket> buildMonthWeekBuckets(LocalDate today, ZonedDateTime now, Locale locale) {
        YearMonth yearMonth = YearMonth.from(today);
        LocalDate monthStartDate = yearMonth.atDay(1);
        LocalDate monthEndDate = yearMonth.atEndOfMonth();
        List<PeriodBucket> periodBuckets = new ArrayList<>();

        LocalDate bucketStartDate = monthStartDate;
        int weekNumber = 1;
        while (!bucketStartDate.isAfter(monthEndDate)) {
            LocalDate weekEndDate = bucketStartDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            if (weekEndDate.isAfter(monthEndDate)) {
                weekEndDate = monthEndDate;
            }
            LocalDate exclusiveEndDate = weekEndDate.plusDays(1);
            Instant startInclusive = bucketStartDate.atStartOfDay(zoneId).toInstant();
            Instant endExclusive = exclusiveEndDate.isAfter(today)
                    ? now.toInstant()
                    : exclusiveEndDate.atStartOfDay(zoneId).toInstant();
            String label = formatWeekBucketLabel(weekNumber, bucketStartDate, weekEndDate, locale);
            periodBuckets.add(new PeriodBucket(label, startInclusive, endExclusive));
            bucketStartDate = weekEndDate.plusDays(1);
            weekNumber++;
        }
        return periodBuckets;
    }

    /**
     * Недели текущего месяца до сегодня (пн–вс, обрезанные границами месяца) — для недельных таблиц отчёта.
     */
    public List<MonthWeekRange> listCurrentMonthWeekRanges() {
        LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
        Locale locale = UserLocaleContext.getLanguage().toLocale();
        YearMonth yearMonth = YearMonth.from(today);
        LocalDate monthStartDate = yearMonth.atDay(1);
        LocalDate monthEndDate = yearMonth.atEndOfMonth();
        List<MonthWeekRange> monthWeekRanges = new ArrayList<>();

        LocalDate bucketStartDate = monthStartDate;
        int weekNumber = 1;
        while (!bucketStartDate.isAfter(monthEndDate)) {
            LocalDate weekEndDate = bucketStartDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            if (weekEndDate.isAfter(monthEndDate)) {
                weekEndDate = monthEndDate;
            }
            if (!bucketStartDate.isAfter(today)) {
                LocalDate clippedEndDate = weekEndDate.isAfter(today) ? today : weekEndDate;
                monthWeekRanges.add(new MonthWeekRange(
                        weekNumber,
                        bucketStartDate,
                        clippedEndDate,
                        formatWeekBucketLabel(weekNumber, bucketStartDate, weekEndDate, locale)
                ));
            }
            bucketStartDate = weekEndDate.plusDays(1);
            weekNumber++;
        }
        return monthWeekRanges;
    }

    public record MonthWeekRange(int weekNumber, LocalDate startDate, LocalDate endDate, String label) {
    }

    private String formatWeekBucketLabel(int weekNumber, LocalDate startDate, LocalDate endDate, Locale locale) {
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d", locale);
        return "W" + weekNumber + " (" + startDate.format(dayFormatter) + "–" + endDate.format(dayFormatter) + ")";
    }

    private List<PeriodBucket> buildYearMonthBuckets(LocalDate today, ZonedDateTime now, Locale locale) {
        int year = today.getYear();
        return IntStream.rangeClosed(1, 12)
                .mapToObj(monthNumber -> {
                    Month month = Month.of(monthNumber);
                    YearMonth yearMonth = YearMonth.of(year, month);
                    LocalDate monthStartDate = yearMonth.atDay(1);
                    LocalDate monthEndExclusiveDate = monthStartDate.plusMonths(1);
                    Instant startInclusive = monthStartDate.atStartOfDay(zoneId).toInstant();
                    Instant endExclusive;
                    if (yearMonth.getYear() == today.getYear() && yearMonth.getMonthValue() == today.getMonthValue()) {
                        endExclusive = now.toInstant();
                    } else if (monthStartDate.isAfter(today)) {
                        endExclusive = startInclusive;
                    } else {
                        endExclusive = monthEndExclusiveDate.atStartOfDay(zoneId).toInstant();
                    }
                    String label = month.getDisplayName(TextStyle.SHORT, locale);
                    return new PeriodBucket(label, startInclusive, endExclusive);
                })
                .collect(Collectors.toList());
    }

    private List<PeriodBucket> buildAllTimeYearBuckets(ZonedDateTime now) {
        int currentYear = now.getYear();
        int earliestYear = collectActiveIntervals().stream()
                .map(activityInterval -> toLocalDate(activityInterval.getStartInstant()).getYear())
                .min(Integer::compareTo)
                .orElse(currentYear);
        return IntStream.rangeClosed(earliestYear, currentYear)
                .mapToObj(year -> {
                    LocalDate yearStartDate = LocalDate.of(year, 1, 1);
                    Instant startInclusive = yearStartDate.atStartOfDay(zoneId).toInstant();
                    Instant endExclusive = year == currentYear
                            ? now.toInstant()
                            : yearStartDate.plusYears(1).atStartOfDay(zoneId).toInstant();
                    return new PeriodBucket(String.valueOf(year), startInclusive, endExclusive);
                })
                .collect(Collectors.toList());
    }

    private InstantRange resolveInstantRange(
            StatsPeriod statsPeriod,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();
        return switch (statsPeriod) {
            case DAY -> new InstantRange(
                    today.atStartOfDay(zoneId).toInstant(),
                    now.toInstant()
            );
            case WEEK -> {
                LocalDate weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new InstantRange(
                        weekStartDate.atStartOfDay(zoneId).toInstant(),
                        now.toInstant()
                );
            }
            case MONTH -> new InstantRange(
                    today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant(),
                    now.toInstant()
            );
            case YEAR -> new InstantRange(
                    LocalDate.of(today.getYear(), 1, 1).atStartOfDay(zoneId).toInstant(),
                    now.toInstant()
            );
            case ALL_TIME -> new InstantRange(Instant.EPOCH, now.toInstant());
            case CUSTOM -> {
                if (Objects.isNull(rangeStartDate) || Objects.isNull(rangeEndDate)) {
                    yield new InstantRange(now.toInstant(), now.toInstant());
                }
                LocalDate normalizedStartDate = rangeStartDate.isAfter(rangeEndDate) ? rangeEndDate : rangeStartDate;
                LocalDate normalizedEndDate = rangeStartDate.isAfter(rangeEndDate) ? rangeStartDate : rangeEndDate;
                if (normalizedEndDate.isAfter(today)) {
                    normalizedEndDate = today;
                }
                Instant endExclusive = normalizedEndDate.equals(today)
                        ? now.toInstant()
                        : normalizedEndDate.plusDays(1).atStartOfDay(zoneId).toInstant();
                yield new InstantRange(
                        normalizedStartDate.atStartOfDay(zoneId).toInstant(),
                        endExclusive
                );
            }
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
