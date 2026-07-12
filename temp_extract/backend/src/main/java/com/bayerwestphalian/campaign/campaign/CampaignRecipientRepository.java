package com.bayerwestphalian.campaign.campaign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for campaign audience recipient rows (KB {@code campaign_recipients}). */
public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, UUID> {

    List<CampaignRecipient> findByCampaign_Id(UUID campaignId);

    List<CampaignRecipient> findByCampaign_IdAndEligibilityStatus(
            UUID campaignId, CampaignRecipientStatus eligibilityStatus);

    Optional<CampaignRecipient> findByCampaign_IdAndCustomer_Id(UUID campaignId, UUID customerId);

    boolean existsByCampaign_IdAndCustomer_Id(UUID campaignId, UUID customerId);

    long countByCampaign_IdAndEligibilityStatus(
            UUID campaignId, CampaignRecipientStatus eligibilityStatus);

    List<CampaignRecipient> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    void deleteByCampaign_Id(UUID campaignId);

    default List<CampaignRecipient> findByCampaignId(UUID campaignId) {
        return findByCampaign_Id(campaignId);
    }

    default List<CampaignRecipient> findByCampaignIdAndEligibilityStatus(
            UUID campaignId, CampaignRecipientStatus eligibilityStatus) {
        return findByCampaign_IdAndEligibilityStatus(campaignId, eligibilityStatus);
    }

    default Optional<CampaignRecipient> findByCampaignIdAndCustomerId(
            UUID campaignId, UUID customerId) {
        return findByCampaign_IdAndCustomer_Id(campaignId, customerId);
    }

    default boolean existsByCampaignIdAndCustomerId(UUID campaignId, UUID customerId) {
        return existsByCampaign_IdAndCustomer_Id(campaignId, customerId);
    }

    default long countByCampaignIdAndEligibilityStatus(
            UUID campaignId, CampaignRecipientStatus eligibilityStatus) {
        return countByCampaign_IdAndEligibilityStatus(campaignId, eligibilityStatus);
    }

    default List<CampaignRecipient> findByCustomerId(UUID customerId) {
        return findByCustomer_IdOrderByCreatedAtDesc(customerId);
    }
}
