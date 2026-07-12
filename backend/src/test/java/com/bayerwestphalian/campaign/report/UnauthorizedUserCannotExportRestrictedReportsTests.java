package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 458 acceptance: Unauthorized user cannot export restricted reports.
 *
 * <p>Campaign report exports (FR-109 CSV / FR-110 PDF) and export history under {@code
 * /api/reports/**} are restricted to Admin, BI Analyst, Campaign Manager, Marketing Analyst, and
 * Executive Viewer. Unauthenticated callers receive {@code 401}; authenticated roles outside that
 * set receive {@code 403} and the export service is never invoked.
 *
 * <p>Companion coverage also lives in {@link CampaignCsvReportEndpointTests}, {@link
 * CampaignPdfReportEndpointTests}, and {@link ReportExportHistoryEndpointTests}.
 *
 * <p>Sprint 16 critical restatement: item <b>663</b> — {@link
 * ReportExportIsRestrictedToAuthorizedRolesTests}.
 */
@WebMvcTest(controllers = ReportController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
@DisplayName("458 Unauthorized user cannot export restricted reports")
class UnauthorizedUserCannotExportRestrictedReportsTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000458");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000458");
    private static final UUID EXPORT_ID =
            UUID.fromString("56000000-0000-0000-0000-000000000458");

    private static final String CSV_PATH = "/api/reports/campaigns/{campaignId}/csv";
    private static final String PDF_PATH = "/api/reports/campaigns/{campaignId}/pdf";
    private static final String EXPORTS_PATH = "/api/reports/exports";
    private static final String EXPORT_DETAIL_PATH = "/api/reports/exports/{exportId}";

    @Autowired private MockMvc mockMvc;

    @MockBean private ReportService reportService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Nested
    @DisplayName("HTTP: unauthenticated cannot export")
    class Unauthenticated {

        @Test
        void unauthenticatedCannotDownloadCampaignCsv() throws Exception {
            mockMvc.perform(get(CSV_PATH, CAMPAIGN_ID)).andExpect(status().isUnauthorized());
            verify(reportService, never()).campaignCsv(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).exportCampaignCsv(eq(CAMPAIGN_ID), any());
        }

        @Test
        void unauthenticatedCannotDownloadCampaignPdf() throws Exception {
            mockMvc.perform(get(PDF_PATH, CAMPAIGN_ID)).andExpect(status().isUnauthorized());
            verify(reportService, never()).campaignPdf(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).generateCampaignPdf(eq(CAMPAIGN_ID), any());
        }

        @Test
        void unauthenticatedCannotListExportHistory() throws Exception {
            mockMvc.perform(get(EXPORTS_PATH)).andExpect(status().isUnauthorized());
            verify(reportService, never()).listExportHistory();
        }

        @Test
        void unauthenticatedCannotGetExportHistoryDetail() throws Exception {
            mockMvc.perform(get(EXPORT_DETAIL_PATH, EXPORT_ID)).andExpect(status().isUnauthorized());
            verify(reportService, never()).getExportHistory(eq(EXPORT_ID));
        }
    }

    @Nested
    @DisplayName("HTTP: unauthorized roles receive 403 and no export body")
    class UnauthorizedRoles {

        @ParameterizedTest
        @MethodSource(
                "com.bayerwestphalian.campaign.report.UnauthorizedUserCannotExportRestrictedReportsTests#unauthorizedRoles")
        void unauthorizedRolesCannotDownloadCampaignCsv(SystemRoleName role) throws Exception {
            when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(role));

            mockMvc.perform(
                            get(CSV_PATH, CAMPAIGN_ID)
                                    .header("Authorization", "Bearer denied-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("campaignId"))))
                    .andExpect(content().string(not(containsString("Spring Life Drive"))));

            verify(reportService, never()).campaignCsv(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).exportCampaignCsv(eq(CAMPAIGN_ID), any());
        }

        @ParameterizedTest
        @MethodSource(
                "com.bayerwestphalian.campaign.report.UnauthorizedUserCannotExportRestrictedReportsTests#unauthorizedRoles")
        void unauthorizedRolesCannotDownloadCampaignPdf(SystemRoleName role) throws Exception {
            when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(role));

            mockMvc.perform(
                            get(PDF_PATH, CAMPAIGN_ID)
                                    .header("Authorization", "Bearer denied-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("%PDF-1.4"))));

            verify(reportService, never()).campaignPdf(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).generateCampaignPdf(eq(CAMPAIGN_ID), any());
        }

        @ParameterizedTest
        @MethodSource(
                "com.bayerwestphalian.campaign.report.UnauthorizedUserCannotExportRestrictedReportsTests#unauthorizedRoles")
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
            verify(reportService, never()).listExportHistoryForUser(any());
        }

        @ParameterizedTest
        @MethodSource(
                "com.bayerwestphalian.campaign.report.UnauthorizedUserCannotExportRestrictedReportsTests#unauthorizedRoles")
        void unauthorizedRolesCannotGetExportHistoryDetail(SystemRoleName role) throws Exception {
            when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(role));

            mockMvc.perform(
                            get(EXPORT_DETAIL_PATH, EXPORT_ID)
                                    .header("Authorization", "Bearer denied-token"))
                    .andExpect(status().isForbidden());

            verify(reportService, never()).getExportHistory(eq(EXPORT_ID));
        }

        @Test
        void systemAuditorCanAccessAuditLogsButNotCampaignReportExport() throws Exception {
            // COMP / least privilege: SYSTEM_AUDITOR is outside campaign report export roles.
            when(jwtService.validateToken("auditor-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.SYSTEM_AUDITOR));

            mockMvc.perform(
                            get(CSV_PATH, CAMPAIGN_ID)
                                    .header("Authorization", "Bearer auditor-token"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(
                            get(PDF_PATH, CAMPAIGN_ID)
                                    .header("Authorization", "Bearer auditor-token"))
                    .andExpect(status().isForbidden());

            verify(reportService, never()).campaignCsv(any(), any());
            verify(reportService, never()).campaignPdf(any(), any());
        }

        @Test
        void complianceOfficerCannotExportCampaignReports() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));

            mockMvc.perform(
                            get(CSV_PATH, CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(
                            get(PDF_PATH, CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(
                            get(EXPORTS_PATH).header("Authorization", "Bearer compliance-token"))
                    .andExpect(status().isForbidden());

            verify(reportService, never()).campaignCsv(any(), any());
            verify(reportService, never()).campaignPdf(any(), any());
            verify(reportService, never()).listExportHistory();
        }
    }

    @Nested
    @DisplayName("Authorization matrix: method and security config")
    class AuthorizationMatrix {

        @Test
        void reportControllerExportMethodsRequireReportReadRoles() throws Exception {
            for (String methodName : List.of("campaignCsv", "campaignPdf", "listExportHistory")) {
                Method method =
                        methodName.equals("listExportHistory")
                                ? ReportController.class.getMethod(
                                        methodName, boolean.class, ReportExportStatus.class)
                                : ReportController.class.getMethod(methodName, UUID.class);
                assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
                String expression = method.getAnnotation(PreAuthorize.class).value();
                assertThat(expression)
                        .contains("ADMIN")
                        .contains("BI_ANALYST")
                        .contains("CAMPAIGN_MANAGER")
                        .contains("MARKETING_ANALYST")
                        .contains("EXECUTIVE_VIEWER");
                assertThat(expression)
                        .doesNotContain("PRODUCT_MANAGER")
                        .doesNotContain("CUSTOMER_SERVICE_AGENT")
                        .doesNotContain("SALES_AGENT")
                        .doesNotContain("SYSTEM_AUDITOR");
            }

            Method detail =
                    ReportController.class.getMethod("getExportHistory", UUID.class);
            assertThat(detail.isAnnotationPresent(PreAuthorize.class)).isTrue();
            assertThat(detail.getAnnotation(PreAuthorize.class).value()).contains("BI_ANALYST");
        }

        @Test
        void reportServiceExportMethodsRequireReportReadRoles() throws Exception {
            for (String methodName :
                    List.of(
                            "exportCampaignCsv",
                            "generateCampaignPdf",
                            "campaignCsv",
                            "campaignPdf")) {
                Method method =
                        ReportService.class.getMethod(methodName, UUID.class, UUID.class);
                assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
                String expression = method.getAnnotation(PreAuthorize.class).value();
                assertThat(expression)
                        .contains("ADMIN")
                        .contains("BI_ANALYST")
                        .contains("CAMPAIGN_MANAGER")
                        .contains("MARKETING_ANALYST")
                        .contains("EXECUTIVE_VIEWER");
                assertThat(expression)
                        .doesNotContain("PRODUCT_MANAGER")
                        .doesNotContain("CUSTOMER_SERVICE_AGENT")
                        .doesNotContain("SALES_AGENT");
            }
        }

        @Test
        void auditReportExportIsRestrictedToAuditRolesNotCampaignReportRolesAlone()
                throws Exception {
            // Audit export is a different restricted report surface (not campaign FR-109/110).
            Method audit = ReportService.class.getMethod("exportAuditReport", UUID.class);
            assertThat(audit.isAnnotationPresent(PreAuthorize.class)).isTrue();
            String expression = audit.getAnnotation(PreAuthorize.class).value();
            assertThat(expression)
                    .contains("ADMIN")
                    .contains("COMPLIANCE_OFFICER")
                    .contains("SYSTEM_AUDITOR");
            assertThat(expression)
                    .doesNotContain("BI_ANALYST")
                    .doesNotContain("CAMPAIGN_MANAGER")
                    .doesNotContain("EXECUTIVE_VIEWER")
                    .doesNotContain("MARKETING_ANALYST");
        }

        @Test
        void securityConfigurationRestrictsReportsApiToAuthorizedRoles() {
            assertThat(SecurityConfiguration.BI_CAMPAIGN_EXECUTIVE_ROLES)
                    .containsExactlyInAnyOrder(
                            SystemRoleName.ADMIN.name(),
                            SystemRoleName.BI_ANALYST.name(),
                            SystemRoleName.CAMPAIGN_MANAGER.name(),
                            SystemRoleName.MARKETING_ANALYST.name(),
                            SystemRoleName.EXECUTIVE_VIEWER.name());
            assertThat(Arrays.asList(SecurityConfiguration.BI_CAMPAIGN_EXECUTIVE_ROLES))
                    .doesNotContain(
                            SystemRoleName.PRODUCT_MANAGER.name(),
                            SystemRoleName.COMPLIANCE_OFFICER.name(),
                            SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                            SystemRoleName.SALES_AGENT.name(),
                            SystemRoleName.SYSTEM_AUDITOR.name());
        }
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
                REQUESTER_ID, "restricted-report.user@bayer-westphalian.test", List.of(role));
    }
}
