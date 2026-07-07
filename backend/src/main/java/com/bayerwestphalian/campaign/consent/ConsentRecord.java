package com.bayerwestphalian.campaign.consent;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "consent_records")
public class ConsentRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "consent_type", nullable = false, columnDefinition = "consent_type")
    private ConsentType consentType;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "consent_status")
    private ConsentStatus status;

    @NotBlank @Column(name = "purpose", nullable = false, columnDefinition = "text")
    private String purpose;

    @Size(max = 100) @Column(name = "source", length = 100)
    private String source;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "evidence_file_url", columnDefinition = "text")
    private String evidenceFileUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConsentRecord() {}

    private ConsentRecord(
            Customer customer,
            ConsentType consentType,
            ConsentStatus status,
            String purpose,
            String source) {
        this.customer = customer;
        this.consentType = consentType;
        this.status = status;
        this.purpose = purpose;
        this.source = source;
    }

    public static ConsentRecord create(
            Customer customer,
            ConsentType consentType,
            ConsentStatus status,
            String purpose,
            String source) {
        return new ConsentRecord(customer, consentType, status, purpose, source);
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public ConsentType getConsentType() {
        return consentType;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getSource() {
        return source;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getEvidenceFileUrl() {
        return evidenceFileUrl;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void grant(
            Instant grantedAt, Instant expiresAt, String evidenceFileUrl, User createdBy) {
        status = ConsentStatus.GIVEN;
        this.grantedAt = grantedAt;
        withdrawnAt = null;
        this.expiresAt = expiresAt;
        this.evidenceFileUrl = evidenceFileUrl;
        this.createdBy = createdBy;
    }

    public void withdraw(Instant withdrawnAt) {
        status = ConsentStatus.WITHDRAWN;
        this.withdrawnAt = withdrawnAt;
    }

    public void expire() {
        status = ConsentStatus.EXPIRED;
    }

    public void reject() {
        status = ConsentStatus.REJECTED;
    }

    public boolean isValid(Instant now) {
        return status == ConsentStatus.GIVEN && (expiresAt == null || expiresAt.isAfter(now));
    }

    public boolean requiresAction(Instant now) {
        return status == ConsentStatus.REQUIRED
                || status == ConsentStatus.REJECTED
                || status == ConsentStatus.WITHDRAWN
                || status == ConsentStatus.EXPIRED
                || (status == ConsentStatus.GIVEN && expiresAt != null && !expiresAt.isAfter(now));
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
}
