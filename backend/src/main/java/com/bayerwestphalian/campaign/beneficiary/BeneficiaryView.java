package com.bayerwestphalian.campaign.beneficiary;

import com.bayerwestphalian.campaign.customer.Customer;
import java.time.Instant;
import java.util.UUID;

public record BeneficiaryView(
        UUID id,
        UUID policyholderCustomerId,
        String policyholderFullName,
        UUID beneficiaryCustomerId,
        String beneficiaryFullName,
        String relationship,
        String guardianName,
        String guardianEmail,
        boolean guardianConsentRequired,
        boolean hasGuardianRequirement,
        Instant createdAt) {

    public static BeneficiaryView from(Beneficiary beneficiary) {
        Customer policyholder = beneficiary.getPolicyholderCustomer();
        Customer beneficiaryCustomer = beneficiary.getBeneficiaryCustomer();

        return new BeneficiaryView(
                beneficiary.getId(),
                customerId(policyholder),
                fullName(policyholder),
                customerId(beneficiaryCustomer),
                fullName(beneficiaryCustomer),
                beneficiary.getRelationship(),
                beneficiary.getGuardianName(),
                beneficiary.getGuardianEmail(),
                beneficiary.isGuardianConsentRequired(),
                beneficiary.hasGuardianRequirement(),
                beneficiary.getCreatedAt());
    }

    private static UUID customerId(Customer customer) {
        return customer == null ? null : customer.getId();
    }

    private static String fullName(Customer customer) {
        return customer == null ? null : customer.getFullName();
    }
}
