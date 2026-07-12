package com.bayerwestphalian.campaign.segment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSegmentCriteriaRequest(
        @NotBlank @Size(max = 100) String fieldName,
        @NotNull SegmentOperator operator,
        @NotBlank String value,
        @Size(max = 50) String logicalGroup,
        SegmentJoinOperator joinOperator) {

    CreateSegmentCriteriaCommand toCommand() {
        return new CreateSegmentCriteriaCommand(
                fieldName,
                operator,
                value,
                logicalGroup,
                joinOperator != null ? joinOperator : SegmentJoinOperator.AND);
    }
}
