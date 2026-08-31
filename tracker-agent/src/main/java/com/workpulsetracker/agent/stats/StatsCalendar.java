package com.workpulsetracker.agent.stats;

import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import com.workpulsetracker.common.i18n.UserLocaleContext;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Календарная навигация по периодам статистики: нормализация якоря, перелистывание,
 * границы «можно ли назад/вперёд», подписи периодов. Только даты — без данных активности.
 */
public final class StatsCalendar {

    private final Clock clock;

    public StatsCalendar(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static Locale locale() {
        return UserLocaleContext.getLanguage().toLocale();
    }

    /**
     * Нормализует якорь к началу выбранного периода (пн / 1-е число / 1 янв) и не даёт уйти в будущее.
     */
    public LocalDate normalizeAnchorDate(StatsPeriod statsPeriod, LocalDate anchorDate) {
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : today();
        LocalDate clampedAnchorDate = resolvedAnchorDate.isAfter(today()) ? today() : resolvedAnchorDate;
        return switch (statsPeriod) {
            case WEEK -> mondayOf(clampedAnchorDate);
            case MONTH -> clampedAnchorDate.withDayOfMonth(1);
            case YEAR -> LocalDate.of(clampedAnchorDate.getYear(), 1, 1);
            case DAY, ALL_TIME, CUSTOM -> clampedAnchorDate;
        };
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

    public boolean canNavigateToNextPeriod(StatsPeriod statsPeriod, LocalDate anchorDate) {
        LocalDate normalizedAnchorDate = normalizeAnchorDate(statsPeriod, anchorDate);
        LocalDate nextAnchorDate = shiftAnchorDate(statsPeriod, normalizedAnchorDate, 1);
        return !normalizeAnchorDate(statsPeriod, nextAnchorDate).isAfter(normalizeAnchorDate(statsPeriod, today()));
    }

    /**
     * Можно ли листать в прошлое, не уходя раньше первой локальной активности.
     *
     * @param earliestActivityDate дата самого раннего интервала, либо {@code null} если активности нет
     */
    public boolean canNavigateToPreviousPeriod(
            StatsPeriod statsPeriod,
            LocalDate anchorDate,
            LocalDate earliestActivityDate
    ) {
        LocalDate normalizedAnchorDate = normalizeAnchorDate(statsPeriod, anchorDate);
        if (Objects.isNull(earliestActivityDate)) {
            return false;
        }
        if (statsPeriod == StatsPeriod.YEAR) {
            return normalizedAnchorDate.getYear() > earliestActivityDate.getYear();
        }
        LocalDate previousAnchorDate = shiftAnchorDate(statsPeriod, normalizedAnchorDate, -1);
        return !previousAnchorDate.isBefore(normalizeAnchorDate(statsPeriod, earliestActivityDate));
    }

    /**
     * Семь дат недели (пн–вс) для якоря недели.
     */
    public List<LocalDate> listWeekDates(LocalDate weekAnchorDate) {
        LocalDate weekMonday = normalizeAnchorDate(StatsPeriod.WEEK, weekAnchorDate);
        return IntStream.range(0, 7)
                .mapToObj(weekMonday::plusDays)
                .toList();
    }

    // --- Навигация по неделям внутри выбранного месяца (вкладка Statistics, режим MONTH) ---

    public LocalDate firstWeekMondayOfMonth(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        return mondayOf(yearMonth.atDay(1));
    }

    public boolean weekIntersectsMonth(LocalDate weekMonday, YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        LocalDate monday = mondayOf(Objects.nonNull(weekMonday) ? weekMonday : today());
        LocalDate sunday = monday.plusDays(6);
        return !sunday.isBefore(yearMonth.atDay(1)) && !monday.isAfter(yearMonth.atEndOfMonth());
    }

    /**
     * Стартовая неделя месяца: текущая (если это текущий месяц), иначе первая неделя месяца.
     */
    public LocalDate resolveDefaultWeekMondayForMonth(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        YearMonth currentYearMonth = YearMonth.from(today());
        if (yearMonth.equals(currentYearMonth)) {
            return mondayOf(today());
        }
        if (yearMonth.isAfter(currentYearMonth)) {
            return firstWeekMondayOfMonth(currentYearMonth);
        }
        return firstWeekMondayOfMonth(yearMonth);
    }

    public boolean canNavigateToPreviousWeekInMonth(YearMonth yearMonth, LocalDate weekMonday) {
        LocalDate currentWeekMonday = resolveWeekMondayOrDefault(yearMonth, weekMonday);
        return weekIntersectsMonth(currentWeekMonday.minusWeeks(1), yearMonth);
    }

    public boolean canNavigateToNextWeekInMonth(YearMonth yearMonth, LocalDate weekMonday) {
        LocalDate currentWeekMonday = resolveWeekMondayOrDefault(yearMonth, weekMonday);
        LocalDate nextWeekMonday = currentWeekMonday.plusWeeks(1);
        return weekIntersectsMonth(nextWeekMonday, yearMonth) && !nextWeekMonday.isAfter(mondayOf(today()));
    }

    public LocalDate shiftWeekMondayInMonth(YearMonth yearMonth, LocalDate weekMonday, int weekOffset) {
        LocalDate currentWeekMonday = resolveWeekMondayOrDefault(yearMonth, weekMonday);
        LocalDate shiftedWeekMonday = currentWeekMonday.plusWeeks(weekOffset);
        if (!weekIntersectsMonth(shiftedWeekMonday, yearMonth) || shiftedWeekMonday.isAfter(mondayOf(today()))) {
            return currentWeekMonday;
        }
        return shiftedWeekMonday;
    }

    private LocalDate resolveWeekMondayOrDefault(YearMonth yearMonth, LocalDate weekMonday) {
        return Objects.nonNull(weekMonday) ? mondayOf(weekMonday) : resolveDefaultWeekMondayForMonth(yearMonth);
    }

    // --- Подписи периодов для шапки UI ---

    public String formatPeriodCaption(StatsPeriod statsPeriod, LocalDate anchorDate) {
        LocalDate resolvedAnchorDate = Objects.nonNull(anchorDate) ? anchorDate : today();
        return switch (statsPeriod) {
            case WEEK -> formatWeekRangeCaption(resolvedAnchorDate);
            case MONTH -> formatMonthCaption(YearMonth.from(resolvedAnchorDate));
            case YEAR -> String.valueOf(resolvedAnchorDate.getYear());
            case DAY -> resolvedAnchorDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale()));
            case ALL_TIME -> Messages.get(MessageCodes.UI_STATS_PERIOD_ALL);
            case CUSTOM -> resolvedAnchorDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale()));
        };
    }

    public String formatMonthCaption(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth);
        return yearMonth.getMonth().getDisplayName(TextStyle.FULL, locale()) + " " + yearMonth.getYear();
    }

    public String formatWeekRangeCaption(LocalDate anchorDate) {
        LocalDate weekStartDate = mondayOf(Objects.nonNull(anchorDate) ? anchorDate : today());
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        if (weekStartDate.getYear() == weekEndDate.getYear()) {
            DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", locale());
            return weekStartDate.format(dayMonthFormatter)
                    + " – " + weekEndDate.format(dayMonthFormatter)
                    + " " + weekEndDate.getYear();
        }
        DateTimeFormatter dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale());
        return weekStartDate.format(dayMonthYearFormatter) + " – " + weekEndDate.format(dayMonthYearFormatter);
    }
}
