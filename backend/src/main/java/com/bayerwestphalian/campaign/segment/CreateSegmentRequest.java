package com.bayerwestphalian.campaign.segment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateSegmentRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        SegmentVisibility visibility,
        @Valid List<CreateSegmentCriteriaRequest> criteria) {

    CreateSegmentCommand toCommand() {
        List<CreateSegmentCriteriaCommand> criteriaCommands =
                criteria == null
                        ? List.of()
                        : criteria.stream().map(CreateSegmentCriteriaRequest::toCommand).toList();
        return new CreateSegmentCommand(name, description, visibility, criteriaCommands);
    }
}
