package com.timetracker.server.service;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class AgentKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AgentKeyGenerator() {
    }

    public static String generatePlaintextKey() {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "tt_" + HexFormat.of().formatHex(randomBytes);
    }

    public static String hashKey(String plaintextKey) {
        if (StringUtils.isBlank(plaintextKey)) {
            throw new IllegalArgumentException("Agent key must not be blank");
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(plaintextKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static String prefixOf(String plaintextKey) {
        if (StringUtils.isBlank(plaintextKey) || plaintextKey.length() < 10) {
            return "tt_****";
        }
        return plaintextKey.substring(0, 10) + "...";
    }
}
