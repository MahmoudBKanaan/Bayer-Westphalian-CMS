package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KB item 439: Store report export history.
 *
 * <p>{@code GET /api/reports/exports} and {@code GET /api/reports/exports/{exportId}} expose
 * persisted {@code report_exports} rows for authorized report roles.
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
class ReportExportHistoryEndpointTests {

    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000439");
    private static final UUID EXPORT_B_ID =
            UUID.fromString("56000000-0000-0000-0000-000000000440");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000439");
    private static final UUID MISSING_ID =
            UUID.fromString("56000000-0000-0000-0000-00000000dead");

    private static final String EXPORTS_PATH = "/api/reports/exports";
    private static final String EXPORT_DETAIL_PATH = "/api/reports/exports/{exportId}";

    @Autowired private MockMvc mockMvc;

    @MockBean private ReportService reportService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void exportHistoryEndpointsAreMappedUnderReportsApi() throws Exception {
        assertThat(ReportController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(ReportController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/reports");

        Method listMethod =
                ReportController.class.getMethod(
                        "listExportHistory", boolean.class, ReportExportStatus.class);
        assertThat(listMethod.getAnnotation(GetMapping.class).value()).containsExactly("/exports");
        assertThat(listMethod.isAnnotationPresent(PreAuthorize.class)).isTrue();

        Method getMethod = ReportController.class.getMethod("getExportHistory", UUID.class);
        assertThat(getMethod.getAnnotation(GetMapping.class).value())
                .containsExactly("/exports/{exportId}");
        assertThat(getMethod.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(getMethod.getAnnotation(PreAuthorize.class).value())
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("EXECUTIVE_VIEWER");
    }

    @Test
    void biAnalystReceivesFullExportHistory() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(reportService.listExportHistory())
                .thenReturn(List.of(sampleCompletedExport(), sampleFailedExport()));

        mockMvc.perform(
                        get(EXPORTS_PATH)
                                .header("Authorization", "Bearer bi-token")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report export history loaded"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(EXPORT_ID.toString()))
                .andExpect(jsonPath("$.data[0].reportName").value("Campaign CSV: Spring Life Drive"))
                .andExpect(jsonPath("$.data[0].exportType").value("CSV"))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].requestedByUserId").value(REQUESTER_ID.toString()))
                .andExpect(
                        jsonPath("$.data[0].fileUrl")
                                .value("local://reports/" + EXPORT_ID + "/Spring-Life-Drive.csv"))
                .andExpect(jsonPath("$.data[1].id").value(EXPORT_B_ID.toString()))
                .andExpect(jsonPath("$.data[1].status").value("FAILED"))
                .andExpect(jsonPath("$.data[1].fileUrl").value(nullValue()));

        verify(reportService).listExportHistory();
        verify(reportService, never()).listExportHistoryForUser(eq(REQUESTER_ID));
    }

    @Test
    void mineTrueLimitsHistoryToCurrentUser() throws Exception {
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.EXECUTIVE_VIEWER));
        when(reportService.listExportHistoryForUser(REQUESTER_ID))
                .thenReturn(List.of(sampleCompletedExport()));

        mockMvc.perform(
                        get(EXPORTS_PATH)
                                .param("mine", "true")
                                .header("Authorization", "Bearer exec-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].requestedByUserId").value(REQUESTER_ID.toString()));

        verify(reportService).listExportHistoryForUser(REQUESTER_ID);
        verify(reportService, never()).listExportHistory();
    }

    @Test
    void statusFilterUsesCompletedHistoryQuery() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(reportService.listExportHistoryByStatus(ReportExportStatus.COMPLETED))
                .thenReturn(List.of(sampleCompletedExport()));

        mockMvc.perform(
                        get(EXPORTS_PATH)
                                .param("status", "COMPLETED")
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));

        verify(reportService).listExportHistoryByStatus(ReportExportStatus.COMPLETED);
    }

    @Test
    void emptyHistoryReturnsEmptyArray() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(reportService.listExportHistory()).thenReturn(List.of());

        mockMvc.perform(get(EXPORTS_PATH).header("Authorization", "Bearer bi-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void biAnalystReceivesSingleExportHistoryRow() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(reportService.getExportHistory(EXPORT_ID)).thenReturn(sampleCompletedExport());

        mockMvc.perform(
                        get(EXPORT_DETAIL_PATH, EXPORT_ID)
                                .header("Authorization", "Bearer bi-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report export loaded"))
                .andExpect(jsonPath("$.data.id").value(EXPORT_ID.toString()))
                .andExpect(jsonPath("$.data.exportType").value("CSV"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(reportService).getExportHistory(EXPORT_ID);
    }

    @Test
    void missingExportReturnsNotFound() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(reportService.getExportHistory(MISSING_ID))
                .thenThrow(new ResourceNotFoundException("ReportExport", MISSING_ID));

        mockMvc.perform(
                        get(EXPORT_DETAIL_PATH, MISSING_ID)
                                .header("Authorization", "Bearer bi-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @MethodSource("authorizedRoles")
    void authorizedRolesCanListExportHistory(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("ok-token", JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(reportService.listExportHistory()).thenReturn(List.of(sampleCompletedExport()));

        mockMvc.perform(get(EXPORTS_PATH).header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(reportService).listExportHistory();
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(EXPORTS_PATH)).andExpect(status().isUnauthorized());
        verify(reportService, never()).listExportHistory();
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotListExportHistory(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(get(EXPORTS_PATH).header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        not(
                                                containsString(
                                                        "Report export history loaded"))));

        verify(reportService, never()).listExportHistory();
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
                REQUESTER_ID, "export-history.user@bayer-westphalian.test", List.of(role));
    }

    private static ReportExportView sampleCompletedExport() {
        return new ReportExportView(
                EXPORT_ID,
                REQUESTER_ID,
                "Campaign CSV: Spring Life Drive",
                ReportExportType.CSV,
                ReportExportStatus.COMPLETED,
                "local://reports/" + EXPORT_ID + "/Spring-Life-Drive.csv",
                Instant.parse("2026-07-11T12:00:00Z"),
                Instant.parse("2026-07-11T12:00:01Z"));
    }

    private static ReportExportView sampleFailedExport() {
        return new ReportExportView(
                EXPORT_B_ID,
                REQUESTER_ID,
                "Campaign PDF: Spring Life Drive",
                ReportExportType.PDF,
                ReportExportStatus.FAILED,
                null,
                Instant.parse("2026-07-11T11:00:00Z"),
                null);
    }
}
