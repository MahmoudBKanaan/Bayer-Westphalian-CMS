package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.analytics.AnalyticsService;
import com.bayerwestphalian.campaign.analytics.CampaignAnalyticsView;
import com.bayerwestphalian.campaign.analytics.CampaignMetricsView;
import com.bayerwestphalian.campaign.audit.AuditLogView;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 436: ReportService exports campaign CSV/PDF and audit reports, and records export
 * history.
 *
 * <p>Item 437 (FR-109) campaign CSV export is exercised by {@link
 * #exportCampaignCsvBuildsCsvMarksCompletedAndReturnsFile()} and the HTTP contract in {@link
 * CampaignCsvReportEndpointTests}. Acceptance item 455 formalizes end-to-end CSV export in {@link
 * CsvExportWorksTests}. Item 438 (FR-110) campaign PDF export is exercised by {@link
 * #generateCampaignPdfBuildsPdfMarksCompletedAndReturnsFile()} and {@link
 * CampaignPdfReportEndpointTests}. Item 439 export history store/list is exercised by history
 * methods and {@link ReportExportHistoryEndpointTests}. Unauthorized export is formalized under
 * KB item 458 in {@link UnauthorizedUserCannotExportRestrictedReportsTests}.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000436");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000436");
    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000436");
    private static final UUID AUDIT_ID = UUID.fromString("57000000-0000-0000-0000-000000000436");

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

    @Test
    void exportMethodsDeclareMethodLevelAuthorization() throws Exception {
        for (String methodName :
                List.of("exportCampaignCsv", "generateCampaignPdf", "campaignCsv", "campaignPdf")) {
            Method method =
                    ReportService.class.getMethod(methodName, UUID.class, UUID.class);
            assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
            assertThat(method.getAnnotation(PreAuthorize.class).value())
                    .contains("BI_ANALYST")
                    .contains("CAMPAIGN_MANAGER")
                    .contains("EXECUTIVE_VIEWER")
                    .contains("MARKETING_ANALYST");
        }

        Method audit = ReportService.class.getMethod("exportAuditReport", UUID.class);
        assertThat(audit.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(audit.getAnnotation(PreAuthorize.class).value())
                .contains("SYSTEM_AUDITOR")
                .contains("COMPLIANCE_OFFICER")
                .contains("ADMIN");

        for (String historyMethod :
                List.of("listExportHistory", "listExportHistoryForUser", "getExportHistory")) {
            Method method =
                    historyMethod.equals("listExportHistory")
                            ? ReportService.class.getMethod(historyMethod)
                            : ReportService.class.getMethod(historyMethod, UUID.class);
            assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        }
        Method byStatus =
                ReportService.class.getMethod("listExportHistoryByStatus", ReportExportStatus.class);
        assertThat(byStatus.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    @Test
    void exportCampaignCsvBuildsCsvMarksCompletedAndReturnsFile() {
        // KB item 437 / FR-109: service-side campaign CSV generation for the export endpoint.
        User requester = sampleRequester();
        Campaign campaign = sampleCampaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
        java.util.List<ReportExportStatus> savedStatuses = new java.util.ArrayList<>();
        java.util.List<String> savedFileUrls = new java.util.ArrayList<>();
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            savedStatuses.add(export.getStatus());
                            savedFileUrls.add(export.getFileUrl());
                            if (export.getId() == null) {
                                ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            }
                            if (export.getRequestedAt() == null) {
                                ReflectionTestUtils.setField(
                                        export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
                            }
                            return export;
                        });

        ReportFile file = reportService.exportCampaignCsv(CAMPAIGN_ID, REQUESTER_ID);

        assertThat(file.filename()).isEqualTo("Spring-Life-Drive.csv");
        assertThat(file.contentType()).isEqualTo(ReportFile.CSV_CONTENT_TYPE);
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
        assertThat(csv).contains("ACTIVE");
        assertThat(csv).contains("EMAIL");
        assertThat(csv).contains("0.5000");
        assertThat(csv).contains("150.00");

        verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
        verify(reportExportRepository, times(2)).save(any(ReportExport.class));
        // Item 531: successful CSV export writes EXPORT_REPORT audit trail.
        verify(auditService).logReportExport(eq(REQUESTER_ID), eq(EXPORT_ID), any(Map.class));
    }

    @Test
    void campaignCsvFr109IncludesKpiColumnsEvenWhenMetricsMissing() {
        // KB item 437: draft/not-launched campaigns still export identity columns with empty KPIs.
        Campaign campaign = sampleCampaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .thenReturn(
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
                                Instant.parse("2026-07-11T12:00:00Z")));
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            ReflectionTestUtils.setField(
                                    export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
                            return export;
                        });

        ReportFile file = reportService.campaignCsv(CAMPAIGN_ID, null);

        String csv = new String(file.content(), StandardCharsets.UTF_8);
        assertThat(file.export().exportType()).isEqualTo(ReportExportType.CSV);
        assertThat(csv).contains("campaignId,campaignName");
        assertThat(csv).contains(CAMPAIGN_ID.toString());
        assertThat(csv).contains("DRAFT");
        assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
    }

    @Test
    void campaignCsvAliasDelegatesToExportCampaignCsv() {
        Campaign campaign = sampleCampaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            ReflectionTestUtils.setField(
                                    export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
                            return export;
                        });

        ReportFile file = reportService.campaignCsv(CAMPAIGN_ID, null);

        assertThat(file.export().exportType()).isEqualTo(ReportExportType.CSV);
        assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(file.content().length).isPositive();
    }

    @Test
    void generateCampaignPdfBuildsPdfMarksCompletedAndReturnsFile() {
        // KB item 438 / FR-110: service-side campaign PDF generation for the export endpoint.
        Campaign campaign = sampleCampaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            ReflectionTestUtils.setField(
                                    export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
                            return export;
                        });

        ReportFile file = reportService.generateCampaignPdf(CAMPAIGN_ID, null);

        assertThat(file.filename()).isEqualTo("Spring-Life-Drive.pdf");
        assertThat(file.contentType()).isEqualTo(ReportFile.PDF_CONTENT_TYPE);
        assertThat(file.export().exportType()).isEqualTo(ReportExportType.PDF);
        assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(file.export().reportName()).isEqualTo("Campaign PDF: Spring Life Drive");
        assertThat(file.export().fileUrl()).contains(EXPORT_ID.toString()).endsWith(".pdf");
        assertThat(file.export().completedAt()).isNotNull();
        String pdf = new String(file.content(), StandardCharsets.US_ASCII);
        assertThat(pdf).startsWith("%PDF-1.4");
        assertThat(pdf).contains("Bayer-Westphalian Campaign Report");
        assertThat(pdf).contains("Spring Life Drive");
        assertThat(pdf).contains("Audience size");
        assertThat(pdf).contains("Open rate");
        assertThat(pdf).contains("Estimated ROI");
        assertThat(pdf).contains("%%EOF");

        verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
        verify(reportExportRepository, times(2)).save(any(ReportExport.class));
    }

    @Test
    void campaignPdfAliasDelegatesToGenerateCampaignPdf() {
        Campaign campaign = sampleCampaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            ReflectionTestUtils.setField(
                                    export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
                            return export;
                        });

        ReportFile file = reportService.campaignPdf(CAMPAIGN_ID, null);

        assertThat(file.export().exportType()).isEqualTo(ReportExportType.PDF);
        assertThat(file.contentType()).isEqualTo(ReportFile.PDF_CONTENT_TYPE);
    }

    @Test
    void campaignPdfFr110IncludesIdentityWhenMetricsMissing() {
        // KB item 438: draft/not-launched campaigns still export identity lines without KPIs.
        Campaign campaign = sampleCampaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .thenReturn(
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
                                Instant.parse("2026-07-11T12:00:00Z")));
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            ReflectionTestUtils.setField(
                                    export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
                            return export;
                        });

        ReportFile file = reportService.campaignPdf(CAMPAIGN_ID, null);

        String pdf = new String(file.content(), StandardCharsets.US_ASCII);
        assertThat(file.export().exportType()).isEqualTo(ReportExportType.PDF);
        assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(pdf).startsWith("%PDF-1.4");
        assertThat(pdf).contains("Spring Life Drive");
        assertThat(pdf).contains("DRAFT");
        assertThat(pdf).contains("Metrics: \\(none recorded yet\\)");
    }

    @Test
    void exportCampaignCsvRequiresCampaignId() {
        assertThatThrownBy(() -> reportService.exportCampaignCsv(null, REQUESTER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campaign report validation failed");
        verify(reportExportRepository, never()).save(any());
    }

    @Test
    void exportCampaignCsvThrowsWhenCampaignMissing() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.exportCampaignCsv(CAMPAIGN_ID, REQUESTER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaign");
        verify(reportExportRepository, never()).save(any());
        verify(analyticsService, never()).getCampaignAnalytics(any());
    }

    @Test
    void exportCampaignCsvMarksFailedWhenAnalyticsThrows() {
        Campaign campaign = sampleCampaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));
        java.util.List<ReportExportStatus> savedStatuses = new java.util.ArrayList<>();
        java.util.List<String> savedFileUrls = new java.util.ArrayList<>();
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            savedStatuses.add(export.getStatus());
                            savedFileUrls.add(export.getFileUrl());
                            if (export.getId() == null) {
                                ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            }
                            if (export.getRequestedAt() == null) {
                                ReflectionTestUtils.setField(
                                        export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
                            }
                            return export;
                        });

        assertThatThrownBy(() -> reportService.exportCampaignCsv(CAMPAIGN_ID, null))
                .isInstanceOf(ResourceNotFoundException.class);

        ArgumentCaptor<ReportExport> captor = ArgumentCaptor.forClass(ReportExport.class);
        verify(reportExportRepository, times(2)).save(captor.capture());
        ReportExport last = captor.getAllValues().get(1);
        assertThat(last.getStatus()).isEqualTo(ReportExportStatus.FAILED);
        assertThat(last.getFileUrl()).isNull();
        // Item 531: failed exports do not emit EXPORT_REPORT.
        verify(auditService, never()).logReportExport(any(), any(), any());
    }

    @Test
    void exportAuditReportBuildsCsvAndCompletesHistory() {
        User requester = sampleRequester();
        when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
        when(auditService.listAuditLogs())
                .thenReturn(
                        List.of(
                                new AuditLogView(
                                        AUDIT_ID,
                                        REQUESTER_ID,
                                        "APPROVE",
                                        "campaigns",
                                        CAMPAIGN_ID,
                                        null,
                                        null,
                                        "127.0.0.1",
                                        Instant.parse("2026-07-11T09:00:00Z"))));
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            ReflectionTestUtils.setField(export, "id", EXPORT_ID);
                            ReflectionTestUtils.setField(
                                    export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
                            return export;
                        });

        ReportFile file = reportService.exportAuditReport(REQUESTER_ID);

        assertThat(file.filename()).isEqualTo("audit-history.csv");
        assertThat(file.contentType()).isEqualTo(ReportFile.CSV_CONTENT_TYPE);
        assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(file.export().reportName()).isEqualTo("Audit history export");
        String csv = new String(file.content(), StandardCharsets.UTF_8);
        assertThat(csv).contains("id,actorUserId,action,entityType");
        assertThat(csv).contains("APPROVE");
        assertThat(csv).contains(CAMPAIGN_ID.toString());

        ReportFile alias = reportService.auditReport(REQUESTER_ID);
        assertThat(alias.filename()).isEqualTo("audit-history.csv");
        // Item 531: audit-history export also writes EXPORT_REPORT (twice: method + alias).
        verify(auditService, times(2)).logReportExport(eq(REQUESTER_ID), eq(EXPORT_ID), any(Map.class));
    }

    @Test
    void listExportHistoryMapsRepositoryRows() {
        // KB item 439: list stored report_exports history newest first.
        ReportExport completed =
                ReportExport.request(sampleRequester(), "Campaign CSV: X", ReportExportType.CSV);
        ReflectionTestUtils.setField(completed, "id", EXPORT_ID);
        ReflectionTestUtils.setField(
                completed, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
        completed.markCompleted("local://reports/" + EXPORT_ID + "/x.csv");

        when(reportExportRepository.findAllByOrderByRequestedAtDesc())
                .thenReturn(List.of(completed));

        List<ReportExportView> history = reportService.listExportHistory();

        assertThat(history).hasSize(1);
        assertThat(history.get(0).id()).isEqualTo(EXPORT_ID);
        assertThat(history.get(0).status()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(history.get(0).fileUrl()).contains("x.csv");
        assertThat(history.get(0).reportName()).isEqualTo("Campaign CSV: X");
        assertThat(history.get(0).exportType()).isEqualTo(ReportExportType.CSV);
    }

    @Test
    void listExportHistoryForUserRequiresUserId() {
        assertThatThrownBy(() -> reportService.listExportHistoryForUser(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("requestedByUserId");
    }

    @Test
    void listExportHistoryForUserDelegatesToRepository() {
        when(reportExportRepository.findByRequestedByUserId(REQUESTER_ID)).thenReturn(List.of());

        assertThat(reportService.listExportHistoryForUser(REQUESTER_ID)).isEmpty();
        verify(reportExportRepository).findByRequestedByUserId(REQUESTER_ID);
    }

    @Test
    void listExportHistoryByStatusRequiresStatusAndMapsRows() {
        // KB item 439: filter history by COMPLETED / FAILED / REQUESTED.
        ReportExport failed =
                ReportExport.request(sampleRequester(), "Campaign PDF: Y", ReportExportType.PDF);
        ReflectionTestUtils.setField(failed, "id", EXPORT_ID);
        ReflectionTestUtils.setField(failed, "requestedAt", Instant.parse("2026-07-11T11:00:00Z"));
        failed.markFailed();

        when(reportExportRepository.findByStatusOrderByRequestedAtDesc(ReportExportStatus.FAILED))
                .thenReturn(List.of(failed));

        List<ReportExportView> history =
                reportService.listExportHistoryByStatus(ReportExportStatus.FAILED);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo(ReportExportStatus.FAILED);
        assertThat(history.get(0).fileUrl()).isNull();

        assertThatThrownBy(() -> reportService.listExportHistoryByStatus(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("status");
    }

    @Test
    void getExportHistoryReturnsStoredRow() {
        ReportExport completed =
                ReportExport.request(sampleRequester(), "Campaign CSV: X", ReportExportType.CSV);
        ReflectionTestUtils.setField(completed, "id", EXPORT_ID);
        ReflectionTestUtils.setField(
                completed, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
        completed.markCompleted("local://reports/" + EXPORT_ID + "/x.csv");
        when(reportExportRepository.findById(EXPORT_ID)).thenReturn(Optional.of(completed));

        ReportExportView view = reportService.getExportHistory(EXPORT_ID);

        assertThat(view.id()).isEqualTo(EXPORT_ID);
        assertThat(view.status()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(view.fileUrl()).contains("x.csv");
    }

    @Test
    void getExportHistoryThrowsWhenMissing() {
        when(reportExportRepository.findById(EXPORT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getExportHistory(EXPORT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ReportExport");
    }

    @Test
    void getExportHistoryRequiresExportId() {
        assertThatThrownBy(() -> reportService.getExportHistory(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exportId");
    }

    @Test
    void exportCampaignCsvStoresRequestedThenCompletedHistory() {
        // KB item 439: every successful export persists REQUESTED then COMPLETED history.
        User requester = sampleRequester();
        Campaign campaign = sampleCampaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
        java.util.List<ReportExportStatus> savedStatuses = new java.util.ArrayList<>();
        java.util.List<String> savedFileUrls = new java.util.ArrayList<>();
        when(reportExportRepository.save(any(ReportExport.class)))
                .thenAnswer(
                        invocation -> {
                            ReportExport export = invocation.getArgument(0);
                            savedStatuses.add(export.getStatus());
                            savedFileUrls.add(export.getFileUrl());
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

        verify(reportExportRepository, times(2)).save(any(ReportExport.class));
        assertThat(savedStatuses)
                .containsExactly(ReportExportStatus.REQUESTED, ReportExportStatus.COMPLETED);
        assertThat(savedFileUrls.get(1)).contains(EXPORT_ID.toString()).endsWith(".csv");
    }

    private static User sampleRequester() {
        User user =
                User.create("report.user@bayer-westphalian.test", "{noop}x", "Report User");
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
