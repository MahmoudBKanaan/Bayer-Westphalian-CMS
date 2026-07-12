package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigurationTests {

    @Test
    void exposesBCryptPasswordEncoderForKbPasswordHashingRule() {
        SecurityConfiguration configuration = new SecurityConfiguration();

        PasswordEncoder passwordEncoder = configuration.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(passwordEncoder.encode("StrongPassword!2026")).startsWith("$2");
    }

    @Test
    void enablesMethodSecurityForServerSideRoleAuthorization() {
        assertThat(SecurityConfiguration.class.isAnnotationPresent(EnableMethodSecurity.class))
                .isTrue();
    }

    @Test
    void exposesSecurityHeaderConfigurationMethodForItem545() throws Exception {
        assertThat(
                        SecurityConfiguration.class.getDeclaredMethod(
                                "configureSecurityHeaders",
                                org.springframework.security.config.annotation.web.configurers
                                        .HeadersConfigurer.class))
                .isNotNull();
    }

    @Test
    void exposesKbPublicSecurityEndpoints() {
        assertThat(SecurityConfiguration.PUBLIC_ENDPOINTS)
                .contains(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/health/**",
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html");
    }

    @Test
    void exposesKbEndpointRoleGroupsForUserAndCustomerApis() {
        assertThat(SecurityConfiguration.ADMIN_ROLES).containsExactly(SystemRoleName.ADMIN.name());
        assertThat(SecurityConfiguration.ADMIN_OR_AGENT_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CUSTOMER_SERVICE_AGENT.name());
        assertThat(SecurityConfiguration.AUTHORIZED_CUSTOMER_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name(),
                        SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                        SystemRoleName.SALES_AGENT.name(),
                        SystemRoleName.PRODUCT_MANAGER.name());
        assertThat(SecurityConfiguration.BENEFICIARY_READ_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name(),
                        SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                        SystemRoleName.SYSTEM_AUDITOR.name());
        assertThat(SecurityConfiguration.BENEFICIARY_UPDATE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name());
        assertThat(SecurityConfiguration.CONSENT_READ_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name(),
                        SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                        SystemRoleName.SYSTEM_AUDITOR.name());
        assertThat(SecurityConfiguration.CONSENT_WRITE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name());
    }

    @Test
    void exposesKbEndpointRoleGroupsForProductSegmentAndCampaignApis() {
        assertThat(SecurityConfiguration.ADMIN_OR_PRODUCT_MANAGER_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.PRODUCT_MANAGER.name());
        assertThat(SecurityConfiguration.PRODUCT_READ_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.PRODUCT_MANAGER.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name(),
                        SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                        SystemRoleName.SALES_AGENT.name(),
                        SystemRoleName.EXECUTIVE_VIEWER.name());
        assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
        assertThat(SecurityConfiguration.SEGMENT_CREATE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
        assertThat(SecurityConfiguration.SEGMENT_MANAGE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
        // KB item 200: BI Analyst is not a manage/create role (edit only if dual-role with manage).
        assertThat(SecurityConfiguration.SEGMENT_MANAGE_ROLES)
                .doesNotContain(SystemRoleName.BI_ANALYST.name());
        assertThat(SecurityConfiguration.SEGMENT_CREATE_ROLES)
                .doesNotContain(SystemRoleName.BI_ANALYST.name());
        assertThat(SecurityConfiguration.SEGMENT_READ_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name());
        assertThat(SecurityConfiguration.SEGMENT_PREVIEW_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name());
        assertThat(SecurityConfiguration.CAMPAIGN_SEGMENT_PREVIEW_ROLES)
                .isEqualTo(SecurityConfiguration.SEGMENT_PREVIEW_ROLES);
        assertThat(SecurityConfiguration.COMPLIANCE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.COMPLIANCE_OFFICER.name());
        assertThat(SecurityConfiguration.CAMPAIGN_RECIPIENT_ROLES)
                .containsExactly(
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name());
        assertThat(SecurityConfiguration.CAMPAIGN_READ_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.PRODUCT_MANAGER.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name(),
                        SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                        SystemRoleName.SALES_AGENT.name(),
                        SystemRoleName.EXECUTIVE_VIEWER.name(),
                        SystemRoleName.SYSTEM_AUDITOR.name());
        assertThat(SecurityConfiguration.CONTACT_EVENT_READ_ROLES)
                .isEqualTo(SecurityConfiguration.CAMPAIGN_READ_ROLES);
        assertThat(SecurityConfiguration.REMINDER_READ_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name(),
                        SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                        SystemRoleName.SALES_AGENT.name(),
                        SystemRoleName.SYSTEM_AUDITOR.name());
    }

    @Test
    void exposesKbEndpointRoleGroupsForAnalyticsReportAiAndAuditApis() {
        assertThat(SecurityConfiguration.BI_CAMPAIGN_EXECUTIVE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.MARKETING_ANALYST.name(),
                        SystemRoleName.EXECUTIVE_VIEWER.name());
        assertThat(SecurityConfiguration.AI_RECOMMENDATION_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.PRODUCT_MANAGER.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name(),
                        SystemRoleName.EXECUTIVE_VIEWER.name(),
                        SystemRoleName.SYSTEM_AUDITOR.name());
        assertThat(SecurityConfiguration.AUDIT_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name(),
                        SystemRoleName.SYSTEM_AUDITOR.name());
    }

    @Test
    void buildsCorsConfigurationFromAllowedFrontendOrigins() {
        SecurityConfiguration configuration = new SecurityConfiguration();
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty(
                                "app.cors.allowed-origins",
                                "http://localhost:5173, http://127.0.0.1:5173");

        CorsConfigurationSource source = configuration.corsConfigurationSource(environment);
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins())
                .containsExactly("http://localhost:5173", "http://127.0.0.1:5173");
        assertThat(cors.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders())
                .containsExactly("Authorization", "Content-Type", "X-Request-Id");
        assertThat(cors.getExposedHeaders()).containsExactly("X-Request-Id");
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void defaultsCorsConfigurationToLocalFrontendOrigins() {
        SecurityConfiguration configuration = new SecurityConfiguration();

        CorsConfigurationSource source =
                configuration.corsConfigurationSource(new MockEnvironment());
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins())
                .isEqualTo(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
    }

    @Test
    void productionCorsUsesExplicitOriginsWithoutLocalhostDefaults() {
        SecurityConfiguration configuration = new SecurityConfiguration();
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty(
                                "app.cors.allowed-origins",
                                "https://campaign.example.com, https://admin.example.com")
                        .withProperty("spring.profiles.active", "prod");

        CorsConfigurationSource source = configuration.corsConfigurationSource(environment);
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins())
                .containsExactly("https://campaign.example.com", "https://admin.example.com");
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getAllowedOrigins())
                .noneMatch(origin -> origin.contains("localhost") || origin.contains("127.0.0.1"));
    }

    @Test
    @DisplayName("556 CORS is restricted in production")
    void productionCorsRejectsMissingWildcardLocalhostAndPlainHttpOrigins() {
        assertThatThrownBy(() -> SecurityConfiguration.validateProductionOrigins(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production CORS requires")
                .hasMessageContaining("explicit frontend origin");

        assertThatThrownBy(() -> SecurityConfiguration.validateProductionOrigins(List.of("*")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not use wildcard");

        assertThatThrownBy(
                        () ->
                                SecurityConfiguration.validateProductionOrigins(
                                        List.of("http://localhost:5173")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not allow localhost");

        assertThatThrownBy(
                        () ->
                                SecurityConfiguration.validateProductionOrigins(
                                        List.of("http://campaign.example.com")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must use HTTPS");

        assertThat(SecurityConfiguration.validateProductionOrigins(
                        List.of("https://campaign.example.com", "https://admin.example.com")))
                .containsExactly("https://campaign.example.com", "https://admin.example.com");
    }
}
