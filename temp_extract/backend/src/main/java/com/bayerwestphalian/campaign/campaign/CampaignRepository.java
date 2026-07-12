package com.bayerwestphalian.campaign.campaign;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link Campaign} (KB CampaignRepository: findByStatus, findByOwnerUserId,
 * findActiveCampaigns).
 */
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByStatusOrderByNameAsc(CampaignStatus status);

    List<Campaign> findByOwner_IdOrderByNameAsc(UUID ownerUserId);

    List<Campaign> findByStatusInOrderByNameAsc(List<CampaignStatus> statuses);

    List<Campaign> findBySegment_IdOrderByNameAsc(UUID segmentId);

    /** KB {@code findByStatus} — campaigns in a given lifecycle status, ordered by name. */
    default List<Campaign> findByStatus(CampaignStatus status) {
        return findByStatusOrderByNameAsc(status);
    }

    /** KB {@code findByOwnerUserId} — campaigns owned by the given user. */
    default List<Campaign> findByOwnerUserId(UUID ownerUserId) {
        return findByOwner_IdOrderByNameAsc(ownerUserId);
    }

    /**
     * KB {@code findActiveCampaigns} — campaigns currently running ({@link CampaignStatus#ACTIVE}).
     */
    default List<Campaign> findActiveCampaigns() {
        return findByStatus(CampaignStatus.ACTIVE);
    }

    /** Campaigns awaiting compliance review (submitted for approve/reject). */
    default List<Campaign> findSubmittedCampaigns() {
        return findByStatus(CampaignStatus.SUBMITTED);
    }
}
