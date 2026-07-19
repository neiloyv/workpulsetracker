package com.timetracker.agent.focus;

import com.timetracker.agent.focus.linux.LinuxNativeOSService;
import com.timetracker.agent.focus.macos.MacNativeOSService;
import com.timetracker.agent.focus.windows.WindowsNativeOSService;
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
            case LINUX -> new LinuxNativeOSService();
            case MACOS -> new MacNativeOSService();
            case UNKNOWN -> {
                logger.warn("Unknown OS '{}', window focus will be unavailable",
                        System.getProperty("os.name"));
                yield new NativeOSService() {
                    @Override
                    public WindowInfo getActiveWindowInfo() {
                        return new WindowInfo("unknown-process", "Unsupported OS");
                    }

                    @Override
                    public String getOperatingSystemName() {
                        return System.getProperty("os.name", "unknown");
                    }
                };
            }
        };
    }
}
