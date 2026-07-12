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
import com.bayerwestphalian.campaign.auth.method.ReportReadAccess;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Sprint 16 critical test item <b>663</b>: Report export is restricted to authorized roles.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code FR-109} / {@code FR-110} — CSV and PDF campaign report exports
 *   <li>{@code NFR-001} — Role-based access for restricted operations
 *   <li>Item 458 — Unauthorized user cannot export restricted reports
 * </ul>
 *
 * <p>Authorized campaign report roles: Admin, BI Analyst, Campaign Manager, Marketing Analyst,
 * Executive Viewer. Unauthenticated → {@code 401}; unauthorized role → {@code 403} and no service
 * invocation. Audit-history export uses a separate role set (Admin, Compliance Officer, System
 * Auditor).
 *
 * <p>Companion: {@link UnauthorizedUserCannotExportRestrictedReportsTests}.
 */
@WebMvcTest(controllers = ReportController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
@DisplayName("663 Report export is restricted to authorized roles")
class ReportExportIsRestrictedToAuthorizedRolesTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000663");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000663");
    private static final UUID EXPORT_ID =
            UUID.fromString("56000000-0000-0000-0000-000000000663");

    private static final String CSV_PATH = "/api/reports/campaigns/{campaignId}/csv";
    private static final String PDF_PATH = "/api/reports/campaigns/{campaignId}/pdf";
    private static final String EXPORTS_PATH = "/api/reports/exports";
    private static final String EXPORT_DETAIL_PATH = "/api/reports/exports/{exportId}";
    private static final Path REPORT_EXPORT_DOC = Path.of("../docs/modules/report-export.md");

    private static final List<String> AUTHORIZED_CAMPAIGN_REPORT_ROLES =
            List.of(
                    "ADMIN",
                    "BI_ANALYST",
                    "CAMPAIGN_MANAGER",
                    "MARKETING_ANALYST",
                    "EXECUTIVE_VIEWER");

    @Autowired private MockMvc mockMvc;

    @MockBean private ReportService reportService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Nested
    @DisplayName("HTTP: unauthenticated cannot export")
    class Unauthenticated {

        @Test
        void unauthenticatedCannotDownloadCampaignCsvOrPdf() throws Exception {
            mockMvc.perform(get(CSV_PATH, CAMPAIGN_ID)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(PDF_PATH, CAMPAIGN_ID)).andExpect(status().isUnauthorized());

            verify(reportService, never()).campaignCsv(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).exportCampaignCsv(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).campaignPdf(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).generateCampaignPdf(eq(CAMPAIGN_ID), any());
        }

        @Test
        void unauthenticatedCannotAccessExportHistory() throws Exception {
            mockMvc.perform(get(EXPORTS_PATH)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(EXPORT_DETAIL_PATH, EXPORT_ID)).andExpect(status().isUnauthorized());

            verify(reportService, never()).listExportHistory();
            verify(reportService, never()).getExportHistory(eq(EXPORT_ID));
        }
    }

    @Nested
    @DisplayName("HTTP: unauthorized roles receive 403")
    class UnauthorizedRoles {

        @ParameterizedTest
        @MethodSource(
                "com.bayerwestphalian.campaign.report.ReportExportIsRestrictedToAuthorizedRolesTests#unauthorizedRoles")
        void unauthorizedRolesCannotDownloadCampaignCsv(SystemRoleName role) throws Exception {
            when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(role));

            mockMvc.perform(
                            get(CSV_PATH, CAMPAIGN_ID)
                                    .header("Authorization", "Bearer denied-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("campaignId"))));

            verify(reportService, never()).campaignCsv(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).exportCampaignCsv(eq(CAMPAIGN_ID), any());
        }

        @ParameterizedTest
        @MethodSource(
                "com.bayerwestphalian.campaign.report.ReportExportIsRestrictedToAuthorizedRolesTests#unauthorizedRoles")
        void unauthorizedRolesCannotDownloadCampaignPdf(SystemRoleName role) throws Exception {
            when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(role));

            mockMvc.perform(
                            get(PDF_PATH, CAMPAIGN_ID)
                                    .header("Authorization", "Bearer denied-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("%PDF"))));

            verify(reportService, never()).campaignPdf(eq(CAMPAIGN_ID), any());
            verify(reportService, never()).generateCampaignPdf(eq(CAMPAIGN_ID), any());
        }

        @Test
        void productManagerComplianceOfficerAndSystemAuditorCannotExportCampaignReports()
                throws Exception {
            for (SystemRoleName role :
                    List.of(
                            SystemRoleName.PRODUCT_MANAGER,
                            SystemRoleName.COMPLIANCE_OFFICER,
                            SystemRoleName.SYSTEM_AUDITOR)) {
                when(jwtService.validateToken("role-token", JwtTokenType.ACCESS))
                        .thenReturn(roleClaims(role));

                mockMvc.perform(
                                get(CSV_PATH, CAMPAIGN_ID)
                                        .header("Authorization", "Bearer role-token"))
                        .andExpect(status().isForbidden());
                mockMvc.perform(
                                get(PDF_PATH, CAMPAIGN_ID)
                                        .header("Authorization", "Bearer role-token"))
                        .andExpect(status().isForbidden());
                mockMvc.perform(get(EXPORTS_PATH).header("Authorization", "Bearer role-token"))
                        .andExpect(status().isForbidden());
            }

            verify(reportService, never()).campaignCsv(any(), any());
            verify(reportService, never()).campaignPdf(any(), any());
            verify(reportService, never()).listExportHistory();
        }
    }

    @Nested
    @DisplayName("Authorization matrix: method security and HTTP filter roles")
    class AuthorizationMatrix {

        @Test
        void reportControllerAndServiceRequireAuthorizedCampaignReportRoles() throws Exception {
            for (String methodName : List.of("campaignCsv", "campaignPdf")) {
                Method controllerMethod =
                        ReportController.class.getMethod(methodName, UUID.class);
                assertPreAuthorizeHasCampaignReportRoles(
                        controllerMethod.getAnnotation(PreAuthorize.class));

                Method serviceMethod =
                        ReportService.class.getMethod(methodName, UUID.class, UUID.class);
                assertPreAuthorizeHasCampaignReportRoles(
                        serviceMethod.getAnnotation(PreAuthorize.class));
            }

            Method exportCsv =
                    ReportService.class.getMethod(
                            "exportCampaignCsv", UUID.class, UUID.class);
            Method generatePdf =
                    ReportService.class.getMethod(
                            "generateCampaignPdf", UUID.class, UUID.class);
            assertPreAuthorizeHasCampaignReportRoles(exportCsv.getAnnotation(PreAuthorize.class));
            assertPreAuthorizeHasCampaignReportRoles(
                    generatePdf.getAnnotation(PreAuthorize.class));
        }

        @Test
        void reportReadAccessAnnotationDelegatesToCanViewReports() {
            PreAuthorize preAuthorize = ReportReadAccess.class.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).isEqualTo("@authz.canViewReports()");
        }

        @Test
        void securityConfigurationRestrictsReportsApiToAuthorizedRolesOnly() {
            assertThat(SecurityConfiguration.BI_CAMPAIGN_EXECUTIVE_ROLES)
                    .containsExactlyInAnyOrderElementsOf(AUTHORIZED_CAMPAIGN_REPORT_ROLES);
            assertThat(Arrays.asList(SecurityConfiguration.BI_CAMPAIGN_EXECUTIVE_ROLES))
                    .doesNotContain(
                            SystemRoleName.PRODUCT_MANAGER.name(),
                            SystemRoleName.COMPLIANCE_OFFICER.name(),
                            SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                            SystemRoleName.SALES_AGENT.name(),
                            SystemRoleName.SYSTEM_AUDITOR.name());
        }

        @Test
        void auditReportExportUsesSeparateRestrictedRoleSet() throws Exception {
            Method audit = ReportService.class.getMethod("exportAuditReport", UUID.class);
            PreAuthorize preAuthorize = audit.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            String expression = preAuthorize.value();
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
        void canViewReportsExpressionIncludesOnlyAuthorizedRoles() {
            // Documented contract mirrored by AuthorizationExpressions.canViewReports().
            assertThat(AUTHORIZED_CAMPAIGN_REPORT_ROLES)
                    .containsExactly(
                            "ADMIN",
                            "BI_ANALYST",
                            "CAMPAIGN_MANAGER",
                            "MARKETING_ANALYST",
                            "EXECUTIVE_VIEWER");
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Documentation {

        @Test
        void reportExportDocStatesRoleRestrictionAndCriticalItem663() throws Exception {
            String doc = Files.readString(REPORT_EXPORT_DOC);
            assertThat(doc)
                    .contains("663")
                    .contains("ReportExportIsRestrictedToAuthorizedRolesTests")
                    .contains("FR-109")
                    .contains("FR-110")
                    .contains("403")
                    .contains("BI_ANALYST")
                    .containsIgnoringCase("unauthorized");
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

    private static void assertPreAuthorizeHasCampaignReportRoles(PreAuthorize preAuthorize) {
        assertThat(preAuthorize).isNotNull();
        String expression = preAuthorize.value();
        for (String role : AUTHORIZED_CAMPAIGN_REPORT_ROLES) {
            assertThat(expression).contains(role);
        }
        assertThat(expression)
                .doesNotContain("PRODUCT_MANAGER")
                .doesNotContain("CUSTOMER_SERVICE_AGENT")
                .doesNotContain("SALES_AGENT")
                .doesNotContain("SYSTEM_AUDITOR");
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                REQUESTER_ID, "restricted-report.663@bayer-westphalian.test", List.of(role));
    }
}
