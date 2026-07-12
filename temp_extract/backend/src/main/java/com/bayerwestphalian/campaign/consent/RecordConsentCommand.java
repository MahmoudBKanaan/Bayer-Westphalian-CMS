package com.bayerwestphalian.campaign.consent;

import java.time.Instant;
import java.util.UUID;

public record RecordConsentCommand(
        UUID customerId,
        ConsentType consentType,
        ConsentStatus status,
        String purpose,
        String source,
        Instant grantedAt,
        Instant expiresAt,
        String evidenceFileUrl,
        UUID createdBy) {}
