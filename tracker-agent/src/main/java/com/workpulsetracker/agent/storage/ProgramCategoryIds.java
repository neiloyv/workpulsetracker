package com.workpulsetracker.agent.storage;

import java.util.List;

/**
 * Стабильные идентификаторы категорий программ (дефолтные нельзя удалять).
 */
public final class ProgramCategoryIds {

    public static final String WORK = "Work";
    public static final String COMMUNICATION = "Communication";
    public static final String TRAINING = "Training";
    public static final String OTHER = "Other";

    public static final List<String> DEFAULT_CATEGORY_IDS = List.of(
            WORK,
            COMMUNICATION,
            TRAINING,
            OTHER
    );

    private ProgramCategoryIds() {
    }

    public static boolean isDefaultCategoryId(String categoryId) {
        return DEFAULT_CATEGORY_IDS.stream()
                .anyMatch(defaultCategoryId -> defaultCategoryId.equalsIgnoreCase(categoryId));
    }

    public static String normalizeDefaultCategoryId(String categoryId) {
        return DEFAULT_CATEGORY_IDS.stream()
                .filter(defaultCategoryId -> defaultCategoryId.equalsIgnoreCase(categoryId))
                .findFirst()
                .orElse(categoryId);
    }
}
