package com.bayerwestphalian.campaign.segment;

import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record SegmentSearchRequest(
        @Size(max = 255) String term, UUID ownerUserId, SegmentVisibility visibility) {

    SegmentSearchCriteria toCriteria() {
        return new SegmentSearchCriteria(normalize(term), ownerUserId, visibility);
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
