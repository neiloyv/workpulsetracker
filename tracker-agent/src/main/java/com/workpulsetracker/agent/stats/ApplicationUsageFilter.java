package com.workpulsetracker.agent.stats;

import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Группирует программы с длительностью ниже порога в категорию Others / Інші.
 */
public final class ApplicationUsageFilter {

    public static final int DEFAULT_MINOR_USAGE_THRESHOLD_MINUTES = 5;

    private ApplicationUsageFilter() {
    }

    public static List<ApplicationUsageSummary> groupMinorApplications(
            List<ApplicationUsageSummary> applicationUsageSummaries,
            int minorUsageThresholdMinutes
    ) {
        if (Objects.isNull(applicationUsageSummaries) || applicationUsageSummaries.isEmpty()) {
            return List.of();
        }

        long minorUsageThresholdSeconds = toThresholdSeconds(minorUsageThresholdMinutes);

        List<ApplicationUsageSummary> majorApplicationUsageSummaries = applicationUsageSummaries.stream()
                .filter(applicationUsageSummary -> isMajorApplication(
                        applicationUsageSummary.getDurationSeconds(),
                        minorUsageThresholdSeconds
                ))
                .collect(Collectors.toCollection(ArrayList::new));

        long othersDurationSeconds = applicationUsageSummaries.stream()
                .filter(applicationUsageSummary -> !isMajorApplication(
                        applicationUsageSummary.getDurationSeconds(),
                        minorUsageThresholdSeconds
                ))
                .mapToLong(ApplicationUsageSummary::getDurationSeconds)
                .sum();

        if (othersDurationSeconds > 0L) {
            majorApplicationUsageSummaries.add(new ApplicationUsageSummary(
                    Messages.get(MessageCodes.UI_STATS_OTHERS),
                    othersDurationSeconds
            ));
        }
        return List.copyOf(majorApplicationUsageSummaries);
    }

    /**
     * Фильтр minor по суммарному времени группы (браузер целиком, а не каждая вкладка).
     */
    public static List<ApplicationUsageGroup> groupMinorApplicationGroups(
            List<ApplicationUsageGroup> applicationUsageGroups,
            int minorUsageThresholdMinutes
    ) {
        if (Objects.isNull(applicationUsageGroups) || applicationUsageGroups.isEmpty()) {
            return List.of();
        }

        long minorUsageThresholdSeconds = toThresholdSeconds(minorUsageThresholdMinutes);
        List<ApplicationUsageGroup> majorApplicationUsageGroups = applicationUsageGroups.stream()
                .filter(applicationUsageGroup -> isMajorApplication(
                        applicationUsageGroup.getDurationSeconds(),
                        minorUsageThresholdSeconds
                ))
                .collect(Collectors.toCollection(ArrayList::new));

        long othersDurationSeconds = applicationUsageGroups.stream()
                .filter(applicationUsageGroup -> !isMajorApplication(
                        applicationUsageGroup.getDurationSeconds(),
                        minorUsageThresholdSeconds
                ))
                .mapToLong(ApplicationUsageGroup::getDurationSeconds)
                .sum();

        if (othersDurationSeconds > 0L) {
            majorApplicationUsageGroups.add(ApplicationUsageGroup.leaf(
                    Messages.get(MessageCodes.UI_STATS_OTHERS),
                    othersDurationSeconds
            ));
        }
        return List.copyOf(majorApplicationUsageGroups);
    }

    public static ApplicationUsageMatrix groupMinorApplications(
            ApplicationUsageMatrix applicationUsageMatrix,
            int minorUsageThresholdMinutes
    ) {
        if (Objects.isNull(applicationUsageMatrix) || applicationUsageMatrix.getApplicationNames().isEmpty()) {
            return applicationUsageMatrix;
        }

        long totalActiveSeconds = applicationUsageMatrix.getTotalActiveSeconds();
        long minorUsageThresholdSeconds = toThresholdSeconds(minorUsageThresholdMinutes);

        List<Integer> majorApplicationIndexes = IntStream.range(0, applicationUsageMatrix.getApplicationNames().size())
                .filter(applicationIndex -> isMajorApplication(
                        applicationUsageMatrix.getApplicationTotalSeconds(applicationIndex),
                        minorUsageThresholdSeconds
                ))
                .boxed()
                .collect(Collectors.toList());

        List<Integer> minorApplicationIndexes = IntStream.range(0, applicationUsageMatrix.getApplicationNames().size())
                .filter(applicationIndex -> !isMajorApplication(
                        applicationUsageMatrix.getApplicationTotalSeconds(applicationIndex),
                        minorUsageThresholdSeconds
                ))
                .boxed()
                .collect(Collectors.toList());

        if (minorApplicationIndexes.isEmpty()) {
            return applicationUsageMatrix;
        }

        int bucketCount = applicationUsageMatrix.getPeriodBuckets().size();
        int groupedApplicationCount = majorApplicationIndexes.size() + 1;
        List<String> groupedApplicationNames = new ArrayList<>(groupedApplicationCount);
        long[][] groupedDurationSeconds = new long[groupedApplicationCount][bucketCount];
        long[] groupedApplicationTotalSeconds = new long[groupedApplicationCount];

        for (int groupedIndex = 0; groupedIndex < majorApplicationIndexes.size(); groupedIndex++) {
            int sourceApplicationIndex = majorApplicationIndexes.get(groupedIndex);
            groupedApplicationNames.add(applicationUsageMatrix.getApplicationNames().get(sourceApplicationIndex));
            groupedApplicationTotalSeconds[groupedIndex] =
                    applicationUsageMatrix.getApplicationTotalSeconds(sourceApplicationIndex);
            for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
                groupedDurationSeconds[groupedIndex][bucketIndex] =
                        applicationUsageMatrix.getDurationSeconds(sourceApplicationIndex, bucketIndex);
            }
        }

        int othersIndex = majorApplicationIndexes.size();
        groupedApplicationNames.add(Messages.get(MessageCodes.UI_STATS_OTHERS));
        for (int minorApplicationIndex : minorApplicationIndexes) {
            groupedApplicationTotalSeconds[othersIndex] +=
                    applicationUsageMatrix.getApplicationTotalSeconds(minorApplicationIndex);
            for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
                groupedDurationSeconds[othersIndex][bucketIndex] +=
                        applicationUsageMatrix.getDurationSeconds(minorApplicationIndex, bucketIndex);
            }
        }

        return new ApplicationUsageMatrix(
                applicationUsageMatrix.getStatsPeriod(),
                applicationUsageMatrix.getPeriodBuckets(),
                groupedApplicationNames,
                groupedDurationSeconds,
                groupedApplicationTotalSeconds,
                totalActiveSeconds
        );
    }

    private static long toThresholdSeconds(int minorUsageThresholdMinutes) {
        int safeThresholdMinutes = Math.max(minorUsageThresholdMinutes, 0);
        return safeThresholdMinutes * 60L;
    }

    private static boolean isMajorApplication(long durationSeconds, long minorUsageThresholdSeconds) {
        return durationSeconds >= minorUsageThresholdSeconds;
    }
}
