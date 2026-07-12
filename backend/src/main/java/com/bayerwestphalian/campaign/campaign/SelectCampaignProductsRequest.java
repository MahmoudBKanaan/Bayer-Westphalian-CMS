package com.bayerwestphalian.campaign.campaign;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * HTTP body for {@code PUT /api/campaigns/{id}/products} (KB FR-052 / item 221).
 *
 * <p>Replaces the campaign's promoted product set. An empty list clears all product links.
 */
public record SelectCampaignProductsRequest(@NotNull List<UUID> productIds) {

    SelectCampaignProductsCommand toCommand() {
        return new SelectCampaignProductsCommand(
                productIds == null ? List.of() : List.copyOf(productIds));
    }
}
