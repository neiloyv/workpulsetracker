package com.timetracker.server.web;

import com.timetracker.server.config.AppProperties;
import com.timetracker.server.domain.AppUserEntity;
import com.timetracker.server.security.AuthUserService;
import com.timetracker.server.service.OrganizationService;
import com.timetracker.server.web.dto.CreateUserRequest;
import com.timetracker.server.web.dto.CreateUserResponse;
import com.timetracker.server.web.dto.DownloadsResponse;
import com.timetracker.server.web.dto.MeResponse;
import com.timetracker.server.web.dto.OnboardingRequest;
import com.timetracker.server.web.dto.OrganizationResponse;
import com.timetracker.server.web.dto.OrganizationStatsResponse;
import com.timetracker.server.web.dto.OrganizationUserResponse;
import com.timetracker.server.web.dto.UpdateSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthUserService authUserService;
    private final OrganizationService organizationService;
    private final AppProperties appProperties;

    public ApiController(
            AuthUserService authUserService,
            OrganizationService organizationService,
            AppProperties appProperties
    ) {
        this.authUserService = authUserService;
        this.organizationService = organizationService;
        this.appProperties = appProperties;
    }

    @GetMapping("/me")
    public MeResponse me(OAuth2AuthenticationToken authenticationToken) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authenticationToken);
        return organizationService.getMe(currentUser);
    }

    @PostMapping("/onboarding")
    public MeResponse onboarding(
            OAuth2AuthenticationToken authenticationToken,
            @Valid @RequestBody OnboardingRequest onboardingRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authenticationToken);
        return organizationService.completeOnboarding(currentUser, onboardingRequest);
    }

    @GetMapping("/organization")
    public OrganizationResponse organization(OAuth2AuthenticationToken authenticationToken) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authenticationToken);
        return organizationService.getOrganization(currentUser);
    }

    @GetMapping("/organization/users")
    public List<OrganizationUserResponse> users(OAuth2AuthenticationToken authenticationToken) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authenticationToken);
        return organizationService.listUsers(currentUser);
    }

    @PostMapping("/organization/users")
    public CreateUserResponse createUser(
            OAuth2AuthenticationToken authenticationToken,
            @Valid @RequestBody CreateUserRequest createUserRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authenticationToken);
        return organizationService.createUser(currentUser, createUserRequest);
    }

    @GetMapping("/organization/settings")
    public Map<String, String> settings(OAuth2AuthenticationToken authenticationToken) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authenticationToken);
        return organizationService.getSettings(currentUser);
    }

    @PutMapping("/organization/settings")
    public Map<String, String> updateSettings(
            OAuth2AuthenticationToken authenticationToken,
            @RequestBody UpdateSettingsRequest updateSettingsRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authenticationToken);
        Map<String, String> settings = Objects.nonNull(updateSettingsRequest)
                ? updateSettingsRequest.settings()
                : Map.of();
        return organizationService.updateSettings(currentUser, settings);
    }

    @GetMapping("/organization/stats")
    public OrganizationStatsResponse stats(OAuth2AuthenticationToken authenticationToken) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authenticationToken);
        return organizationService.getStats(currentUser);
    }

    @GetMapping("/downloads")
    public DownloadsResponse downloads() {
        return new DownloadsResponse(
                appProperties.getDownload().getWindowsUrl(),
                appProperties.getDownload().getMacosUrl(),
                appProperties.getDownload().getLinuxUrl()
        );
    }
}
