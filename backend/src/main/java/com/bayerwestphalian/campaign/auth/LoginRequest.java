package com.bayerwestphalian.campaign.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String password) {}
