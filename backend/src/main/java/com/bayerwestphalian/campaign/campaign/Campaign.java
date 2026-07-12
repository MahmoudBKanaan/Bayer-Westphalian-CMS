package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Campaign definition and lifecycle aggregate (KB {@code campaigns} table, FR-050–062).
 *
 * <p>Factory {@link #create} starts campaigns in {@link CampaignStatus#DRAFT}. Domain methods
 * {@link #submit()}, {@link #approve(User)}, {@link #reject(String)}, {@link #launch()}, {@link
 * #pause()}, {@link #complete()}, and {@link #archive()} enforce the controlled status transitions.
 */
@Entity
@Table(name = "campaigns")
public class Campaign extends BaseEntity {

    @NotBlank @Size(max = 255) @Column(name = "name", nullable = false)
    private String name;

    @NotBlank @Column(name = "objective", nullable = false, columnDefinition = "text")
    private String objective;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "campaign_status")
    private CampaignStatus status = CampaignStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private Segment segment;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "channel", nullable = false, columnDefinition = "campaign_channel")
    private CampaignChannel channel;

    @Size(max = 255) @Column(name = "message_subject", length = 255)
    private String messageSubject;

    @Column(name = "message_body", columnDefinition = "text")
    private String messageBody;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    /**
     * Formal Compliance Officer rejection reason (item 232 / KB {@code rejection_reason}). Required
     * when moving a submitted campaign to {@link CampaignStatus#REJECTED}; cleared on resubmit or
     * subsequent approve.
     */
    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    /**
     * Optional Compliance Officer notes captured during review (item 231). Distinct from {@link
     * #rejectionReason}, which is the formal reject reason.
     */
    @Column(name = "compliance_review_notes", columnDefinition = "text")
    private String complianceReviewNotes;

    protected Campaign() {}

    private Campaign(
            String name, String objective, User owner, Segment segment, CampaignChannel channel) {
        updateName(name);
        updateObjective(objective);
        this.owner = owner;
        this.segment = segment;
        this.channel = Objects.requireNonNull(channel, "Campaign channel is required");
        this.status = CampaignStatus.DRAFT;
    }

    /**
     * Creates a draft campaign (KB FR-050 / FR-057). Status is always {@link CampaignStatus#DRAFT}.
     */
    public static Campaign create(
            String name, String objective, User owner, Segment segment, CampaignChannel channel) {
        return new Campaign(name, objective, owner, segment, channel);
    }

    public String getName() {
        return name;
    }

    public String getObjective() {
        return objective;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public User getOwner() {
        return owner;
    }

    public UUID getOwnerUserId() {
        return owner != null ? owner.getId() : null;
    }

    public Segment getSegment() {
        return segment;
    }

    public UUID getSegmentId() {
        return segment != null ? segment.getId() : null;
    }

    public CampaignChannel getChannel() {
        return channel;
    }

    public String getMessageSubject() {
        return messageSubject;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public UUID getApprovedByUserId() {
        return approvedBy != null ? approvedBy.getId() : null;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getComplianceReviewNotes() {
        return complianceReviewNotes;
    }

    public void updateName(String name) {
        requireEditable();
        this.name = requireNonBlank(name, "Campaign name");
    }

    public void updateObjective(String objective) {
        requireEditable();
        this.objective = requireNonBlank(objective, "Campaign objective");
    }

    public void assignOwner(User owner) {
        requireEditable();
        this.owner = owner;
    }

    public void assignSegment(Segment segment) {
        requireEditable();
        this.segment = segment;
    }

    public void changeChannel(CampaignChannel channel) {
        requireEditable();
        this.channel = Objects.requireNonNull(channel, "Campaign channel is required");
    }

    public void updateMessage(String messageSubject, String messageBody) {
        requireEditable();
        if (messageSubject != null && messageSubject.length() > 255) {
            throw new IllegalArgumentException(
                    "Campaign message subject must not exceed 255 characters");
        }
        this.messageSubject = normalizeOptional(messageSubject);
        this.messageBody = messageBody;
    }

    public void updateSchedule(LocalDate startDate, LocalDate endDate) {
        requireEditable();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Campaign end date must not be before start date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Submits a draft (or rejected) campaign for compliance review (FR-058).
     *
     * @return this campaign
     */
    public Campaign submit() {
        if (status != CampaignStatus.DRAFT && status != CampaignStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only DRAFT or REJECTED campaigns can be submitted; current status is "
                            + status);
        }
        this.status = CampaignStatus.SUBMITTED;
        this.rejectionReason = null;
        this.complianceReviewNotes = null;
        this.approvedBy = null;
        this.approvedAt = null;
        return this;
    }

    /**
     * Compliance Officer approves a submitted campaign (FR-059, BR-005).
     *
     * @param approver user performing approval
     * @return this campaign
     */
    public Campaign approve(User approver) {
        return approve(approver, null);
    }

    /**
     * Compliance Officer approves a submitted campaign with optional review notes (item 231).
     *
     * @param approver user performing approval
     * @param complianceReviewNotes optional notes for the CM (may be blank/null)
     * @return this campaign
     */
    public Campaign approve(User approver, String complianceReviewNotes) {
        if (status != CampaignStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only SUBMITTED campaigns can be approved; current status is " + status);
        }
        Objects.requireNonNull(approver, "Approver is required");
        this.status = CampaignStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = Instant.now();
        this.rejectionReason = null;
        this.complianceReviewNotes = normalizeOptional(complianceReviewNotes);
        return this;
    }

    /**
     * Compliance Officer rejects a submitted campaign with a required reason (FR-059 / item 232).
     *
     * @param reason rejection reason (required, non-blank)
     * @return this campaign
     */
    public Campaign reject(String reason) {
        return reject(reason, null);
    }

    /**
     * Compliance Officer rejects a submitted campaign with a formal required reason (item 232) and
     * optional review notes (item 231).
     *
     * @param reason rejection reason (required, non-blank; stored trimmed)
     * @param complianceReviewNotes optional additional review notes
     * @return this campaign
     */
    public Campaign reject(String reason, String complianceReviewNotes) {
        if (status != CampaignStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only SUBMITTED campaigns can be rejected; current status is " + status);
        }
        String normalizedReason = requireNonBlank(reason, "Rejection reason");
        this.status = CampaignStatus.REJECTED;
        this.rejectionReason = normalizedReason;
        this.complianceReviewNotes = normalizeOptional(complianceReviewNotes);
        this.approvedBy = null;
        this.approvedAt = null;
        return this;
    }

    /**
     * Records or updates compliance review notes while the campaign is under review (SUBMITTED) or
     * after a decision (APPROVED/REJECTED). Used when notes are captured without changing status.
     */
    public Campaign recordComplianceReviewNotes(String complianceReviewNotes) {
        if (status != CampaignStatus.SUBMITTED
                && status != CampaignStatus.APPROVED
                && status != CampaignStatus.REJECTED) {
            throw new IllegalStateException(
                    "Compliance review notes can only be recorded for SUBMITTED, APPROVED, or REJECTED campaigns; current status is "
                            + status);
        }
        this.complianceReviewNotes = normalizeOptional(complianceReviewNotes);
        return this;
    }

    /**
     * Launches an approved campaign (FR-060, BR-005 / TC-001).
     *
     * @return this campaign
     */
    public Campaign launch() {
        if (!canLaunch()) {
            throw new IllegalStateException(
                    "Only APPROVED campaigns can be launched; current status is " + status);
        }
        this.status = CampaignStatus.ACTIVE;
        return this;
    }

    /**
     * Pauses an active campaign (FR-061).
     *
     * @return this campaign
     */
    public Campaign pause() {
        if (status != CampaignStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE campaigns can be paused; current status is " + status);
        }
        this.status = CampaignStatus.PAUSED;
        return this;
    }

    /**
     * Completes an active or paused campaign.
     *
     * @return this campaign
     */
    public Campaign complete() {
        if (status != CampaignStatus.ACTIVE && status != CampaignStatus.PAUSED) {
            throw new IllegalStateException(
                    "Only ACTIVE or PAUSED campaigns can be completed; current status is "
                            + status);
        }
        this.status = CampaignStatus.COMPLETED;
        return this;
    }

    /**
     * Archives a completed or rejected campaign (FR-062).
     *
     * @return this campaign
     */
    public Campaign archive() {
        if (status != CampaignStatus.COMPLETED && status != CampaignStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only COMPLETED or REJECTED campaigns can be archived; current status is "
                            + status);
        }
        this.status = CampaignStatus.ARCHIVED;
        return this;
    }

    /** Resume a paused campaign back to ACTIVE. */
    public Campaign resume() {
        if (status != CampaignStatus.PAUSED) {
            throw new IllegalStateException(
                    "Only PAUSED campaigns can be resumed; current status is " + status);
        }
        this.status = CampaignStatus.ACTIVE;
        return this;
    }

    /**
     * Draft and rejected campaigns may be edited by the owner/campaign manager workflow. Submitted
     * and later statuses require proper workflow transitions (item 247 later).
     */
    public boolean canEdit() {
        return status == CampaignStatus.DRAFT || status == CampaignStatus.REJECTED;
    }

    /** Launch is allowed only when status is APPROVED (BR-005). */
    public boolean canLaunch() {
        return status == CampaignStatus.APPROVED;
    }

    public boolean isOwnedBy(UUID userId) {
        return owner != null && userId != null && userId.equals(owner.getId());
    }

    public boolean isDraft() {
        return status == CampaignStatus.DRAFT;
    }

    public boolean isActive() {
        return status == CampaignStatus.ACTIVE;
    }

    private void requireEditable() {
        if (!canEdit()) {
            throw new IllegalStateException(
                    "Campaign cannot be edited in status " + status + "; only DRAFT or REJECTED");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
