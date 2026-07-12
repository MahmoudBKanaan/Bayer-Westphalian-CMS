package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KB ReportController smoke tests.
 *
 * <p>Item 437 CSV export contract is covered in depth by {@link CampaignCsvReportEndpointTests}.
 * Item 438 PDF export contract is covered in depth by {@link CampaignPdfReportEndpointTests}.
 * Item 439 export history is covered in depth by {@link ReportExportHistoryEndpointTests}.
 */
@WebMvcTest(controllers = ReportController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class ReportControllerTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000437");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000437");
    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000437");

    @Autowired private MockMvc mockMvc;

    @MockBean private ReportService reportService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void exposesReportsApiBasePath() {
        assertThat(ReportController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(ReportController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/reports");
    }

    @Test
    void campaignManagerCanDownloadCampaignCsv() throws Exception {
        // KB item 437 smoke coverage; full contract in CampaignCsvReportEndpointTests.
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(
                        new JwtTokenClaims(
                                REQUESTER_ID,
                                "cm@bayer-westphalian.test",
                                List.of(SystemRoleName.CAMPAIGN_MANAGER)));
        byte[] content = "campaignId,campaignName\nid,name\n".getBytes(StandardCharsets.UTF_8);
        when(reportService.campaignCsv(eq(CAMPAIGN_ID), eq(REQUESTER_ID)))
                .thenReturn(
                        new ReportFile(
                                "name.csv",
                                ReportFile.CSV_CONTENT_TYPE,
                                content,
                                new ReportExportView(
                                        EXPORT_ID,
                                        REQUESTER_ID,
                                        "Campaign CSV: name",
                                        ReportExportType.CSV,
                                        ReportExportStatus.COMPLETED,
                                        "local://reports/name.csv",
                                        Instant.parse("2026-07-11T12:00:00Z"),
                                        Instant.parse("2026-07-11T12:00:01Z"))));

        mockMvc.perform(
                        get("/api/reports/campaigns/{campaignId}/csv", CAMPAIGN_ID)
                                .header("Authorization", "Bearer cm-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION, containsString("name.csv")))
                .andExpect(content().bytes(content));

        verify(reportService).campaignCsv(CAMPAIGN_ID, REQUESTER_ID);
    }

    @Test
    void executiveViewerCanDownloadCampaignPdf() throws Exception {
        // KB item 438 smoke coverage; full contract in CampaignPdfReportEndpointTests.
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(
                        new JwtTokenClaims(
                                REQUESTER_ID,
                                "exec@bayer-westphalian.test",
                                List.of(SystemRoleName.EXECUTIVE_VIEWER)));
        byte[] content =
                "%PDF-1.4\nBT (Campaign Report) Tj ET\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        when(reportService.campaignPdf(eq(CAMPAIGN_ID), eq(REQUESTER_ID)))
                .thenReturn(
                        new ReportFile(
                                "name.pdf",
                                ReportFile.PDF_CONTENT_TYPE,
                                content,
                                new ReportExportView(
                                        EXPORT_ID,
                                        REQUESTER_ID,
                                        "Campaign PDF: name",
                                        ReportExportType.PDF,
                                        ReportExportStatus.COMPLETED,
                                        "local://reports/name.pdf",
                                        Instant.parse("2026-07-11T12:00:00Z"),
                                        Instant.parse("2026-07-11T12:00:01Z"))));

        mockMvc.perform(
                        get("/api/reports/campaigns/{campaignId}/pdf", CAMPAIGN_ID)
                                .header("Authorization", "Bearer exec-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION, containsString("name.pdf")))
                .andExpect(content().bytes(content));

        verify(reportService).campaignPdf(CAMPAIGN_ID, REQUESTER_ID);
    }

    @Test
    void biAnalystCanListStoredExportHistory() throws Exception {
        // KB item 439 smoke coverage; full contract in ReportExportHistoryEndpointTests.
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(
                        new JwtTokenClaims(
                                REQUESTER_ID,
                                "bi@bayer-westphalian.test",
                                List.of(SystemRoleName.BI_ANALYST)));
        when(reportService.listExportHistory())
                .thenReturn(
                        List.of(
                                new ReportExportView(
                                        EXPORT_ID,
                                        REQUESTER_ID,
                                        "Campaign CSV: name",
                                        ReportExportType.CSV,
                                        ReportExportStatus.COMPLETED,
                                        "local://reports/name.csv",
                                        Instant.parse("2026-07-11T12:00:00Z"),
                                        Instant.parse("2026-07-11T12:00:01Z"))));

        mockMvc.perform(get("/api/reports/exports").header("Authorization", "Bearer bi-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));

        verify(reportService).listExportHistory();
    }
}
