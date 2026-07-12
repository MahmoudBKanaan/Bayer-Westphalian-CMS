package com.bayerwestphalian.campaign.segment;

import java.util.UUID;

public record SegmentCriteriaView(
        UUID id,
        UUID segmentId,
        String fieldName,
        SegmentOperator operator,
        String value,
        String logicalGroup,
        SegmentJoinOperator joinOperator) {

    public static SegmentCriteriaView from(SegmentCriteria criterion) {
        return new SegmentCriteriaView(
                criterion.getId(),
                criterion.getSegmentId(),
                criterion.getFieldName(),
                criterion.getOperator(),
                criterion.getValue(),
                criterion.getLogicalGroup(),
                criterion.getJoinOperator());
    }
}