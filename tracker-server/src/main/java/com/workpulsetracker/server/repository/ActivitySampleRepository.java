package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.ActivitySampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ActivitySampleRepository extends JpaRepository<ActivitySampleEntity, UUID> {

    List<ActivitySampleEntity> findByUserIdAndActivityDateBetween(
            UUID userId,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<ActivitySampleEntity> findByUserIdInAndActivityDateBetween(
            Collection<UUID> userIds,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            select a.userId as userId, sum(a.seconds) as totalSeconds
            from ActivitySampleEntity a
            where a.userId in :userIds
              and a.activityDate between :fromDate and :toDate
            group by a.userId
            """)
    List<UserSecondsAggregate> sumSecondsByUserIdsAndDateRange(
            @Param("userIds") Collection<UUID> userIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            select a.appName as appName, a.idle as idle, sum(a.seconds) as totalSeconds
            from ActivitySampleEntity a
            where a.userId = :userId
              and a.activityDate between :fromDate and :toDate
            group by a.appName, a.idle
            order by sum(a.seconds) desc
            """)
    List<AppSecondsAggregate> sumSecondsByAppForUserAndDateRange(
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    interface UserSecondsAggregate {
        UUID getUserId();

        Long getTotalSeconds();
    }

    interface AppSecondsAggregate {
        String getAppName();

        Boolean getIdle();

        Long getTotalSeconds();
    }
}
