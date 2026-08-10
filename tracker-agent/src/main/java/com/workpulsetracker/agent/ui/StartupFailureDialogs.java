package com.workpulsetracker.agent.ui;

import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Objects;

/**
 * Диалоги ошибок запуска агента.
 */
public final class StartupFailureDialogs {

    private StartupFailureDialogs() {
    }

    public static void showStartupFailure(Component parentComponent, Throwable throwable) {
        String errorMessage = Objects.nonNull(throwable) && StringUtils.isNotBlank(throwable.getMessage())
                ? throwable.getMessage()
                : Messages.get(MessageCodes.ERROR_AGENT_NATIVE_HOOK_FAILED, "unknown");
        JOptionPane.showMessageDialog(
                parentComponent,
                errorMessage,
                Messages.get(MessageCodes.UI_APP_TITLE),
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void showAutoStartTrackingFailure(Component parentComponent, Throwable throwable) {
        String details = Objects.nonNull(throwable) && StringUtils.isNotBlank(throwable.getMessage())
                ? throwable.getMessage()
                : Messages.get(MessageCodes.ERROR_AGENT_NATIVE_HOOK_FAILED, "unknown");
        JOptionPane.showMessageDialog(
                parentComponent,
                Messages.get(MessageCodes.ERROR_AGENT_NATIVE_HOOK_FAILED, details),
                Messages.get(MessageCodes.UI_APP_TITLE),
                JOptionPane.WARNING_MESSAGE
        );
    }
}
