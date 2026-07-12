package com.bayerwestphalian.campaign.segment;

import java.util.List;

public record CreateSegmentCommand(
        String name,
        String description,
        SegmentVisibility visibility,
        List<CreateSegmentCriteriaCommand> criteria) {}
