package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.AppCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppCatalogRepository extends JpaRepository<AppCatalogEntity, Long> {

    @Query("""
            select a from AppCatalogEntity a
            where lower(a.appIdentifier) = lower(:appIdentifier)
            """)
    Optional<AppCatalogEntity> findByAppIdentifierIgnoreCase(@Param("appIdentifier") String appIdentifier);
}
