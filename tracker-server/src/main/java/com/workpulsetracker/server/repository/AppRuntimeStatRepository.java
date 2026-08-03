package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.AppRuntimeStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppRuntimeStatRepository extends JpaRepository<AppRuntimeStatEntity, Long> {

    @Query("""
            select s from AppRuntimeStatEntity s
            where s.workerId = :workerId
              and s.deviceId = :deviceId
              and lower(s.appIdentifier) = lower(:appIdentifier)
            """)
    Optional<AppRuntimeStatEntity> findByWorkerIdAndDeviceIdAndAppIdentifierIgnoreCase(
            @Param("workerId") Long workerId,
            @Param("deviceId") Long deviceId,
            @Param("appIdentifier") String appIdentifier
    );

    List<AppRuntimeStatEntity> findByWorkerId(Long workerId);

    List<AppRuntimeStatEntity> findByWorkerIdAndDeviceId(Long workerId, Long deviceId);

    @Query("""
            select s.appIdentifier as appIdentifier, sum(s.totalSeconds) as totalSeconds
            from AppRuntimeStatEntity s
            where s.workerId = :workerId
            group by s.appIdentifier
            order by sum(s.totalSeconds) desc
            """)
    List<AppTotalAggregate> sumTotalSecondsByWorkerIdGroupedByApp(@Param("workerId") Long workerId);

    interface AppTotalAggregate {
        String getAppIdentifier();

        Long getTotalSeconds();
    }
}
