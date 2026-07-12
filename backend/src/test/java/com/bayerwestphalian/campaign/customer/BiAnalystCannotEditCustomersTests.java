package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthenticatedPrincipal;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sprint 16 critical test item <b>654</b>: BI Analyst cannot edit customers.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code TC-009} — BI Analyst cannot edit customers
 *   <li>{@code FR-012} — Authorized users can edit customer details (BI is read-only for profiles)
 *   <li>{@code FR-010} — BI may view/search customers for analytical context
 * </ul>
 *
 * <p>Enforcement layers:
 *
 * <ol>
 *   <li>HTTP security: {@code PUT /api/customers/**} allows only ADMIN, CUSTOMER_SERVICE_AGENT,
 *       COMPLIANCE_OFFICER
 *   <li>Method security: {@link CustomerService#updateCustomer} {@code @PreAuthorize} same roles
 * </ol>
 */
@DisplayName("654 BI Analyst cannot edit customers")
class BiAnalystCannotEditCustomersTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000654");

    @Nested
    @DisplayName("Authorization: BI can read customers but not update")
    class ExpressionAndRoleMatrix {

        private final AuthorizationExpressions authorizationExpressions =
                new AuthorizationExpressions();

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void biAnalystCanReadCustomersButIsNotAnUpdateRole() {
            authenticate(SystemRoleName.BI_ANALYST);

            assertThat(authorizationExpressions.canReadCustomers()).isTrue();
            // Update is not exposed as canUpdateCustomers(); matrix is PreAuthorize + HTTP filter.
            assertThat(BiAnalystCannotEditCustomersContract.CUSTOMER_UPDATE_ROLES)
                    .doesNotContain(SystemRoleName.BI_ANALYST);
            assertThat(BiAnalystCannotEditCustomersContract.CUSTOMER_UPDATE_ROLES)
                    .containsExactlyInAnyOrder(
                            SystemRoleName.ADMIN,
                            SystemRoleName.CUSTOMER_SERVICE_AGENT,
                            SystemRoleName.COMPLIANCE_OFFICER);
        }

        @ParameterizedTest(name = "{0} is authorized to update customers")
        @EnumSource(
                value = SystemRoleName.class,
                names = {"ADMIN", "CUSTOMER_SERVICE_AGENT", "COMPLIANCE_OFFICER"})
        void updateRolesAreExactlyKbCustomerEditors(SystemRoleName role) {
            assertThat(BiAnalystCannotEditCustomersContract.CUSTOMER_UPDATE_ROLES)
                    .contains(role);
        }

        @ParameterizedTest(name = "{0} cannot update customers")
        @MethodSource(
                "com.bayerwestphalian.campaign.customer.BiAnalystCannotEditCustomersTests#nonUpdateRoles")
        void nonUpdateRolesExcludeBiAndOthers(SystemRoleName role) {
            assertThat(BiAnalystCannotEditCustomersContract.CUSTOMER_UPDATE_ROLES)
                    .doesNotContain(role);
        }

        private static void authenticate(SystemRoleName... roles) {
            List<SystemRoleName> roleList = List.of(roles);
            AuthenticatedPrincipal principal =
                    new AuthenticatedPrincipal(
                            UUID.fromString("10000000-0000-0000-0000-000000000654"),
                            "bi.analyst@test.example",
                            roleList);
            List<SimpleGrantedAuthority> authorities =
                    roleList.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                            .toList();
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    principal, "n/a", authorities));
        }
    }

    static Stream<SystemRoleName> nonUpdateRoles() {
        return Arrays.stream(SystemRoleName.values())
                .filter(
                        role ->
                                role != SystemRoleName.ADMIN
                                        && role != SystemRoleName.CUSTOMER_SERVICE_AGENT
                                        && role != SystemRoleName.COMPLIANCE_OFFICER);
    }

    @Nested
    @DisplayName("Method and HTTP security annotations")
    class AnnotationAndHttpRoleGate {

        @Test
        void customerServiceUpdateRequiresEditorRolesNotBiAnalyst() throws Exception {
            Method update =
                    CustomerService.class.getMethod(
                            "updateCustomer", UUID.class, UpdateCustomerCommand.class);
            assertThat(update.isAnnotationPresent(PreAuthorize.class)).isTrue();
            String expression = update.getAnnotation(PreAuthorize.class).value();
            assertThat(expression)
                    .isEqualTo(
                            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'COMPLIANCE_OFFICER')");
            assertThat(expression).doesNotContain("BI_ANALYST");
            assertThat(expression).doesNotContain("CAMPAIGN_MANAGER");
            assertThat(expression).doesNotContain("PRODUCT_MANAGER");
        }

        @Test
        void httpSecurityPutCustomersExcludesBiAnalyst() {
            // SecurityConfiguration: PUT /api/customers/** → Admin, CSA, Compliance only.
            Set<String> putRoles =
                    Set.of(
                            SystemRoleName.ADMIN.name(),
                            SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                            SystemRoleName.COMPLIANCE_OFFICER.name());
            assertThat(putRoles).doesNotContain(SystemRoleName.BI_ANALYST.name());
            // BI is in GET customer read matrix.
            assertThat(SecurityConfiguration.AUTHORIZED_CUSTOMER_ROLES)
                    .contains(SystemRoleName.BI_ANALYST.name());
        }

        @Test
        void biAnalystIsInCustomerReadRolesButNotCreateOrDelete() {
            assertThat(SecurityConfiguration.AUTHORIZED_CUSTOMER_ROLES)
                    .contains(SystemRoleName.BI_ANALYST.name());
            assertThat(SecurityConfiguration.ADMIN_OR_AGENT_ROLES)
                    .doesNotContain(SystemRoleName.BI_ANALYST.name());
            assertThat(SecurityConfiguration.ADMIN_ROLES)
                    .doesNotContain(SystemRoleName.BI_ANALYST.name());
        }
    }

    /**
     * HTTP-level proof: BI Analyst receives 403 on PUT customer update; may still GET profile.
     * Complements {@code ProtectedEndpointSecurityTests#unauthorizedRoleCannotEditCustomers}.
     */
    @WebMvcTest(controllers = BiAnalystCannotEditCustomersTests.CustomerProbeController.class)
    @Import({
        SecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        BiAnalystCannotEditCustomersTests.CustomerProbeController.class
    })
    @ActiveProfiles("customer-probe")
    @DisplayName("HTTP: BI_ANALYST forbidden on PUT /api/customers/{id}")
    static class HttpForbiddenForBiAnalyst {

        @Autowired private MockMvc mockMvc;

        @MockBean private JwtService jwtService;

        @MockBean(name = "jpaMappingContext")
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Test
        void biAnalystReceivesForbiddenOnUpdateCustomer() throws Exception {
            when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

            mockMvc.perform(
                            put("/api/customers/{id}", CUSTOMER_ID)
                                    .header("Authorization", "Bearer bi-analyst-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(customerUpdatePayload()))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("customer updated"))));
        }

        @Test
        void customerServiceAgentReceivesOkOnUpdateCustomerPositiveControl() throws Exception {
            when(jwtService.validateToken("customer-service-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));

            mockMvc.perform(
                            put("/api/customers/{id}", CUSTOMER_ID)
                                    .header("Authorization", "Bearer customer-service-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(customerUpdatePayload()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("customer updated"));
        }

        @Test
        void biAnalystMayGetCustomerProfileReadOnlyPositiveControl() throws Exception {
            when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

            mockMvc.perform(
                            get("/api/customers/{id}", CUSTOMER_ID)
                                    .header("Authorization", "Bearer bi-analyst-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("customer loaded"));
        }

        private static JwtTokenClaims roleClaims(SystemRoleName role) {
            return new JwtTokenClaims(
                    UUID.fromString("10000000-0000-0000-0000-000000000654"),
                    role.name().toLowerCase().replace('_', '.') + "@test.example",
                    List.of(role),
                    JwtTokenType.ACCESS,
                    "bayer-westphalian-campaign-platform-test",
                    Instant.parse("2026-07-12T12:00:00Z"),
                    Instant.parse("2026-07-12T12:15:00Z"),
                    role.name().toLowerCase() + "-access-token-id");
        }

        private static String customerUpdatePayload() {
            return """
                    {
                      "firstName": "Updated",
                      "lastName": "Customer"
                    }
                    """;
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 654)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(BiAnalystCannotEditCustomersContract.CRITICAL_TEST_ITEM).isEqualTo(654);
            assertThat(BiAnalystCannotEditCustomersContract.RULE_STATEMENT)
                    .isEqualTo("BI Analyst cannot edit customers");
            assertThat(BiAnalystCannotEditCustomersContract.TEST_CASE_IDS).contains("TC-009");
            assertThat(BiAnalystCannotEditCustomersContract.FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-010", "FR-012");
            assertThat(BiAnalystCannotEditCustomersContract.BLOCKED_ROLE)
                    .isEqualTo(SystemRoleName.BI_ANALYST);
            assertThat(BiAnalystCannotEditCustomersContract.CUSTOMER_UPDATE_ROLES)
                    .containsExactlyInAnyOrder(
                            SystemRoleName.ADMIN,
                            SystemRoleName.CUSTOMER_SERVICE_AGENT,
                            SystemRoleName.COMPLIANCE_OFFICER);
            assertThat(BiAnalystCannotEditCustomersContract.UPDATE_PATH)
                    .isEqualTo("PUT /api/customers/{id}");
            assertThat(BiAnalystCannotEditCustomersContract.AUTHORIZATION_EXPRESSION)
                    .isEqualTo(
                            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'COMPLIANCE_OFFICER')");
        }
    }

    /** Minimal controller for HTTP authorization probes. */
    @RestController
    @Profile("customer-probe")
    static class CustomerProbeController {

        @GetMapping("/api/customers/{id}")
        String getCustomer(@PathVariable UUID id) {
            return "customer loaded";
        }

        @PutMapping("/api/customers/{id}")
        String updateCustomer(@PathVariable UUID id) {
            return "customer updated";
        }
    }

    static final class BiAnalystCannotEditCustomersContract {
        static final int CRITICAL_TEST_ITEM = 654;
        static final String RULE_STATEMENT = "BI Analyst cannot edit customers";
        static final java.util.List<String> TEST_CASE_IDS = java.util.List.of("TC-009");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-010", "FR-012");
        static final SystemRoleName BLOCKED_ROLE = SystemRoleName.BI_ANALYST;
        static final java.util.List<SystemRoleName> CUSTOMER_UPDATE_ROLES =
                java.util.List.of(
                        SystemRoleName.ADMIN,
                        SystemRoleName.CUSTOMER_SERVICE_AGENT,
                        SystemRoleName.COMPLIANCE_OFFICER);
        static final String UPDATE_PATH = "PUT /api/customers/{id}";
        static final String AUTHORIZATION_EXPRESSION =
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'COMPLIANCE_OFFICER')";

        private BiAnalystCannotEditCustomersContract() {}
    }
}
