package com.workpulsetracker.agent.stats;

import com.workpulsetracker.agent.buffer.ActivityInterval;
import com.workpulsetracker.agent.buffer.DataBuffer;
import com.workpulsetracker.agent.storage.ActivityStore;
import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.util.TrackedApplicationNameResolver;
import com.workpulsetracker.common.i18n.UserLocaleContext;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Агрегация локальной статистики по сохранённым и текущим интервалам.
 */
public final class StatisticsService {

    private final Supplier<List<ActivityInterval>> storedIntervalsSupplier;
    private final Supplier<ActivityInterval> currentIntervalSupplier;
    private final UserSettings userSettings;
    private final ZoneId zoneId;
    private final StatsCalendar calendar;

    public StatisticsService(ActivityStore activityStore, DataBuffer dataBuffer, UserSettings userSettings) {
        this(activityStore, dataBuffer, userSettings, ZoneId.systemDefault());
    }

    public StatisticsService(
            ActivityStore activityStore,
            DataBuffer dataBuffer,
            UserSettings userSettings,
            ZoneId zoneId
    ) {
        this(activityStore::getAllIntervals, dataBuffer::getCurrentInterval, userSettings, zoneId);
    }

    StatisticsService(
            Supplier<List<ActivityInterval>> storedIntervalsSupplier,
            Supplier<ActivityInterval> currentIntervalSupplier,
            UserSettings userSettings,
            ZoneId zoneId
    ) {
        this.storedIntervalsSupplier = Objects.requireNonNull(storedIntervalsSupplier);
        this.currentIntervalSupplier = Objects.requireNonNull(currentIntervalSupplier);
        this.userSettings = Objects.requireNonNull(userSettings);
        this.zoneId = Objects.requireNonNull(zoneId);
        this.calendar = new StatsCalendar(Clock.system(this.zoneId));
    }

    public StatsCalendar getCalendar() {
        return calendar;
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
        List<Slice> relevantSlices = collectClippedActiveSlices(instantRange);

        long totalActiveSeconds = relevantSlices.stream()
                .mapToLong(Slice::durationSeconds)
                .sum();

        List<DailyUsageSummary> dailyUsageSummaries = relevantSlices.stream()
                .collect(Collectors.groupingBy(
                        slice -> toLocalDate(slice.start()),
                        Collectors.summingLong(Slice::durationSeconds)
                ))
                .entrySet()
                .stream()
                .map(entry -> new DailyUsageSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DailyUsageSummary::getDate).reversed())
                .collect(Collectors.toList());

        List<ApplicationUsageSummary> applicationUsageSummaries = buildApplicationUsageSummaries(relevantSlices);

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
        List<Slice> relevantSlices = collectClippedActiveSlices(instantRange);
        List<ApplicationUsageSummary> applicationUsageSummaries = buildApplicationUsageSummaries(relevantSlices);
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

        // Бакеты отсортированы и не пересекаются, поэтому для каждого интервала проходим
        // только пересекающиеся колонки, а не всю строку (важно для MONTH/YEAR/ALL_TIME).
        for (Slice slice : relevantSlices) {
            Integer applicationIndex = applicationIndexByName.get(slice.applicationName());
            if (Objects.isNull(applicationIndex)) {
                continue;
            }
            for (int bucketIndex = 0; bucketIndex < periodBuckets.size(); bucketIndex++) {
                PeriodBucket periodBucket = periodBuckets.get(bucketIndex);
                if (!periodBucket.getStartInclusive().isBefore(slice.end())) {
                    break;
                }
                if (!periodBucket.getEndExclusive().isAfter(slice.start())) {
                    continue;
                }
                long durationSeconds = overlapSeconds(slice, periodBucket);
                if (durationSeconds <= 0L) {
                    continue;
                }
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

    private LocalDate earliestActivityDate() {
        return collectActiveIntervals().stream()
                .map(activityInterval -> toLocalDate(activityInterval.getStartInstant()))
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    // --- Календарная навигация: логика дат в StatsCalendar, сервис лишь подставляет данные активности. ---

    public String formatPeriodCaption(StatsPeriod statsPeriod, LocalDate anchorDate) {
        return calendar.formatPeriodCaption(statsPeriod, anchorDate);
    }

    public String formatWeekRangeCaption(LocalDate anchorDate) {
        return calendar.formatWeekRangeCaption(anchorDate);
    }

    public boolean weekIntersectsMonth(LocalDate weekMonday, YearMonth yearMonth) {
        return calendar.weekIntersectsMonth(weekMonday, yearMonth);
    }

    public LocalDate resolveDefaultWeekMondayForMonth(YearMonth yearMonth) {
        return calendar.resolveDefaultWeekMondayForMonth(yearMonth);
    }

    public boolean canNavigateToPreviousWeekInMonth(YearMonth yearMonth, LocalDate weekMonday) {
        return calendar.canNavigateToPreviousWeekInMonth(yearMonth, weekMonday);
    }

    public boolean canNavigateToNextWeekInMonth(YearMonth yearMonth, LocalDate weekMonday) {
        return calendar.canNavigateToNextWeekInMonth(yearMonth, weekMonday);
    }

    public LocalDate shiftWeekMondayInMonth(YearMonth yearMonth, LocalDate weekMonday, int weekOffset) {
        return calendar.shiftWeekMondayInMonth(yearMonth, weekMonday, weekOffset);
    }

    public LocalDate normalizeAnchorDate(StatsPeriod statsPeriod, LocalDate anchorDate) {
        return calendar.normalizeAnchorDate(statsPeriod, anchorDate);
    }

    public LocalDate shiftAnchorDate(StatsPeriod statsPeriod, LocalDate anchorDate, int periodOffset) {
        return calendar.shiftAnchorDate(statsPeriod, anchorDate, periodOffset);
    }

    public boolean canNavigateToNextPeriod(StatsPeriod statsPeriod, LocalDate anchorDate) {
        return calendar.canNavigateToNextPeriod(statsPeriod, anchorDate);
    }

    public boolean canNavigateToPreviousPeriod(StatsPeriod statsPeriod, LocalDate anchorDate) {
        return calendar.canNavigateToPreviousPeriod(statsPeriod, anchorDate, earliestActivityDate());
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
        return collectClippedTimelineSlices(dayRange).stream()
                .filter(this::countsAsIdleForTimelineMetrics)
                .mapToLong(Slice::durationSeconds)
                .sum();
    }

    /**
     * Время сегодня в программах с Track OFF (виняток / exception на таймлайне).
     * При выключенном показе исключений возвращает 0 — это время уже входит в Idle.
     */
    public long buildTodayExcludedSeconds() {
        if (!userSettings.isShowExceptionsOnTimeline()) {
            return 0L;
        }
        InstantRange dayRange = resolveInstantRange(StatsPeriod.DAY, LocalDate.now(zoneId), null, null);
        return collectClippedTimelineSlices(dayRange).stream()
                .filter(slice -> !slice.idle())
                .filter(slice -> !userSettings.isApplicationTracked(slice.applicationName()))
                .mapToLong(Slice::durationSeconds)
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
    private DayActivityTimeline buildActivityTimelineForDate(LocalDate dayDate) {
        LocalDate resolvedDayDate = Objects.nonNull(dayDate) ? dayDate : LocalDate.now(zoneId);
        LocalDateTime rangeStartDateTime = resolvedDayDate.atStartOfDay();
        LocalDateTime rangeEndDateTime = resolvedDayDate.plusDays(1).atStartOfDay();
        InstantRange dayRange = new InstantRange(
                rangeStartDateTime.atZone(zoneId).toInstant(),
                rangeEndDateTime.atZone(zoneId).toInstant()
        );

        List<Slice> clippedSlices = collectClippedTimelineSlices(dayRange).stream()
                .sorted(Comparator.comparing(Slice::start))
                .collect(Collectors.toList());
        if (clippedSlices.isEmpty()) {
            return DayActivityTimeline.empty(rangeStartDateTime, rangeEndDateTime);
        }

        List<DayActivityTimelineSegment> timelineSegments = new ArrayList<>();
        Slice mergeCandidate = clippedSlices.get(0);
        for (int sliceIndex = 1; sliceIndex < clippedSlices.size(); sliceIndex++) {
            Slice nextSlice = clippedSlices.get(sliceIndex);
            if (canMergeTimelineStates(mergeCandidate, nextSlice)) {
                Instant effectiveEnd = nextSlice.end().isAfter(mergeCandidate.end())
                        ? nextSlice.end()
                        : mergeCandidate.end();
                mergeCandidate = new Slice(
                        mergeCandidate.start(),
                        effectiveEnd,
                        mergeCandidate.applicationName(),
                        mergeCandidate.idle()
                );
            } else {
                timelineSegments.add(toTimelineSegment(mergeCandidate));
                mergeCandidate = nextSlice;
            }
        }
        timelineSegments.add(toTimelineSegment(mergeCandidate));
        return new DayActivityTimeline(rangeStartDateTime, rangeEndDateTime, timelineSegments);
    }

    /**
     * Таймлайны по дням недели для якоря (пн–вс).
     */
    public List<DayActivityTimelineRow> buildWeekActivityTimelines(LocalDate weekAnchorDate) {
        return calendar.listWeekDates(weekAnchorDate).stream()
                .map(dayDate -> new DayActivityTimelineRow(dayDate, buildActivityTimelineForDate(dayDate)))
                .collect(Collectors.toList());
    }

    /**
     * Ключи программ, по которым когда-либо было non-IDLE время (для вкладки Programs).
     */
    public List<String> listKnownProgramApplicationKeys() {
        return collectNonIdleIntervalsIncludingExcluded().stream()
                .map(ActivityInterval::getApplicationName)
                .map(TrackedApplicationNameResolver::resolveProgramKey)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private DayActivityTimelineSegment toTimelineSegment(Slice slice) {
        return new DayActivityTimelineSegment(
                resolveTimelineState(slice),
                LocalDateTime.ofInstant(slice.start(), zoneId),
                LocalDateTime.ofInstant(slice.end(), zoneId)
        );
    }

    private DayActivityState resolveTimelineState(Slice slice) {
        if (slice.idle()) {
            return DayActivityState.IDLE;
        }
        if (!userSettings.isApplicationTracked(slice.applicationName())) {
            return userSettings.isShowExceptionsOnTimeline()
                    ? DayActivityState.EXCLUDED
                    : DayActivityState.IDLE;
        }
        return DayActivityState.ACTIVE;
    }

    private boolean countsAsIdleForTimelineMetrics(Slice slice) {
        if (slice.idle()) {
            return true;
        }
        return !userSettings.isShowExceptionsOnTimeline()
                && !userSettings.isApplicationTracked(slice.applicationName());
    }

    private boolean canMergeTimelineStates(Slice left, Slice right) {
        if (resolveTimelineState(left) != resolveTimelineState(right)) {
            return false;
        }
        return !right.start().isAfter(left.end().plusSeconds(1L));
    }

    private List<ApplicationUsageSummary> buildApplicationUsageSummaries(List<Slice> slices) {
        return slices.stream()
                .collect(Collectors.groupingBy(
                        Slice::applicationName,
                        Collectors.summingLong(Slice::durationSeconds)
                ))
                .entrySet()
                .stream()
                .map(entry -> new ApplicationUsageSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ApplicationUsageSummary::getDurationSeconds).reversed())
                .collect(Collectors.toList());
    }

    private List<Slice> collectClippedActiveSlices(InstantRange instantRange) {
        return clipToRange(collectActiveIntervals(), instantRange);
    }

    private List<Slice> collectClippedTimelineSlices(InstantRange instantRange) {
        return clipToRange(collectTimelineIntervals(), instantRange);
    }

    private List<Slice> clipToRange(List<ActivityInterval> activityIntervals, InstantRange instantRange) {
        List<Slice> slices = new ArrayList<>();
        for (ActivityInterval activityInterval : activityIntervals) {
            Slice slice = clipToRange(activityInterval, instantRange);
            if (Objects.nonNull(slice) && slice.durationSeconds() > 0L) {
                slices.add(slice);
            }
        }
        return slices;
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
        List<ActivityInterval> activityIntervals = storedIntervalsSupplier.get().stream()
                .filter(activityInterval -> !activityInterval.isIdle())
                .collect(Collectors.toCollection(ArrayList::new));
        ActivityInterval currentActivityInterval = currentIntervalSupplier.get();
        if (Objects.nonNull(currentActivityInterval) && !currentActivityInterval.isIdle()) {
            activityIntervals.add(currentActivityInterval);
        }
        return activityIntervals;
    }

    private List<ActivityInterval> collectTimelineIntervals() {
        List<ActivityInterval> activityIntervals = new ArrayList<>(storedIntervalsSupplier.get());
        ActivityInterval currentActivityInterval = currentIntervalSupplier.get();
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

    private Slice clipToRange(ActivityInterval activityInterval, InstantRange instantRange) {
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

        return new Slice(
                clippedStartInstant,
                clippedEndInstant,
                activityInterval.getApplicationName(),
                activityInterval.isIdle()
        );
    }

    private static long overlapSeconds(Slice slice, PeriodBucket periodBucket) {
        Instant overlapStart = slice.start().isAfter(periodBucket.getStartInclusive())
                ? slice.start()
                : periodBucket.getStartInclusive();
        Instant overlapEnd = slice.end().isBefore(periodBucket.getEndExclusive())
                ? slice.end()
                : periodBucket.getEndExclusive();
        if (!overlapStart.isBefore(overlapEnd)) {
            return 0L;
        }
        return Duration.between(overlapStart, overlapEnd).getSeconds();
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(zoneId).toLocalDate();
    }

    private record InstantRange(Instant startInclusive, Instant endExclusive) {
    }

    /**
     * Кусок интервала, обрезанный по границам периода/бакета. Лёгкая замена
     * повторному созданию {@link ActivityInterval} в горячих циклах агрегации:
     * нормализация имени и вычисление идентификаторов здесь уже не нужны.
     */
    private record Slice(Instant start, Instant end, String applicationName, boolean idle) {
        long durationSeconds() {
            return Math.max(Duration.between(start, end).getSeconds(), 0L);
        }
    }
}
