package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.UserAccountEntity;
import com.workpulsetracker.server.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {

    @Query("select u from UserAccountEntity u where lower(u.email) = lower(:email)")
    Optional<UserAccountEntity> findByEmailIgnoreCase(@Param("email") String email);

    List<UserAccountEntity> findByOrganizationIdOrderByCreatedAtAsc(Long organizationId);

    List<UserAccountEntity> findByOrganizationIdAndRoleOrderByCreatedAtAsc(Long organizationId, UserRole role);

    Optional<UserAccountEntity> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<UserAccountEntity> findByWorkerId(Long workerId);
}
