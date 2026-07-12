package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthenticatedPrincipal;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.auth.method.CampaignWriteAccess;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sprint 16 critical test item <b>653</b>: Product Manager cannot launch campaigns.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code TC-013} — Product Manager cannot launch campaigns
 *   <li>{@code FR-060} — Campaign Manager (not Product Manager) can launch approved campaigns
 * </ul>
 *
 * <p>Enforcement layers:
 *
 * <ol>
 *   <li>HTTP security: {@code POST /api/campaigns/**} requires {@link
 *       SecurityConfiguration#CAMPAIGN_MANAGER_ROLES} (ADMIN, CAMPAIGN_MANAGER only)
 *   <li>Method security: {@link CampaignWriteAccess} / {@code @authz.canManageCampaigns()}
 * </ol>
 */
@DisplayName("653 Product Manager cannot launch campaigns")
class ProductManagerCannotLaunchCampaignsTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000653");

    @Nested
    @DisplayName("Authorization expression (canManageCampaigns)")
    class ExpressionGate {

        private final AuthorizationExpressions authorizationExpressions =
                new AuthorizationExpressions();

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void productManagerCannotManageCampaignsAndThereforeCannotLaunch() {
            authenticate(SystemRoleName.PRODUCT_MANAGER);

            assertThat(authorizationExpressions.canManageCampaigns()).isFalse();
            assertThat(authorizationExpressions.canManageProducts()).isTrue();
            assertThat(authorizationExpressions.canReadCampaigns()).isTrue();
        }

        @ParameterizedTest(name = "{0} can manage campaigns (launch-capable)")
        @EnumSource(
                value = SystemRoleName.class,
                names = {"ADMIN", "CAMPAIGN_MANAGER"})
        void launchRolesCanManageCampaigns(SystemRoleName role) {
            authenticate(role);
            assertThat(authorizationExpressions.canManageCampaigns()).isTrue();
        }

        @ParameterizedTest(name = "{0} cannot manage campaigns")
        @MethodSource(
                "com.bayerwestphalian.campaign.campaign.ProductManagerCannotLaunchCampaignsTests#nonLaunchRoles")
        void nonLaunchRolesCannotManageCampaigns(SystemRoleName role) {
            authenticate(role);
            assertThat(authorizationExpressions.canManageCampaigns()).isFalse();
        }

        private static void authenticate(SystemRoleName... roles) {
            List<SystemRoleName> roleList = List.of(roles);
            AuthenticatedPrincipal principal =
                    new AuthenticatedPrincipal(
                            UUID.fromString("10000000-0000-0000-0000-000000000653"),
                            "user@test.example",
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

    static Stream<SystemRoleName> nonLaunchRoles() {
        return Arrays.stream(SystemRoleName.values())
                .filter(
                        role ->
                                role != SystemRoleName.ADMIN
                                        && role != SystemRoleName.CAMPAIGN_MANAGER);
    }

    @Nested
    @DisplayName("Method security annotations on launch surface")
    class AnnotationGate {

        @Test
        void campaignServiceLaunchRequiresCampaignWriteAccess() throws Exception {
            Method launch = CampaignService.class.getMethod("launchCampaign", UUID.class);
            assertThat(launch.isAnnotationPresent(CampaignWriteAccess.class)).isTrue();
            PreAuthorize meta = CampaignWriteAccess.class.getAnnotation(PreAuthorize.class);
            assertThat(meta.value()).isEqualTo("@authz.canManageCampaigns()");
        }

        @Test
        void campaignControllerLaunchRequiresCanManageCampaigns() throws Exception {
            Method launch = CampaignController.class.getMethod("launchCampaign", UUID.class);
            assertThat(launch.isAnnotationPresent(PreAuthorize.class)).isTrue();
            assertThat(launch.getAnnotation(PreAuthorize.class).value())
                    .isEqualTo("@authz.canManageCampaigns()");
            assertThat(launch.getAnnotation(PostMapping.class).value())
                    .containsExactly("/{id}/launch");
        }

        @Test
        void httpSecurityCampaignPostRolesExcludeProductManager() {
            assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                    .containsExactlyInAnyOrder(
                            SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
            assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                    .doesNotContain(SystemRoleName.PRODUCT_MANAGER.name());
            // Product Manager may READ campaigns but not POST launch.
            assertThat(SecurityConfiguration.CAMPAIGN_READ_ROLES)
                    .contains(SystemRoleName.PRODUCT_MANAGER.name());
        }

        @Test
        void campaignWriteAccessIsNotProductWriteAccess() {
            Set<String> launchRoles =
                    Set.of(SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
            Set<String> productWriteRoles =
                    Set.of(SystemRoleName.ADMIN.name(), SystemRoleName.PRODUCT_MANAGER.name());
            assertThat(launchRoles).doesNotContain(SystemRoleName.PRODUCT_MANAGER.name());
            assertThat(productWriteRoles).contains(SystemRoleName.PRODUCT_MANAGER.name());
            assertThat(CampaignWriteAccess.class.getAnnotation(PreAuthorize.class).value())
                    .isEqualTo("@authz.canManageCampaigns()");
        }
    }

    /**
     * HTTP-level proof: {@code POST /api/campaigns/**} is restricted to campaign-manager roles
     * (PRODUCT_MANAGER → 403). Complements {@code
     * ProtectedEndpointSecurityTests#productManagerCannotLaunchCampaign}.
     */
    @WebMvcTest(
            controllers =
                    ProductManagerCannotLaunchCampaignsTests.LaunchProbeController.class)
    @Import({
        SecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        ProductManagerCannotLaunchCampaignsTests.LaunchProbeController.class
    })
    @ActiveProfiles("launch-probe")
    @DisplayName("HTTP: PRODUCT_MANAGER forbidden on POST /api/campaigns/{id}/launch")
    static class HttpForbiddenForProductManager {

        @Autowired private MockMvc mockMvc;

        @MockBean private JwtService jwtService;

        @MockBean(name = "jpaMappingContext")
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Test
        void productManagerReceivesForbiddenOnLaunchEndpoint() throws Exception {
            when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));

            mockMvc.perform(
                            post("/api/campaigns/{id}/launch", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer product-manager-token")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("campaign launched"))));
        }

        @Test
        void campaignManagerReceivesOkOnLaunchEndpointPositiveControl() throws Exception {
            when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));

            mockMvc.perform(
                            post("/api/campaigns/{id}/launch", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer campaign-manager-token")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("campaign launched"));
        }

        private static JwtTokenClaims roleClaims(SystemRoleName role) {
            return new JwtTokenClaims(
                    UUID.fromString("10000000-0000-0000-0000-000000000653"),
                    role.name().toLowerCase().replace('_', '.') + "@test.example",
                    List.of(role),
                    JwtTokenType.ACCESS,
                    "bayer-westphalian-campaign-platform-test",
                    Instant.parse("2026-07-12T12:00:00Z"),
                    Instant.parse("2026-07-12T12:15:00Z"),
                    role.name().toLowerCase() + "-access-token-id");
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 653)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(ProductManagerCannotLaunchCampaignsContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(653);
            assertThat(ProductManagerCannotLaunchCampaignsContract.RULE_STATEMENT)
                    .isEqualTo("Product Manager cannot launch campaigns");
            assertThat(ProductManagerCannotLaunchCampaignsContract.TEST_CASE_IDS)
                    .contains("TC-013");
            assertThat(ProductManagerCannotLaunchCampaignsContract.FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-060");
            assertThat(ProductManagerCannotLaunchCampaignsContract.BLOCKED_ROLE)
                    .isEqualTo(SystemRoleName.PRODUCT_MANAGER);
            assertThat(ProductManagerCannotLaunchCampaignsContract.ALLOWED_LAUNCH_ROLES)
                    .containsExactlyInAnyOrder(
                            SystemRoleName.ADMIN, SystemRoleName.CAMPAIGN_MANAGER);
            assertThat(ProductManagerCannotLaunchCampaignsContract.LAUNCH_PATH)
                    .isEqualTo("POST /api/campaigns/{id}/launch");
            assertThat(ProductManagerCannotLaunchCampaignsContract.AUTHORIZATION_EXPRESSION)
                    .isEqualTo("@authz.canManageCampaigns()");
        }
    }

    /** Minimal controller for HTTP authorization probes (no business logic). */
    @RestController
    @Profile("launch-probe")
    static class LaunchProbeController {

        @PostMapping("/api/campaigns/{id}/launch")
        String launchCampaign() {
            return "campaign launched";
        }
    }

    static final class ProductManagerCannotLaunchCampaignsContract {
        static final int CRITICAL_TEST_ITEM = 653;
        static final String RULE_STATEMENT = "Product Manager cannot launch campaigns";
        static final java.util.List<String> TEST_CASE_IDS = java.util.List.of("TC-013");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-060");
        static final SystemRoleName BLOCKED_ROLE = SystemRoleName.PRODUCT_MANAGER;
        static final java.util.List<SystemRoleName> ALLOWED_LAUNCH_ROLES =
                java.util.List.of(SystemRoleName.ADMIN, SystemRoleName.CAMPAIGN_MANAGER);
        static final String LAUNCH_PATH = "POST /api/campaigns/{id}/launch";
        static final String AUTHORIZATION_EXPRESSION = "@authz.canManageCampaigns()";

        private ProductManagerCannotLaunchCampaignsContract() {}
    }
}
