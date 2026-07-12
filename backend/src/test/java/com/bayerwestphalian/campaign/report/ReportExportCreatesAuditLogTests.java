package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.analytics.AnalyticsService;
import com.bayerwestphalian.campaign.analytics.CampaignAnalyticsView;
import com.bayerwestphalian.campaign.analytics.CampaignMetricsView;
import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditLogRepository;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 531 (FR-109–FR-110): successful report exports write an immutable {@code EXPORT_REPORT}
 * audit row on entity type {@code report_exports}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("531 Log report exports")
class ReportExportCreatesAuditLogTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000531");
    private static final UUID EXPORT_ID =
            UUID.fromString("43000000-0000-0000-0000-000000000531");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000531");

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AnalyticsService analyticsService;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ReportExportRepository reportExportRepository;
    @Mock private UserRepository userRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        AuditService auditService = new AuditService(auditLogRepository);
        lenient()
                .when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reportService =
                new ReportService(
                        analyticsService,
                        campaignRepository,
                        reportExportRepository,
                        userRepository,
                        auditService);
    }

    @Test
    void exportCampaignCsvPersistsExportReportAuditWithActorAndPayload() throws Exception {
        User requester = requester();
        Campaign campaign = campaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
        stubExportSave();

        ReportFile file = reportService.exportCampaignCsv(CAMPAIGN_ID, REQUESTER_ID);

        assertThat(file.export().status()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(file.export().exportType()).isEqualTo(ReportExportType.CSV);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("EXPORT_REPORT");
        assertThat(auditLog.getEntityType()).isEqualTo("report_exports");
        assertThat(auditLog.getEntityId()).isEqualTo(EXPORT_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(REQUESTER_ID);
        assertThat(auditLog.getOldValue()).isNull();
        assertThat(auditLog.getNewValue())
                .containsEntry("id", EXPORT_ID.toString())
                .containsEntry("reportName", "Campaign CSV: Spring Life Drive")
                .containsEntry("exportType", "CSV")
                .containsEntry("status", "COMPLETED")
                .containsEntry("campaignId", CAMPAIGN_ID.toString())
                .containsEntry("requestedByUserId", REQUESTER_ID.toString())
                .containsKey("fileUrl");
        assertThat(auditLog.getNewValue().get("fileUrl").toString()).contains(EXPORT_ID.toString());
    }

    @Test
    void generateCampaignPdfPersistsExportReportAudit() throws Exception {
        User requester = requester();
        Campaign campaign = campaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleAnalytics());
        stubExportSave();

        ReportFile file = reportService.generateCampaignPdf(CAMPAIGN_ID, REQUESTER_ID);

        assertThat(file.export().exportType()).isEqualTo(ReportExportType.PDF);
        assertThat(file.contentType()).isEqualTo(ReportFile.PDF_CONTENT_TYPE);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("EXPORT_REPORT");
        assertThat(auditLog.getNewValue())
                .containsEntry("exportType", "PDF")
                .containsEntry("reportName", "Campaign PDF: Spring Life Drive")
                .containsEntry("campaignId", CAMPAIGN_ID.toString())
                .containsEntry("status", "COMPLETED");
    }

    @Test
    void exportAuditReportPersistsExportReportAuditWithoutCampaignId() throws Exception {
        User requester = requester();
        when(userRepository.findById(REQUESTER_ID)).thenReturn(Optional.of(requester));
        // listAuditLogs requires AUDIT_READ roles; unit path uses real AuditService with empty repo.
        lenient().when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        stubExportSave();

        ReportFile file = reportService.exportAuditReport(REQUESTER_ID);

        assertThat(file.filename()).isEqualTo("audit-history.csv");
        assertThat(file.export().reportName()).isEqualTo("Audit history export");

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("EXPORT_REPORT");
        assertThat(auditLog.getEntityType()).isEqualTo("report_exports");
        assertThat(auditLog.getActorUserId()).isEqualTo(REQUESTER_ID);
        assertThat(auditLog.getNewValue())
                .containsEntry("reportName", "Audit history export")
                .containsEntry("exportType", "CSV")
                .containsEntry("status", "COMPLETED")
                .doesNotContainKey("campaignId");
    }

    @Test
    void missingCampaignDoesNotWriteExportReportAudit() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.exportCampaignCsv(CAMPAIGN_ID, REQUESTER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reportExportRepository, never()).save(any(ReportExport.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void nullCampaignIdDoesNotWriteExportReportAudit() {
        assertThatThrownBy(() -> reportService.exportCampaignCsv(null, REQUESTER_ID))
                .isInstanceOf(ValidationException.class);

        verify(reportExportRepository, never()).save(any(ReportExport.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void failedGenerationDoesNotWriteExportReportAudit() {
        Campaign campaign = campaign();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        stubExportSave();
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .thenThrow(new IllegalStateException("analytics unavailable"));

        assertThatThrownBy(() -> reportService.exportCampaignCsv(CAMPAIGN_ID, REQUESTER_ID))
                .isInstanceOf(IllegalStateException.class);

        // REQUESTED + FAILED history only; no EXPORT_REPORT on failure path.
        verify(auditLogRepository, never()).save(any(AuditLog.class));
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

    private AuditLog captureSavedAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private Campaign campaign() {
        try {
            Campaign campaign =
                    Campaign.create(
                            "Spring Life Drive",
                            "Raise awareness",
                            requester(),
                            null,
                            CampaignChannel.EMAIL);
            setId(campaign, CAMPAIGN_ID);
            return campaign;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private User requester() throws Exception {
        User user =
                User.create(
                        "bi.analyst@bayer-westphalian.test", "$2a$10$hash", "BI Analyst");
        setId(user, REQUESTER_ID);
        return user;
    }

    private CampaignAnalyticsView sampleAnalytics() {
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
                "BI Analyst",
                metrics,
                Instant.parse("2026-07-11T12:00:00Z"));
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
