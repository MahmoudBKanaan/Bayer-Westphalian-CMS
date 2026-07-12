package com.bayerwestphalian.campaign.segment;

import jakarta.validation.Valid;
import java.util.List;

public record SegmentPreviewRequest(@Valid List<CreateSegmentCriteriaRequest> criteria) {

    SegmentPreviewCommand toCommand() {
        List<CreateSegmentCriteriaCommand> criteriaCommands =
                criteria == null
                        ? List.of()
                        : criteria.stream().map(CreateSegmentCriteriaRequest::toCommand).toList();
        return new SegmentPreviewCommand(criteriaCommands);
    }
}