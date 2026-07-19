package com.timetracker.agent.activity;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.timetracker.common.i18n.MessageCodes;
import com.timetracker.common.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Глобальный монитор клавиатуры и мыши на базе JNativeHook.
 */
public final class NativeActivityMonitor implements ActivityMonitor, NativeKeyListener, NativeMouseInputListener {

    private static final Logger logger = LoggerFactory.getLogger(NativeActivityMonitor.class);

    private final List<ActivityListener> activityListeners = new CopyOnWriteArrayList<>();
    private volatile boolean started;

    @Override
    public void start() {
        if (started) {
            return;
        }
        suppressJNativeHookLogging();
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);
            GlobalScreen.addNativeMouseMotionListener(this);
            started = true;
            logger.info("ActivityMonitor запущен (JNativeHook)");
        } catch (NativeHookException exception) {
            throw new IllegalStateException(
                    Messages.get(MessageCodes.ERROR_AGENT_NATIVE_HOOK_FAILED, exception.getMessage()),
                    exception
            );
        }
    }

    @Override
    public void addListener(ActivityListener activityListener) {
        if (Objects.nonNull(activityListener)) {
            activityListeners.add(activityListener);
        }
    }

    @Override
    public void removeListener(ActivityListener activityListener) {
        if (Objects.nonNull(activityListener)) {
            activityListeners.remove(activityListener);
        }
    }

    @Override
    public void close() {
        if (!started) {
            return;
        }
        GlobalScreen.removeNativeKeyListener(this);
        GlobalScreen.removeNativeMouseListener(this);
        GlobalScreen.removeNativeMouseMotionListener(this);
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException exception) {
            logger.warn("Ошибка при остановке ActivityMonitor: {}", exception.getMessage());
        }
        started = false;
        logger.info("ActivityMonitor остановлен");
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        notifyActivity();
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        // no-op
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
        // no-op
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent nativeMouseEvent) {
        notifyActivity();
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent nativeMouseEvent) {
        notifyActivity();
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent nativeMouseEvent) {
        // no-op
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent nativeMouseEvent) {
        notifyActivity();
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent nativeMouseEvent) {
        notifyActivity();
    }

    private void notifyActivity() {
        activityListeners.forEach(ActivityListener::onUserActivity);
    }

    private static void suppressJNativeHookLogging() {
        java.util.logging.Logger jNativeHookLogger = java.util.logging.Logger.getLogger(
                GlobalScreen.class.getPackage().getName()
        );
        jNativeHookLogger.setLevel(Level.WARNING);
        jNativeHookLogger.setUseParentHandlers(false);
    }
}
