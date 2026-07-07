package com.bayerwestphalian.campaign.customer;

public record CustomerImportError(int lineNumber, String field, String message, String value) {}
