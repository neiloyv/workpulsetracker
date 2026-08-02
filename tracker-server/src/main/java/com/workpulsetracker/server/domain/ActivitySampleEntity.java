package com.workpulsetracker.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "activity_sample", schema = "public")
public class ActivitySampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false)
    private Long workerId;

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
            Long workerId,
            String appName,
            long seconds,
            boolean idle,
            LocalDate activityDate
    ) {
        this.workerId = workerId;
        this.appName = appName;
        this.seconds = seconds;
        this.idle = idle;
        this.activityDate = activityDate;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkerId() {
        return workerId;
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
