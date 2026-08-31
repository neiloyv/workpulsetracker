package com.workpulsetracker.agent.focus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Заглушка для ОС без реализации отслеживания активного окна (Linux, macOS, неизвестная ОС).
 * <p>
 * JNativeHook для клавиатуры/мыши на Linux и macOS работает и без этого класса
 * (на macOS может потребоваться Accessibility permission).
 */
public final class UnsupportedNativeOSService implements NativeOSService {

    private static final Logger logger = LoggerFactory.getLogger(UnsupportedNativeOSService.class);

    private final String operatingSystemName;

    public UnsupportedNativeOSService(String operatingSystemName) {
        this.operatingSystemName = operatingSystemName;
        logger.warn("Window focus tracking is not implemented for {} (stub)", operatingSystemName);
    }

    @Override
    public WindowInfo getActiveWindowInfo() {
        return new WindowInfo("unknown-process", "Window focus tracking not implemented for " + operatingSystemName);
    }

    @Override
    public String getOperatingSystemName() {
        return operatingSystemName;
    }
}
