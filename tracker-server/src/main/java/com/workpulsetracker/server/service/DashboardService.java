package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.AccountType;
import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.domain.BranchEntity;
import com.workpulsetracker.server.domain.DashboardPeriod;
import com.workpulsetracker.server.domain.DepartmentEntity;
import com.workpulsetracker.server.repository.ActivitySampleRepository;
import com.workpulsetracker.server.repository.AppUserRepository;
import com.workpulsetracker.server.repository.BranchRepository;
import com.workpulsetracker.server.repository.DepartmentRepository;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final String schema = "public";

    private final AppUserRepository appUserRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final ActivitySampleRepository activitySampleRepository;

    public DashboardService(
            AppUserRepository appUserRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            ActivitySampleRepository activitySampleRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.activitySampleRepository = activitySampleRepository;
    }

    @Transactional(readOnly = true)
    public List<DashboardWorkerResponse> getDashboard(
            AppUserEntity currentUser,
            String search,
            UUID branchId,
            UUID departmentId
    ) {
        List<AppUserEntity> workers = resolveWorkers(currentUser, search, branchId, departmentId);
        if (workers.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<DashboardPeriod, LocalDate> periodStarts = resolvePeriodStarts(today);
        List<UUID> workerIds = workers.stream().map(AppUserEntity::getId).collect(Collectors.toList());

        Map<UUID, Long> todaySecondsByUserId = sumSecondsMap(workerIds, periodStarts.get(DashboardPeriod.TODAY), today);
        Map<UUID, Long> weekSecondsByUserId = sumSecondsMap(workerIds, periodStarts.get(DashboardPeriod.WEEK), today);
        Map<UUID, Long> monthSecondsByUserId = sumSecondsMap(workerIds, periodStarts.get(DashboardPeriod.MONTH), today);
        Map<UUID, Long> yearSecondsByUserId = sumSecondsMap(workerIds, periodStarts.get(DashboardPeriod.YEAR), today);

        Map<UUID, String> branchNamesById = loadBranchNames(workers);
        Map<UUID, String> departmentNamesById = loadDepartmentNames(workers);

        logger.info("schema={} Built dashboard for {} workers", schema, workers.size());
        return workers.stream()
                .map(worker -> new DashboardWorkerResponse(
                        worker.getId(),
                        worker.getDisplayName(),
                        worker.getEmail(),
                        Objects.nonNull(worker.getDepartmentId())
                                ? departmentNamesById.get(worker.getDepartmentId())
                                : null,
                        Objects.nonNull(worker.getBranchId())
                                ? branchNamesById.get(worker.getBranchId())
                                : null,
                        todaySecondsByUserId.getOrDefault(worker.getId(), 0L),
                        weekSecondsByUserId.getOrDefault(worker.getId(), 0L),
                        monthSecondsByUserId.getOrDefault(worker.getId(), 0L),
                        yearSecondsByUserId.getOrDefault(worker.getId(), 0L),
                        worker.isAgentInstalled(),
                        worker.getAgentVersion()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppUsageResponse> getUserApps(
            AppUserEntity currentUser,
            UUID userId,
            DashboardPeriod period
    ) {
        AppUserEntity targetUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        assertCanViewUser(currentUser, targetUser);

        DashboardPeriod resolvedPeriod = Objects.nonNull(period) ? period : DashboardPeriod.TODAY;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate = resolvePeriodStarts(today).get(resolvedPeriod);

        List<ActivitySampleRepository.AppSecondsAggregate> aggregates =
                activitySampleRepository.sumSecondsByAppForUserAndDateRange(userId, fromDate, today);

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

    private List<AppUserEntity> resolveWorkers(
            AppUserEntity currentUser,
            String search,
            UUID branchId,
            UUID departmentId
    ) {
        if (currentUser.getAccountType() == AccountType.PERSONAL
                || Objects.isNull(currentUser.getOrganizationId())) {
            return List.of(currentUser);
        }
        List<AppUserEntity> organizationUsers = resolveOrganizationUsers(
                currentUser.getOrganizationId(),
                branchId,
                departmentId
        );
        String normalizedSearch = StringUtils.isBlank(search) ? null : search.trim().toLowerCase();
        return organizationUsers.stream()
                .filter(appUserEntity -> matchesSearch(appUserEntity, normalizedSearch))
                .collect(Collectors.toList());
    }

    private List<AppUserEntity> resolveOrganizationUsers(
            UUID organizationId,
            UUID branchId,
            UUID departmentId
    ) {
        if (Objects.nonNull(branchId) && Objects.nonNull(departmentId)) {
            return appUserRepository.findByOrganizationIdAndBranchIdAndDepartmentIdOrderByCreatedAtAsc(
                    organizationId,
                    branchId,
                    departmentId
            );
        }
        if (Objects.nonNull(branchId)) {
            return appUserRepository.findByOrganizationIdAndBranchIdOrderByCreatedAtAsc(organizationId, branchId);
        }
        if (Objects.nonNull(departmentId)) {
            return appUserRepository.findByOrganizationIdAndDepartmentIdOrderByCreatedAtAsc(
                    organizationId,
                    departmentId
            );
        }
        return appUserRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId);
    }

    private static boolean matchesSearch(AppUserEntity appUserEntity, String normalizedSearch) {
        if (StringUtils.isBlank(normalizedSearch)) {
            return true;
        }
        String displayName = StringUtils.defaultString(appUserEntity.getDisplayName()).toLowerCase();
        String email = StringUtils.defaultString(appUserEntity.getEmail()).toLowerCase();
        return displayName.contains(normalizedSearch) || email.contains(normalizedSearch);
    }

    private void assertCanViewUser(AppUserEntity currentUser, AppUserEntity targetUser) {
        if (Objects.equals(currentUser.getId(), targetUser.getId())) {
            return;
        }
        if (currentUser.getAccountType() == AccountType.ORGANIZATION
                && Objects.nonNull(currentUser.getOrganizationId())
                && Objects.equals(currentUser.getOrganizationId(), targetUser.getOrganizationId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view activity for this user");
    }

    private Map<UUID, Long> sumSecondsMap(List<UUID> userIds, LocalDate fromDate, LocalDate toDate) {
        return activitySampleRepository.sumSecondsByUserIdsAndDateRange(userIds, fromDate, toDate).stream()
                .collect(Collectors.toMap(
                        ActivitySampleRepository.UserSecondsAggregate::getUserId,
                        aggregate -> Objects.requireNonNullElse(aggregate.getTotalSeconds(), 0L)
                ));
    }

    private Map<UUID, String> loadBranchNames(List<AppUserEntity> workers) {
        List<UUID> branchIds = workers.stream()
                .map(AppUserEntity::getBranchId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (branchIds.isEmpty()) {
            return Map.of();
        }
        return branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getName));
    }

    private Map<UUID, String> loadDepartmentNames(List<AppUserEntity> workers) {
        List<UUID> departmentIds = workers.stream()
                .map(AppUserEntity::getDepartmentId)
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
