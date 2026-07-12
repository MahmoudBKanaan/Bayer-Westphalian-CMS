package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SegmentView(
        UUID id,
        String name,
        String description,
        UUID ownerUserId,
        String ownerFullName,
        SegmentVisibility visibility,
        List<SegmentCriteriaView> criteria,
        Instant createdAt,
        Instant updatedAt) {

    public static SegmentView from(Segment segment) {
        User owner = segment.getOwner();

        return new SegmentView(
                segment.getId(),
                segment.getName(),
                segment.getDescription(),
                segment.getOwnerUserId(),
                ownerFullName(owner),
                segment.getVisibility(),
                segment.getCriteria().stream().map(SegmentCriteriaView::from).toList(),
                segment.getCreatedAt(),
                segment.getUpdatedAt());
    }

    private static String ownerFullName(User owner) {
        return owner == null ? null : owner.getFullName();
    }
}
