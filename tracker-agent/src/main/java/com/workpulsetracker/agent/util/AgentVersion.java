package com.workpulsetracker.agent.util;

import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Версия агента из манифеста / fallback.
 */
public final class AgentVersion {

    private static final String FALLBACK_VERSION = "0.1.0";

    private AgentVersion() {
    }

    public static String get() {
        Package agentPackage = AgentVersion.class.getPackage();
        if (Objects.nonNull(agentPackage) && StringUtils.isNotBlank(agentPackage.getImplementationVersion())) {
            return agentPackage.getImplementationVersion();
        }
        try (InputStream inputStream = AgentVersion.class.getClassLoader()
                .getResourceAsStream("version.properties")) {
            if (Objects.nonNull(inputStream)) {
                Properties properties = new Properties();
                properties.load(inputStream);
                String version = properties.getProperty("agent.version");
                if (StringUtils.isNotBlank(version)) {
                    return version.trim();
                }
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return FALLBACK_VERSION;
    }
}
