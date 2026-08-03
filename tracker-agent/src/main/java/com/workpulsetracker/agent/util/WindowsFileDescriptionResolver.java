package com.workpulsetracker.agent.util;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Читает FileDescription (VERSIONINFO) через Win32 Version API (JNA).
 * При ошибке возвращает null — вызывающий код использует fallback на имя процесса.
 */
public final class WindowsFileDescriptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(WindowsFileDescriptionResolver.class);

    private WindowsFileDescriptionResolver() {
    }

    public static String resolveFileDescription(String executablePath) {
        if (StringUtils.isBlank(executablePath)) {
            return null;
        }
        try {
            IntByReference dummyHandle = new IntByReference();
            int versionInfoSize = Version.INSTANCE.GetFileVersionInfoSize(executablePath, dummyHandle);
            if (versionInfoSize <= 0) {
                return null;
            }

            Memory versionInfoMemory = new Memory(versionInfoSize);
            boolean versionInfoLoaded = Version.INSTANCE.GetFileVersionInfo(
                    executablePath,
                    0,
                    versionInfoSize,
                    versionInfoMemory
            );
            if (!versionInfoLoaded) {
                return null;
            }

            String fileDescription = queryStringValue(versionInfoMemory, "FileDescription");
            if (StringUtils.isNotBlank(fileDescription)) {
                return fileDescription.trim();
            }
            String productName = queryStringValue(versionInfoMemory, "ProductName");
            if (StringUtils.isNotBlank(productName)) {
                return productName.trim();
            }
            return null;
        } catch (Exception exception) {
            logger.debug(
                    "Failed to resolve FileDescription for path={}: {}",
                    executablePath,
                    exception.getMessage()
            );
            return null;
        }
    }

    private static String queryStringValue(Memory versionInfoMemory, String valueName) {
        PointerByReference translationPointerReference = new PointerByReference();
        IntByReference translationLengthReference = new IntByReference();
        boolean translationFound = Version.INSTANCE.VerQueryValue(
                versionInfoMemory,
                "\\VarFileInfo\\Translation",
                translationPointerReference,
                translationLengthReference
        );
        if (!translationFound || Objects.isNull(translationPointerReference.getValue())) {
            return queryStringValueWithCodePage(versionInfoMemory, "040904B0", valueName);
        }

        Pointer translationPointer = translationPointerReference.getValue();
        int language = translationPointer.getShort(0) & 0xffff;
        int codePage = translationPointer.getShort(2) & 0xffff;
        String codePageKey = String.format("%04X%04X", language, codePage);
        String value = queryStringValueWithCodePage(versionInfoMemory, codePageKey, valueName);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }
        return queryStringValueWithCodePage(versionInfoMemory, "040904B0", valueName);
    }

    private static String queryStringValueWithCodePage(
            Memory versionInfoMemory,
            String codePageKey,
            String valueName
    ) {
        PointerByReference valuePointerReference = new PointerByReference();
        IntByReference valueLengthReference = new IntByReference();
        String subBlock = "\\StringFileInfo\\" + codePageKey + "\\" + valueName;
        boolean valueFound = Version.INSTANCE.VerQueryValue(
                versionInfoMemory,
                subBlock,
                valuePointerReference,
                valueLengthReference
        );
        if (!valueFound || Objects.isNull(valuePointerReference.getValue()) || valueLengthReference.getValue() <= 0) {
            return null;
        }
        return Native.toString(valuePointerReference.getValue().getCharArray(0, valueLengthReference.getValue()));
    }
}
