package com.bayerwestphalian.campaign.segment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentCriteriaRepository extends JpaRepository<SegmentCriteria, UUID> {

    List<SegmentCriteria> findBySegment_IdOrderByFieldNameAsc(UUID segmentId);

    default List<SegmentCriteria> findBySegmentId(UUID segmentId) {
        return findBySegment_IdOrderByFieldNameAsc(segmentId);
    }
}
