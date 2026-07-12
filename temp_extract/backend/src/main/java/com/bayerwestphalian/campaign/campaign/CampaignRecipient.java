package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.StringUtils;

/** Recipient snapshot for a campaign audience row (KB {@code campaign_recipients}). */
@Entity
@Table(
        name = "campaign_recipients",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "campaign_recipients_campaign_customer_unique",
                        columnNames = {"campaign_id", "customer_id"}),
        indexes =
                @Index(
                        name = "campaign_recipients_status_idx",
                        columnList = "campaign_id, eligibility_status"))
public class CampaignRecipient {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
            name = "eligibility_status",
            nullable = false,
            columnDefinition = "campaign_recipient_status")
    private CampaignRecipientStatus eligibilityStatus;

    @Column(name = "exclusion_reason", columnDefinition = "text")
    private String exclusionReason;

    @Column(name = "eligibility_explanation", columnDefinition = "text")
    private String eligibilityExplanation;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CampaignRecipient() {}

    private CampaignRecipient(
            Campaign campaign,
            Customer customer,
            CampaignRecipientStatus eligibilityStatus,
            String exclusionReason,
            String eligibilityExplanation) {
        this.campaign = Objects.requireNonNull(campaign, "Campaign is required");
        this.customer = Objects.requireNonNull(customer, "Customer is required");
        this.eligibilityStatus =
                Objects.requireNonNull(eligibilityStatus, "Recipient status is required");
        this.exclusionReason = normalizeOptional(exclusionReason);
        this.eligibilityExplanation = normalizeOptional(eligibilityExplanation);
    }

    public static CampaignRecipient eligible(Campaign campaign, Customer customer) {
        return eligible(campaign, customer, null);
    }

    public static CampaignRecipient eligible(
            Campaign campaign, Customer customer, String eligibilityExplanation) {
        return new CampaignRecipient(
                campaign,
                customer,
                CampaignRecipientStatus.ELIGIBLE,
                null,
                eligibilityExplanation);
    }

    public static CampaignRecipient excluded(
            Campaign campaign, Customer customer, String exclusionReason, String explanation) {
        String normalizedReason = requireNonBlank(exclusionReason, "Exclusion reason");
        return new CampaignRecipient(
                campaign,
                customer,
                CampaignRecipientStatus.EXCLUDED,
                normalizedReason,
                explanation);
    }

    public UUID getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public UUID getCampaignId() {
        return campaign != null ? campaign.getId() : null;
    }

    public Customer getCustomer() {
        return customer;
    }

    public UUID getCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    public CampaignRecipientStatus getEligibilityStatus() {
        return eligibilityStatus;
    }

    public String getExclusionReason() {
        return exclusionReason;
    }

    public String getEligibilityExplanation() {
        return eligibilityExplanation;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isEligible() {
        return eligibilityStatus == CampaignRecipientStatus.ELIGIBLE;
    }

    public boolean isExcluded() {
        return eligibilityStatus == CampaignRecipientStatus.EXCLUDED;
    }

    public void markSent() {
        this.eligibilityStatus = CampaignRecipientStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markOpened() {
        this.eligibilityStatus = CampaignRecipientStatus.OPENED;
        this.openedAt = Instant.now();
    }

    public void markClicked() {
        this.eligibilityStatus = CampaignRecipientStatus.CLICKED;
        this.clickedAt = Instant.now();
    }

    public void markReplied() {
        this.eligibilityStatus = CampaignRecipientStatus.REPLIED;
    }

    public void markConverted() {
        this.eligibilityStatus = CampaignRecipientStatus.CONVERTED;
        this.convertedAt = Instant.now();
    }

    public void markFailed(String explanation) {
        this.eligibilityStatus = CampaignRecipientStatus.FAILED;
        this.eligibilityExplanation = normalizeOptional(explanation);
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

    private static String requireNonBlank(String value, String label) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
