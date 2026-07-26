package com.workpulsetracker.server.web.dto;

import java.util.List;

public record OrganizationStatsResponse(
        long totalUsers,
        long activeUsersWithAgentKey,
        List<UserStatItem> users
) {
    public record UserStatItem(
            String email,
            String fullName,
            long trackedSeconds
    ) {
    }
}
