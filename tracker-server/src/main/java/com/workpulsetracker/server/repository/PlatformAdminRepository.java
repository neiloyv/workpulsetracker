package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.PlatformAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdminEntity, Long> {

    @Query("select p from PlatformAdminEntity p where lower(p.email) = lower(:email)")
    Optional<PlatformAdminEntity> findByEmailIgnoreCase(@Param("email") String email);
}
