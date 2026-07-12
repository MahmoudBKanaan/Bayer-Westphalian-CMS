package com.bayerwestphalian.campaign.segment;

/**
 * How consecutive segment criteria are combined (KB FR-078, {@code
 * segment_criteria.join_operator}).
 *
 * <p>{@link #AND} is the default: every linked criterion must match. {@link #OR} unions the next
 * criterion with the accumulated prior result (left-to-right).
 */
public enum SegmentJoinOperator {
    /** Conjunctive join — KB default when join operator is omitted. */
    AND,
    /** Disjunctive join with the previous accumulated match result. */
    OR
}
