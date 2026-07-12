package com.bayerwestphalian.campaign.consent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record RecordConsentRequest(
        @NotNull UUID customerId,
        @NotNull ConsentType consentType,
        @NotNull ConsentStatus status,
        @NotBlank String purpose,
        @Size(max = 100) String source,
        Instant grantedAt,
        Instant expiresAt,
        String evidenceFileUrl,
        UUID createdBy) {

    RecordConsentCommand toCommand() {
        return new RecordConsentCommand(
                customerId,
                consentType,
                status,
                purpose,
                source,
                grantedAt,
                expiresAt,
                evidenceFileUrl,
                createdBy);
    }
}
