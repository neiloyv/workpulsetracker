package com.workpulsetracker.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "organization_setting", schema = "public")
@IdClass(OrganizationSettingEntity.OrganizationSettingId.class)
public class OrganizationSettingEntity {

    @Id
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Id
    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    @Column(name = "setting_value", nullable = false)
    private String settingValue;

    protected OrganizationSettingEntity() {
    }

    public OrganizationSettingEntity(Long organizationId, String settingKey, String settingValue) {
        this.organizationId = organizationId;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public static final class OrganizationSettingId implements Serializable {

        private Long organizationId;
        private String settingKey;

        public OrganizationSettingId() {
        }

        public OrganizationSettingId(Long organizationId, String settingKey) {
            this.organizationId = organizationId;
            this.settingKey = settingKey;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof OrganizationSettingId that)) {
                return false;
            }
            return Objects.equals(organizationId, that.organizationId)
                    && Objects.equals(settingKey, that.settingKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(organizationId, settingKey);
        }
    }
}
