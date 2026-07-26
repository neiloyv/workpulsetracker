package com.workpulsetracker.server.web.dto;

import java.util.List;
import java.util.UUID;

public record StructureResponse(
        List<BranchNode> branches
) {
    public record BranchNode(
            UUID id,
            String name,
            List<DepartmentNode> departments
    ) {
    }

    public record DepartmentNode(
            UUID id,
            String name
    ) {
    }
}
