package com.timetracker.agent.config;

import com.timetracker.common.i18n.AppLanguage;
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

    private static final long DEFAULT_IDLE_TIMEOUT_SECONDS = 180L;
    private static final long DEFAULT_IDLE_CHECK_INTERVAL_SECONDS = 5L;
    private static final long DEFAULT_FOCUS_POLL_INTERVAL_SECONDS = 10L;

    private final AppLanguage language;
    private final long idleTimeoutSeconds;
    private final long idleCheckIntervalSeconds;
    private final long focusPollIntervalSeconds;

    private AgentConfig(
            AppLanguage language,
            long idleTimeoutSeconds,
            long idleCheckIntervalSeconds,
            long focusPollIntervalSeconds
    ) {
        this.language = language;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
        this.idleCheckIntervalSeconds = idleCheckIntervalSeconds;
        this.focusPollIntervalSeconds = focusPollIntervalSeconds;
    }

    public static AgentConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = AgentConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (Objects.nonNull(inputStream)) {
                properties.load(inputStream);
            } else {
                logger.warn("application.properties не найден, используются значения по умолчанию");
            }
        } catch (IOException exception) {
            logger.warn("Не удалось прочитать application.properties: {}", exception.getMessage());
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

        AgentConfig agentConfig = new AgentConfig(
                language,
                idleTimeoutSeconds,
                idleCheckIntervalSeconds,
                focusPollIntervalSeconds
        );
        logger.info(
                "Конфигурация загружена: language={}, idleTimeout={}s, idleCheck={}s, focusPoll={}s",
                language.getCode(),
                idleTimeoutSeconds,
                idleCheckIntervalSeconds,
                focusPollIntervalSeconds
        );
        return agentConfig;
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
                    "Некорректное значение '{}' для '{}', используется {}",
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
}
