package com.bayerwestphalian.campaign.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link CampaignProduct} (KB {@code campaign_products}: campaign_id +
 * product_id).
 */
@Embeddable
public class CampaignProductId implements Serializable {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    protected CampaignProductId() {}

    public CampaignProductId(UUID campaignId, UUID productId) {
        this.campaignId = campaignId;
        this.productId = productId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public UUID getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CampaignProductId that)) {
            return false;
        }
        return Objects.equals(campaignId, that.campaignId)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(campaignId, productId);
    }
}
