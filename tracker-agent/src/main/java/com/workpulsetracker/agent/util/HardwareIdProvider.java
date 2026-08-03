package com.workpulsetracker.agent.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Стабильный hardware id устройства для pairing с сервером.
 */
public final class HardwareIdProvider {

    private static final Logger logger = LoggerFactory.getLogger(HardwareIdProvider.class);
    private static final String WINDOWS_MACHINE_GUID_PATH = "SOFTWARE\\Microsoft\\Cryptography";
    private static final String WINDOWS_MACHINE_GUID_VALUE = "MachineGuid";

    private HardwareIdProvider() {
    }

    public static String resolveHardwareId() {
        String operatingSystemName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (operatingSystemName.contains("win")) {
                String machineGuid = Advapi32Util.registryGetStringValue(
                        WinReg.HKEY_LOCAL_MACHINE,
                        WINDOWS_MACHINE_GUID_PATH,
                        WINDOWS_MACHINE_GUID_VALUE
                );
                if (StringUtils.isNotBlank(machineGuid)) {
                    return "win-" + machineGuid.trim().toLowerCase(Locale.ROOT);
                }
            }
        } catch (Exception exception) {
            logger.debug("Failed to read Windows MachineGuid: {}", exception.getMessage());
        }

        String macFingerprint = resolveMacFingerprint();
        if (StringUtils.isNotBlank(macFingerprint)) {
            return macFingerprint;
        }

        String fallbackSeed = Stream.of(
                        System.getProperty("os.name"),
                        System.getProperty("os.arch"),
                        System.getProperty("user.name"),
                        System.getenv("COMPUTERNAME"),
                        System.getenv("HOSTNAME")
                )
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("|"));
        if (StringUtils.isBlank(fallbackSeed)) {
            fallbackSeed = UUID.randomUUID().toString();
        }
        return "fallback-" + sha256Hex(fallbackSeed);
    }

    private static String resolveMacFingerprint() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (Objects.isNull(networkInterfaces)) {
                return null;
            }
            String joinedMacAddresses = Collections.list(networkInterfaces).stream()
                    .filter(networkInterface -> {
                        try {
                            return !networkInterface.isLoopback()
                                    && !networkInterface.isVirtual()
                                    && Objects.nonNull(networkInterface.getHardwareAddress());
                        } catch (Exception exception) {
                            return false;
                        }
                    })
                    .map(networkInterface -> {
                        try {
                            byte[] hardwareAddress = networkInterface.getHardwareAddress();
                            if (Objects.isNull(hardwareAddress) || hardwareAddress.length == 0) {
                                return null;
                            }
                            return HexFormat.of().formatHex(hardwareAddress);
                        } catch (Exception exception) {
                            return null;
                        }
                    })
                    .filter(StringUtils::isNotBlank)
                    .sorted()
                    .collect(Collectors.joining("-"));
            if (StringUtils.isBlank(joinedMacAddresses)) {
                return null;
            }
            return "mac-" + sha256Hex(joinedMacAddresses).substring(0, 32);
        } catch (Exception exception) {
            logger.debug("Failed to resolve MAC fingerprint: {}", exception.getMessage());
            return null;
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digestBytes);
        } catch (Exception exception) {
            return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        }
    }
}
