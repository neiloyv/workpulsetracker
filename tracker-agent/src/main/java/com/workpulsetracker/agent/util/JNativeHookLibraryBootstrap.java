package com.workpulsetracker.agent.util;

import com.workpulsetracker.agent.storage.LocalDataDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

/**
 * JNativeHook по умолчанию распаковывает DLL рядом с JAR.
 * В Program Files это запрещено — перенаправляем в ~/.workpulsetracker/native.
 */
public final class JNativeHookLibraryBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(JNativeHookLibraryBootstrap.class);
    private static final String schema = "local";
    private static final String J_NATIVE_HOOK_LIBRARY_PATH_PROPERTY = "jnativehook.lib.path";

    private JNativeHookLibraryBootstrap() {
    }

    public static void configureLibraryPath() {
        try {
            Files.createDirectories(LocalDataDirectory.getNativeLibrariesDirectoryPath());
            System.setProperty(
                    J_NATIVE_HOOK_LIBRARY_PATH_PROPERTY,
                    LocalDataDirectory.getNativeLibrariesDirectoryPath().toAbsolutePath().toString()
            );
            logger.info(
                    "schema={} JNativeHook library path configured: {}",
                    schema,
                    LocalDataDirectory.getNativeLibrariesDirectoryPath().toAbsolutePath()
            );
        } catch (IOException exception) {
            logger.warn(
                    "schema={} Failed to configure JNativeHook library path: {}",
                    schema,
                    exception.getMessage()
            );
        }
    }
}
