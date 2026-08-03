package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.DeviceEntity;
import com.workpulsetracker.server.domain.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    Optional<DeviceEntity> findByWorkerIdAndHardwareId(Long workerId, String hardwareId);

    List<DeviceEntity> findByWorkerIdAndStatus(Long workerId, EntityStatus status);

    long countByWorkerIdAndStatus(Long workerId, EntityStatus status);

    Optional<DeviceEntity> findByIdAndWorkerId(Long id, Long workerId);
}
