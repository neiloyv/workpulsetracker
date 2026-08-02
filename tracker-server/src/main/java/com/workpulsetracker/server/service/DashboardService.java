package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.BranchEntity;
import com.workpulsetracker.server.enums.DashboardPeriod;
import com.workpulsetracker.server.domain.DepartmentEntity;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.enums.OrganizationType;
import com.workpulsetracker.server.domain.UserAccountEntity;
import com.workpulsetracker.server.enums.UserRole;
import com.workpulsetracker.server.domain.WorkerEntity;
import com.workpulsetracker.server.repository.ActivitySampleRepository;
import com.workpulsetracker.server.repository.BranchRepository;
import com.workpulsetracker.server.repository.DepartmentRepository;
import com.workpulsetracker.server.repository.WorkerRepository;
import com.workpulsetracker.server.web.dto.AppUsageResponse;
import com.workpulsetracker.server.web.dto.DashboardWorkerResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final String schema = "public";

    private final OrganizationService organizationService;
    private final WorkerRepository workerRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final ActivitySampleRepository activitySampleRepository;

    public DashboardService(
            OrganizationService organizationService,
            WorkerRepository workerRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            ActivitySampleRepository activitySampleRepository
    ) {
        this.organizationService = organizationService;
        this.workerRepository = workerRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.activitySampleRepository = activitySampleRepository;
    }

    @Transactional(readOnly = true)
    public List<DashboardWorkerResponse> getDashboard(
            UserAccountEntity currentUser,
            String search,
            Long branchId,
            Long departmentId
    ) {
        OrganizationEntity organizationEntity = organizationService.requireOrganization(currentUser);
        List<WorkerEntity> workers = resolveWorkers(currentUser, organizationEntity, search, branchId, departmentId);
        if (workers.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<DashboardPeriod, LocalDate> periodStarts = resolvePeriodStarts(today);
        List<Long> workerIds = workers.stream().map(WorkerEntity::getId).collect(Collectors.toList());

        Map<Long, Long> todaySecondsByWorkerId = sumSecondsMap(workerIds, periodStarts.get(DashboardPeriod.TODAY), today);
        Map<Long, Long> weekSecondsByWorkerId = sumSecondsMap(workerIds, periodStarts.get(DashboardPeriod.WEEK), today);
        Map<Long, Long> monthSecondsByWorkerId = sumSecondsMap(workerIds, periodStarts.get(DashboardPeriod.MONTH), today);
        Map<Long, Long> yearSecondsByWorkerId = sumSecondsMap(workerIds, periodStarts.get(DashboardPeriod.YEAR), today);

        Map<Long, String> branchNamesById = loadBranchNames(workers);
        Map<Long, String> departmentNamesById = loadDepartmentNames(workers);

        logger.info("schema={} Built dashboard for {} workers", schema, workers.size());
        return workers.stream()
                .map(worker -> new DashboardWorkerResponse(
                        worker.getId(),
                        worker.getDisplayName(),
                        worker.getEmail(),
                        departmentNamesById.get(worker.getDepartmentId()),
                        branchNamesById.get(worker.getBranchId()),
                        todaySecondsByWorkerId.getOrDefault(worker.getId(), 0L),
                        weekSecondsByWorkerId.getOrDefault(worker.getId(), 0L),
                        monthSecondsByWorkerId.getOrDefault(worker.getId(), 0L),
                        yearSecondsByWorkerId.getOrDefault(worker.getId(), 0L),
                        worker.isAgentInstalled(),
                        worker.getAgentVersion()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppUsageResponse> getWorkerApps(
            UserAccountEntity currentUser,
            Long workerId,
            DashboardPeriod period
    ) {
        WorkerEntity targetWorker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        assertCanViewWorker(currentUser, targetWorker);

        DashboardPeriod resolvedPeriod = Objects.nonNull(period) ? period : DashboardPeriod.TODAY;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate = resolvePeriodStarts(today).get(resolvedPeriod);

        List<ActivitySampleRepository.AppSecondsAggregate> aggregates =
                activitySampleRepository.sumSecondsByAppForWorkerAndDateRange(workerId, fromDate, today);

        long totalSeconds = aggregates.stream()
                .map(ActivitySampleRepository.AppSecondsAggregate::getTotalSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        return aggregates.stream()
                .map(aggregate -> {
                    long seconds = Objects.requireNonNullElse(aggregate.getTotalSeconds(), 0L);
                    double percent = totalSeconds == 0L ? 0.0 : (seconds * 100.0) / totalSeconds;
                    return new AppUsageResponse(
                            aggregate.getAppName(),
                            seconds,
                            Boolean.TRUE.equals(aggregate.getIdle()),
                            Math.round(percent * 10.0) / 10.0
                    );
                })
                .collect(Collectors.toList());
    }

    private List<WorkerEntity> resolveWorkers(
            UserAccountEntity currentUser,
            OrganizationEntity organizationEntity,
            String search,
            Long branchId,
            Long departmentId
    ) {
        if (organizationEntity.getType() == OrganizationType.INDIVIDUAL || currentUser.getRole() == UserRole.WORKER) {
            return resolveOwnWorker(currentUser);
        }
        List<WorkerEntity> organizationWorkers = findOrganizationWorkers(organizationEntity.getId(), branchId, departmentId);
        String normalizedSearch = StringUtils.isBlank(search) ? null : search.trim().toLowerCase();
        return organizationWorkers.stream()
                .filter(workerEntity -> matchesSearch(workerEntity, normalizedSearch))
                .collect(Collectors.toList());
    }

    private List<WorkerEntity> resolveOwnWorker(UserAccountEntity currentUser) {
        if (Objects.isNull(currentUser.getWorkerId())) {
            return List.of();
        }
        return workerRepository.findById(currentUser.getWorkerId())
                .map(List::of)
                .orElse(List.of());
    }

    private List<WorkerEntity> findOrganizationWorkers(Long organizationId, Long branchId, Long departmentId) {
        if (Objects.nonNull(branchId) && Objects.nonNull(departmentId)) {
            return workerRepository.findByOrganizationIdAndBranchIdAndDepartmentIdOrderByCreatedAtAsc(
                    organizationId,
                    branchId,
                    departmentId
            );
        }
        if (Objects.nonNull(branchId)) {
            return workerRepository.findByOrganizationIdAndBranchIdOrderByCreatedAtAsc(organizationId, branchId);
        }
        if (Objects.nonNull(departmentId)) {
            return workerRepository.findByOrganizationIdAndDepartmentIdOrderByCreatedAtAsc(organizationId, departmentId);
        }
        return workerRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId);
    }

    private static boolean matchesSearch(WorkerEntity workerEntity, String normalizedSearch) {
        if (StringUtils.isBlank(normalizedSearch)) {
            return true;
        }
        String displayName = StringUtils.defaultString(workerEntity.getDisplayName()).toLowerCase();
        String email = StringUtils.defaultString(workerEntity.getEmail()).toLowerCase();
        return displayName.contains(normalizedSearch) || email.contains(normalizedSearch);
    }

    private void assertCanViewWorker(UserAccountEntity currentUser, WorkerEntity targetWorker) {
        if (Objects.equals(currentUser.getWorkerId(), targetWorker.getId())) {
            return;
        }
        boolean isOwnerOrManager = currentUser.getRole() == UserRole.OWNER || currentUser.getRole() == UserRole.MANAGER;
        if (isOwnerOrManager && Objects.equals(currentUser.getOrganizationId(), targetWorker.getOrganizationId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view activity for this worker");
    }

    private Map<Long, Long> sumSecondsMap(List<Long> workerIds, LocalDate fromDate, LocalDate toDate) {
        return activitySampleRepository.sumSecondsByWorkerIdsAndDateRange(workerIds, fromDate, toDate).stream()
                .collect(Collectors.toMap(
                        ActivitySampleRepository.WorkerSecondsAggregate::getWorkerId,
                        aggregate -> Objects.requireNonNullElse(aggregate.getTotalSeconds(), 0L)
                ));
    }

    private Map<Long, String> loadBranchNames(List<WorkerEntity> workers) {
        List<Long> branchIds = workers.stream()
                .map(WorkerEntity::getBranchId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (branchIds.isEmpty()) {
            return Map.of();
        }
        return branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getName));
    }

    private Map<Long, String> loadDepartmentNames(List<WorkerEntity> workers) {
        List<Long> departmentIds = workers.stream()
                .map(WorkerEntity::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (departmentIds.isEmpty()) {
            return Map.of();
        }
        return departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(DepartmentEntity::getId, DepartmentEntity::getName));
    }

    private static Map<DashboardPeriod, LocalDate> resolvePeriodStarts(LocalDate today) {
        Map<DashboardPeriod, LocalDate> periodStarts = new EnumMap<>(DashboardPeriod.class);
        periodStarts.put(DashboardPeriod.TODAY, today);
        periodStarts.put(DashboardPeriod.WEEK, today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        periodStarts.put(DashboardPeriod.MONTH, today.withDayOfMonth(1));
        periodStarts.put(DashboardPeriod.YEAR, today.withDayOfYear(1));
        return periodStarts;
    }
}
