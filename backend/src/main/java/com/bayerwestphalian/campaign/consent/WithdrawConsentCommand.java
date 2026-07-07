package com.bayerwestphalian.campaign.consent;

import java.time.Instant;
import java.util.UUID;

public record WithdrawConsentCommand(UUID consentRecordId, Instant withdrawnAt) {}
