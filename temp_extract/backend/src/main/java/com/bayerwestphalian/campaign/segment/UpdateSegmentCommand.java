package com.bayerwestphalian.campaign.segment;

import java.util.List;

public record UpdateSegmentCommand(
        String name,
        String description,
        SegmentVisibility visibility,
        List<CreateSegmentCriteriaCommand> criteria) {}