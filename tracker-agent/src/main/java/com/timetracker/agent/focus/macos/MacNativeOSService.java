package com.timetracker.agent.focus.macos;

import com.timetracker.agent.focus.NativeOSService;
import com.timetracker.agent.focus.WindowInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Заглушка под macOS.
 * <p>
 * Сюда позже пойдёт получение активного окна через Cocoa / CoreGraphics (JNA или JNI).
 * JNativeHook для клавиатуры/мыши на macOS уже работает без этого класса
 * (может потребоваться Accessibility permission в настройках macOS).
 */
public final class MacNativeOSService implements NativeOSService {

    private static final Logger logger = LoggerFactory.getLogger(MacNativeOSService.class);

    public MacNativeOSService() {
        logger.warn("MacNativeOSService: window focus is not implemented yet (stub)");
    }

    @Override
    public WindowInfo getActiveWindowInfo() {
        return new WindowInfo("unknown-process", "macOS focus tracking not implemented yet");
    }

    @Override
    public String getOperatingSystemName() {
        return "macOS";
    }
}
