/**
 * Campaign package for campaign drafting, submission, compliance review, launch, pause, completion,
 * and archiving (KB epic E13 / FR-050–062).
 *
 * <p>Core domain types:
 *
 * <ul>
 *   <li>{@link com.bayerwestphalian.campaign.campaign.Campaign} — {@code campaigns} aggregate
 *   <li>{@link com.bayerwestphalian.campaign.campaign.CampaignProduct} — {@code campaign_products}
 *       promoted-product links (FR-052)
 *   <li>{@link com.bayerwestphalian.campaign.campaign.CampaignProductId} — composite key
 *   <li>{@link com.bayerwestphalian.campaign.campaign.CampaignRepository} — status/owner/active
 *       lookups
 *   <li>Campaign DTOs — create/update/search/view/approve/reject/product-selection/compliance-notes
 *       request and command types
 *   <li>{@link com.bayerwestphalian.campaign.campaign.CampaignService} — lifecycle + FR-052 product
 *       selection + FR-053 segment selection + compliance review notes (item 231) + rejection
 *       reason (item 232) + CREATE audit on creation (item 233) + SUBMIT audit on submission (item
 *       528 / FR-058) + APPROVE/REJECT audit on compliance decision (item 529 / FR-059) + LAUNCH
 *       audit on launch (item 530 / FR-060) + Campaign Manager draft creation (item 243 / FR-050 /
 *       FR-057)
 *   <li>{@link com.bayerwestphalian.campaign.campaign.CampaignController} — {@code /api/campaigns}
 *   <li>{@link com.bayerwestphalian.campaign.campaign.CampaignStatus} — lifecycle status enum
 *   <li>{@link com.bayerwestphalian.campaign.campaign.CampaignChannel} — communication channel enum
 *   <li>{@link com.bayerwestphalian.campaign.campaign.EligibilityService} — contact eligibility
 *       gate
 * </ul>
 */
package com.bayerwestphalian.campaign.campaign;
