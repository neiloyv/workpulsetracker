package com.workpulsetracker.agent.ui;

import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.Objects;

/**
 * Иконка в системном трее: показать окно / выход.
 */
public final class TrayService {

    private static final Logger logger = LoggerFactory.getLogger(TrayService.class);

    private final TrackerMainFrame trackerMainFrame;
    private final Runnable exitAction;
    private TrayIcon trayIcon;

    public TrayService(TrackerMainFrame trackerMainFrame, Runnable exitAction) {
        this.trackerMainFrame = trackerMainFrame;
        this.exitAction = exitAction;
    }

    public void install() {
        if (!SystemTray.isSupported()) {
            logger.warn("SystemTray is not supported on this OS");
            return;
        }
        try {
            PopupMenu popupMenu = new PopupMenu();
            MenuItem openMenuItem = new MenuItem(Messages.get(MessageCodes.UI_TRAY_OPEN));
            MenuItem exitMenuItem = new MenuItem(Messages.get(MessageCodes.UI_TRAY_EXIT));
            openMenuItem.addActionListener(actionEvent -> trackerMainFrame.restoreFromTray());
            exitMenuItem.addActionListener(actionEvent -> exitAction.run());
            popupMenu.add(openMenuItem);
            popupMenu.add(exitMenuItem);

            trayIcon = new TrayIcon(
                    UiImages.loadTrayIconImage(32),
                    Messages.get(MessageCodes.UI_APP_TITLE),
                    popupMenu
            );
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(actionEvent -> trackerMainFrame.restoreFromTray());
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException exception) {
            logger.warn("Failed to add tray icon: {}", exception.getMessage());
        }
    }

    public void uninstall() {
        if (Objects.nonNull(trayIcon) && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    public void retranslate() {
        if (Objects.isNull(trayIcon)) {
            return;
        }
        trayIcon.setToolTip(Messages.get(MessageCodes.UI_APP_TITLE));
        PopupMenu popupMenu = trayIcon.getPopupMenu();
        if (Objects.isNull(popupMenu) || popupMenu.getItemCount() < 2) {
            return;
        }
        popupMenu.getItem(0).setLabel(Messages.get(MessageCodes.UI_TRAY_OPEN));
        popupMenu.getItem(1).setLabel(Messages.get(MessageCodes.UI_TRAY_EXIT));
    }

    public void showNotification(String caption, String message) {
        if (Objects.isNull(trayIcon)) {
            return;
        }
        trayIcon.displayMessage(
                Objects.requireNonNullElse(caption, Messages.get(MessageCodes.UI_APP_TITLE)),
                Objects.requireNonNullElse(message, ""),
                TrayIcon.MessageType.INFO
        );
    }
}
