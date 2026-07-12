package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.product.Product;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;

/**
 * Campaign–product link (KB {@code campaign_products} table, FR-052 — select promoted products).
 *
 * <p>Schema uses a composite primary key ({@code campaign_id}, {@code product_id}) rather than a
 * surrogate UUID. The class diagram's {@code id} maps to {@link CampaignProductId}.
 */
@Entity
@Table(name = "campaign_products")
public class CampaignProduct {

    @EmbeddedId private CampaignProductId id = new CampaignProductId();

    @NotNull @MapsId("campaignId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @NotNull @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    protected CampaignProduct() {}

    private CampaignProduct(Campaign campaign, Product product) {
        this.campaign = Objects.requireNonNull(campaign, "Campaign is required");
        this.product = Objects.requireNonNull(product, "Product is required");
    }

    /**
     * Links a product to a campaign for promotion targeting (FR-052).
     *
     * @param campaign parent campaign (must eventually have an id before persist)
     * @param product promoted product (must eventually have an id before persist)
     */
    public static CampaignProduct link(Campaign campaign, Product product) {
        return new CampaignProduct(campaign, product);
    }

    public CampaignProductId getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public Product getProduct() {
        return product;
    }

    public UUID getCampaignId() {
        if (id != null && id.getCampaignId() != null) {
            return id.getCampaignId();
        }
        return campaign != null ? campaign.getId() : null;
    }

    public UUID getProductId() {
        if (id != null && id.getProductId() != null) {
            return id.getProductId();
        }
        return product != null ? product.getId() : null;
    }

    public boolean linksCampaign(UUID campaignId) {
        return campaignId != null && campaignId.equals(getCampaignId());
    }

    public boolean linksProduct(UUID productId) {
        return productId != null && productId.equals(getProductId());
    }

    public boolean links(UUID campaignId, UUID productId) {
        return linksCampaign(campaignId) && linksProduct(productId);
    }
}
