package com.workpulsetracker.agent.util;

import com.workpulsetracker.agent.storage.ProgramCategoryIds;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Отображаемые названия категорий программ (дефолтные — через i18n).
 */
public final class ProgramCategoryDisplayNames {

    private ProgramCategoryDisplayNames() {
    }

    public static String resolveDisplayName(String categoryId) {
        if (StringUtils.isBlank(categoryId)) {
            return Messages.get(MessageCodes.UI_PROGRAMS_CATEGORY_WORK);
        }
        String normalizedCategoryId = ProgramCategoryIds.normalizeDefaultCategoryId(categoryId.trim());
        if (ProgramCategoryIds.WORK.equals(normalizedCategoryId)) {
            return Messages.get(MessageCodes.UI_PROGRAMS_CATEGORY_WORK);
        }
        if (ProgramCategoryIds.COMMUNICATION.equals(normalizedCategoryId)) {
            return Messages.get(MessageCodes.UI_PROGRAMS_CATEGORY_COMMUNICATION);
        }
        if (ProgramCategoryIds.TRAINING.equals(normalizedCategoryId)) {
            return Messages.get(MessageCodes.UI_PROGRAMS_CATEGORY_TRAINING);
        }
        if (ProgramCategoryIds.OTHER.equals(normalizedCategoryId)) {
            return Messages.get(MessageCodes.UI_PROGRAMS_CATEGORY_OTHER);
        }
        return Objects.requireNonNullElse(categoryId, Messages.get(MessageCodes.UI_PROGRAMS_CATEGORY_OTHER));
    }
}
