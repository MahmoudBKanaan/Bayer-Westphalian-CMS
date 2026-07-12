package com.bayerwestphalian.campaign.user;

public record CreateUserCommand(String email, String rawPassword, String fullName) {}
