package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByGoogleSub(String googleSub);

    @Query("select u from AppUserEntity u where lower(u.email) = lower(:email)")
    Optional<AppUserEntity> findByEmailIgnoreCase(@Param("email") String email);

    List<AppUserEntity> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);
}
