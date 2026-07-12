package com.bayerwestphalian.campaign.beneficiary;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByPolicyholderCustomerIdOrderByCreatedAtAsc(UUID policyholderCustomerId);

    List<Beneficiary> findByBeneficiaryCustomerIdOrderByCreatedAtAsc(UUID beneficiaryCustomerId);

    List<Beneficiary> findByGuardianConsentRequiredTrueOrderByCreatedAtAsc();

    List<Beneficiary> findAllByOrderByCreatedAtAsc();

    boolean existsByPolicyholderCustomerIdAndBeneficiaryCustomerId(
            UUID policyholderCustomerId, UUID beneficiaryCustomerId);

    default List<Beneficiary> findByPolicyholderCustomerId(UUID policyholderCustomerId) {
        return findByPolicyholderCustomerIdOrderByCreatedAtAsc(policyholderCustomerId);
    }

    default List<Beneficiary> findByBeneficiaryCustomerId(UUID beneficiaryCustomerId) {
        return findByBeneficiaryCustomerIdOrderByCreatedAtAsc(beneficiaryCustomerId);
    }
}
