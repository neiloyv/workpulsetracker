package com.workpulsetracker.server.web;

import com.workpulsetracker.server.config.AppProperties;
import com.workpulsetracker.server.domain.AppUserEntity;
import com.workpulsetracker.server.domain.DashboardPeriod;
import com.workpulsetracker.server.security.AuthUserService;
import com.workpulsetracker.server.service.DashboardService;
import com.workpulsetracker.server.service.DemoService;
import com.workpulsetracker.server.service.EmployeeService;
import com.workpulsetracker.server.service.OrganizationService;
import com.workpulsetracker.server.service.StructureService;
import com.workpulsetracker.server.web.dto.AppUsageResponse;
import com.workpulsetracker.server.web.dto.CreateBranchRequest;
import com.workpulsetracker.server.web.dto.CreateDepartmentRequest;
import com.workpulsetracker.server.web.dto.CreateEmployeeRequest;
import com.workpulsetracker.server.web.dto.CreateEmployeeResponse;
import com.workpulsetracker.server.web.dto.CreateUserRequest;
import com.workpulsetracker.server.web.dto.CreateUserResponse;
import com.workpulsetracker.server.web.dto.DashboardWorkerResponse;
import com.workpulsetracker.server.web.dto.DownloadsResponse;
import com.workpulsetracker.server.web.dto.EmployeeResponse;
import com.workpulsetracker.server.web.dto.MeResponse;
import com.workpulsetracker.server.web.dto.OrganizationResponse;
import com.workpulsetracker.server.web.dto.OrganizationStatsResponse;
import com.workpulsetracker.server.web.dto.OrganizationUserResponse;
import com.workpulsetracker.server.web.dto.StructureResponse;
import com.workpulsetracker.server.web.dto.UpdateEmployeeRequest;
import com.workpulsetracker.server.web.dto.UpdateSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthUserService authUserService;
    private final OrganizationService organizationService;
    private final StructureService structureService;
    private final EmployeeService employeeService;
    private final DashboardService dashboardService;
    private final DemoService demoService;
    private final AppProperties appProperties;

    public ApiController(
            AuthUserService authUserService,
            OrganizationService organizationService,
            StructureService structureService,
            EmployeeService employeeService,
            DashboardService dashboardService,
            DemoService demoService,
            AppProperties appProperties
    ) {
        this.authUserService = authUserService;
        this.organizationService = organizationService;
        this.structureService = structureService;
        this.employeeService = employeeService;
        this.dashboardService = dashboardService;
        this.demoService = demoService;
        this.appProperties = appProperties;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.getMe(currentUser);
    }

    @GetMapping("/structure")
    public StructureResponse structure(Authentication authentication) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return structureService.getStructure(currentUser);
    }

    @PostMapping("/structure/branches")
    public StructureResponse.BranchNode createBranch(
            Authentication authentication,
            @Valid @RequestBody CreateBranchRequest createBranchRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return structureService.createBranch(currentUser, createBranchRequest);
    }

    @PostMapping("/structure/departments")
    public StructureResponse.DepartmentNode createDepartment(
            Authentication authentication,
            @Valid @RequestBody CreateDepartmentRequest createDepartmentRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return structureService.createDepartment(currentUser, createDepartmentRequest);
    }

    @GetMapping("/employees")
    public List<EmployeeResponse> employees(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID branchId
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return employeeService.listEmployees(currentUser, search, branchId, departmentId);
    }

    @PostMapping("/employees")
    public CreateEmployeeResponse createEmployee(
            Authentication authentication,
            @Valid @RequestBody CreateEmployeeRequest createEmployeeRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return employeeService.createEmployee(currentUser, createEmployeeRequest);
    }

    @PutMapping("/employees/{id}")
    public EmployeeResponse updateEmployee(
            Authentication authentication,
            @PathVariable("id") UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest updateEmployeeRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return employeeService.updateEmployee(currentUser, employeeId, updateEmployeeRequest);
    }

    @GetMapping("/dashboard")
    public List<DashboardWorkerResponse> dashboard(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID branchId
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return dashboardService.getDashboard(currentUser, search, branchId, departmentId);
    }

    @GetMapping("/dashboard/users/{userId}/apps")
    public List<AppUsageResponse> dashboardUserApps(
            Authentication authentication,
            @PathVariable UUID userId,
            @RequestParam(required = false, defaultValue = "TODAY") DashboardPeriod period
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return dashboardService.getUserApps(currentUser, userId, period);
    }

    @PostMapping("/demo/simulate-activity")
    public Map<String, Object> simulateActivity(Authentication authentication) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        int insertedSamples = demoService.simulateActivity(currentUser);
        return Map.of("insertedSamples", insertedSamples);
    }

    @GetMapping("/organization")
    public OrganizationResponse organization(Authentication authentication) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.getOrganization(currentUser);
    }

    @GetMapping("/organization/users")
    public List<OrganizationUserResponse> users(Authentication authentication) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.listUsers(currentUser);
    }

    @PostMapping("/organization/users")
    public CreateUserResponse createUser(
            Authentication authentication,
            @Valid @RequestBody CreateUserRequest createUserRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.createUser(currentUser, createUserRequest);
    }

    @GetMapping("/organization/settings")
    public Map<String, String> settings(Authentication authentication) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.getSettings(currentUser);
    }

    @PutMapping("/organization/settings")
    public Map<String, String> updateSettings(
            Authentication authentication,
            @RequestBody UpdateSettingsRequest updateSettingsRequest
    ) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
        Map<String, String> settings = Objects.nonNull(updateSettingsRequest)
                ? updateSettingsRequest.settings()
                : Map.of();
        return organizationService.updateSettings(currentUser, settings);
    }

    @GetMapping("/organization/stats")
    public OrganizationStatsResponse stats(Authentication authentication) {
        AppUserEntity currentUser = authUserService.requireCurrentUser(authentication);
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
