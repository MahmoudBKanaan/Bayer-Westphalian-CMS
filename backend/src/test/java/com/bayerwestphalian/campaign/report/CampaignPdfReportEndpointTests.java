package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KB item 438: Add PDF campaign report export.
 *
 * <p>{@code GET /api/reports/campaigns/{campaignId}/pdf} returns a downloadable campaign
 * performance PDF (FR-110) for authorized report roles.
 *
 * <p>Unauthorized access is formalized under KB item 458 in {@link
 * UnauthorizedUserCannotExportRestrictedReportsTests}.
 */
@WebMvcTest(controllers = ReportController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class CampaignPdfReportEndpointTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000438");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000438");
    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000438");
    private static final UUID MISSING_ID =
            UUID.fromString("50000000-0000-0000-0000-00000000dead");

    private static final String PDF_PATH = "/api/reports/campaigns/{campaignId}/pdf";

    @Autowired private MockMvc mockMvc;

    @MockBean private ReportService reportService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignPdfEndpointIsMappedUnderReportsApi() throws Exception {
        assertThat(ReportController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(ReportController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/reports");

        Method method = ReportController.class.getMethod("campaignPdf", UUID.class);
        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly("/campaigns/{campaignId}/pdf");
        assertThat(method.getAnnotation(GetMapping.class).produces())
                .contains(MediaType.APPLICATION_PDF_VALUE);
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("ADMIN")
                .contains("EXECUTIVE_VIEWER")
                .contains("MARKETING_ANALYST");
    }

    @Test
    void biAnalystReceivesCampaignPdfAttachment() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(reportService.campaignPdf(eq(CAMPAIGN_ID), eq(REQUESTER_ID)))
                .thenReturn(samplePdfFile());

        byte[] expected = samplePdfBytes();

        mockMvc.perform(
                        get(PDF_PATH, CAMPAIGN_ID)
                                .header("Authorization", "Bearer bi-token")
                                .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        containsString("attachment")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        containsString("Spring-Life-Drive.pdf")))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, expected.length))
                .andExpect(content().bytes(expected));

        String pdf = new String(expected, StandardCharsets.US_ASCII);
        assertThat(pdf).startsWith("%PDF-1.4");
        assertThat(pdf).contains("Bayer-Westphalian Campaign Report");
        assertThat(pdf).contains("Spring Life Drive");

        verify(reportService).campaignPdf(CAMPAIGN_ID, REQUESTER_ID);
    }

    @ParameterizedTest
    @MethodSource("authorizedRoles")
    void authorizedRolesCanDownloadCampaignPdf(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("ok-token", JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(reportService.campaignPdf(eq(CAMPAIGN_ID), eq(REQUESTER_ID)))
                .thenReturn(samplePdfFile());

        mockMvc.perform(get(PDF_PATH, CAMPAIGN_ID).header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        containsString("Spring-Life-Drive.pdf")));

        verify(reportService).campaignPdf(CAMPAIGN_ID, REQUESTER_ID);
    }

    @Test
    void missingCampaignReturnsNotFound() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(reportService.campaignPdf(eq(MISSING_ID), eq(REQUESTER_ID)))
                .thenThrow(new ResourceNotFoundException("Campaign", MISSING_ID));

        mockMvc.perform(get(PDF_PATH, MISSING_ID).header("Authorization", "Bearer bi-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("RESOURCE_NOT_FOUND")));

        verify(reportService).campaignPdf(MISSING_ID, REQUESTER_ID);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(PDF_PATH, CAMPAIGN_ID)).andExpect(status().isUnauthorized());
        verify(reportService, never()).campaignPdf(eq(CAMPAIGN_ID), eq(REQUESTER_ID));
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotDownloadCampaignPdf(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(get(PDF_PATH, CAMPAIGN_ID).header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("%PDF-1.4"))));

        verify(reportService, never()).campaignPdf(eq(CAMPAIGN_ID), eq(REQUESTER_ID));
    }

    static Stream<SystemRoleName> authorizedRoles() {
        return Stream.of(
                SystemRoleName.ADMIN,
                SystemRoleName.BI_ANALYST,
                SystemRoleName.CAMPAIGN_MANAGER,
                SystemRoleName.MARKETING_ANALYST,
                SystemRoleName.EXECUTIVE_VIEWER);
    }

    static Stream<SystemRoleName> unauthorizedRoles() {
        return Stream.of(
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.COMPLIANCE_OFFICER,
                SystemRoleName.CUSTOMER_SERVICE_AGENT,
                SystemRoleName.SALES_AGENT,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                REQUESTER_ID, "campaign-pdf.user@bayer-westphalian.test", List.of(role));
    }

    private static ReportFile samplePdfFile() {
        byte[] content = samplePdfBytes();
        ReportExportView export =
                new ReportExportView(
                        EXPORT_ID,
                        REQUESTER_ID,
                        "Campaign PDF: Spring Life Drive",
                        ReportExportType.PDF,
                        ReportExportStatus.COMPLETED,
                        "local://reports/" + EXPORT_ID + "/Spring-Life-Drive.pdf",
                        Instant.parse("2026-07-11T12:00:00Z"),
                        Instant.parse("2026-07-11T12:00:01Z"));
        return new ReportFile(
                "Spring-Life-Drive.pdf", ReportFile.PDF_CONTENT_TYPE, content, export);
    }

    private static byte[] samplePdfBytes() {
        return """
                %PDF-1.4
                1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj
                BT /F1 10 Tf (Bayer-Westphalian Campaign Report) Tj T* (Spring Life Drive) Tj ET
                %%EOF
                """
                .getBytes(StandardCharsets.US_ASCII);
    }
}
