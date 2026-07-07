package com.bayerwestphalian.campaign.beneficiary;

import com.bayerwestphalian.campaign.common.exception.ConflictException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository, CustomerRepository customerRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.customerRepository = customerRepository;
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public BeneficiaryView createBeneficiary(CreateBeneficiaryCommand command) {
        validateCreateCommand(command);
        validateDistinctCustomers(
                command.policyholderCustomerId(), command.beneficiaryCustomerId());
        if (beneficiaryRepository.existsByPolicyholderCustomerIdAndBeneficiaryCustomerId(
                command.policyholderCustomerId(), command.beneficiaryCustomerId())) {
            throw new ConflictException(
                    "BENEFICIARY_LINK_EXISTS", "Beneficiary link already exists");
        }

        Customer policyholder =
                findCustomer(command.policyholderCustomerId(), "Policyholder customer");
        Customer beneficiaryCustomer =
                findCustomer(command.beneficiaryCustomerId(), "Beneficiary customer");
        Beneficiary beneficiary =
                Beneficiary.create(
                        policyholder, beneficiaryCustomer, command.relationship().trim());
        applyGuardianDetails(
                beneficiary,
                command.guardianName(),
                command.guardianEmail(),
                command.guardianConsentRequired());

        return BeneficiaryView.from(beneficiaryRepository.save(beneficiary));
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'COMPLIANCE_OFFICER')")
    @Transactional
    public BeneficiaryView updateBeneficiary(UUID beneficiaryId, UpdateBeneficiaryCommand command) {
        validateBeneficiaryId(beneficiaryId);
        validateUpdateCommand(command);
        Beneficiary beneficiary = findBeneficiary(beneficiaryId);

        beneficiary.updateRelationship(command.relationship().trim());
        applyGuardianDetails(
                beneficiary,
                command.guardianName(),
                command.guardianEmail(),
                command.guardianConsentRequired());

        return BeneficiaryView.from(beneficiaryRepository.save(beneficiary));
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public void deleteBeneficiary(UUID beneficiaryId) {
        validateBeneficiaryId(beneficiaryId);
        Beneficiary beneficiary = findBeneficiary(beneficiaryId);

        beneficiaryRepository.delete(beneficiary);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public BeneficiaryView findById(UUID beneficiaryId) {
        validateBeneficiaryId(beneficiaryId);
        return BeneficiaryView.from(findBeneficiary(beneficiaryId));
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public List<BeneficiaryView> searchBeneficiaries(BeneficiarySearchCriteria criteria) {
        BeneficiarySearchCriteria normalized = normalize(criteria);

        return loadCandidates(normalized).stream()
                .filter(beneficiary -> matches(beneficiary, normalized))
                .map(BeneficiaryView::from)
                .toList();
    }

    private void applyGuardianDetails(
            Beneficiary beneficiary,
            String guardianName,
            String guardianEmail,
            Boolean guardianConsentRequired) {
        String normalizedGuardianName = normalize(guardianName);
        String normalizedGuardianEmail = normalize(guardianEmail);

        if (Boolean.TRUE.equals(guardianConsentRequired)) {
            beneficiary.requireGuardianConsent(normalizedGuardianName, normalizedGuardianEmail);
            return;
        }

        beneficiary.updateGuardian(normalizedGuardianName, normalizedGuardianEmail);
        if (Boolean.FALSE.equals(guardianConsentRequired)) {
            beneficiary.clearGuardianConsentRequirement();
        }
    }

    private List<Beneficiary> loadCandidates(BeneficiarySearchCriteria criteria) {
        if (criteria.policyholderCustomerId() != null) {
            return beneficiaryRepository.findByPolicyholderCustomerId(
                    criteria.policyholderCustomerId());
        }
        if (criteria.beneficiaryCustomerId() != null) {
            return beneficiaryRepository.findByBeneficiaryCustomerId(
                    criteria.beneficiaryCustomerId());
        }
        if (Boolean.TRUE.equals(criteria.guardianConsentRequired())) {
            return beneficiaryRepository.findByGuardianConsentRequiredTrueOrderByCreatedAtAsc();
        }
        return beneficiaryRepository.findAllByOrderByCreatedAtAsc();
    }

    private boolean matches(Beneficiary beneficiary, BeneficiarySearchCriteria criteria) {
        return matchesCustomer(
                        beneficiary.getPolicyholderCustomer(), criteria.policyholderCustomerId())
                && matchesCustomer(
                        beneficiary.getBeneficiaryCustomer(), criteria.beneficiaryCustomerId())
                && matchesGuardianRequirement(beneficiary, criteria.guardianConsentRequired());
    }

    private boolean matchesCustomer(Customer customer, UUID customerId) {
        return customerId == null || Objects.equals(customer.getId(), customerId);
    }

    private boolean matchesGuardianRequirement(
            Beneficiary beneficiary, Boolean guardianConsentRequired) {
        return guardianConsentRequired == null
                || beneficiary.isGuardianConsentRequired() == guardianConsentRequired;
    }

    private BeneficiarySearchCriteria normalize(BeneficiarySearchCriteria criteria) {
        if (criteria == null) {
            return new BeneficiarySearchCriteria(null, null, null);
        }
        return criteria;
    }

    private Beneficiary findBeneficiary(UUID beneficiaryId) {
        return beneficiaryRepository
                .findById(beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", beneficiaryId));
    }

    private Customer findCustomer(UUID customerId, String resourceName) {
        return customerRepository
                .findById(customerId)
                .filter(customer -> !customer.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(resourceName, customerId));
    }

    private void validateCreateCommand(CreateBeneficiaryCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Beneficiary validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required(
                                        "policyholderCustomerId", command.policyholderCustomerId()),
                                required("beneficiaryCustomerId", command.beneficiaryCustomerId()),
                                required("relationship", command.relationship()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Beneficiary validation failed", errors);
        }
    }

    private void validateUpdateCommand(UpdateBeneficiaryCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Beneficiary validation failed", List.of("command: is required"));
        }
        if (!StringUtils.hasText(command.relationship())) {
            throw new ValidationException(
                    "Beneficiary validation failed", List.of("relationship: must not be blank"));
        }
    }

    private void validateBeneficiaryId(UUID beneficiaryId) {
        if (beneficiaryId == null) {
            throw new ValidationException(
                    "Beneficiary validation failed", List.of("beneficiaryId: is required"));
        }
    }

    private void validateDistinctCustomers(
            UUID policyholderCustomerId, UUID beneficiaryCustomerId) {
        if (Objects.equals(policyholderCustomerId, beneficiaryCustomerId)) {
            throw new ValidationException(
                    "Beneficiary validation failed",
                    List.of(
                            "beneficiaryCustomerId: must be different from policyholderCustomerId"));
        }
    }

    private String required(String fieldName, UUID value) {
        return value == null ? fieldName + ": must not be null" : "";
    }

    private String required(String fieldName, String value) {
        return StringUtils.hasText(value) ? "" : fieldName + ": must not be blank";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
