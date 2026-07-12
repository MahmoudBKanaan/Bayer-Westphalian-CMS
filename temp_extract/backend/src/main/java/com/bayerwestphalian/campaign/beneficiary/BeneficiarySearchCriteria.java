package com.bayerwestphalian.campaign.beneficiary;

import java.util.UUID;

public record BeneficiarySearchCriteria(
        UUID policyholderCustomerId, UUID beneficiaryCustomerId, Boolean guardianConsentRequired) {}
