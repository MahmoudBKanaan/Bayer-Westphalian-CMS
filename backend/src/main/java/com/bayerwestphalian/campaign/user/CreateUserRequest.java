package com.bayerwestphalian.campaign.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String password,
        @NotBlank @Size(max = 255) String fullName) {

    CreateUserCommand toCommand() {
        return new CreateUserCommand(email, password, fullName);
    }
}
