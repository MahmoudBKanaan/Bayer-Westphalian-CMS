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
 * KB item 456 acceptance: PDF export works.
 *
 * <p>Item 438 / FR-110 definition: authorized users can export campaign performance as PDF via
 * {@code GET /api/reports/campaigns/{campaignId}/pdf}. The export is a minimal PDF 1.4 document
 * with campaign identity and the same KPI narrative as CSV (audience, engagement, rates,
 * cost/revenue/ROI), uses {@code application/pdf} content type, and records a completed {@link
 * ReportExport} history row.
 *
 * <p>Companion coverage also lives in {@link CampaignReportDocumentTests}, {@link
 * ReportServiceTests}, and the HTTP contract in {@link CampaignPdfReportEndpointTests} (item 438).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("456 PDF export works")
class PdfExportWorksTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000456");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000456");
    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000456");
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
    @DisplayName("Document: campaign PDF content (FR-110)")
    class Document {

        @Test
        void campaignPdfIsValidPdf14WithTitleIdentityAndKpis() {
            String pdf =
                    new String(
                            CampaignReportDocument.campaignPdf(sampleAnalytics()),
                            StandardCharsets.US_ASCII);

            assertThat(pdf).startsWith("%PDF-1.4");
            assertThat(pdf).contains("%%EOF");
            assertThat(pdf).contains("Bayer-Westphalian Campaign Report");
            assertThat(pdf).contains("Spring Life Drive");
            assertThat(pdf).contains(CAMPAIGN_ID.toString());
            assertThat(pdf).contains("Raise awareness");
            assertThat(pdf).contains("ACTIVE");
            assertThat(pdf).contains("EMAIL");
            assertThat(pdf).contains("Report User");
            assertThat(pdf).contains("Audience size");
            assertThat(pdf).contains("Eligible");
            assertThat(pdf).contains("Excluded");
            assertThat(pdf).contains("Sent");
            assertThat(pdf).contains("Opened");
            assertThat(pdf).contains("Clicked");
            assertThat(pdf).contains("Replied");
            assertThat(pdf).contains("Converted");
            assertThat(pdf).contains("Open rate");
            assertThat(pdf).contains("Click rate");
            assertThat(pdf).contains("Conversion rate");
            assertThat(pdf).contains("Estimated cost");
            assertThat(pdf).contains("Estimated revenue");
            assertThat(pdf).contains("Estimated ROI");
            assertThat(pdf).contains("Generated at");
            // KPI values from metrics (same analytics source as CSV / item 455).
            assertThat(pdf).contains("100");
            assertThat(pdf).contains("80");
            assertThat(pdf).contains("0.5000");
            assertThat(pdf).contains("0.50");
        }

        @Test
        void campaignPdfShowsPlaceholderWhenMetricsMissing() {
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

            String pdf =
                    new String(
                            CampaignReportDocument.campaignPdf(draft), StandardCharsets.US_ASCII);

            assertThat(pdf).startsWith("%PDF-1.4");
            assertThat(pdf).contains("Bayer-Westphalian Campaign Report");
            assertThat(pdf).contains(CAMPAIGN_ID.toString());
            assertThat(pdf).contains("DRAFT");
            // Parentheses are PDF-escaped in content streams.
            assertThat(pdf).contains("none recorded yet");
            assertThat(pdf).doesNotContain("Audience size:");
            assertThat(pdf).doesNotContain("Open rate:");
        }

        @Test
        void campaignPdfEscapesParenthesesInTextContent() {
            CampaignAnalyticsView analytics =
                    new CampaignAnalyticsView(
                            CAMPAIGN_ID,
                            "Life (Premium) Plan",
                            "Win (retain)",
                            CampaignStatus.ACTIVE,
                            CampaignChannel.EMAIL,
                            null,
                            null,
                            REQUESTER_ID,
                            "Ada (Lovelace)",
                            null,
                            Instant.parse("2026-07-11T12:00:00Z"));

            String pdf =
                    new String(
                            CampaignReportDocument.campaignPdf(analytics),
                            StandardCharsets.US_ASCII);

            // PDF string literals escape parentheses as \( \)
            assertThat(pdf).contains("Life \\(Premium\\) Plan");
            assertThat(pdf).contains("Win \\(retain\\)");
            assertThat(pdf).contains("Ada \\(Lovelace\\)");
        }

        @Test
        void campaignFilenameUsesPdfExtension() {
            assertThat(
                            CampaignReportDocument.campaignFilename(
                                    sampleAnalytics(), ReportExportType.PDF))
                    .isEqualTo("Spring-Life-Drive.pdf");
        }
    }

    @Nested
    @DisplayName("Service: generateCampaignPdf / campaignPdf")
    class ServiceExport {

        @Test
        void generateCampaignPdfBuildsPdfMarksCompletedAndReturnsFile() {
            User requester = sampleRequester();
            Campaign campaign = sampleCampaign();
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
            when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
            stubExportSave();

            ReportFile file = reportService.generateCampaignPdf(CAMPAIGN_ID, REQUESTER_ID);

            assertThat(file.filename()).isEqualTo("Spring-Life-Drive.pdf");
            assertThat(file.contentType()).isEqualTo(ReportFile.PDF_CONTENT_TYPE);
            assertThat(file.contentType()).isEqualTo("application/pdf");
            assertThat(file.content().length).isPositive();
            assertThat(file.export().id()).isEqualTo(EXPORT_ID);
            assertThat(file.export().exportType()).isEqualTo(ReportExportType.PDF);
            assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
            assertThat(file.export().reportName()).isEqualTo("Campaign PDF: Spring Life Drive");
            assertThat(file.export().requestedByUserId()).isEqualTo(REQUESTER_ID);
            assertThat(file.export().fileUrl()).contains(EXPORT_ID.toString()).endsWith(".pdf");
            assertThat(file.export().completedAt()).isNotNull();

            String pdf = new String(file.content(), StandardCharsets.US_ASCII);
            assertThat(pdf).startsWith("%PDF-1.4");
            assertThat(pdf).contains("Bayer-Westphalian Campaign Report");
            assertThat(pdf).contains("Spring Life Drive");
            assertThat(pdf).contains(CAMPAIGN_ID.toString());
            assertThat(pdf).contains("Audience size");
            assertThat(pdf).contains("Open rate");
            assertThat(pdf).contains("Estimated ROI");
            assertThat(pdf).contains("%%EOF");

            verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
            verify(reportExportRepository, times(2)).save(any(ReportExport.class));
        }

        @Test
        void campaignPdfAliasProducesCompletedPdfExport() {
            when(campaignRepository.findById(CAMPAIGN_ID))
                    .thenReturn(Optional.of(sampleCampaign()));
            when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
            stubExportSave();

            ReportFile file = reportService.campaignPdf(CAMPAIGN_ID, null);

            assertThat(file.export().exportType()).isEqualTo(ReportExportType.PDF);
            assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
            assertThat(file.contentType()).isEqualTo(ReportFile.PDF_CONTENT_TYPE);
            assertThat(new String(file.content(), StandardCharsets.US_ASCII))
                    .contains("Spring Life Drive")
                    .startsWith("%PDF-1.4");
        }

        @Test
        void generateCampaignPdfStoresRequestedThenCompletedHistory() {
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

            reportService.generateCampaignPdf(CAMPAIGN_ID, REQUESTER_ID);

            assertThat(statuses)
                    .containsExactly(ReportExportStatus.REQUESTED, ReportExportStatus.COMPLETED);
            assertThat(types.get(0)).isEqualTo(ReportExportType.PDF);
            assertThat(names.get(0)).isEqualTo("Campaign PDF: Spring Life Drive");
            assertThat(urls.get(1)).endsWith(".pdf");
        }

        @Test
        void generateCampaignPdfThrowsWhenCampaignMissing() {
            when(campaignRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.generateCampaignPdf(MISSING_ID, REQUESTER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(reportExportRepository, never()).save(any(ReportExport.class));
        }

        @Test
        void generateCampaignPdfRequiresCampaignId() {
            assertThatThrownBy(() -> reportService.generateCampaignPdf(null, REQUESTER_ID))
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
        User user = User.create("pdf456.user@bayer-westphalian.test", "{noop}x", "Report User");
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
