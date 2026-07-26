package com.workpulsetracker.server.security;

import com.workpulsetracker.server.domain.AccountType;
import com.workpulsetracker.server.domain.AgentKeyEntity;
import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.domain.BranchEntity;
import com.workpulsetracker.server.domain.DepartmentEntity;
import com.workpulsetracker.server.domain.OrganizationEntity;
import com.workpulsetracker.server.domain.UserRole;
import com.workpulsetracker.server.repository.AgentKeyRepository;
import com.workpulsetracker.server.repository.AppUserRepository;
import com.workpulsetracker.server.repository.BranchRepository;
import com.workpulsetracker.server.repository.DepartmentRepository;
import com.workpulsetracker.server.repository.OrganizationRepository;
import com.workpulsetracker.server.service.AgentKeyGenerator;
import com.workpulsetracker.server.service.OrganizationService;
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
import java.util.UUID;

@Service
public class AuthUserService {

    private static final Logger logger = LoggerFactory.getLogger(AuthUserService.class);
    private static final String schema = "public";
    private static final String DEFAULT_BRANCH_NAME = "Главный офис";
    private static final String DEFAULT_DEPARTMENT_NAME = "Общий";

    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final AgentKeyRepository agentKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final OrganizationService organizationService;

    public AuthUserService(
            AppUserRepository appUserRepository,
            OrganizationRepository organizationRepository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            AgentKeyRepository agentKeyRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            OrganizationService organizationService
    ) {
        this.appUserRepository = appUserRepository;
        this.organizationRepository = organizationRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.agentKeyRepository = agentKeyRepository;
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
        if (appUserRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(registerRequest.password());
        String displayName = registerRequest.displayName().trim();
        OffsetDateTime createdAt = OffsetDateTime.now();
        AppUserEntity createdAppUser;

        if (registerRequest.accountType() == AccountType.PERSONAL) {
            createdAppUser = new AppUserEntity(
                    UUID.randomUUID(),
                    null,
                    email,
                    passwordHash,
                    displayName,
                    null,
                    null,
                    null,
                    AccountType.PERSONAL,
                    UserRole.OWNER,
                    null,
                    null,
                    false,
                    null,
                    true,
                    createdAt
            );
            appUserRepository.save(createdAppUser);
            issueAgentKey(createdAppUser.getId());
            appUserRepository.flush();
            logger.info("schema={} Registered personal account {}", schema, email);
        } else {
            if (StringUtils.isBlank(registerRequest.companyName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyName is required for organization account");
            }
            OrganizationEntity organizationEntity = organizationRepository.save(new OrganizationEntity(
                    UUID.randomUUID(),
                    registerRequest.companyName().trim(),
                    createdAt
            ));
            BranchEntity branchEntity = branchRepository.save(new BranchEntity(
                    UUID.randomUUID(),
                    organizationEntity.getId(),
                    DEFAULT_BRANCH_NAME,
                    createdAt
            ));
            DepartmentEntity departmentEntity = departmentRepository.save(new DepartmentEntity(
                    UUID.randomUUID(),
                    branchEntity.getId(),
                    DEFAULT_DEPARTMENT_NAME,
                    createdAt
            ));
            createdAppUser = new AppUserEntity(
                    UUID.randomUUID(),
                    organizationEntity.getId(),
                    email,
                    passwordHash,
                    displayName,
                    null,
                    null,
                    null,
                    AccountType.ORGANIZATION,
                    UserRole.OWNER,
                    branchEntity.getId(),
                    departmentEntity.getId(),
                    false,
                    null,
                    true,
                    createdAt
            );
            appUserRepository.save(createdAppUser);
            issueAgentKey(createdAppUser.getId());
            appUserRepository.flush();
            logger.info(
                    "schema={} Registered organization account {} for org {}",
                    schema,
                    email,
                    organizationEntity.getName()
            );
        }

        authenticateAndPersistSession(email, registerRequest.password(), httpServletRequest, httpServletResponse);
        return organizationService.getMe(createdAppUser);
    }

    @Transactional(readOnly = true)
    public MeResponse login(
            LoginRequest loginRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        String email = loginRequest.email().trim().toLowerCase();
        authenticateAndPersistSession(email, loginRequest.password(), httpServletRequest, httpServletResponse);
        AppUserEntity appUserEntity = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        logger.info("schema={} User logged in {}", schema, email);
        return organizationService.getMe(appUserEntity);
    }

    public AppUserEntity requireCurrentUser(Authentication authentication) {
        if (Objects.isNull(authentication) || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AppUserPrincipal appUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return appUserRepository.findById(appUserPrincipal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user was not found"));
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

    private void issueAgentKey(UUID userId) {
        String plaintextKey = AgentKeyGenerator.generatePlaintextKey();
        agentKeyRepository.save(new AgentKeyEntity(
                UUID.randomUUID(),
                userId,
                AgentKeyGenerator.hashKey(plaintextKey),
                AgentKeyGenerator.prefixOf(plaintextKey),
                OffsetDateTime.now(),
                null
        ));
    }
}
