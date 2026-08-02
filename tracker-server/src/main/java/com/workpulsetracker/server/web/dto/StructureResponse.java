package com.workpulsetracker.server.web.dto;

import java.util.List;

public record StructureResponse(
        List<BranchNode> branches
) {
    public record BranchNode(
            Long id,
            String name,
            boolean isDefault,
            List<DepartmentNode> departments
    ) {
    }

    public record DepartmentNode(
            Long id,
            String name,
            boolean isDefault
    ) {
    }
}
