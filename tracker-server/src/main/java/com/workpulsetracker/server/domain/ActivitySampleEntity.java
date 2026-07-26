package com.workpulsetracker.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "activity_sample", schema = "public")
public class ActivitySampleEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "app_name", nullable = false)
    private String appName;

    @Column(nullable = false)
    private long seconds;

    @Column(nullable = false)
    private boolean idle;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    protected ActivitySampleEntity() {
    }

    public ActivitySampleEntity(
            UUID id,
            UUID userId,
            String appName,
            long seconds,
            boolean idle,
            LocalDate activityDate
    ) {
        this.id = id;
        this.userId = userId;
        this.appName = appName;
        this.seconds = seconds;
        this.idle = idle;
        this.activityDate = activityDate;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAppName() {
        return appName;
    }

    public long getSeconds() {
        return seconds;
    }

    public boolean isIdle() {
        return idle;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }
}
