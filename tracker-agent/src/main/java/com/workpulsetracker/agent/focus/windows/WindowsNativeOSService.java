package com.workpulsetracker.agent.focus.windows;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.workpulsetracker.agent.focus.NativeOSService;
import com.workpulsetracker.agent.focus.WindowInfo;
import com.workpulsetracker.agent.icons.ApplicationIconService;
import com.workpulsetracker.agent.util.ApplicationTitleResolver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Получение активного окна через WinAPI (JNA) для Windows.
 */
public final class WindowsNativeOSService implements NativeOSService {

    private static final Logger logger = LoggerFactory.getLogger(WindowsNativeOSService.class);

    @Override
    public WindowInfo getActiveWindowInfo() {
        HWND foregroundWindowHandle = User32.INSTANCE.GetForegroundWindow();
        if (Objects.isNull(foregroundWindowHandle)) {
            return new WindowInfo("unknown", "");
        }

        String windowTitle = readWindowTitle(foregroundWindowHandle);
        ProcessIdentity processIdentity = readProcessIdentity(foregroundWindowHandle);
        if (StringUtils.isNotBlank(processIdentity.processImagePath())) {
            ApplicationIconService.getInstance().rememberExecutablePath(
                    processIdentity.processName(),
                    processIdentity.processImagePath()
            );
        }
        String displayTitle = ApplicationTitleResolver.resolveDisplayTitle(
                processIdentity.processName(),
                processIdentity.processImagePath()
        );

        return new WindowInfo(
                processIdentity.processName(),
                windowTitle,
                processIdentity.processImagePath(),
                displayTitle
        );
    }

    @Override
    public String getOperatingSystemName() {
        return "Windows";
    }

    private String readWindowTitle(HWND windowHandle) {
        char[] windowTitleBuffer = new char[1024];
        int titleLength = User32.INSTANCE.GetWindowText(windowHandle, windowTitleBuffer, windowTitleBuffer.length);
        if (titleLength <= 0) {
            return "";
        }
        return Native.toString(windowTitleBuffer);
    }

    private ProcessIdentity readProcessIdentity(HWND windowHandle) {
        IntByReference processIdReference = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(windowHandle, processIdReference);
        int processId = processIdReference.getValue();
        if (processId <= 0) {
            return new ProcessIdentity("unknown", null);
        }

        try {
            String processImagePath = Kernel32Util.QueryFullProcessImageName(processId, 0);
            return new ProcessIdentity(extractFileName(processImagePath), processImagePath);
        } catch (Exception exception) {
            logger.debug("Failed to resolve process name for pid={}: {}", processId, exception.getMessage());
            return readProcessIdentityFallback(processId);
        }
    }

    private ProcessIdentity readProcessIdentityFallback(int processId) {
        WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
                false,
                processId
        );
        if (Objects.isNull(processHandle)) {
            return new ProcessIdentity("unknown", null);
        }
        try {
            String processImagePath = Kernel32Util.QueryFullProcessImageName(processHandle, 0);
            return new ProcessIdentity(extractFileName(processImagePath), processImagePath);
        } catch (Exception exception) {
            logger.debug("Process name fallback failed: {}", exception.getMessage());
            return new ProcessIdentity("unknown", null);
        } finally {
            Kernel32.INSTANCE.CloseHandle(processHandle);
        }
    }

    private static String extractFileName(String processPath) {
        if (StringUtils.isBlank(processPath)) {
            return "unknown";
        }
        int lastSeparatorIndex = Math.max(processPath.lastIndexOf('\\'), processPath.lastIndexOf('/'));
        if (lastSeparatorIndex >= 0 && lastSeparatorIndex < processPath.length() - 1) {
            return processPath.substring(lastSeparatorIndex + 1);
        }
        return processPath;
    }

    private record ProcessIdentity(String processName, String processImagePath) {
    }
}
