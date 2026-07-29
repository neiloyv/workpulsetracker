package com.workpulsetracker.agent.stats;

import com.workpulsetracker.agent.util.BrowserSiteTitleParser;
import com.workpulsetracker.agent.util.TrackedApplicationNameResolver;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Для браузеров склеивает вкладки/сайты ({@code msedge · youtube.com}) в одну группу.
 * На Main — с раскрытием; в Statistics/отчётах — одна строка без детей.
 */
public final class ApplicationUsageBrowserGrouper {

    private ApplicationUsageBrowserGrouper() {
    }

    public static List<ApplicationUsageGroup> group(List<ApplicationUsageSummary> applicationUsageSummaries) {
        if (Objects.isNull(applicationUsageSummaries) || applicationUsageSummaries.isEmpty()) {
            return List.of();
        }

        Map<String, List<ApplicationUsageSummary>> summariesByGroupKey = applicationUsageSummaries.stream()
                .collect(Collectors.groupingBy(
                        applicationUsageSummary -> resolveGroupKey(applicationUsageSummary.getApplicationName()),
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)
                ));

        return summariesByGroupKey.entrySet().stream()
                .map(entry -> toGroup(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ApplicationUsageGroup::getDurationSeconds).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Схлопывает строки браузера с сайтами в одну строку на браузер (без раскрытия).
     */
    public static ApplicationUsageMatrix collapseBrowserApplications(ApplicationUsageMatrix applicationUsageMatrix) {
        if (Objects.isNull(applicationUsageMatrix) || applicationUsageMatrix.getApplicationNames().isEmpty()) {
            return applicationUsageMatrix;
        }

        int bucketCount = applicationUsageMatrix.getPeriodBuckets().size();
        Map<String, List<Integer>> applicationIndexesByGroupKey = new LinkedHashMap<>();
        IntStream.range(0, applicationUsageMatrix.getApplicationNames().size()).forEach(applicationIndex -> {
            String applicationName = applicationUsageMatrix.getApplicationNames().get(applicationIndex);
            String groupKey = resolveGroupKey(applicationName);
            applicationIndexesByGroupKey
                    .computeIfAbsent(groupKey, ignoredKey -> new ArrayList<>())
                    .add(applicationIndex);
        });

        List<CollapsedApplicationRow> collapsedApplicationRows = applicationIndexesByGroupKey.entrySet().stream()
                .map(entry -> collapseRows(
                        entry.getKey(),
                        entry.getValue(),
                        applicationUsageMatrix,
                        bucketCount
                ))
                .sorted(Comparator.comparingLong(CollapsedApplicationRow::applicationTotalSeconds).reversed())
                .collect(Collectors.toList());

        int groupedApplicationCount = collapsedApplicationRows.size();
        List<String> groupedApplicationNames = collapsedApplicationRows.stream()
                .map(CollapsedApplicationRow::applicationName)
                .collect(Collectors.toList());
        long[][] groupedDurationSeconds = new long[groupedApplicationCount][bucketCount];
        long[] groupedApplicationTotalSeconds = new long[groupedApplicationCount];
        for (int groupedIndex = 0; groupedIndex < groupedApplicationCount; groupedIndex++) {
            CollapsedApplicationRow collapsedApplicationRow = collapsedApplicationRows.get(groupedIndex);
            groupedApplicationTotalSeconds[groupedIndex] = collapsedApplicationRow.applicationTotalSeconds();
            System.arraycopy(
                    collapsedApplicationRow.durationSecondsByBucket(),
                    0,
                    groupedDurationSeconds[groupedIndex],
                    0,
                    bucketCount
            );
        }

        return new ApplicationUsageMatrix(
                applicationUsageMatrix.getStatsPeriod(),
                applicationUsageMatrix.getPeriodBuckets(),
                groupedApplicationNames,
                groupedDurationSeconds,
                groupedApplicationTotalSeconds,
                applicationUsageMatrix.getTotalActiveSeconds()
        );
    }

    private static CollapsedApplicationRow collapseRows(
            String groupKey,
            List<Integer> applicationIndexes,
            ApplicationUsageMatrix applicationUsageMatrix,
            int bucketCount
    ) {
        long[] durationSecondsByBucket = new long[bucketCount];
        long applicationTotalSeconds = 0L;
        for (int applicationIndex : applicationIndexes) {
            applicationTotalSeconds += applicationUsageMatrix.getApplicationTotalSeconds(applicationIndex);
            for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
                durationSecondsByBucket[bucketIndex] +=
                        applicationUsageMatrix.getDurationSeconds(applicationIndex, bucketIndex);
            }
        }
        return new CollapsedApplicationRow(groupKey, applicationTotalSeconds, durationSecondsByBucket);
    }

    private static String resolveGroupKey(String applicationName) {
        String baseApplicationName = TrackedApplicationNameResolver.extractBaseApplicationName(applicationName);
        if (BrowserSiteTitleParser.isBrowserProcess(baseApplicationName)) {
            return baseApplicationName;
        }
        return applicationName;
    }

    private static ApplicationUsageGroup toGroup(
            String groupKey,
            List<ApplicationUsageSummary> applicationUsageSummaries
    ) {
        long totalDurationSeconds = applicationUsageSummaries.stream()
                .mapToLong(ApplicationUsageSummary::getDurationSeconds)
                .sum();

        if (!BrowserSiteTitleParser.isBrowserProcess(groupKey)) {
            return ApplicationUsageGroup.leaf(groupKey, totalDurationSeconds);
        }

        Map<String, Long> durationBySiteLabel = applicationUsageSummaries.stream()
                .filter(applicationUsageSummary -> TrackedApplicationNameResolver.hasSiteDetail(
                        applicationUsageSummary.getApplicationName()
                ))
                .collect(Collectors.groupingBy(
                        applicationUsageSummary -> TrackedApplicationNameResolver.extractSiteLabel(
                                applicationUsageSummary.getApplicationName()
                        ),
                        LinkedHashMap::new,
                        Collectors.summingLong(ApplicationUsageSummary::getDurationSeconds)
                ));

        List<ApplicationUsageSummary> siteChildren = durationBySiteLabel.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()))
                .map(entry -> new ApplicationUsageSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ApplicationUsageSummary::getDurationSeconds).reversed())
                .collect(Collectors.toList());

        if (siteChildren.isEmpty()) {
            return ApplicationUsageGroup.leaf(groupKey, totalDurationSeconds);
        }
        return new ApplicationUsageGroup(groupKey, totalDurationSeconds, siteChildren);
    }

    private record CollapsedApplicationRow(
            String applicationName,
            long applicationTotalSeconds,
            long[] durationSecondsByBucket
    ) {
    }
}
