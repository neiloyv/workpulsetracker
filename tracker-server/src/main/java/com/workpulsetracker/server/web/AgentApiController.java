package com.workpulsetracker.server.web;

import com.workpulsetracker.server.security.AgentDevicePrincipal;
import com.workpulsetracker.server.service.AgentAuthService;
import com.workpulsetracker.server.service.AgentSyncService;
import com.workpulsetracker.server.service.TelemetryService;
import com.workpulsetracker.server.web.dto.AgentAuthRequest;
import com.workpulsetracker.server.web.dto.AgentAuthResponse;
import com.workpulsetracker.server.web.dto.AgentSyncResponse;
import com.workpulsetracker.server.web.dto.TelemetryIngestRequest;
import com.workpulsetracker.server.web.dto.TelemetryIngestResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
@RequestMapping("/api/agent")
public class AgentApiController {

    private final AgentAuthService agentAuthService;
    private final TelemetryService telemetryService;
    private final AgentSyncService agentSyncService;

    public AgentApiController(
            AgentAuthService agentAuthService,
            TelemetryService telemetryService,
            AgentSyncService agentSyncService
    ) {
        this.agentAuthService = agentAuthService;
        this.telemetryService = telemetryService;
        this.agentSyncService = agentSyncService;
    }

    @PostMapping("/auth")
    public AgentAuthResponse authenticate(@Valid @RequestBody AgentAuthRequest agentAuthRequest) {
        return agentAuthService.authenticate(agentAuthRequest);
    }

    @PostMapping("/telemetry")
    public TelemetryIngestResponse ingestTelemetry(
            Authentication authentication,
            @Valid @RequestBody TelemetryIngestRequest telemetryIngestRequest
    ) {
        AgentDevicePrincipal agentDevicePrincipal = requireAgentPrincipal(authentication);
        return telemetryService.ingest(agentDevicePrincipal, telemetryIngestRequest);
    }

    @GetMapping("/sync")
    public AgentSyncResponse sync(
            Authentication authentication,
            @RequestParam(name = "scope", defaultValue = "account") String scope
    ) {
        AgentDevicePrincipal agentDevicePrincipal = requireAgentPrincipal(authentication);
        if ("device".equalsIgnoreCase(scope)) {
            return agentSyncService.getDeviceTotals(agentDevicePrincipal);
        }
        return agentSyncService.getAccountTotals(agentDevicePrincipal);
    }

    private static AgentDevicePrincipal requireAgentPrincipal(Authentication authentication) {
        if (Objects.isNull(authentication) || !(authentication.getPrincipal() instanceof AgentDevicePrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agent authentication required");
        }
        return (AgentDevicePrincipal) authentication.getPrincipal();
    }
}
