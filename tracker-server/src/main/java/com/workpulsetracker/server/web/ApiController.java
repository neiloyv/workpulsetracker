package com.workpulsetracker.server.web;

import com.workpulsetracker.server.domain.DashboardPeriod;
import com.workpulsetracker.server.domain.UserAccountEntity;
import com.workpulsetracker.server.security.AuthUserService;
import com.workpulsetracker.server.service.DashboardService;
import com.workpulsetracker.server.service.DownloadsService;
import com.workpulsetracker.server.service.OrganizationService;
import com.workpulsetracker.server.service.StructureService;
import com.workpulsetracker.server.service.WorkerService;
import com.workpulsetracker.server.web.dto.AccessKeyResponse;
import com.workpulsetracker.server.web.dto.AgentInfoResponse;
import com.workpulsetracker.server.web.dto.AppUsageResponse;
import com.workpulsetracker.server.web.dto.CreateBranchRequest;
import com.workpulsetracker.server.web.dto.CreateDepartmentRequest;
import com.workpulsetracker.server.web.dto.CreateWorkerRequest;
import com.workpulsetracker.server.web.dto.CreateWorkerResponse;
import com.workpulsetracker.server.web.dto.DashboardWorkerResponse;
import com.workpulsetracker.server.web.dto.DownloadsResponse;
import com.workpulsetracker.server.web.dto.MeResponse;
import com.workpulsetracker.server.web.dto.OrganizationResponse;
import com.workpulsetracker.server.web.dto.OrganizationStatsResponse;
import com.workpulsetracker.server.web.dto.StructureResponse;
import com.workpulsetracker.server.web.dto.UpdateSettingsRequest;
import com.workpulsetracker.server.web.dto.UpdateWorkerRequest;
import com.workpulsetracker.server.web.dto.WorkerResponse;
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

@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthUserService authUserService;
    private final OrganizationService organizationService;
    private final StructureService structureService;
    private final WorkerService workerService;
    private final DashboardService dashboardService;
    private final DownloadsService downloadsService;

    public ApiController(
            AuthUserService authUserService,
            OrganizationService organizationService,
            StructureService structureService,
            WorkerService workerService,
            DashboardService dashboardService,
            DownloadsService downloadsService
    ) {
        this.authUserService = authUserService;
        this.organizationService = organizationService;
        this.structureService = structureService;
        this.workerService = workerService;
        this.dashboardService = dashboardService;
        this.downloadsService = downloadsService;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.getMe(currentUser);
    }

    @GetMapping("/structure")
    public StructureResponse structure(Authentication authentication) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return structureService.getStructure(currentUser);
    }

    @PostMapping("/structure/branches")
    public StructureResponse.BranchNode createBranch(
            Authentication authentication,
            @Valid @RequestBody CreateBranchRequest createBranchRequest
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return structureService.createBranch(currentUser, createBranchRequest);
    }

    @PostMapping("/structure/departments")
    public StructureResponse.DepartmentNode createDepartment(
            Authentication authentication,
            @Valid @RequestBody CreateDepartmentRequest createDepartmentRequest
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return structureService.createDepartment(currentUser, createDepartmentRequest);
    }

    @GetMapping("/workers")
    public List<WorkerResponse> workers(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long branchId
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return workerService.listWorkers(currentUser, search, branchId, departmentId);
    }

    @PostMapping("/workers")
    public CreateWorkerResponse createWorker(
            Authentication authentication,
            @Valid @RequestBody CreateWorkerRequest createWorkerRequest
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return workerService.createWorker(currentUser, createWorkerRequest);
    }

    @PutMapping("/workers/{id}")
    public WorkerResponse updateWorker(
            Authentication authentication,
            @PathVariable("id") Long workerId,
            @Valid @RequestBody UpdateWorkerRequest updateWorkerRequest
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return workerService.updateWorker(currentUser, workerId, updateWorkerRequest);
    }

    @GetMapping("/workers/{id}/access-key")
    public AccessKeyResponse workerAccessKey(
            Authentication authentication,
            @PathVariable("id") Long workerId
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return workerService.getAccessKey(currentUser, workerId);
    }

    @PostMapping("/workers/{id}/resend-access-key")
    public void resendWorkerAccessKey(
            Authentication authentication,
            @PathVariable("id") Long workerId
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        workerService.resendAccessKey(currentUser, workerId);
    }

    @GetMapping("/agent")
    public AgentInfoResponse agent(Authentication authentication) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return workerService.getMyAgentInfo(currentUser);
    }

    @GetMapping("/agent/access-key")
    public AccessKeyResponse myAccessKey(Authentication authentication) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return workerService.getMyAccessKey(currentUser);
    }

    @PostMapping("/agent/resend-access-key")
    public void resendMyAccessKey(Authentication authentication) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        workerService.resendMyAccessKey(currentUser);
    }

    @GetMapping("/dashboard")
    public List<DashboardWorkerResponse> dashboard(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long branchId
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return dashboardService.getDashboard(currentUser, search, branchId, departmentId);
    }

    @GetMapping("/dashboard/workers/{workerId}/apps")
    public List<AppUsageResponse> dashboardWorkerApps(
            Authentication authentication,
            @PathVariable Long workerId,
            @RequestParam(required = false, defaultValue = "TODAY") DashboardPeriod period
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return dashboardService.getWorkerApps(currentUser, workerId, period);
    }

    @GetMapping("/organization")
    public OrganizationResponse organization(Authentication authentication) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.getOrganization(currentUser);
    }

    @GetMapping("/organization/settings")
    public Map<String, String> settings(Authentication authentication) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.getSettings(currentUser);
    }

    @PutMapping("/organization/settings")
    public Map<String, String> updateSettings(
            Authentication authentication,
            @RequestBody UpdateSettingsRequest updateSettingsRequest
    ) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        Map<String, String> settings = Objects.nonNull(updateSettingsRequest)
                ? updateSettingsRequest.settings()
                : Map.of();
        return organizationService.updateSettings(currentUser, settings);
    }

    @GetMapping("/organization/stats")
    public OrganizationStatsResponse stats(Authentication authentication) {
        UserAccountEntity currentUser = authUserService.requireCurrentUser(authentication);
        return organizationService.getStats(currentUser);
    }

    @GetMapping("/downloads")
    public DownloadsResponse downloads() {
        return downloadsService.getDownloads();
    }
}
