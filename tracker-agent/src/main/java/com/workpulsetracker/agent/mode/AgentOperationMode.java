package com.workpulsetracker.agent.mode;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Режим работы локального агента: автономный Free Solo или синхронизация с бэкендом.
 */
public enum AgentOperationMode {

    /**
     * Офлайн Free Solo: трекинг и локальная статистика без сетевых вызовов.
     */
    LOCAL_SOLO,

    /**
     * Аккаунт привязан через access_key; телеметрия уходит на сервер.
     */
    NETWORK_SYNC;

    public static AgentOperationMode getDefault() {
        return LOCAL_SOLO;
    }

    public static AgentOperationMode fromCode(String operationModeCode) {
        if (StringUtils.isBlank(operationModeCode)) {
            return getDefault();
        }
        String normalizedOperationModeCode = operationModeCode.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(agentOperationMode -> Objects.equals(agentOperationMode.name(), normalizedOperationModeCode))
                .findFirst()
                .orElseGet(AgentOperationMode::getDefault);
    }

    public boolean isLocalSolo() {
        return this == LOCAL_SOLO;
    }

    public boolean isNetworkSync() {
        return this == NETWORK_SYNC;
    }
}
