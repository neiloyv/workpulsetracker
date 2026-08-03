package com.workpulsetracker.server.domain;

import com.workpulsetracker.server.enums.EntityStatus;
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
@Table(name = "worker", schema = "public")
public class WorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String email;

    @Column(name = "access_key", nullable = false)
    private String accessKey;

    @Column(name = "access_key_prefix", nullable = false)
    private String accessKeyPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityStatus status;

    @Column(name = "agent_installed", nullable = false)
    private boolean agentInstalled;

    @Column(name = "agent_version")
    private String agentVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected WorkerEntity() {
    }

    public WorkerEntity(
            Long organizationId,
            Long branchId,
            Long departmentId,
            String displayName,
            String email,
            String accessKey,
            String accessKeyPrefix,
            EntityStatus status,
            boolean agentInstalled,
            String agentVersion,
            OffsetDateTime createdAt
    ) {
        this.organizationId = organizationId;
        this.branchId = branchId;
        this.departmentId = departmentId;
        this.displayName = displayName;
        this.email = email;
        this.accessKey = accessKey;
        this.accessKeyPrefix = accessKeyPrefix;
        this.status = status;
        this.agentInstalled = agentInstalled;
        this.agentVersion = agentVersion;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getAccessKeyPrefix() {
        return accessKeyPrefix;
    }

    public void setAccessKeyPrefix(String accessKeyPrefix) {
        this.accessKeyPrefix = accessKeyPrefix;
    }

    public EntityStatus getStatus() {
        return status;
    }

    public void setStatus(EntityStatus status) {
        this.status = status;
    }

    public boolean isAgentInstalled() {
        return agentInstalled;
    }

    public void setAgentInstalled(boolean agentInstalled) {
        this.agentInstalled = agentInstalled;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
