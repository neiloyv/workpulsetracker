package com.workpulsetracker.agent.stats;

import com.workpulsetracker.agent.storage.UserSettings;
import com.workpulsetracker.agent.util.ProgramCategoryDisplayNames;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Агрегирует матрицу использования приложений по категориям программ.
 */
public final class ApplicationUsageCategoryAggregator {

    private ApplicationUsageCategoryAggregator() {
    }

    public static ApplicationUsageMatrix aggregateByCategories(
            ApplicationUsageMatrix applicationUsageMatrix,
            UserSettings userSettings
    ) {
        Objects.requireNonNull(applicationUsageMatrix);
        Objects.requireNonNull(userSettings);
        List<String> applicationNames = applicationUsageMatrix.getApplicationNames();
        if (applicationNames.isEmpty()) {
            return applicationUsageMatrix;
        }

        int bucketCount = applicationUsageMatrix.getPeriodBuckets().size();
        Map<String, long[]> durationSecondsByCategoryAndBucket = new LinkedHashMap<>();
        Map<String, Long> categoryTotalSecondsById = new LinkedHashMap<>();

        IntStream.range(0, applicationNames.size()).forEach(applicationIndex -> {
            String categoryId = userSettings.getApplicationCategoryId(applicationNames.get(applicationIndex));
            long[] bucketDurations = durationSecondsByCategoryAndBucket.computeIfAbsent(
                    categoryId,
                    unusedCategoryId -> new long[bucketCount]
            );
            IntStream.range(0, bucketCount).forEach(bucketIndex ->
                    bucketDurations[bucketIndex] += applicationUsageMatrix.getDurationSeconds(
                            applicationIndex,
                            bucketIndex
                    )
            );
            categoryTotalSecondsById.merge(
                    categoryId,
                    applicationUsageMatrix.getApplicationTotalSeconds(applicationIndex),
                    Long::sum
            );
        });

        List<String> sortedCategoryIds = categoryTotalSecondsById.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> categoryDisplayNames = sortedCategoryIds.stream()
                .map(ProgramCategoryDisplayNames::resolveDisplayName)
                .collect(Collectors.toList());

        long[][] durationSecondsByCategoryAndBucketMatrix = new long[sortedCategoryIds.size()][bucketCount];
        long[] categoryTotalSeconds = new long[sortedCategoryIds.size()];
        IntStream.range(0, sortedCategoryIds.size()).forEach(categoryIndex -> {
            String categoryId = sortedCategoryIds.get(categoryIndex);
            durationSecondsByCategoryAndBucketMatrix[categoryIndex] =
                    durationSecondsByCategoryAndBucket.get(categoryId);
            categoryTotalSeconds[categoryIndex] = categoryTotalSecondsById.get(categoryId);
        });

        return new ApplicationUsageMatrix(
                applicationUsageMatrix.getStatsPeriod(),
                applicationUsageMatrix.getPeriodBuckets(),
                categoryDisplayNames,
                durationSecondsByCategoryAndBucketMatrix,
                categoryTotalSeconds,
                applicationUsageMatrix.getTotalActiveSeconds()
        );
    }
}
