package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    Optional<SubscriptionEntity> findByOrganizationId(Long organizationId);
}
