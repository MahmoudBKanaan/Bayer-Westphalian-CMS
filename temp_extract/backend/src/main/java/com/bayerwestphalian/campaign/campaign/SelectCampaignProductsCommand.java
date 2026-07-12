package com.bayerwestphalian.campaign.campaign;

import java.util.List;
import java.util.UUID;

/** Service command for FR-052 campaign product selection (replaces promoted product set). */
public record SelectCampaignProductsCommand(List<UUID> productIds) {}
