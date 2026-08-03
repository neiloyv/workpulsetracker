package com.workpulsetracker.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "app_runtime_stat", schema = "public")
public class AppRuntimeStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false)
    private Long workerId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "app_identifier", nullable = false)
    private String appIdentifier;

    @Column(name = "total_seconds", nullable = false)
    private long totalSeconds;

    @Column(name = "last_agent_value", nullable = false)
    private long lastAgentValue;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AppRuntimeStatEntity() {
    }

    public AppRuntimeStatEntity(
            Long workerId,
            Long deviceId,
            String appIdentifier,
            long totalSeconds,
            long lastAgentValue,
            OffsetDateTime updatedAt
    ) {
        this.workerId = workerId;
        this.deviceId = deviceId;
        this.appIdentifier = appIdentifier;
        this.totalSeconds = totalSeconds;
        this.lastAgentValue = lastAgentValue;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getAppIdentifier() {
        return appIdentifier;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(long totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    public long getLastAgentValue() {
        return lastAgentValue;
    }

    public void setLastAgentValue(long lastAgentValue) {
        this.lastAgentValue = lastAgentValue;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
