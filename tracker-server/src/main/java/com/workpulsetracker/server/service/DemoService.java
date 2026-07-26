package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.AccountType;
import com.workpulsetracker.server.domain.ActivitySampleEntity;
import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.repository.ActivitySampleRepository;
import com.workpulsetracker.server.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Service
public class DemoService {

    private static final Logger logger = LoggerFactory.getLogger(DemoService.class);
    private static final String schema = "public";
    private static final String DEMO_AGENT_VERSION = "1.2";
    private static final List<String> ACTIVE_APP_NAMES = List.of("IDEA", "Chrome", "Slack", "Excel");

    private final AppUserRepository appUserRepository;
    private final ActivitySampleRepository activitySampleRepository;

    public DemoService(
            AppUserRepository appUserRepository,
            ActivitySampleRepository activitySampleRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.activitySampleRepository = activitySampleRepository;
    }

    @Transactional
    public int simulateActivity(AppUserEntity currentUser) {
        List<AppUserEntity> targetUsers = resolveTargetUsers(currentUser);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Random random = new Random();
        List<ActivitySampleEntity> activitySamples = new ArrayList<>();

        targetUsers.forEach(appUserEntity -> {
            appUserEntity.setAgentInstalled(true);
            appUserEntity.setAgentVersion(DEMO_AGENT_VERSION);
            appUserRepository.save(appUserEntity);

            ACTIVE_APP_NAMES.stream()
                    .map(appName -> new ActivitySampleEntity(
                            UUID.randomUUID(),
                            appUserEntity.getId(),
                            appName,
                            300L + random.nextInt(7200),
                            false,
                            today
                    ))
                    .forEach(activitySamples::add);

            activitySamples.add(new ActivitySampleEntity(
                    UUID.randomUUID(),
                    appUserEntity.getId(),
                    "Idle",
                    300L + random.nextInt(3600),
                    true,
                    today
            ));
        });

        activitySampleRepository.saveAll(activitySamples);
        logger.info(
                "schema={} Simulated activity for {} users, inserted {} samples",
                schema,
                targetUsers.size(),
                activitySamples.size()
        );
        return activitySamples.size();
    }

    private List<AppUserEntity> resolveTargetUsers(AppUserEntity currentUser) {
        if (currentUser.getAccountType() == AccountType.ORGANIZATION
                && Objects.nonNull(currentUser.getOrganizationId())) {
            return appUserRepository.findByOrganizationIdOrderByCreatedAtAsc(currentUser.getOrganizationId());
        }
        return List.of(currentUser);
    }
}
