package com.workpulsetracker.server.service;

import com.workpulsetracker.server.domain.BranchEntity;
import com.workpulsetracker.server.domain.DepartmentEntity;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.domain.SubscriptionEntity;
import com.workpulsetracker.server.domain.UserAccountEntity;
import com.workpulsetracker.server.domain.WorkerEntity;
import com.workpulsetracker.server.enums.EntityStatus;
import com.workpulsetracker.server.enums.OrganizationStatus;
import com.workpulsetracker.server.enums.OrganizationType;
import com.workpulsetracker.server.enums.SubscriptionPlan;
import com.workpulsetracker.server.enums.SubscriptionStatus;
import com.workpulsetracker.server.enums.UserRole;
import com.workpulsetracker.server.repository.BranchRepository;
import com.workpulsetracker.server.repository.DepartmentRepository;
import com.workpulsetracker.server.repository.OrganizationRepository;
import com.workpulsetracker.server.repository.SubscriptionRepository;
import com.workpulsetracker.server.repository.UserAccountRepository;
import com.workpulsetracker.server.repository.WorkerRepository;
import com.workpulsetracker.server.security.UserAccountPrincipal;
import com.workpulsetracker.server.util.AccessKeyGenerator;
import com.workpulsetracker.server.web.dto.LoginRequest;
import com.workpulsetracker.server.web.dto.MeResponse;
import com.workpulsetracker.server.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class AuthUserService {

    private static final Logger logger = LoggerFactory.getLogger(AuthUserService.class);
    private static final String schema = "public";

    private static final String DEFAULT_COMPANY_BRANCH_NAME = "Главный офис";
    private static final String DEFAULT_COMPANY_DEPARTMENT_NAME = "Общий";
    private static final String DEFAULT_INDIVIDUAL_BRANCH_NAME = "Personal";
    private static final String DEFAULT_INDIVIDUAL_DEPARTMENT_NAME = "Personal";
    private static final int FREE_PLAN_MAX_PERSONS = 5;
    private static final int FREE_PLAN_MAX_BRANCHES = 1;

    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkerRepository workerRepository;
    private final AccessKeyEmailService accessKeyEmailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final OrganizationService organizationService;

    public AuthUserService(
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            SubscriptionRepository subscriptionRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            WorkerRepository workerRepository,
            AccessKeyEmailService accessKeyEmailService,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            OrganizationService organizationService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.workerRepository = workerRepository;
        this.accessKeyEmailService = accessKeyEmailService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.organizationService = organizationService;
    }

    @Transactional
    public MeResponse register(
            RegisterRequest registerRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        String email = registerRequest.email().trim().toLowerCase();
        if (userAccountRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(registerRequest.password());
        String displayName = registerRequest.displayName().trim();
        OffsetDateTime now = OffsetDateTime.now();

        UserAccountEntity createdUserAccount = registerRequest.organizationType() == OrganizationType.COMPANY
                ? registerCompany(registerRequest, email, passwordHash, displayName, now)
                : registerIndividual(email, passwordHash, displayName, now);

        authenticateAndPersistSession(email, registerRequest.password(), httpServletRequest, httpServletResponse);
        return organizationService.getMe(createdUserAccount);
    }

    private UserAccountEntity registerCompany(
            RegisterRequest registerRequest,
            String email,
            String passwordHash,
            String displayName,
            OffsetDateTime now
    ) {
        if (StringUtils.isBlank(registerRequest.companyName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyName is required for company organization");
        }

        OrganizationEntity organizationEntity = organizationRepository.save(new OrganizationEntity(
                OrganizationType.COMPANY,
                registerRequest.companyName().trim(),
                OrganizationStatus.ACTIVE,
                now,
                now
        ));
        createFreeSubscription(organizationEntity.getId(), now);

        BranchEntity branchEntity = branchRepository.save(new BranchEntity(
                organizationEntity.getId(),
                DEFAULT_COMPANY_BRANCH_NAME,
                true,
                now
        ));
        departmentRepository.save(new DepartmentEntity(
                organizationEntity.getId(),
                branchEntity.getId(),
                DEFAULT_COMPANY_DEPARTMENT_NAME,
                true,
                now
        ));

        UserAccountEntity userAccountEntity = new UserAccountEntity(
                organizationEntity.getId(),
                email,
                passwordHash,
                displayName,
                UserRole.OWNER,
                null,
                EntityStatus.ACTIVE,
                now
        );
        userAccountRepository.save(userAccountEntity);

        logger.info(
                "schema={} Registered company account {} for organization {}",
                schema,
                email,
                organizationEntity.getName()
        );
        return userAccountEntity;
    }

    private UserAccountEntity registerIndividual(
            String email,
            String passwordHash,
            String displayName,
            OffsetDateTime now
    ) {
        OrganizationEntity organizationEntity = organizationRepository.save(new OrganizationEntity(
                OrganizationType.INDIVIDUAL,
                displayName,
                OrganizationStatus.ACTIVE,
                now,
                now
        ));
        createFreeSubscription(organizationEntity.getId(), now);

        BranchEntity branchEntity = branchRepository.save(new BranchEntity(
                organizationEntity.getId(),
                DEFAULT_INDIVIDUAL_BRANCH_NAME,
                true,
                now
        ));
        DepartmentEntity departmentEntity = departmentRepository.save(new DepartmentEntity(
                organizationEntity.getId(),
                branchEntity.getId(),
                DEFAULT_INDIVIDUAL_DEPARTMENT_NAME,
                true,
                now
        ));

        String plaintextAccessKey = AccessKeyGenerator.generatePlaintextKey();
        WorkerEntity workerEntity = workerRepository.save(new WorkerEntity(
                organizationEntity.getId(),
                branchEntity.getId(),
                departmentEntity.getId(),
                displayName,
                email,
                plaintextAccessKey,
                AccessKeyGenerator.prefixOf(plaintextAccessKey),
                EntityStatus.ACTIVE,
                false,
                null,
                now
        ));
        accessKeyEmailService.sendAccessKey(email, displayName, plaintextAccessKey);

        UserAccountEntity userAccountEntity = new UserAccountEntity(
                organizationEntity.getId(),
                email,
                passwordHash,
                displayName,
                UserRole.OWNER,
                workerEntity.getId(),
                EntityStatus.ACTIVE,
                now
        );
        userAccountRepository.save(userAccountEntity);

        logger.info("schema={} Registered individual account {}", schema, email);
        return userAccountEntity;
    }

    private void createFreeSubscription(Long organizationId, OffsetDateTime startsAt) {
        subscriptionRepository.save(new SubscriptionEntity(
                organizationId,
                SubscriptionPlan.FREE,
                SubscriptionStatus.ACTIVE,
                startsAt,
                null,
                FREE_PLAN_MAX_PERSONS,
                FREE_PLAN_MAX_BRANCHES
        ));
    }

    @Transactional(readOnly = true)
    public MeResponse login(
            LoginRequest loginRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        String email = loginRequest.email().trim().toLowerCase();
        authenticateAndPersistSession(email, loginRequest.password(), httpServletRequest, httpServletResponse);
        UserAccountEntity userAccountEntity = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (userAccountEntity.getStatus() != EntityStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        logger.info("schema={} User logged in {}", schema, email);
        return organizationService.getMe(userAccountEntity);
    }

    public UserAccountEntity requireCurrentUser(Authentication authentication) {
        if (Objects.isNull(authentication) || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserAccountPrincipal userAccountPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        UserAccountEntity userAccountEntity = userAccountRepository.findById(userAccountPrincipal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user was not found"));
        if (userAccountEntity.getStatus() != EntityStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        return userAccountEntity;
    }

    private void authenticateAndPersistSession(
            String email,
            String password,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);
    }
}
