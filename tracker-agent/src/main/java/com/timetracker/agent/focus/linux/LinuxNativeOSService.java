package com.timetracker.agent.focus.linux;

import com.timetracker.agent.focus.NativeOSService;
import com.timetracker.agent.focus.WindowInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Заглушка под Linux.
 * <p>
 * Сюда позже пойдёт получение активного окна через X11 / Wayland (JNA или отдельный native-helper).
 * JNativeHook для клавиатуры/мыши на Linux уже работает без этого класса.
 */
public final class LinuxNativeOSService implements NativeOSService {

    private static final Logger logger = LoggerFactory.getLogger(LinuxNativeOSService.class);

    public LinuxNativeOSService() {
        logger.warn("LinuxNativeOSService: window focus is not implemented yet (stub)");
    }

    @Override
    public WindowInfo getActiveWindowInfo() {
        return new WindowInfo("unknown-process", "Linux focus tracking not implemented yet");
    }

    @Override
    public String getOperatingSystemName() {
        return "Linux";
    }
}
