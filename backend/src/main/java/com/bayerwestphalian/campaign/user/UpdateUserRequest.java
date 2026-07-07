package com.bayerwestphalian.campaign.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@NotBlank @Size(max = 255) String fullName, UserStatus status) {

    UpdateUserCommand toCommand() {
        return new UpdateUserCommand(fullName, status);
    }
}
