package com.bayerwestphalian.campaign.consent;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WithdrawConsentRequest(@NotNull UUID consentRecordId, Instant withdrawnAt) {

    WithdrawConsentCommand toCommand() {
        return new WithdrawConsentCommand(consentRecordId, withdrawnAt);
    }
}
