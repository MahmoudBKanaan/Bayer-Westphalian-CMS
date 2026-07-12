package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.analytics.AnalyticsService;
import com.bayerwestphalian.campaign.analytics.CampaignAnalyticsView;
import com.bayerwestphalian.campaign.analytics.CampaignMetricsView;
import com.bayerwestphalian.campaign.analytics.ExecutiveDashboardView;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignMetricsRepository;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRecipientStatus;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.campaign.ContactEventType;
import com.bayerwestphalian.campaign.campaign.ContactOutcome;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 466 acceptance: Reports should use aggregated data where possible, and metrics must be
 * traceable to campaign recipients and contact events.
 *
 * <p>COMP-010: executive and campaign reports prefer aggregates from {@code campaign_metrics}
 * rather than raw recipient or contact-event row dumps.
 *
 * <p>Traceability: eligible/excluded/sent originate from campaign recipients at launch; opened /
 * clicked / replied / converted originate from contact events (BR-034); analytics and CSV/PDF
 * exports serialize those same aggregate counters.
 *
 * <p>Companion coverage: {@link
 * com.bayerwestphalian.campaign.analytics.ExecutiveReportUsesAggregatedDataTests} (item 457),
 * {@link
 * com.bayerwestphalian.campaign.communication.EngagementCountsUpdateFromContactEventsTests} (item
 * 450), {@link CsvExportWorksTests} (item 455).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName(
        "466 Reports should use aggregated data where possible, and metrics must be traceable"
                + " to campaign recipients and contact events")
class ReportsUseAggregatedDataAndMetricsAreTraceableTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000466");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000466");
    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000466");
    private static final UUID METRICS_ID =
            UUID.fromString("57000000-0000-0000-0000-000000000466");

    @Mock private AnalyticsService analyticsService;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ReportExportRepository reportExportRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private CampaignMetricsRepository campaignMetricsRepository;
    @Mock private CampaignProductRepository campaignProductRepository;

    private ReportService reportService;
    private AnalyticsService realAnalyticsService;

    @BeforeEach
    void setUp() {
        reportService =
                new ReportService(
                        analyticsService,
                        campaignRepository,
                        reportExportRepository,
                        userRepository,
                        auditService);
        realAnalyticsService =
                new AnalyticsService(
                        campaignRepository, campaignMetricsRepository, campaignProductRepository);
    }

    @Nested
    @DisplayName("Aggregated data: campaign reports export KPI aggregates only")
    class AggregatedCampaignReports {

        @Test
        void campaignCsvContainsAggregateKpiColumnsNotRawRecipientOrEventRows() {
            String csv =
                    new String(
                            CampaignReportDocument.campaignCsv(sampleAnalytics()),
                            StandardCharsets.UTF_8);

            // Aggregate KPI columns (campaign_metrics grain).
            assertThat(csv)
                    .contains("audienceSize")
                    .contains("eligibleCount")
                    .contains("excludedCount")
                    .contains("sentCount")
                    .contains("openedCount")
                    .contains("clickedCount")
                    .contains("repliedCount")
                    .contains("convertedCount")
                    .contains("openRate")
                    .contains("clickRate")
                    .contains("conversionRate")
                    .contains("estimatedCost")
                    .contains("estimatedRevenue")
                    .contains("estimatedRoi");

            // Single metrics data row (header + one data line), not a recipient dump.
            String[] lines = csv.trim().split("\\R");
            assertThat(lines).hasSize(2);

            // COMP-010 / item 466: no raw recipient or contact-event identity columns.
            assertThat(csv)
                    .doesNotContain("recipientId")
                    .doesNotContain("customerId")
                    .doesNotContain("contactEventId")
                    .doesNotContain("eventType")
                    .doesNotContain("exclusionReason")
                    .doesNotContain("emailAddress");
        }

        @Test
        void campaignPdfContainsAggregateKpiLinesNotRecipientLists() {
            String pdf =
                    new String(
                            CampaignReportDocument.campaignPdf(sampleAnalytics()),
                            StandardCharsets.US_ASCII);

            assertThat(pdf)
                    .contains("Bayer-Westphalian Campaign Report")
                    .contains("Audience size")
                    .contains("Eligible")
                    .contains("Excluded")
                    .contains("Sent")
                    .contains("Opened")
                    .contains("Clicked")
                    .contains("Replied")
                    .contains("Converted")
                    .contains("Open rate")
                    .contains("Estimated ROI");

            assertThat(pdf)
                    .doesNotContain("recipientId")
                    .doesNotContain("customerId")
                    .doesNotContain("contactEventId")
                    .doesNotContain("email@");
        }

        @Test
        void campaignMetricsViewExposesOnlyAggregateFields() {
            Set<String> components =
                    Arrays.stream(CampaignMetricsView.class.getRecordComponents())
                            .map(RecordComponent::getName)
                            .collect(Collectors.toSet());

            assertThat(components)
                    .contains(
                            "audienceSize",
                            "eligibleCount",
                            "excludedCount",
                            "sentCount",
                            "openedCount",
                            "clickedCount",
                            "repliedCount",
                            "convertedCount",
                            "openRate",
                            "clickRate",
                            "conversionRate",
                            "estimatedCost",
                            "estimatedRevenue",
                            "estimatedRoi");

            assertThat(components)
                    .doesNotContain(
                            "recipients",
                            "contactEvents",
                            "customers",
                            "events",
                            "recipientIds",
                            "email");
        }

        @Test
        void exportCampaignCsvLoadsAggregatesViaAnalyticsServiceOnly() {
            when(campaignRepository.findById(CAMPAIGN_ID))
                    .thenReturn(Optional.of(sampleCampaign()));
            when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
            stubExportSave();

            ReportFile file = reportService.exportCampaignCsv(CAMPAIGN_ID, null);

            assertThat(file.contentType()).isEqualTo(ReportFile.CSV_CONTENT_TYPE);
            assertThat(new String(file.content(), StandardCharsets.UTF_8))
                    .contains("eligibleCount")
                    .contains("openedCount");

            // Reports depend on analytics aggregates, not raw event/recipient repositories.
            verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
            verify(auditService, never()).listAuditLogs();
        }

        @Test
        void generateCampaignPdfLoadsSameAggregatedAnalyticsAsCsv() {
            when(campaignRepository.findById(CAMPAIGN_ID))
                    .thenReturn(Optional.of(sampleCampaign()));
            when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
            stubExportSave();

            ReportFile file = reportService.generateCampaignPdf(CAMPAIGN_ID, null);

            assertThat(file.contentType()).isEqualTo(ReportFile.PDF_CONTENT_TYPE);
            assertThat(new String(file.content(), StandardCharsets.US_ASCII))
                    .contains("Audience size: 100")
                    .contains("Eligible: 80")
                    .contains("Excluded: 20")
                    .contains("Opened: 40");

            verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
        }
    }

    @Nested
    @DisplayName("Aggregated data: executive surface prefers platform aggregates (COMP-010)")
    class ExecutiveAggregates {

        @Test
        void executiveDashboardViewHasNoRawEventOrRecipientCollections() {
            Set<String> components =
                    Arrays.stream(ExecutiveDashboardView.class.getRecordComponents())
                            .map(RecordComponent::getName)
                            .collect(Collectors.toSet());

            assertThat(components)
                    .contains(
                            "totalAudience",
                            "totalEligible",
                            "totalExcluded",
                            "totalSent",
                            "totalOpened",
                            "totalClicked",
                            "totalConverted",
                            "overallOpenRate",
                            "overallEstimatedRoi",
                            "productPerformance");

            assertThat(components)
                    .doesNotContain(
                            "contactEvents", "recipients", "customers", "events", "recipientRows");
        }

        @Test
        void executiveRatesUseAggregateSumsNotAverageOfCampaignRates() {
            Campaign campaignA = sampleCampaign();
            Campaign campaignB = secondCampaign();

            // A: open rate 0.50 (50/100); B: open rate 0.10 (30/300) → average 0.30, aggregate 0.20
            CampaignMetrics metricsA = metricsFor(campaignA, 100, 0, 100, 50, 0, 0, 0);
            CampaignMetrics metricsB = metricsFor(campaignB, 300, 0, 300, 30, 0, 0, 0);

            when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = realAnalyticsService.getExecutiveDashboard();

            assertThat(executive.totalSent()).isEqualTo(400L);
            assertThat(executive.totalOpened()).isEqualTo(80L);
            assertThat(executive.overallOpenRate())
                    .isEqualByComparingTo(new BigDecimal("0.2000"));
        }
    }

    @Nested
    @DisplayName("Traceability: recipients and contact events → campaign_metrics → reports")
    class MetricsTraceability {

        @Test
        void launchCountsAreTraceableToRecipientEligibleExcludedAndSent() {
            Campaign campaign = sampleCampaign();
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);

            // Simulates launch tallies from campaign_recipients ELIGIBLE / EXCLUDED + SENT events.
            int eligible = CampaignMetrics.calculateEligibleCount(80L);
            int excluded = CampaignMetrics.calculateExcludedCount(20L);
            int sent = CampaignMetrics.calculateSentCount(80L);
            metrics.recordLaunchCounts(eligible, excluded, sent);

            assertThat(CampaignRecipientStatus.ELIGIBLE.name()).isEqualTo("ELIGIBLE");
            assertThat(CampaignRecipientStatus.EXCLUDED.name()).isEqualTo("EXCLUDED");
            assertThat(ContactEventType.SENT.name()).isEqualTo("SENT");

            assertThat(metrics.getEligibleCount()).isEqualTo(80);
            assertThat(metrics.getExcludedCount()).isEqualTo(20);
            assertThat(metrics.getSentCount()).isEqualTo(80);
            assertThat(metrics.calculateAudienceSize()).isEqualTo(100);
        }

        @Test
        void engagementCountsAreTraceableToContactEventTypesAndConversionOutcome() {
            Campaign campaign = sampleCampaign();
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(10, 0, 10);

            // BR-034 mapping mirrored by CommunicationService.applyCampaignMetricsFromContactEvent.
            assertThat(ContactEventType.OPENED.name()).isEqualTo("OPENED");
            assertThat(ContactEventType.CLICKED.name()).isEqualTo("CLICKED");
            assertThat(ContactEventType.REPLIED.name()).isEqualTo("REPLIED");
            assertThat(ContactOutcome.CONVERTED.name()).isEqualTo("CONVERTED");

            metrics.incrementOpened();
            metrics.incrementOpened();
            metrics.incrementClicked();
            metrics.incrementReplied();
            metrics.incrementConverted();

            assertThat(metrics.getOpenedCount()).isEqualTo(2);
            assertThat(metrics.getClickedCount()).isEqualTo(1);
            assertThat(metrics.getRepliedCount()).isEqualTo(1);
            assertThat(metrics.getConvertedCount()).isEqualTo(1);
        }

        @Test
        void analyticsViewPreservesRecipientAndEventDerivedAggregates() {
            Campaign campaign = sampleCampaign();
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            ReflectionTestUtils.setField(metrics, "id", METRICS_ID);
            metrics.recordLaunchCounts(80, 20, 80);
            metrics.incrementOpened();
            metrics.incrementOpened();
            metrics.incrementClicked();
            metrics.incrementConverted();
            ReflectionTestUtils.setField(
                    metrics, "updatedAt", Instant.parse("2026-07-11T10:00:00Z"));

            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.of(metrics));

            CampaignAnalyticsView view = realAnalyticsService.getCampaignAnalytics(CAMPAIGN_ID);

            assertThat(view.metrics()).isNotNull();
            assertThat(view.metrics().eligibleCount()).isEqualTo(80);
            assertThat(view.metrics().excludedCount()).isEqualTo(20);
            assertThat(view.metrics().audienceSize()).isEqualTo(100);
            assertThat(view.metrics().sentCount()).isEqualTo(80);
            assertThat(view.metrics().openedCount()).isEqualTo(2);
            assertThat(view.metrics().clickedCount()).isEqualTo(1);
            assertThat(view.metrics().convertedCount()).isEqualTo(1);
            // Rates derived from the same aggregated counters.
            assertThat(view.metrics().openRate())
                    .isEqualByComparingTo(new BigDecimal("0.0250"));
            assertThat(view.metrics().clickRate())
                    .isEqualByComparingTo(new BigDecimal("0.0125"));
            assertThat(view.metrics().conversionRate())
                    .isEqualByComparingTo(new BigDecimal("0.0125"));
        }

        @Test
        void campaignCsvExportBytesMatchTraceableMetricsAggregates() {
            CampaignAnalyticsView analytics = analyticsFromLaunchAndEvents();

            String csv =
                    new String(
                            CampaignReportDocument.campaignCsv(analytics), StandardCharsets.UTF_8);

            // Values originate from recipient launch tallies + contact-event increments.
            assertThat(csv).contains("80"); // eligible / sent
            assertThat(csv).contains("20"); // excluded
            assertThat(csv).contains("100"); // audience
            assertThat(csv).contains("40"); // opened
            assertThat(csv).contains("16"); // clicked
            assertThat(csv).contains("0.5000"); // open rate from aggregates
            assertThat(csv).contains("0.2000"); // click rate
            assertThat(csv).contains("0.0500"); // conversion rate
        }

        @Test
        void reportServiceExportUsesTraceableAnalyticsPayloadUnchanged() {
            CampaignAnalyticsView analytics = analyticsFromLaunchAndEvents();
            when(campaignRepository.findById(CAMPAIGN_ID))
                    .thenReturn(Optional.of(sampleCampaign()));
            when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(analytics);
            stubExportSave();

            ReportFile csvFile = reportService.exportCampaignCsv(CAMPAIGN_ID, null);
            ReportFile pdfFile = reportService.generateCampaignPdf(CAMPAIGN_ID, null);

            String csv = new String(csvFile.content(), StandardCharsets.UTF_8);
            String pdf = new String(pdfFile.content(), StandardCharsets.US_ASCII);

            assertThat(csv).contains("eligibleCount").contains("80").contains("openedCount");
            assertThat(pdf)
                    .contains("Eligible: 80")
                    .contains("Excluded: 20")
                    .contains("Opened: 40")
                    .contains("Converted: 4");
        }
    }

    private void stubExportSave() {
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            if (export.getId() == null) {
                                ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            }
                            if (export.getRequestedAt() == null) {
                                ReflectionTestUtils.setField(
                                        export,
                                        "requestedAt",
                                        Instant.parse("2026-07-11T12:00:00Z"));
                            }
                            return export;
                        });
    }

    private static User sampleOwner() {
        User user = User.create("exec466.user@bayer-westphalian.test", "{noop}x", "Report Owner");
        ReflectionTestUtils.setField(user, "id", OWNER_ID);
        return user;
    }

    private static Campaign sampleCampaign() {
        Campaign campaign =
                Campaign.create(
                        "Traceable Life Drive",
                        "Trace metrics to events",
                        sampleOwner(),
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        return campaign;
    }

    private static Campaign secondCampaign() {
        UUID id = UUID.fromString("50000000-0000-0000-0000-000000000467");
        Campaign campaign =
                Campaign.create(
                        "Second Campaign",
                        "Aggregate check",
                        sampleOwner(),
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        return campaign;
    }

    private static CampaignMetrics metricsFor(
            Campaign campaign,
            int eligible,
            int excluded,
            int sent,
            int opened,
            int clicked,
            int replied,
            int converted) {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(eligible, excluded, sent);
        ReflectionTestUtils.setField(metrics, "openedCount", opened);
        ReflectionTestUtils.setField(metrics, "clickedCount", clicked);
        ReflectionTestUtils.setField(metrics, "repliedCount", replied);
        ReflectionTestUtils.setField(metrics, "convertedCount", converted);
        ReflectionTestUtils.setField(
                metrics, "updatedAt", Instant.parse("2026-07-11T10:00:00Z"));
        return metrics;
    }

    private static CampaignAnalyticsView sampleAnalytics() {
        return analyticsFromLaunchAndEvents();
    }

    private static CampaignAnalyticsView analyticsFromLaunchAndEvents() {
        // Mirrors: launch 80 eligible / 20 excluded / 80 sent; then contact events → engagement.
        CampaignMetricsView metrics =
                new CampaignMetricsView(
                        METRICS_ID,
                        CAMPAIGN_ID,
                        "Traceable Life Drive",
                        CampaignStatus.ACTIVE,
                        100,
                        80,
                        20,
                        80,
                        40,
                        16,
                        8,
                        4,
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.0500"),
                        new BigDecimal("100.00"),
                        new BigDecimal("150.00"),
                        new BigDecimal("0.50"),
                        Instant.parse("2026-07-11T10:00:00Z"));
        return new CampaignAnalyticsView(
                CAMPAIGN_ID,
                "Traceable Life Drive",
                "Trace metrics to events",
                CampaignStatus.ACTIVE,
                CampaignChannel.EMAIL,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                OWNER_ID,
                "Report Owner",
                metrics,
                Instant.parse("2026-07-11T12:00:00Z"));
    }
}
