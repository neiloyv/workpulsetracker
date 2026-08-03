package com.workpulsetracker.server.domain;

import com.workpulsetracker.server.enums.SubscriptionPlan;
import com.workpulsetracker.server.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "subscription", schema = "public")
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(name = "max_persons", nullable = false)
    private int maxPersons;

    @Column(name = "max_branches", nullable = false)
    private int maxBranches;

    protected SubscriptionEntity() {
    }

    public SubscriptionEntity(
            Long organizationId,
            SubscriptionPlan plan,
            SubscriptionStatus status,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            int maxPersons,
            int maxBranches
    ) {
        this.organizationId = organizationId;
        this.plan = plan;
        this.status = status;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.maxPersons = maxPersons;
        this.maxBranches = maxBranches;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public int getMaxPersons() {
        return maxPersons;
    }

    public void setMaxPersons(int maxPersons) {
        this.maxPersons = maxPersons;
    }

    public int getMaxBranches() {
        return maxBranches;
    }

    public void setMaxBranches(int maxBranches) {
        this.maxBranches = maxBranches;
    }
}
