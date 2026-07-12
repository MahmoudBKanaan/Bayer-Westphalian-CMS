package com.bayerwestphalian.campaign.beneficiary;

import com.bayerwestphalian.campaign.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "beneficiaries",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "beneficiaries_unique_link",
                        columnNames = {"policyholder_customer_id", "beneficiary_customer_id"}))
public class Beneficiary {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policyholder_customer_id", nullable = false)
    private Customer policyholderCustomer;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_customer_id", nullable = false)
    private Customer beneficiaryCustomer;

    @NotBlank @Size(max = 100) @Column(name = "relationship", nullable = false, length = 100)
    private String relationship;

    @Size(max = 255) @Column(name = "guardian_name")
    private String guardianName;

    @Email @Size(max = 255) @Column(name = "guardian_email")
    private String guardianEmail;

    @Column(name = "guardian_consent_required", nullable = false)
    private boolean guardianConsentRequired;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Beneficiary() {}

    private Beneficiary(
            Customer policyholderCustomer, Customer beneficiaryCustomer, String relationship) {
        ensureDistinctCustomers(policyholderCustomer, beneficiaryCustomer);
        this.policyholderCustomer = policyholderCustomer;
        this.beneficiaryCustomer = beneficiaryCustomer;
        this.relationship = relationship;
    }

    public static Beneficiary create(
            Customer policyholderCustomer, Customer beneficiaryCustomer, String relationship) {
        return new Beneficiary(policyholderCustomer, beneficiaryCustomer, relationship);
    }

    public UUID getId() {
        return id;
    }

    public Customer getPolicyholderCustomer() {
        return policyholderCustomer;
    }

    public Customer getBeneficiaryCustomer() {
        return beneficiaryCustomer;
    }

    public String getRelationship() {
        return relationship;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public String getGuardianEmail() {
        return guardianEmail;
    }

    public boolean isGuardianConsentRequired() {
        return guardianConsentRequired;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean hasGuardianRequirement() {
        return guardianConsentRequired;
    }

    public void updateRelationship(String relationship) {
        this.relationship = relationship;
    }

    public void requireGuardianConsent(String guardianName, String guardianEmail) {
        guardianConsentRequired = true;
        updateGuardian(guardianName, guardianEmail);
    }

    public void clearGuardianConsentRequirement() {
        guardianConsentRequired = false;
    }

    public void updateGuardian(String guardianName, String guardianEmail) {
        this.guardianName = guardianName;
        this.guardianEmail = guardianEmail;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private static void ensureDistinctCustomers(
            Customer policyholderCustomer, Customer beneficiaryCustomer) {
        UUID policyholderCustomerId =
                policyholderCustomer == null ? null : policyholderCustomer.getId();
        UUID beneficiaryCustomerId =
                beneficiaryCustomer == null ? null : beneficiaryCustomer.getId();

        if (policyholderCustomer == beneficiaryCustomer
                || (policyholderCustomerId != null
                        && Objects.equals(policyholderCustomerId, beneficiaryCustomerId))) {
            throw new IllegalArgumentException(
                    "Policyholder customer and beneficiary customer must be different");
        }
    }
}
