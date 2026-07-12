package com.bayerwestphalian.campaign.analytics;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics REST API (KB epic E19 / items 416, 431+).
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>{@code GET /api/analytics/dashboard} — platform dashboard KPIs (item 431 / FR-100–FR-107)
 *   <li>{@code GET /api/analytics/campaigns/{campaignId}} — campaign analytics detail (item 432)
 *   <li>{@code GET /api/analytics/products/performance} — product performance rows (item 433)
 *   <li>{@code GET /api/analytics/executive} — executive aggregate dashboard (item 434 /
 *       item 457 / COMP-010)
 * </ul>
 *
 * <p>Access is limited to Admin, BI Analyst, Campaign Manager, Marketing Analyst, and Executive
 * Viewer (matches {@code SecurityConfiguration} {@code /api/analytics/**} rules).
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final String ANALYTICS_READ =
            "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER', "
                    + "'MARKETING_ANALYST', 'EXECUTIVE_VIEWER')";

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Platform dashboard KPIs (KB item 431 / {@code GET /api/analytics/dashboard}).
     *
     * <p>Returns {@link DashboardView} covering FR-100–FR-107:
     *
     * <ul>
     *   <li>FR-100 campaign totals
     *   <li>FR-101 active campaigns
     *   <li>FR-102 audience size
     *   <li>FR-103 messages sent
     *   <li>FR-104 open rate
     *   <li>FR-105 click rate
     *   <li>FR-106 conversion rate
     *   <li>FR-107 estimated ROI
     * </ul>
     *
     * <p>Also includes eligible/excluded/opened/clicked/replied/converted counts and estimated
     * cost/revenue for drill-down consistency with campaign metrics.
     */
    @GetMapping("/dashboard")
    @PreAuthorize(ANALYTICS_READ)
    public ResponseEntity<ApiResponse<DashboardView>> getDashboard() {
        DashboardView dashboard = analyticsService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("Analytics dashboard loaded", dashboard));
    }

    /**
     * Single-campaign analytics detail (KB item 432 / {@code GET
     * /api/analytics/campaigns/{campaignId}}).
     *
     * <p>Returns {@link CampaignAnalyticsView}: campaign identity (name, objective, status,
     * channel, dates, owner) plus optional {@link CampaignMetricsView} counters and rates (audience,
     * eligible/excluded, sent/opened/clicked/replied/converted, open/click/conversion rates, cost,
     * revenue, ROI).
     *
     * <p>Responds {@code 404} when the campaign does not exist. Metrics may be {@code null} when the
     * campaign has not been launched / has no metrics row yet.
     */
    @GetMapping("/campaigns/{campaignId}")
    @PreAuthorize(ANALYTICS_READ)
    public ResponseEntity<ApiResponse<CampaignAnalyticsView>> getCampaignAnalytics(
            @PathVariable UUID campaignId) {
        CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(campaignId);
        return ResponseEntity.ok(ApiResponse.success("Campaign analytics loaded", analytics));
    }

    /**
     * Product performance aggregates (KB item 433 / {@code GET
     * /api/analytics/products/performance}).
     *
     * <p>Returns a list of {@link ProductPerformanceView} rows: one per product linked to at least
     * one campaign, with summed audience/eligible/sent/opened/clicked/converted counts, open/click/
     * conversion rates, estimated cost/revenue/ROI, and campaign count. Empty list when no
     * campaign–product links exist.
     */
    @GetMapping("/products/performance")
    @PreAuthorize(ANALYTICS_READ)
    public ResponseEntity<ApiResponse<List<ProductPerformanceView>>> getProductPerformance() {
        List<ProductPerformanceView> rows = analyticsService.getProductPerformance();
        return ResponseEntity.ok(ApiResponse.success("Product performance loaded", rows));
    }

    /**
     * Executive aggregate dashboard (KB item 434 / item 457 / COMP-010 / {@code GET
     * /api/analytics/executive}).
     *
     * <p>Returns {@link ExecutiveDashboardView} with platform-level aggregated KPIs for
     * management reporting:
     *
     * <ul>
     *   <li>Campaign inventory: total, active, and completed campaign counts
     *   <li>Audience funnel: total audience, eligible, excluded, sent
     *   <li>Engagement: opened, clicked, replied, converted totals
     *   <li>Rates from aggregates (opened|clicked|converted ÷ sent) — FR-104–FR-106 style
     *   <li>Estimated cost, revenue, and ROI from summed financials — FR-107 style
     *   <li>Embedded product performance summary rows (same aggregation as item 433)
     * </ul>
     *
     * <p>COMP-010 / item 457: values are aggregates across campaigns/metrics rather than raw
     * contact-event rows. Empty inventory yields zeroed counts/rates and an empty product
     * performance list.
     */
    @GetMapping("/executive")
    @PreAuthorize(ANALYTICS_READ)
    public ResponseEntity<ApiResponse<ExecutiveDashboardView>> getExecutiveDashboard() {
        ExecutiveDashboardView dashboard = analyticsService.getExecutiveDashboard();
        return ResponseEntity.ok(ApiResponse.success("Executive dashboard loaded", dashboard));
    }
}
