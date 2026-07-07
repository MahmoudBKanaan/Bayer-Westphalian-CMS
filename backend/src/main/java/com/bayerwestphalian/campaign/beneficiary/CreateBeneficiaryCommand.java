package com.bayerwestphalian.campaign.beneficiary;

import java.util.UUID;

public record CreateBeneficiaryCommand(
        UUID policyholderCustomerId,
        UUID beneficiaryCustomerId,
        String relationship,
        String guardianName,
        String guardianEmail,
        boolean guardianConsentRequired) {}
