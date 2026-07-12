package com.bayerwestphalian.campaign.segment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentRepository extends JpaRepository<Segment, UUID> {

    List<Segment> findByOwner_IdOrderByNameAsc(UUID ownerUserId);

    List<Segment> findByVisibilityOrderByNameAsc(SegmentVisibility visibility);

    default List<Segment> findByOwner(UUID ownerUserId) {
        return findByOwner_IdOrderByNameAsc(ownerUserId);
    }

    default List<Segment> findByVisibility(SegmentVisibility visibility) {
        return findByVisibilityOrderByNameAsc(visibility);
    }

    default List<Segment> findGlobal() {
        return findByVisibility(SegmentVisibility.GLOBAL);
    }
}
