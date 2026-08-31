package com.workpulsetracker.agent.focus;

import com.workpulsetracker.agent.focus.windows.WindowsNativeOSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Выбирает реализацию {@link NativeOSService} по текущей ОС.
 */
public final class NativeOSServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(NativeOSServiceFactory.class);

    private NativeOSServiceFactory() {
    }

    public static NativeOSService create() {
        OperatingSystemType operatingSystemType = OperatingSystemType.detect();
        logger.info("Detected OS: {}", operatingSystemType);

        return switch (operatingSystemType) {
            case WINDOWS -> new WindowsNativeOSService();
            case LINUX -> new UnsupportedNativeOSService("Linux");
            case MACOS -> new UnsupportedNativeOSService("macOS");
            case UNKNOWN -> new UnsupportedNativeOSService(System.getProperty("os.name", "unknown"));
        };
    }
}
