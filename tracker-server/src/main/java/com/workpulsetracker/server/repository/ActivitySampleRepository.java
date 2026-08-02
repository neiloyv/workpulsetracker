package com.workpulsetracker.server.repository;

import com.workpulsetracker.server.domain.ActivitySampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ActivitySampleRepository extends JpaRepository<ActivitySampleEntity, Long> {

    List<ActivitySampleEntity> findByWorkerIdAndActivityDateBetween(
            Long workerId,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<ActivitySampleEntity> findByWorkerIdInAndActivityDateBetween(
            Collection<Long> workerIds,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            select a.workerId as workerId, sum(a.seconds) as totalSeconds
            from ActivitySampleEntity a
            where a.workerId in :workerIds
              and a.activityDate between :fromDate and :toDate
            group by a.workerId
            """)
    List<WorkerSecondsAggregate> sumSecondsByWorkerIdsAndDateRange(
            @Param("workerIds") Collection<Long> workerIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
            select a.appName as appName, a.idle as idle, sum(a.seconds) as totalSeconds
            from ActivitySampleEntity a
            where a.workerId = :workerId
              and a.activityDate between :fromDate and :toDate
            group by a.appName, a.idle
            order by sum(a.seconds) desc
            """)
    List<AppSecondsAggregate> sumSecondsByAppForWorkerAndDateRange(
            @Param("workerId") Long workerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    interface WorkerSecondsAggregate {
        Long getWorkerId();

        Long getTotalSeconds();
    }

    interface AppSecondsAggregate {
        String getAppName();

        Boolean getIdle();

        Long getTotalSeconds();
    }
}
