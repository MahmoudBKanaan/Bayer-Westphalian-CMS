package com.bayerwestphalian.campaign.beneficiary;

import java.util.UUID;

public record BeneficiarySearchRequest(
        UUID policyholderCustomerId, UUID beneficiaryCustomerId, Boolean guardianConsentRequired) {

    BeneficiarySearchCriteria toCriteria() {
        return new BeneficiarySearchCriteria(
                policyholderCustomerId, beneficiaryCustomerId, guardianConsentRequired);
    }
}
