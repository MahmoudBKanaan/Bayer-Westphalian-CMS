package com.bayerwestphalian.campaign.user;

public record UpdateUserCommand(String fullName, UserStatus status) {}
