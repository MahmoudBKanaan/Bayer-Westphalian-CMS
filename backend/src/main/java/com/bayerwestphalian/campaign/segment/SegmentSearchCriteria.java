package com.bayerwestphalian.campaign.segment;

import java.util.UUID;

public record SegmentSearchCriteria(String term, UUID ownerUserId, SegmentVisibility visibility) {}
