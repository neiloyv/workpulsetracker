package com.workpulsetracker.agent.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Регистрация агента в автозагрузке Windows через {@code HKCU\...\Run}.
 */
public final class WindowsLaunchAtLoginService {

    private static final Logger logger = LoggerFactory.getLogger(WindowsLaunchAtLoginService.class);
    private static final String schema = "local";
    private static final String RUN_REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String RUN_VALUE_NAME = "WorkPulseTrackerAgent";

    private WindowsLaunchAtLoginService() {
    }

    public static boolean isSupported() {
        String operatingSystemName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return operatingSystemName.contains("win");
    }

    /**
     * Синхронизирует запись в автозагрузке с настройкой пользователя.
     */
    public static void apply(boolean launchAtLoginEnabled) {
        if (!isSupported()) {
            return;
        }
        if (launchAtLoginEnabled) {
            enable();
        } else {
            disable();
        }
    }

    private static void enable() {
        String launchCommand = resolveLaunchCommand();
        if (StringUtils.isBlank(launchCommand)) {
            logger.warn(
                    "schema={} Cannot enable launch at login: executable path unresolved",
                    schema
            );
            return;
        }
        try {
            Advapi32Util.registrySetStringValue(
                    WinReg.HKEY_CURRENT_USER,
                    RUN_REGISTRY_PATH,
                    RUN_VALUE_NAME,
                    launchCommand
            );
            logger.info(
                    "schema={} Launch at login enabled: command={}",
                    schema,
                    launchCommand
            );
        } catch (Exception exception) {
            logger.warn(
                    "schema={} Failed to enable launch at login: {}",
                    schema,
                    exception.getMessage()
            );
        }
    }

    private static void disable() {
        try {
            if (Advapi32Util.registryValueExists(
                    WinReg.HKEY_CURRENT_USER,
                    RUN_REGISTRY_PATH,
                    RUN_VALUE_NAME
            )) {
                Advapi32Util.registryDeleteValue(
                        WinReg.HKEY_CURRENT_USER,
                        RUN_REGISTRY_PATH,
                        RUN_VALUE_NAME
                );
            }
            logger.info("schema={} Launch at login disabled", schema);
        } catch (Exception exception) {
            logger.warn(
                    "schema={} Failed to disable launch at login: {}",
                    schema,
                    exception.getMessage()
            );
        }
    }

    private static String resolveLaunchCommand() {
        String jpackageAppPath = System.getProperty("jpackage.app-path");
        if (StringUtils.isNotBlank(jpackageAppPath)) {
            return quotePath(Path.of(jpackageAppPath.trim()).toAbsolutePath().normalize());
        }

        Optional<String> processCommandOptional = ProcessHandle.current().info().command();
        if (processCommandOptional.isPresent()) {
            Path processExecutablePath = Path.of(processCommandOptional.get()).toAbsolutePath().normalize();
            String executableFileName = processExecutablePath.getFileName().toString().toLowerCase(Locale.ROOT);
            if (executableFileName.endsWith(".exe")
                    && !Objects.equals(executableFileName, "java.exe")
                    && !Objects.equals(executableFileName, "javaw.exe")) {
                return quotePath(processExecutablePath);
            }

            Path jarPath = resolveMainJarPath();
            if (Objects.nonNull(jarPath)) {
                Path javaLauncherPath = preferJavawExecutable(processExecutablePath);
                return quotePath(javaLauncherPath) + " -jar " + quotePath(jarPath);
            }
        }

        return null;
    }

    private static Path resolveMainJarPath() {
        String sunJavaCommand = System.getProperty("sun.java.command");
        if (StringUtils.isNotBlank(sunJavaCommand)) {
            String firstToken = sunJavaCommand.trim().split("\\s+")[0];
            if (StringUtils.endsWithIgnoreCase(firstToken, ".jar")) {
                Path jarPath = Path.of(firstToken).toAbsolutePath().normalize();
                if (Files.isRegularFile(jarPath)) {
                    return jarPath;
                }
            }
        }

        String classPath = System.getProperty("java.class.path", "");
        if (StringUtils.isBlank(classPath)) {
            return null;
        }
        return Arrays.stream(classPath.split(System.getProperty("path.separator", ";")))
                .filter(StringUtils::isNotBlank)
                .map(classPathEntry -> Path.of(classPathEntry.trim()).toAbsolutePath().normalize())
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    return fileName.endsWith("-all.jar") || fileName.startsWith("tracker-agent");
                })
                .findFirst()
                .orElse(null);
    }

    private static Path preferJavawExecutable(Path javaExecutablePath) {
        String executableFileName = javaExecutablePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (Objects.equals(executableFileName, "javaw.exe")) {
            return javaExecutablePath;
        }
        if (Objects.equals(executableFileName, "java.exe")
                && Objects.nonNull(javaExecutablePath.getParent())) {
            Path javawExecutablePath = javaExecutablePath.getParent().resolve("javaw.exe");
            if (Files.isRegularFile(javawExecutablePath)) {
                return javawExecutablePath;
            }
        }
        return javaExecutablePath;
    }

    private static String quotePath(Path path) {
        return "\"" + path.toString() + "\"";
    }
}
