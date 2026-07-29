package com.workpulsetracker.agent.icons;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Сопоставление имён процессов с путями exe среди запущенных процессов Windows.
 */
final class WindowsProcessPathDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(WindowsProcessPathDiscovery.class);

    private WindowsProcessPathDiscovery() {
    }

    static Map<String, String> discoverRunningProcesses() {
        Map<String, String> executablePathByProcessName = new LinkedHashMap<>();
        WinNT.HANDLE snapshotHandle = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
                Tlhelp32.TH32CS_SNAPPROCESS,
                new WinDef.DWORD(0)
        );
        if (Objects.isNull(snapshotHandle) || WinNT.INVALID_HANDLE_VALUE.equals(snapshotHandle)) {
            return executablePathByProcessName;
        }
        try {
            Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
            if (!Kernel32.INSTANCE.Process32First(snapshotHandle, processEntry)) {
                return executablePathByProcessName;
            }
            do {
                String processName = toNullTerminatedString(processEntry.szExeFile);
                if (StringUtils.isBlank(processName) || executablePathByProcessName.containsKey(processName)) {
                    continue;
                }
                int processId = processEntry.th32ProcessID.intValue();
                String processImagePath = queryProcessImagePath(processId);
                if (StringUtils.isNotBlank(processImagePath)) {
                    executablePathByProcessName.put(processName, processImagePath);
                }
            } while (Kernel32.INSTANCE.Process32Next(snapshotHandle, processEntry));
        } catch (Exception exception) {
            logger.debug("Failed to discover running process paths: {}", exception.getMessage());
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshotHandle);
        }
        return executablePathByProcessName;
    }

    private static String queryProcessImagePath(int processId) {
        if (processId <= 0) {
            return null;
        }
        try {
            return Kernel32Util.QueryFullProcessImageName(processId, 0);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String toNullTerminatedString(char[] characters) {
        if (Objects.isNull(characters) || characters.length == 0) {
            return "";
        }
        int length = 0;
        while (length < characters.length && characters[length] != 0) {
            length++;
        }
        return new String(characters, 0, length);
    }
}
