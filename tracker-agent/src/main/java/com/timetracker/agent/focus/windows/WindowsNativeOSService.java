package com.timetracker.agent.focus.windows;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.timetracker.agent.focus.NativeOSService;
import com.timetracker.agent.focus.WindowInfo;
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
        String processName = readProcessName(foregroundWindowHandle);
        return new WindowInfo(processName, windowTitle);
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

    private String readProcessName(HWND windowHandle) {
        IntByReference processIdReference = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(windowHandle, processIdReference);
        int processId = processIdReference.getValue();
        if (processId <= 0) {
            return "unknown";
        }

        try {
            String processImagePath = Kernel32Util.QueryFullProcessImageName(processId, 0);
            return extractFileName(processImagePath);
        } catch (Exception exception) {
            logger.debug("Failed to resolve process name for pid={}: {}", processId, exception.getMessage());
            return readProcessNameFallback(processId);
        }
    }

    private String readProcessNameFallback(int processId) {
        WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
                false,
                processId
        );
        if (Objects.isNull(processHandle)) {
            return "unknown";
        }
        try {
            return extractFileName(Kernel32Util.QueryFullProcessImageName(processHandle, 0));
        } catch (Exception exception) {
            logger.debug("Process name fallback failed: {}", exception.getMessage());
            return "unknown";
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
}
