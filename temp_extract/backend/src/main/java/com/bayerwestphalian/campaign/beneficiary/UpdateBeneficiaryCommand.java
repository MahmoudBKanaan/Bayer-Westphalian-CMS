package com.bayerwestphalian.campaign.beneficiary;

public record UpdateBeneficiaryCommand(
        String relationship,
        String guardianName,
        String guardianEmail,
        Boolean guardianConsentRequired) {}
