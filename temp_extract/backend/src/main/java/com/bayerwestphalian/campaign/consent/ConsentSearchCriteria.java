package com.bayerwestphalian.campaign.consent;

import java.util.UUID;

public record ConsentSearchCriteria(
        UUID customerId, ConsentType consentType, ConsentStatus status, Boolean validOnly) {}
