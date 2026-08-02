package com.workpulsetracker.server.service;

import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class AccessKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AccessKeyGenerator() {
    }

    public static String generatePlaintextKey() {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "wpt_" + HexFormat.of().formatHex(randomBytes);
    }

    public static String prefixOf(String plaintextKey) {
        if (StringUtils.isBlank(plaintextKey) || plaintextKey.length() < 10) {
            return "wpt_****";
        }
        return plaintextKey.substring(0, 10) + "...";
    }
}
