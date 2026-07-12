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
 * KB item 437: Add CSV campaign report export.
 *
 * <p>{@code GET /api/reports/campaigns/{campaignId}/csv} returns a downloadable campaign
 * performance CSV (FR-109) for authorized report roles.
 *
 * <p>Acceptance coverage for the same rule is also formalized under KB item 455 in {@link
 * CsvExportWorksTests}. Unauthorized access is formalized under KB item 458 in {@link
 * UnauthorizedUserCannotExportRestrictedReportsTests}.
 */
@WebMvcTest(controllers = ReportController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class CampaignCsvReportEndpointTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000437");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000437");
    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000437");
    private static final UUID MISSING_ID =
            UUID.fromString("50000000-0000-0000-0000-00000000dead");

    private static final String CSV_PATH = "/api/reports/campaigns/{campaignId}/csv";

    @Autowired private MockMvc mockMvc;

    @MockBean private ReportService reportService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignCsvEndpointIsMappedUnderReportsApi() throws Exception {
        assertThat(ReportController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(ReportController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/reports");

        Method method = ReportController.class.getMethod("campaignCsv", UUID.class);
        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly("/campaigns/{campaignId}/csv");
        assertThat(method.getAnnotation(GetMapping.class).produces()).contains("text/csv");
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("ADMIN")
                .contains("EXECUTIVE_VIEWER")
                .contains("MARKETING_ANALYST");
    }

    @Test
    void biAnalystReceivesCampaignCsvAttachment() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(reportService.campaignCsv(eq(CAMPAIGN_ID), eq(REQUESTER_ID)))
                .thenReturn(sampleCsvFile());

        byte[] expected =
                "campaignId,campaignName\n50000000-0000-0000-0000-000000000437,Spring Life Drive\n"
                        .getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(
                        get(CSV_PATH, CAMPAIGN_ID)
                                .header("Authorization", "Bearer bi-token")
                                .accept(MediaType.parseMediaType("text/csv")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        containsString("attachment")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        containsString("Spring-Life-Drive.csv")))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, expected.length))
                .andExpect(content().bytes(expected))
                .andExpect(content().string(containsString("campaignId,campaignName")))
                .andExpect(content().string(containsString("Spring Life Drive")));

        verify(reportService).campaignCsv(CAMPAIGN_ID, REQUESTER_ID);
    }

    @ParameterizedTest
    @MethodSource("authorizedRoles")
    void authorizedRolesCanDownloadCampaignCsv(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("ok-token", JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(reportService.campaignCsv(eq(CAMPAIGN_ID), eq(REQUESTER_ID)))
                .thenReturn(sampleCsvFile());

        mockMvc.perform(get(CSV_PATH, CAMPAIGN_ID).header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Spring Life Drive")));

        verify(reportService).campaignCsv(CAMPAIGN_ID, REQUESTER_ID);
    }

    @Test
    void missingCampaignReturnsNotFound() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(reportService.campaignCsv(eq(MISSING_ID), eq(REQUESTER_ID)))
                .thenThrow(new ResourceNotFoundException("Campaign", MISSING_ID));

        mockMvc.perform(get(CSV_PATH, MISSING_ID).header("Authorization", "Bearer bi-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("RESOURCE_NOT_FOUND")));

        verify(reportService).campaignCsv(MISSING_ID, REQUESTER_ID);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(CSV_PATH, CAMPAIGN_ID)).andExpect(status().isUnauthorized());
        verify(reportService, never()).campaignCsv(eq(CAMPAIGN_ID), eq(REQUESTER_ID));
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotDownloadCampaignCsv(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(get(CSV_PATH, CAMPAIGN_ID).header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Spring Life Drive"))));

        verify(reportService, never()).campaignCsv(eq(CAMPAIGN_ID), eq(REQUESTER_ID));
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
                REQUESTER_ID, "campaign-csv.user@bayer-westphalian.test", List.of(role));
    }

    private static ReportFile sampleCsvFile() {
        byte[] content =
                "campaignId,campaignName\n50000000-0000-0000-0000-000000000437,Spring Life Drive\n"
                        .getBytes(StandardCharsets.UTF_8);
        ReportExportView export =
                new ReportExportView(
                        EXPORT_ID,
                        REQUESTER_ID,
                        "Campaign CSV: Spring Life Drive",
                        ReportExportType.CSV,
                        ReportExportStatus.COMPLETED,
                        "local://reports/" + EXPORT_ID + "/Spring-Life-Drive.csv",
                        Instant.parse("2026-07-11T12:00:00Z"),
                        Instant.parse("2026-07-11T12:00:01Z"));
        return new ReportFile(
                "Spring-Life-Drive.csv", ReportFile.CSV_CONTENT_TYPE, content, export);
    }
}
