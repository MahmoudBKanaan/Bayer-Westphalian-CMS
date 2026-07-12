package com.bayerwestphalian.campaign.user;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRoleRequest(@NotNull SystemRoleName roleName, UUID assignedByUserId) {}
