package com.bayerwestphalian.campaign.campaign;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link CampaignProduct} links (FR-052 promoted products). */
public interface CampaignProductRepository
        extends JpaRepository<CampaignProduct, CampaignProductId> {

    List<CampaignProduct> findByCampaign_IdOrderByProduct_NameAsc(UUID campaignId);

    List<CampaignProduct> findByProduct_Id(UUID productId);

    void deleteByCampaign_Id(UUID campaignId);

    boolean existsByCampaign_IdAndProduct_Id(UUID campaignId, UUID productId);

    default List<CampaignProduct> findByCampaignId(UUID campaignId) {
        return findByCampaign_IdOrderByProduct_NameAsc(campaignId);
    }

    /** Campaign–product links for a promoted product (analytics product performance). */
    default List<CampaignProduct> findByProductId(UUID productId) {
        return findByProduct_Id(productId);
    }
}
