package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.analytics.AnalyticsService;
import com.bayerwestphalian.campaign.analytics.CampaignAnalyticsView;
import com.bayerwestphalian.campaign.analytics.CampaignMetricsView;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 455 acceptance: CSV export works.
 *
 * <p>Item 437 / FR-109 definition: authorized users can export campaign performance as CSV via
 * {@code GET /api/reports/campaigns/{campaignId}/csv}. The export includes campaign identity and
 * KPI columns (audience, engagement, rates, cost/revenue/ROI), uses {@code text/csv} content type,
 * and records a completed {@link ReportExport} history row.
 *
 * <p>Companion coverage also lives in {@link CampaignReportDocumentTests}, {@link
 * ReportServiceTests}, and the HTTP contract in {@link CampaignCsvReportEndpointTests} (item 437).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("455 CSV export works")
class CsvExportWorksTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000455");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000455");
    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000455");
    private static final UUID MISSING_ID =
            UUID.fromString("50000000-0000-0000-0000-00000000dead");

    @Mock private AnalyticsService analyticsService;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ReportExportRepository reportExportRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService =
                new ReportService(
                        analyticsService,
                        campaignRepository,
                        reportExportRepository,
                        userRepository,
                        auditService);
    }

    @Nested
    @DisplayName("Document: campaign CSV content (FR-109)")
    class Document {

        @Test
        void campaignCsvIncludesHeaderAndKpiDataRow() {
            String csv =
                    new String(
                            CampaignReportDocument.campaignCsv(sampleAnalytics()),
                            StandardCharsets.UTF_8);

            assertThat(csv).startsWith("campaignId,campaignName,objective,status,channel");
            assertThat(csv).contains("audienceSize,eligibleCount,excludedCount,sentCount");
            assertThat(csv).contains("openedCount,clickedCount,repliedCount,convertedCount");
            assertThat(csv).contains("openRate,clickRate,conversionRate");
            assertThat(csv).contains("estimatedCost,estimatedRevenue,estimatedRoi,generatedAt");
            assertThat(csv).contains(CAMPAIGN_ID.toString());
            assertThat(csv).contains("Spring Life Drive");
            assertThat(csv).contains("ACTIVE");
            assertThat(csv).contains("EMAIL");
            assertThat(csv).contains("0.5000");
            assertThat(csv).contains("0.2000");
            assertThat(csv).contains("0.0500");
            assertThat(csv).contains("100.00");
            assertThat(csv).contains("150.00");
            assertThat(csv).contains("0.50");
        }

        @Test
        void campaignCsvExportsIdentityWhenMetricsMissing() {
            CampaignAnalyticsView draft =
                    new CampaignAnalyticsView(
                            CAMPAIGN_ID,
                            "Spring Life Drive",
                            "Raise awareness",
                            CampaignStatus.DRAFT,
                            CampaignChannel.EMAIL,
                            null,
                            null,
                            REQUESTER_ID,
                            "Report User",
                            null,
                            Instant.parse("2026-07-11T12:00:00Z"));

            String csv =
                    new String(CampaignReportDocument.campaignCsv(draft), StandardCharsets.UTF_8);

            assertThat(csv).contains("campaignId,campaignName");
            assertThat(csv).contains(CAMPAIGN_ID.toString());
            assertThat(csv).contains("DRAFT");
            assertThat(csv).contains("openRate,clickRate,conversionRate");
        }

        @Test
        void campaignCsvEscapesCommasAndQuotes() {
            CampaignAnalyticsView analytics =
                    new CampaignAnalyticsView(
                            CAMPAIGN_ID,
                            "Life, \"Premium\" Plan",
                            "Win, retain",
                            CampaignStatus.ACTIVE,
                            CampaignChannel.EMAIL,
                            null,
                            null,
                            REQUESTER_ID,
                            "Ada, Lovelace",
                            null,
                            Instant.parse("2026-07-11T12:00:00Z"));

            String csv =
                    new String(
                            CampaignReportDocument.campaignCsv(analytics), StandardCharsets.UTF_8);

            assertThat(csv).contains("\"Life, \"\"Premium\"\" Plan\"");
            assertThat(csv).contains("\"Win, retain\"");
            assertThat(csv).contains("\"Ada, Lovelace\"");
        }

        @Test
        void campaignFilenameUsesCsvExtension() {
            assertThat(
                            CampaignReportDocument.campaignFilename(
                                    sampleAnalytics(), ReportExportType.CSV))
                    .isEqualTo("Spring-Life-Drive.csv");
        }
    }

    @Nested
    @DisplayName("Service: exportCampaignCsv / campaignCsv")
    class ServiceExport {

        @Test
        void exportCampaignCsvBuildsCsvMarksCompletedAndReturnsFile() {
            User requester = sampleRequester();
            Campaign campaign = sampleCampaign();
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
            when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
            stubExportSave();

            ReportFile file = reportService.exportCampaignCsv(CAMPAIGN_ID, REQUESTER_ID);

            assertThat(file.filename()).isEqualTo("Spring-Life-Drive.csv");
            assertThat(file.contentType()).isEqualTo(ReportFile.CSV_CONTENT_TYPE);
            assertThat(file.content().length).isPositive();
            assertThat(file.export().id()).isEqualTo(EXPORT_ID);
            assertThat(file.export().exportType()).isEqualTo(ReportExportType.CSV);
            assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
            assertThat(file.export().reportName()).isEqualTo("Campaign CSV: Spring Life Drive");
            assertThat(file.export().requestedByUserId()).isEqualTo(REQUESTER_ID);
            assertThat(file.export().fileUrl()).contains(EXPORT_ID.toString()).endsWith(".csv");
            assertThat(file.export().completedAt()).isNotNull();

            String csv = new String(file.content(), StandardCharsets.UTF_8);
            assertThat(csv).contains("campaignId,campaignName,objective,status");
            assertThat(csv).contains("audienceSize,eligibleCount,excludedCount,sentCount");
            assertThat(csv).contains("openRate,clickRate,conversionRate");
            assertThat(csv).contains("estimatedCost,estimatedRevenue,estimatedRoi");
            assertThat(csv).contains(CAMPAIGN_ID.toString());
            assertThat(csv).contains("Spring Life Drive");
            assertThat(csv).contains("0.5000");
            assertThat(csv).contains("150.00");

            verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
            verify(reportExportRepository, times(2)).save(any(ReportExport.class));
        }

        @Test
        void campaignCsvAliasProducesCompletedCsvExport() {
            when(campaignRepository.findById(CAMPAIGN_ID))
                    .thenReturn(Optional.of(sampleCampaign()));
            when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
            stubExportSave();

            ReportFile file = reportService.campaignCsv(CAMPAIGN_ID, null);

            assertThat(file.export().exportType()).isEqualTo(ReportExportType.CSV);
            assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
            assertThat(file.contentType()).isEqualTo(ReportFile.CSV_CONTENT_TYPE);
            assertThat(new String(file.content(), StandardCharsets.UTF_8))
                    .contains("Spring Life Drive");
        }

        @Test
        void exportCampaignCsvStoresRequestedThenCompletedHistory() {
            User requester = sampleRequester();
            when(campaignRepository.findById(CAMPAIGN_ID))
                    .thenReturn(Optional.of(sampleCampaign()));
            when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
            when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
            stubExportSave();

            java.util.List<ReportExportStatus> statuses = new java.util.ArrayList<>();
            java.util.List<ReportExportType> types = new java.util.ArrayList<>();
            java.util.List<String> names = new java.util.ArrayList<>();
            java.util.List<String> urls = new java.util.ArrayList<>();
            when(reportExportRepository.save(any(ReportExport.class)))
                    .thenAnswer(
                            invocation -> {
                                ReportExport export = invocation.getArgument(0);
                                statuses.add(export.getStatus());
                                types.add(export.getExportType());
                                names.add(export.getReportName());
                                urls.add(export.getFileUrl());
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

            reportService.exportCampaignCsv(CAMPAIGN_ID, REQUESTER_ID);

            assertThat(statuses)
                    .containsExactly(ReportExportStatus.REQUESTED, ReportExportStatus.COMPLETED);
            assertThat(types.get(0)).isEqualTo(ReportExportType.CSV);
            assertThat(names.get(0)).isEqualTo("Campaign CSV: Spring Life Drive");
            assertThat(urls.get(1)).endsWith(".csv");
        }

        @Test
        void exportCampaignCsvThrowsWhenCampaignMissing() {
            when(campaignRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.exportCampaignCsv(MISSING_ID, REQUESTER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(reportExportRepository, never()).save(any(ReportExport.class));
        }

        @Test
        void exportCampaignCsvRequiresCampaignId() {
            assertThatThrownBy(() -> reportService.exportCampaignCsv(null, REQUESTER_ID))
                    .isInstanceOf(ValidationException.class);
        }
    }

    private void stubExportSave() {
        lenient()
                .when(reportExportRepository.save(any(ReportExport.class)))
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

    private static User sampleRequester() {
        User user = User.create("csv455.user@bayer-westphalian.test", "{noop}x", "Report User");
        ReflectionTestUtils.setField(user, "id", REQUESTER_ID);
        return user;
    }

    private static Campaign sampleCampaign() {
        User owner = sampleRequester();
        Campaign campaign =
                Campaign.create(
                        "Spring Life Drive",
                        "Raise awareness",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        return campaign;
    }

    private static CampaignAnalyticsView sampleAnalytics() {
        CampaignMetricsView metrics =
                new CampaignMetricsView(
                        null,
                        CAMPAIGN_ID,
                        "Spring Life Drive",
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
                "Spring Life Drive",
                "Raise awareness",
                CampaignStatus.ACTIVE,
                CampaignChannel.EMAIL,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                REQUESTER_ID,
                "Report User",
                metrics,
                Instant.parse("2026-07-11T12:00:00Z"));
    }
}
