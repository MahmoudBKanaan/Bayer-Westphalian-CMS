/**
 * Analytics package for dashboards, campaign metrics, engagement, conversion, ROI, and product
 * performance (KB epic E19).
 *
 * <p>Module documentation (item 459): {@code docs/modules/analytics-module.md}.
 *
 * <p>KPI definition document (item 461): {@code docs/modules/kpi-definitions.md} — formal catalog
 * of count, rate, and financial KPI formulas, scale/rounding, and multi-campaign aggregation rules.
 *
 * <p>BI Analyst user guide (item 462): {@code docs/user-guides/bi-analyst-guide.md} — analytics,
 * executive, report export, and segmentation-insight workflows for {@code BI_ANALYST}.
 *
 * <p>Executive Viewer user guide (item 463): {@code docs/user-guides/executive-viewer-guide.md} —
 * high-level aggregated dashboards (COMP-010) and management report export for
 * {@code EXECUTIVE_VIEWER}.
 *
 * <p>DTO surface (item 414):
 *
 * <ul>
 *   <li>{@link com.bayerwestphalian.campaign.analytics.CampaignMetricsView}
 *   <li>{@link com.bayerwestphalian.campaign.analytics.DashboardView}
 *   <li>{@link com.bayerwestphalian.campaign.analytics.CampaignAnalyticsView}
 *   <li>{@link com.bayerwestphalian.campaign.analytics.ProductPerformanceView}
 *   <li>{@link com.bayerwestphalian.campaign.analytics.ExecutiveDashboardView}
 *   <li>{@link com.bayerwestphalian.campaign.analytics.AnalyticsRates}
 * </ul>
 *
 * <p>Service (item 415): {@link com.bayerwestphalian.campaign.analytics.AnalyticsService}.
 *
 * <p>Controller (item 416): {@link com.bayerwestphalian.campaign.analytics.AnalyticsController}
 * under {@code /api/analytics}.
 *
 * <p>Dashboard endpoint (item 431 / FR-100–FR-107): {@code GET /api/analytics/dashboard} via {@link
 * com.bayerwestphalian.campaign.analytics.AnalyticsController#getDashboard()} and {@link
 * com.bayerwestphalian.campaign.analytics.AnalyticsService#getDashboard()}.
 *
 * <p>Campaign analytics endpoint (item 432): {@code GET /api/analytics/campaigns/{campaignId}} via
 * {@link com.bayerwestphalian.campaign.analytics.AnalyticsController#getCampaignAnalytics(java.util.UUID)}
 * and {@link
 * com.bayerwestphalian.campaign.analytics.AnalyticsService#getCampaignAnalytics(java.util.UUID)}.
 *
 * <p>Product performance endpoint (item 433): {@code GET /api/analytics/products/performance} via
 * {@link com.bayerwestphalian.campaign.analytics.AnalyticsController#getProductPerformance()} and
 * {@link com.bayerwestphalian.campaign.analytics.AnalyticsService#getProductPerformance()}.
 *
 * <p>Executive aggregate dashboard endpoint (item 434 / acceptance item 457 / COMP-010): {@code
 * GET /api/analytics/executive} via {@link
 * com.bayerwestphalian.campaign.analytics.AnalyticsController#getExecutiveDashboard()} and {@link
 * com.bayerwestphalian.campaign.analytics.AnalyticsService#getExecutiveDashboard()}. Returns
 * platform-level aggregates (campaign inventory, funnel, engagement, rates, cost/revenue/ROI) plus
 * embedded product performance rows — not raw contact-event detail (COMP-010 / item 457).
 *
 * <p>KPI calculations: {@link com.bayerwestphalian.campaign.analytics.AnalyticsCalculations}
 * (audience size item 417/446: eligible + excluded; eligible count item 418/447: ELIGIBLE recipients;
 * excluded count item 419/448: EXCLUDED recipients; sent count item 420/449 / FR-103: messages sent
 * (updated after launch); engagement counts items 421–424 / 450 (BR-034 contact events); opened
 * count item 421: OPENED contact events; clicked count item 422: CLICKED contact events; replied
 * count item 423: REPLIED contact events; converted count item 424: conversion outcomes; open rate
 * item 425/451 / FR-104: opened / sent; click rate item 426/452 / FR-105: clicked / sent; conversion
 * rate item 427/453 / FR-106: converted / sent; estimated cost item 428: normalized monetary
 * estimate;
 * estimated revenue item 429: normalized monetary estimate; estimated ROI item 430/454 / FR-107:
 * (revenue − cost) / cost).
 */
package com.bayerwestphalian.campaign.analytics;
