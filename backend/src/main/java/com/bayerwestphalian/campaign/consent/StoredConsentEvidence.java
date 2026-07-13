package com.bayerwestphalian.campaign.consent;

public record StoredConsentEvidence(String storageReference, String contentType, long sizeBytes) {}
