package com.timetracker.server.repository;

import com.timetracker.server.domain.OrganizationSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationSettingRepository
        extends JpaRepository<OrganizationSettingEntity, OrganizationSettingEntity.OrganizationSettingId> {

    List<OrganizationSettingEntity> findByOrganizationId(UUID organizationId);

    Optional<OrganizationSettingEntity> findByOrganizationIdAndSettingKey(UUID organizationId, String settingKey);
}
