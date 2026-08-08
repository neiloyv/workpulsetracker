package com.workpulsetracker.agent.stats;

import com.workpulsetracker.agent.buffer.ActivityInterval;
import com.workpulsetracker.agent.buffer.DataBuffer;
import com.workpulsetracker.agent.storage.ActivityStore;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.util.ProgramApplicationKeyResolver;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
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
    private final UserSettings userSettings;
    private final ZoneId zoneId;

    public StatisticsService(ActivityStore activityStore, DataBuffer dataBuffer, UserSettings userSettings) {
        this(activityStore, dataBuffer, userSettings, ZoneId.systemDefault());
    }

    public StatisticsService(
            ActivityStore activityStore,
            DataBuffer dataBuffer,
            UserSettings userSettings,
            ZoneId zoneId
    ) {
        this.activityStore = activityStore;
        this.dataBuffer = dataBuffer;
        this.userSettings = Objects.requireNonNull(userSettings);
        this.zoneId = zoneId;
    }

    public StatisticsSnapshot buildSnapshot(StatsPeriod statsPeriod) {
        return buildSnapshotForAnchor(statsPeriod, LocalDate.now(zoneId));
    }

    public StatisticsSnapshot buildSnapshot(
            StatsPeriod statsPeriod,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
        if (statsPeriod == StatsPeriod.CUSTOM) {
            InstantRange instantRange = resolveInstantRange(statsPeriod, null, rangeStartDate, rangeEndDate);
            return buildSnapshotFromRange(statsPeriod, instantRange);
        }
        return buildSnapshotForAnchor(statsPeriod, LocalDate.now(zoneId));
    }

    /**
     * Снимок за календарный период, содержащий {@code anchorDate}.
     */
    public StatisticsSnapshot buildSnapshotForAnchor(StatsPeriod statsPeriod, LocalDate anchorDate) {
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : LocalDate.now(zoneId);
        InstantRange instantRange = resolveInstantRange(statsPeriod, resolvedAnchorDate, null, null);
        return buildSnapshotFromRange(statsPeriod, instantRange);
    }

    private StatisticsSnapshot buildSnapshotFromRange(StatsPeriod statsPeriod, InstantRange instantRange) {
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
     * Матрица: приложения × колонки периода (дни / месяцы / годы).
     */
    public ApplicationUsageMatrix buildApplicationUsageMatrix(StatsPeriod statsPeriod) {
        return buildApplicationUsageMatrixForAnchor(statsPeriod, LocalDate.now(zoneId));
    }

    public ApplicationUsageMatrix buildApplicationUsageMatrix(
            StatsPeriod statsPeriod,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
        if (statsPeriod == StatsPeriod.CUSTOM) {
            return buildApplicationUsageMatrixFromBuckets(
                    statsPeriod,
                    buildPeriodBuckets(statsPeriod, null, rangeStartDate, rangeEndDate)
            );
        }
        return buildApplicationUsageMatrixForAnchor(statsPeriod, LocalDate.now(zoneId));
    }

    /**
     * Матрица за календарный период, содержащий {@code anchorDate}.
     */
    public ApplicationUsageMatrix buildApplicationUsageMatrixForAnchor(
            StatsPeriod statsPeriod,
            LocalDate anchorDate
    ) {
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : LocalDate.now(zoneId);
        return buildApplicationUsageMatrixFromBuckets(
                statsPeriod,
                buildPeriodBuckets(statsPeriod, resolvedAnchorDate, null, null)
        );
    }

    private ApplicationUsageMatrix buildApplicationUsageMatrixFromBuckets(
            StatsPeriod statsPeriod,
            List<PeriodBucket> periodBuckets
    ) {
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
     * Годы, по которым есть локальная активность (включая текущий).
     */
    public List<Integer> listAvailableYears() {
        int currentYear = LocalDate.now(zoneId).getYear();
        int earliestYear = collectActiveIntervals().stream()
                .map(activityInterval -> toLocalDate(activityInterval.getStartInstant()).getYear())
                .min(Integer::compareTo)
                .orElse(currentYear);
        return IntStream.rangeClosed(earliestYear, currentYear)
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * Человекочитаемая подпись выбранного периода для шапки UI.
     */
    public String formatPeriodCaption(StatsPeriod statsPeriod, LocalDate anchorDate) {
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : LocalDate.now(zoneId);
        Locale locale = UserLocaleContext.getLanguage().toLocale();
        return switch (statsPeriod) {
            case WEEK -> formatWeekRangeCaption(resolvedAnchorDate);
            case MONTH -> formatMonthCaption(YearMonth.from(resolvedAnchorDate));
            case YEAR -> String.valueOf(resolvedAnchorDate.getYear());
            case DAY -> resolvedAnchorDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale));
            case ALL_TIME -> Messages.get(MessageCodes.UI_STATS_PERIOD_ALL);
            case CUSTOM -> resolvedAnchorDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale));
        };
    }

    public String formatMonthCaption(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        Locale locale = UserLocaleContext.getLanguage().toLocale();
        return yearMonth.getMonth().getDisplayName(TextStyle.FULL, locale) + " " + yearMonth.getYear();
    }

    public String formatWeekRangeCaption(LocalDate anchorDate) {
        LocalDate weekStartDate = Objects.nonNull(anchorDate)
                ? anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : LocalDate.now(zoneId).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        Locale locale = UserLocaleContext.getLanguage().toLocale();
        DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", locale);
        if (weekStartDate.getYear() == weekEndDate.getYear()) {
            return weekStartDate.format(dayMonthFormatter)
                    + " – "
                    + weekEndDate.format(dayMonthFormatter)
                    + " "
                    + weekEndDate.getYear();
        }
        DateTimeFormatter dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale);
        return weekStartDate.format(dayMonthYearFormatter)
                + " – "
                + weekEndDate.format(dayMonthYearFormatter);
    }

    /**
     * Понедельник первой календарной недели, пересекающей месяц.
     */
    public LocalDate firstWeekMondayOfMonth(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        return yearMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Понедельник последней календарной недели, пересекающей месяц.
     */
    public LocalDate lastWeekMondayOfMonth(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        return yearMonth.atEndOfMonth().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public boolean weekIntersectsMonth(LocalDate weekMonday, YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        LocalDate monday = Objects.nonNull(weekMonday)
                ? weekMonday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : LocalDate.now(zoneId).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        LocalDate monthStartDate = yearMonth.atDay(1);
        LocalDate monthEndDate = yearMonth.atEndOfMonth();
        return !sunday.isBefore(monthStartDate) && !monday.isAfter(monthEndDate);
    }

    /**
     * Стартовая неделя месяца: текущая (если этот месяц), иначе первая неделя месяца.
     */
    public LocalDate resolveDefaultWeekMondayForMonth(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        LocalDate today = LocalDate.now(zoneId);
        YearMonth currentYearMonth = YearMonth.from(today);
        if (yearMonth.equals(currentYearMonth)) {
            return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if (yearMonth.isAfter(currentYearMonth)) {
            return firstWeekMondayOfMonth(currentYearMonth);
        }
        return firstWeekMondayOfMonth(yearMonth);
    }

    public boolean canNavigateToPreviousWeekInMonth(YearMonth yearMonth, LocalDate weekMonday) {
        LocalDate currentWeekMonday = Objects.nonNull(weekMonday)
                ? weekMonday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : resolveDefaultWeekMondayForMonth(yearMonth);
        LocalDate previousWeekMonday = currentWeekMonday.minusWeeks(1);
        return weekIntersectsMonth(previousWeekMonday, yearMonth);
    }

    public boolean canNavigateToNextWeekInMonth(YearMonth yearMonth, LocalDate weekMonday) {
        LocalDate today = LocalDate.now(zoneId);
        LocalDate currentWeekMonday = Objects.nonNull(weekMonday)
                ? weekMonday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : resolveDefaultWeekMondayForMonth(yearMonth);
        LocalDate nextWeekMonday = currentWeekMonday.plusWeeks(1);
        if (!weekIntersectsMonth(nextWeekMonday, yearMonth)) {
            return false;
        }
        LocalDate todayWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return !nextWeekMonday.isAfter(todayWeekMonday);
    }

    public LocalDate shiftWeekMondayInMonth(YearMonth yearMonth, LocalDate weekMonday, int weekOffset) {
        LocalDate currentWeekMonday = Objects.nonNull(weekMonday)
                ? weekMonday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : resolveDefaultWeekMondayForMonth(yearMonth);
        LocalDate shiftedWeekMonday = currentWeekMonday.plusWeeks(weekOffset);
        if (!weekIntersectsMonth(shiftedWeekMonday, yearMonth)) {
            return currentWeekMonday;
        }
        LocalDate todayWeekMonday = LocalDate.now(zoneId).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (shiftedWeekMonday.isAfter(todayWeekMonday)) {
            return currentWeekMonday;
        }
        return shiftedWeekMonday;
    }

    /**
     * Нормализует якорь к началу выбранного периода (пн / 1-е число / 1 янв).
     */
    public LocalDate normalizeAnchorDate(StatsPeriod statsPeriod, LocalDate anchorDate) {
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : LocalDate.now(zoneId);
        LocalDate today = LocalDate.now(zoneId);
        LocalDate clampedAnchorDate = resolvedAnchorDate.isAfter(today) ? today : resolvedAnchorDate;
        return switch (statsPeriod) {
            case WEEK -> clampedAnchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> clampedAnchorDate.withDayOfMonth(1);
            case YEAR -> LocalDate.of(clampedAnchorDate.getYear(), 1, 1);
            case DAY, ALL_TIME, CUSTOM -> clampedAnchorDate;
        };
    }

    public boolean canNavigateToPreviousPeriod(StatsPeriod statsPeriod, LocalDate anchorDate) {
        LocalDate normalizedAnchorDate = normalizeAnchorDate(statsPeriod, anchorDate);
        if (statsPeriod == StatsPeriod.YEAR) {
            List<Integer> availableYears = listAvailableYears();
            int earliestYear = availableYears.isEmpty()
                    ? LocalDate.now(zoneId).getYear()
                    : availableYears.get(0);
            return normalizedAnchorDate.getYear() > earliestYear;
        }
        LocalDate previousAnchorDate = shiftAnchorDate(statsPeriod, normalizedAnchorDate, -1);
        // Не уходим раньше первой локальной активности (если она есть).
        LocalDate earliestActivityDate = collectActiveIntervals().stream()
                .map(activityInterval -> toLocalDate(activityInterval.getStartInstant()))
                .min(LocalDate::compareTo)
                .orElse(null);
        if (Objects.isNull(earliestActivityDate)) {
            return false;
        }
        LocalDate earliestPeriodAnchorDate = normalizeAnchorDate(statsPeriod, earliestActivityDate);
        return !previousAnchorDate.isBefore(earliestPeriodAnchorDate);
    }

    public boolean canNavigateToNextPeriod(StatsPeriod statsPeriod, LocalDate anchorDate) {
        LocalDate normalizedAnchorDate = normalizeAnchorDate(statsPeriod, anchorDate);
        LocalDate nextAnchorDate = shiftAnchorDate(statsPeriod, normalizedAnchorDate, 1);
        LocalDate today = LocalDate.now(zoneId);
        return !normalizeAnchorDate(statsPeriod, nextAnchorDate).isAfter(normalizeAnchorDate(statsPeriod, today));
    }

    public LocalDate shiftAnchorDate(StatsPeriod statsPeriod, LocalDate anchorDate, int periodOffset) {
        LocalDate normalizedAnchorDate = normalizeAnchorDate(statsPeriod, anchorDate);
        return switch (statsPeriod) {
            case WEEK -> normalizedAnchorDate.plusWeeks(periodOffset);
            case MONTH -> normalizedAnchorDate.plusMonths(periodOffset);
            case YEAR -> normalizedAnchorDate.plusYears(periodOffset);
            case DAY -> normalizedAnchorDate.plusDays(periodOffset);
            case ALL_TIME, CUSTOM -> normalizedAnchorDate;
        };
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

    public long buildTodayIdleSeconds() {
        InstantRange dayRange = resolveInstantRange(StatsPeriod.DAY, LocalDate.now(zoneId), null, null);
        return collectClippedTimelineIntervals(dayRange).stream()
                .filter(ActivityInterval::isIdle)
                .mapToLong(ActivityInterval::getDurationSeconds)
                .sum();
    }

    /**
     * Таймлайн состояний за сегодня (00:00–24:00): ACTIVE / IDLE / EXCLUDED; пробелы = PC Off.
     */
    public DayActivityTimeline buildTodayActivityTimeline() {
        return buildActivityTimelineForDate(LocalDate.now(zoneId));
    }

    /**
     * Таймлайн состояний за указанный день (00:00–24:00): ACTIVE / IDLE / EXCLUDED; пробелы = PC Off.
     * Соседние сегменты с одинаковым состоянием сливаются, чтобы не было «зебры» при смене приложений.
     */
    public DayActivityTimeline buildActivityTimelineForDate(LocalDate dayDate) {
        LocalDate resolvedDayDate = Objects.nonNull(dayDate) ? dayDate : LocalDate.now(zoneId);
        LocalDateTime rangeStartDateTime = resolvedDayDate.atStartOfDay();
        LocalDateTime rangeEndDateTime = resolvedDayDate.plusDays(1).atStartOfDay();
        InstantRange dayRange = new InstantRange(
                rangeStartDateTime.atZone(zoneId).toInstant(),
                rangeEndDateTime.atZone(zoneId).toInstant()
        );

        List<ActivityInterval> clippedIntervals = collectClippedTimelineIntervals(dayRange).stream()
                .sorted(Comparator.comparing(ActivityInterval::getStartInstant))
                .collect(Collectors.toList());
        if (clippedIntervals.isEmpty()) {
            return DayActivityTimeline.empty(rangeStartDateTime, rangeEndDateTime);
        }

        List<DayActivityTimelineSegment> timelineSegments = new ArrayList<>();
        ActivityInterval mergeCandidate = clippedIntervals.get(0);
        for (int intervalIndex = 1; intervalIndex < clippedIntervals.size(); intervalIndex++) {
            ActivityInterval nextInterval = clippedIntervals.get(intervalIndex);
            if (canMergeTimelineStateIntervals(mergeCandidate, nextInterval)) {
                Instant mergedEndInstant = Objects.nonNull(nextInterval.getEndInstant())
                        ? nextInterval.getEndInstant()
                        : Instant.now();
                Instant candidateEndInstant = Objects.nonNull(mergeCandidate.getEndInstant())
                        ? mergeCandidate.getEndInstant()
                        : Instant.now();
                Instant effectiveEndInstant = mergedEndInstant.isAfter(candidateEndInstant)
                        ? mergedEndInstant
                        : candidateEndInstant;
                mergeCandidate = new ActivityInterval(
                        mergeCandidate.getStartInstant(),
                        effectiveEndInstant,
                        mergeCandidate.getApplicationName(),
                        mergeCandidate.getWindowTitle(),
                        mergeCandidate.isIdle()
                );
            } else {
                timelineSegments.add(toTimelineSegment(mergeCandidate));
                mergeCandidate = nextInterval;
            }
        }
        timelineSegments.add(toTimelineSegment(mergeCandidate));
        return new DayActivityTimeline(rangeStartDateTime, rangeEndDateTime, timelineSegments);
    }

    /**
     * Семь дат недели (пн–вс) для якоря недели.
     */
    public List<LocalDate> listWeekDates(LocalDate weekAnchorDate) {
        LocalDate weekMonday = normalizeAnchorDate(StatsPeriod.WEEK, weekAnchorDate);
        return IntStream.range(0, 7)
                .mapToObj(weekMonday::plusDays)
                .collect(Collectors.toList());
    }

    /**
     * Таймлайны по дням недели для якоря (пн–вс).
     */
    public List<DayActivityTimelineRow> buildWeekActivityTimelines(LocalDate weekAnchorDate) {
        return listWeekDates(weekAnchorDate).stream()
                .map(dayDate -> new DayActivityTimelineRow(dayDate, buildActivityTimelineForDate(dayDate)))
                .collect(Collectors.toList());
    }

    /**
     * Ключи программ, по которым когда-либо было non-IDLE время (для вкладки Programs).
     */
    public List<String> listKnownProgramApplicationKeys() {
        return collectNonIdleIntervalsIncludingExcluded().stream()
                .map(ActivityInterval::getApplicationName)
                .map(ProgramApplicationKeyResolver::resolveProgramKey)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private DayActivityTimelineSegment toTimelineSegment(ActivityInterval activityInterval) {
        Instant endInstant = Objects.nonNull(activityInterval.getEndInstant())
                ? activityInterval.getEndInstant()
                : Instant.now();
        return new DayActivityTimelineSegment(
                resolveTimelineState(activityInterval),
                LocalDateTime.ofInstant(activityInterval.getStartInstant(), zoneId),
                LocalDateTime.ofInstant(endInstant, zoneId)
        );
    }

    private DayActivityState resolveTimelineState(ActivityInterval activityInterval) {
        if (activityInterval.isIdle()) {
            return DayActivityState.IDLE;
        }
        if (!userSettings.isApplicationTracked(activityInterval.getApplicationName())) {
            return DayActivityState.EXCLUDED;
        }
        return DayActivityState.ACTIVE;
    }

    private boolean canMergeTimelineStateIntervals(
            ActivityInterval leftInterval,
            ActivityInterval rightInterval
    ) {
        if (resolveTimelineState(leftInterval) != resolveTimelineState(rightInterval)) {
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

    private List<ActivityInterval> collectClippedTimelineIntervals(InstantRange instantRange) {
        return collectTimelineIntervals().stream()
                .map(activityInterval -> clipToRange(activityInterval, instantRange))
                .filter(Objects::nonNull)
                .filter(activityInterval -> activityInterval.getDurationSeconds() > 0)
                .collect(Collectors.toList());
    }

    /**
     * ACTIVE-интервалы для work time / usage: non-IDLE и Track ON.
     */
    private List<ActivityInterval> collectActiveIntervals() {
        return collectNonIdleIntervalsIncludingExcluded().stream()
                .filter(activityInterval -> userSettings.isApplicationTracked(activityInterval.getApplicationName()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<ActivityInterval> collectNonIdleIntervalsIncludingExcluded() {
        List<ActivityInterval> activityIntervals = activityStore.getAllIntervals().stream()
                .filter(activityInterval -> !activityInterval.isIdle())
                .collect(Collectors.toCollection(ArrayList::new));
        ActivityInterval currentActivityInterval = dataBuffer.getCurrentInterval();
        if (Objects.nonNull(currentActivityInterval) && !currentActivityInterval.isIdle()) {
            activityIntervals.add(currentActivityInterval);
        }
        return activityIntervals;
    }

    private List<ActivityInterval> collectTimelineIntervals() {
        List<ActivityInterval> activityIntervals = new ArrayList<>(activityStore.getAllIntervals());
        ActivityInterval currentActivityInterval = dataBuffer.getCurrentInterval();
        if (Objects.nonNull(currentActivityInterval)) {
            activityIntervals.add(currentActivityInterval);
        }
        return activityIntervals;
    }

    private List<PeriodBucket> buildPeriodBuckets(
            StatsPeriod statsPeriod,
            LocalDate anchorDate,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();
        Locale locale = UserLocaleContext.getLanguage().toLocale();
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : today;

        return switch (statsPeriod) {
            case DAY -> {
                LocalDate dayDate = resolvedAnchorDate.isAfter(today) ? today : resolvedAnchorDate;
                Instant startInclusive = dayDate.atStartOfDay(zoneId).toInstant();
                Instant endExclusive = dayDate.equals(today)
                        ? now.toInstant()
                        : dayDate.plusDays(1).atStartOfDay(zoneId).toInstant();
                yield List.of(new PeriodBucket(
                        dayDate.format(DateTimeFormatter.ofPattern("dd.MM", locale)),
                        startInclusive,
                        endExclusive
                ));
            }
            case WEEK -> buildWeekDayBuckets(resolvedAnchorDate, today, now, locale);
            case MONTH -> buildMonthDayBuckets(resolvedAnchorDate, today, now, locale);
            case YEAR -> buildYearMonthBuckets(resolvedAnchorDate, today, now, locale);
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

    private List<PeriodBucket> buildWeekDayBuckets(
            LocalDate anchorDate,
            LocalDate today,
            ZonedDateTime now,
            Locale locale
    ) {
        LocalDate weekStartDate = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return IntStream.range(0, 7)
                .mapToObj(dayOffset -> {
                    LocalDate bucketDate = weekStartDate.plusDays(dayOffset);
                    Instant startInclusive = bucketDate.atStartOfDay(zoneId).toInstant();
                    Instant endExclusive;
                    if (bucketDate.isAfter(today)) {
                        endExclusive = startInclusive;
                    } else if (bucketDate.equals(today)) {
                        endExclusive = now.toInstant();
                    } else {
                        endExclusive = bucketDate.plusDays(1).atStartOfDay(zoneId).toInstant();
                    }
                    String label = bucketDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale)
                            + " "
                            + bucketDate.getDayOfMonth();
                    return new PeriodBucket(label, startInclusive, endExclusive);
                })
                .collect(Collectors.toList());
    }

    private List<PeriodBucket> buildMonthDayBuckets(
            LocalDate anchorDate,
            LocalDate today,
            ZonedDateTime now,
            Locale locale
    ) {
        YearMonth yearMonth = YearMonth.from(anchorDate);
        int daysInMonth = yearMonth.lengthOfMonth();
        return IntStream.rangeClosed(1, daysInMonth)
                .mapToObj(dayOfMonth -> {
                    LocalDate bucketDate = yearMonth.atDay(dayOfMonth);
                    Instant startInclusive = bucketDate.atStartOfDay(zoneId).toInstant();
                    Instant endExclusive;
                    if (bucketDate.isAfter(today)) {
                        endExclusive = startInclusive;
                    } else if (bucketDate.equals(today)) {
                        endExclusive = now.toInstant();
                    } else {
                        endExclusive = bucketDate.plusDays(1).atStartOfDay(zoneId).toInstant();
                    }
                    String label = String.valueOf(dayOfMonth);
                    return new PeriodBucket(label, startInclusive, endExclusive);
                })
                .collect(Collectors.toList());
    }

    /**
     * Недели текущего месяца до сегодня (пн–вс, обрезанные границами месяца) — для совместимости отчётов.
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

    private List<PeriodBucket> buildYearMonthBuckets(
            LocalDate anchorDate,
            LocalDate today,
            ZonedDateTime now,
            Locale locale
    ) {
        int year = anchorDate.getYear();
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
            LocalDate anchorDate,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : today;
        return switch (statsPeriod) {
            case DAY -> {
                LocalDate dayDate = resolvedAnchorDate.isAfter(today) ? today : resolvedAnchorDate;
                Instant endExclusive = dayDate.equals(today)
                        ? now.toInstant()
                        : dayDate.plusDays(1).atStartOfDay(zoneId).toInstant();
                yield new InstantRange(dayDate.atStartOfDay(zoneId).toInstant(), endExclusive);
            }
            case WEEK -> {
                LocalDate weekStartDate = resolvedAnchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEndDate = weekStartDate.plusDays(6);
                Instant endExclusive;
                if (weekEndDate.isBefore(today)) {
                    endExclusive = weekStartDate.plusWeeks(1).atStartOfDay(zoneId).toInstant();
                } else {
                    endExclusive = now.toInstant();
                }
                yield new InstantRange(
                        weekStartDate.atStartOfDay(zoneId).toInstant(),
                        endExclusive
                );
            }
            case MONTH -> {
                YearMonth yearMonth = YearMonth.from(resolvedAnchorDate);
                LocalDate monthStartDate = yearMonth.atDay(1);
                Instant endExclusive;
                if (yearMonth.equals(YearMonth.from(today))) {
                    endExclusive = now.toInstant();
                } else if (monthStartDate.isAfter(today)) {
                    endExclusive = monthStartDate.atStartOfDay(zoneId).toInstant();
                } else {
                    endExclusive = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();
                }
                yield new InstantRange(
                        monthStartDate.atStartOfDay(zoneId).toInstant(),
                        endExclusive
                );
            }
            case YEAR -> {
                int year = resolvedAnchorDate.getYear();
                LocalDate yearStartDate = LocalDate.of(year, 1, 1);
                Instant endExclusive;
                if (year == today.getYear()) {
                    endExclusive = now.toInstant();
                } else if (year > today.getYear()) {
                    endExclusive = yearStartDate.atStartOfDay(zoneId).toInstant();
                } else {
                    endExclusive = yearStartDate.plusYears(1).atStartOfDay(zoneId).toInstant();
                }
                yield new InstantRange(
                        yearStartDate.atStartOfDay(zoneId).toInstant(),
                        endExclusive
                );
            }
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
