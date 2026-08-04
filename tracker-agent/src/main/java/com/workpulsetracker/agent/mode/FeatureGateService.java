package com.workpulsetracker.agent.mode;

import com.workpulsetracker.agent.storage.UserSettings;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Лёгкий feature-gate по текущему {@link AgentOperationMode}.
 */
public final class FeatureGateService {

    private final Supplier<UserSettings> userSettingsSupplier;

    public FeatureGateService(UserSettings userSettings) {
        this(() -> userSettings);
    }

    public FeatureGateService(Supplier<UserSettings> userSettingsSupplier) {
        this.userSettingsSupplier = Objects.requireNonNull(userSettingsSupplier);
    }

    public boolean isFeatureAllowed(AgentFeature agentFeature) {
        Objects.requireNonNull(agentFeature);
        AgentOperationMode agentOperationMode = resolveOperationMode();
        return switch (agentFeature) {
            case DAILY_TIMELINE, BASIC_LOCAL_STATS -> true;
            case SYNC_TO_CLOUD, MULTI_DEVICE_AGGREGATION, EXTENDED_HISTORY_EXPORT, ADVANCED_ANALYTICS ->
                    agentOperationMode.isNetworkSync();
        };
    }

    public AgentOperationMode resolveOperationMode() {
        UserSettings userSettings = userSettingsSupplier.get();
        if (Objects.isNull(userSettings)) {
            return AgentOperationMode.getDefault();
        }
        return userSettings.getOperationMode();
    }

    public boolean isNetworkSyncMode() {
        return resolveOperationMode().isNetworkSync();
    }

    public boolean isLocalSoloMode() {
        return resolveOperationMode().isLocalSolo();
    }
}
