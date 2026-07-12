package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API view of a campaign definition and lifecycle state.
 *
 * <p>{@code rejectionReason} is the formal Compliance Officer reason when status is {@code
 * REJECTED} (item 232 / KB field). {@code complianceReviewNotes} are optional review notes (item
 * 231).
 */
public record CampaignView(
        UUID id,
        String name,
        String objective,
        CampaignStatus status,
        UUID ownerUserId,
        String ownerFullName,
        UUID segmentId,
        String segmentName,
        CampaignChannel channel,
        String messageSubject,
        String messageBody,
        LocalDate startDate,
        LocalDate endDate,
        UUID approvedByUserId,
        String approvedByFullName,
        Instant approvedAt,
        String rejectionReason,
        String complianceReviewNotes,
        List<UUID> productIds,
        Instant createdAt,
        Instant updatedAt) {

    public static CampaignView from(Campaign campaign) {
        return from(campaign, List.of());
    }

    public static CampaignView from(Campaign campaign, List<UUID> productIds) {
        User owner = campaign.getOwner();
        Segment segment = campaign.getSegment();
        User approver = campaign.getApprovedBy();
        List<UUID> products =
                productIds == null ? List.of() : List.copyOf(productIds);

        return new CampaignView(
                campaign.getId(),
                campaign.getName(),
                campaign.getObjective(),
                campaign.getStatus(),
                campaign.getOwnerUserId(),
                fullName(owner),
                campaign.getSegmentId(),
                segment == null ? null : segment.getName(),
                campaign.getChannel(),
                campaign.getMessageSubject(),
                campaign.getMessageBody(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getApprovedByUserId(),
                fullName(approver),
                campaign.getApprovedAt(),
                campaign.getRejectionReason(),
                campaign.getComplianceReviewNotes(),
                products,
                campaign.getCreatedAt(),
                campaign.getUpdatedAt());
    }

    private static String fullName(User user) {
        return user == null ? null : user.getFullName();
    }
}
