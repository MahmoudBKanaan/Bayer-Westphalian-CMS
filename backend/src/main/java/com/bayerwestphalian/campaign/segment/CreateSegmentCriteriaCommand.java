package com.bayerwestphalian.campaign.segment;

public record CreateSegmentCriteriaCommand(
        String fieldName,
        SegmentOperator operator,
        String value,
        String logicalGroup,
        SegmentJoinOperator joinOperator) {}
