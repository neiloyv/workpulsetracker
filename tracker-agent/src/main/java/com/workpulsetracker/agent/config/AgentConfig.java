package com.workpulsetracker.agent.config;

import com.workpulsetracker.common.i18n.AppLanguage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Загрузка настроек агента из application.properties.
 */
public final class AgentConfig {

    private static final Logger logger = LoggerFactory.getLogger(AgentConfig.class);

    private static final long DEFAULT_IDLE_TIMEOUT_SECONDS = 60L;
    private static final long DEFAULT_IDLE_CHECK_INTERVAL_SECONDS = 5L;
    private static final long DEFAULT_FOCUS_POLL_INTERVAL_SECONDS = 10L;
    private static final long DEFAULT_TELEMETRY_UPLOAD_INTERVAL_SECONDS = 60L;
    private static final String DEFAULT_SERVER_BASE_URL = "http://localhost:8080";
    private static final String DEFAULT_SUPPORT_EMAIL = "workpulsetracker@gmail.com";

    private final AppLanguage language;
    private final long idleTimeoutSeconds;
    private final long idleCheckIntervalSeconds;
    private final long focusPollIntervalSeconds;
    private final String serverBaseUrl;
    private final long telemetryUploadIntervalSeconds;
    private final String supportEmail;

    private AgentConfig(
            AppLanguage language,
            long idleTimeoutSeconds,
            long idleCheckIntervalSeconds,
            long focusPollIntervalSeconds,
            String serverBaseUrl,
            long telemetryUploadIntervalSeconds,
            String supportEmail
    ) {
        this.language = language;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
        this.idleCheckIntervalSeconds = idleCheckIntervalSeconds;
        this.focusPollIntervalSeconds = focusPollIntervalSeconds;
        this.serverBaseUrl = serverBaseUrl;
        this.telemetryUploadIntervalSeconds = telemetryUploadIntervalSeconds;
        this.supportEmail = supportEmail;
    }

    public static AgentConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = AgentConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (Objects.nonNull(inputStream)) {
                properties.load(inputStream);
            } else {
                logger.warn("application.properties not found, using default values");
            }
        } catch (IOException exception) {
            logger.warn("Failed to read application.properties: {}", exception.getMessage());
        }

        AppLanguage language = AppLanguage.fromCode(properties.getProperty("app.language"));
        long idleTimeoutSeconds = resolveLong(
                properties,
                "idle.timeout.seconds",
                DEFAULT_IDLE_TIMEOUT_SECONDS
        );
        long idleCheckIntervalSeconds = resolveLong(
                properties,
                "idle.check.interval.seconds",
                DEFAULT_IDLE_CHECK_INTERVAL_SECONDS
        );
        long focusPollIntervalSeconds = resolveLong(
                properties,
                "focus.poll.interval.seconds",
                DEFAULT_FOCUS_POLL_INTERVAL_SECONDS
        );
        long telemetryUploadIntervalSeconds = resolveLong(
                properties,
                "telemetry.upload.interval.seconds",
                DEFAULT_TELEMETRY_UPLOAD_INTERVAL_SECONDS
        );
        String serverBaseUrl = resolveServerBaseUrl(properties.getProperty("server.base-url"));
        String supportEmail = resolveSupportEmail(properties.getProperty("support.email"));

        AgentConfig agentConfig = new AgentConfig(
                language,
                idleTimeoutSeconds,
                idleCheckIntervalSeconds,
                focusPollIntervalSeconds,
                serverBaseUrl,
                telemetryUploadIntervalSeconds,
                supportEmail
        );
        logger.info(
                "Configuration loaded: language={}, idleTimeout={}s, idleCheck={}s, focusPoll={}s, serverBaseUrl={}, telemetryUpload={}s, supportEmail={}",
                language.getCode(),
                idleTimeoutSeconds,
                idleCheckIntervalSeconds,
                focusPollIntervalSeconds,
                serverBaseUrl,
                telemetryUploadIntervalSeconds,
                supportEmail
        );
        return agentConfig;
    }

    private static String resolveServerBaseUrl(String configuredBaseUrl) {
        if (StringUtils.isBlank(configuredBaseUrl)) {
            return DEFAULT_SERVER_BASE_URL;
        }
        String trimmedBaseUrl = configuredBaseUrl.trim();
        while (trimmedBaseUrl.endsWith("/")) {
            trimmedBaseUrl = trimmedBaseUrl.substring(0, trimmedBaseUrl.length() - 1);
        }
        return trimmedBaseUrl;
    }

    private static String resolveSupportEmail(String configuredSupportEmail) {
        if (StringUtils.isBlank(configuredSupportEmail)) {
            return DEFAULT_SUPPORT_EMAIL;
        }
        return configuredSupportEmail.trim().toLowerCase();
    }

    private static long resolveLong(Properties properties, String propertyName, long defaultValue) {
        String propertyValue = properties.getProperty(propertyName);
        if (StringUtils.isBlank(propertyValue)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(propertyValue.trim());
        } catch (NumberFormatException exception) {
            logger.warn(
                    "Invalid value '{}' for '{}', using {}",
                    propertyValue,
                    propertyName,
                    defaultValue
            );
            return defaultValue;
        }
    }

    public AppLanguage getLanguage() {
        return language;
    }

    public long getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public long getIdleCheckIntervalSeconds() {
        return idleCheckIntervalSeconds;
    }

    public long getFocusPollIntervalSeconds() {
        return focusPollIntervalSeconds;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public long getTelemetryUploadIntervalSeconds() {
        return telemetryUploadIntervalSeconds;
    }

    public String getSupportEmail() {
        return supportEmail;
    }
}
