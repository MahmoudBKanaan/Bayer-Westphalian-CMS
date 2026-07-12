package com.bayerwestphalian.campaign.segment;

import java.util.List;

public record SegmentPreviewCommand(List<CreateSegmentCriteriaCommand> criteria) {}
