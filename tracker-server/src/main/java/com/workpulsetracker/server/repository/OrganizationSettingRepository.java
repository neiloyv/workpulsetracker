package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.OrganizationSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationSettingRepository
        extends JpaRepository<OrganizationSettingEntity, OrganizationSettingEntity.OrganizationSettingId> {

    List<OrganizationSettingEntity> findByOrganizationId(Long organizationId);

    Optional<OrganizationSettingEntity> findByOrganizationIdAndSettingKey(Long organizationId, String settingKey);
}
