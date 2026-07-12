package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.common.api.SecureErrorResponses;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
// Ensure HTTPS properties bind in all profiles and WebMvcTest slices that import this config
// (HttpsEnforcementFilter depends on ProductionHttpsProperties).
@EnableConfigurationProperties(ProductionHttpsProperties.class)
public class SecurityConfiguration {

    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/health/**",
        "/actuator/health/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    public static final String[] ADMIN_ROLES = roles(SystemRoleName.ADMIN);
    public static final String[] ADMIN_OR_AGENT_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.CUSTOMER_SERVICE_AGENT);
    public static final String[] AUTHORIZED_CUSTOMER_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT,
                    SystemRoleName.PRODUCT_MANAGER);
    public static final String[] BENEFICIARY_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SYSTEM_AUDITOR);
    public static final String[] BENEFICIARY_UPDATE_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.COMPLIANCE_OFFICER);
    public static final String[] CONSENT_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SYSTEM_AUDITOR);
    public static final String[] CONSENT_WRITE_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.COMPLIANCE_OFFICER);
    public static final String[] ADMIN_OR_PRODUCT_MANAGER_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.PRODUCT_MANAGER);
    public static final String[] PRODUCT_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.PRODUCT_MANAGER,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT,
                    SystemRoleName.EXECUTIVE_VIEWER);
    public static final String[] CAMPAIGN_MANAGER_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.CAMPAIGN_MANAGER);
    public static final String[] SEGMENT_CREATE_ROLES = CAMPAIGN_MANAGER_ROLES;
    public static final String[] SEGMENT_MANAGE_ROLES = CAMPAIGN_MANAGER_ROLES;
    public static final String[] SEGMENT_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER);
    public static final String[] SEGMENT_PREVIEW_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.CAMPAIGN_MANAGER, SystemRoleName.BI_ANALYST);
    public static final String[] CAMPAIGN_SEGMENT_PREVIEW_ROLES = SEGMENT_PREVIEW_ROLES;
    public static final String[] COMPLIANCE_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.COMPLIANCE_OFFICER);
    public static final String[] CAMPAIGN_RECIPIENT_ROLES =
            roles(SystemRoleName.CAMPAIGN_MANAGER, SystemRoleName.COMPLIANCE_OFFICER);
    public static final String[] CAMPAIGN_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.PRODUCT_MANAGER,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT,
                    SystemRoleName.EXECUTIVE_VIEWER,
                    SystemRoleName.SYSTEM_AUDITOR);
    public static final String[] CONTACT_EVENT_READ_ROLES = CAMPAIGN_READ_ROLES;
    public static final String[] CONTACT_EVENT_WRITE_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT);
    public static final String[] REMINDER_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT,
                    SystemRoleName.SYSTEM_AUDITOR);
    public static final String[] BI_CAMPAIGN_EXECUTIVE_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.MARKETING_ANALYST,
                    SystemRoleName.EXECUTIVE_VIEWER);
    public static final String[] AI_RECOMMENDATION_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.PRODUCT_MANAGER,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.EXECUTIVE_VIEWER,
                    SystemRoleName.SYSTEM_AUDITOR);
    public static final String[] AUDIT_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.SYSTEM_AUDITOR);

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Item 538: shared secure JSON error writer for filter-chain and JWT authentication failures.
     */
    @Bean
    SecureErrorResponses secureErrorResponses(ObjectMapper objectMapper) {
        return new SecureErrorResponses(objectMapper);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecureErrorResponses secureErrorResponses)
            throws Exception {
        return http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Item 545: API-appropriate security headers (HSTS stays on HttpsEnforcementFilter).
                .headers(SecurityConfiguration::configureSecurityHeaders)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorization ->
                                authorization
                                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS)
                                        .permitAll()
                                        .requestMatchers(PUBLIC_ENDPOINTS)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/auth/me")
                                        .authenticated()
                                        .requestMatchers(HttpMethod.POST, "/api/auth/logout")
                                        .authenticated()
                                        .requestMatchers("/api/users/**", "/api/roles/**")
                                        .hasAnyRole(ADMIN_ROLES)
                                        .requestMatchers(
                                                "/api/system-settings", "/api/system-settings/**")
                                        .hasAnyRole(ADMIN_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/customers/**")
                                        .hasAnyRole(ADMIN_OR_AGENT_ROLES)
                                        .requestMatchers(HttpMethod.PUT, "/api/customers/**")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.COMPLIANCE_OFFICER.name())
                                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**")
                                        .hasAnyRole(ADMIN_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/customers/**")
                                        .hasAnyRole(AUTHORIZED_CUSTOMER_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/beneficiaries/**")
                                        .hasAnyRole(ADMIN_OR_AGENT_ROLES)
                                        .requestMatchers(HttpMethod.PUT, "/api/beneficiaries/**")
                                        .hasAnyRole(BENEFICIARY_UPDATE_ROLES)
                                        .requestMatchers(HttpMethod.DELETE, "/api/beneficiaries/**")
                                        .hasAnyRole(ADMIN_OR_AGENT_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/beneficiaries/**")
                                        .hasAnyRole(BENEFICIARY_READ_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/consents/**")
                                        .hasAnyRole(CONSENT_WRITE_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/consents/**")
                                        .hasAnyRole(CONSENT_READ_ROLES)
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/products/**",
                                                "/api/product-change-requests/**")
                                        .hasAnyRole(PRODUCT_READ_ROLES)
                                        .requestMatchers(
                                                HttpMethod.GET, "/api/product-ownerships/**")
                                        .hasAnyRole(PRODUCT_READ_ROLES)
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/products/**",
                                                "/api/product-ownerships/**",
                                                "/api/product-change-requests/**")
                                        .hasAnyRole(ADMIN_OR_PRODUCT_MANAGER_ROLES)
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/products/**",
                                                "/api/product-ownerships/**",
                                                "/api/product-change-requests/**")
                                        .hasAnyRole(ADMIN_OR_PRODUCT_MANAGER_ROLES)
                                        .requestMatchers(
                                                HttpMethod.PATCH,
                                                "/api/products/**",
                                                "/api/product-change-requests/**")
                                        .hasAnyRole(ADMIN_OR_PRODUCT_MANAGER_ROLES)
                                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                                        .hasAnyRole(ADMIN_OR_PRODUCT_MANAGER_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/segments/preview")
                                        .hasAnyRole(SEGMENT_PREVIEW_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/segments/**")
                                        .hasAnyRole(SEGMENT_CREATE_ROLES)
                                        .requestMatchers(HttpMethod.PUT, "/api/segments/**")
                                        .hasAnyRole(SEGMENT_MANAGE_ROLES)
                                        .requestMatchers(HttpMethod.DELETE, "/api/segments/**")
                                        .hasAnyRole(SEGMENT_MANAGE_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/segments/**")
                                        .hasAnyRole(SEGMENT_READ_ROLES)
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/contact-events",
                                                "/api/contact-events/**")
                                        .hasAnyRole(CONTACT_EVENT_WRITE_ROLES)
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/contact-events",
                                                "/api/contact-events/**")
                                        .hasAnyRole(CONTACT_EVENT_READ_ROLES)
                                        .requestMatchers(
                                                HttpMethod.GET, "/api/campaigns/*/recipients")
                                        .hasAnyRole(CAMPAIGN_RECIPIENT_ROLES)
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/campaigns/*/approve",
                                                "/api/campaigns/*/reject")
                                        .hasAnyRole(COMPLIANCE_ROLES)
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/campaigns/*/compliance-review-notes")
                                        .hasAnyRole(COMPLIANCE_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/campaigns/**")
                                        .hasAnyRole(CAMPAIGN_MANAGER_ROLES)
                                        .requestMatchers(HttpMethod.PUT, "/api/campaigns/**")
                                        .hasAnyRole(CAMPAIGN_MANAGER_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/campaigns/**")
                                        .hasAnyRole(CAMPAIGN_READ_ROLES)
                                        .requestMatchers(HttpMethod.PUT, "/api/payment-records/**")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.SALES_AGENT.name())
                                        .requestMatchers(
                                                HttpMethod.PATCH, "/api/payment-records/**")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.SALES_AGENT.name())
                                        .requestMatchers(HttpMethod.POST, "/api/payment-records/**")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.SALES_AGENT.name())
                                        .requestMatchers(HttpMethod.GET, "/api/payment-records/**")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.SALES_AGENT.name())
                                        // PathPattern forbids segments after **; list generate paths
                                        // explicitly so CSA can bulk-generate without opening all POST.
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/reminders/payment/generate",
                                                "/api/reminders/expiration/3-month/generate",
                                                "/api/reminders/expiration/6-month/generate",
                                                "/api/reminders/expiration/12-month/generate")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CAMPAIGN_MANAGER.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name())
                                        .requestMatchers(HttpMethod.POST, "/api/reminders/**")
                                        .hasAnyRole(CAMPAIGN_MANAGER_ROLES)
                                        .requestMatchers(HttpMethod.PUT, "/api/reminders/**")
                                        .hasAnyRole(CAMPAIGN_MANAGER_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/reminders/**")
                                        .hasAnyRole(REMINDER_READ_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/analytics/**")
                                        .hasAnyRole(BI_CAMPAIGN_EXECUTIVE_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/reports/**")
                                        .hasAnyRole(BI_CAMPAIGN_EXECUTIVE_ROLES)
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/ai/segment-suggestions",
                                                "/api/ai/product-recommendations")
                                        .hasAnyRole(
                                                SystemRoleName.BI_ANALYST.name(),
                                                SystemRoleName.CAMPAIGN_MANAGER.name())
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/ai/duplicate-contact-warning")
                                        .hasAnyRole(AUTHORIZED_CUSTOMER_ROLES)
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/ai/campaign-copy",
                                                "/api/ai/campaign-copy/**")
                                        .hasAnyRole(CAMPAIGN_MANAGER_ROLES)
                                        // AI-001 search is customer-read scoped; other AI GETs use
                                        // AI recommendation roles (ProtectedEndpointSecurityTests).
                                        .requestMatchers(HttpMethod.GET, "/api/ai/customer-search")
                                        .hasAnyRole(AUTHORIZED_CUSTOMER_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/ai/**")
                                        .hasAnyRole(AI_RECOMMENDATION_ROLES)
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/audit-logs",
                                                "/api/audit-logs/**")
                                        .hasAnyRole(AUDIT_ROLES)
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/follow-up-tasks",
                                                "/api/follow-up-tasks/**")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.SALES_AGENT.name(),
                                                SystemRoleName.CAMPAIGN_MANAGER.name())
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/follow-up-tasks/*/assign",
                                                "/api/follow-up-tasks/{id}/assign")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.SALES_AGENT.name(),
                                                SystemRoleName.CAMPAIGN_MANAGER.name())
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/follow-up-tasks",
                                                "/api/follow-up-tasks/**")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.SALES_AGENT.name())
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/follow-up-tasks",
                                                "/api/follow-up-tasks/**")
                                        .hasAnyRole(
                                                SystemRoleName.ADMIN.name(),
                                                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                                                SystemRoleName.SALES_AGENT.name(),
                                                SystemRoleName.CAMPAIGN_MANAGER.name())
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        // Item 538: JSON ErrorResponse — never container HTML/error pages
                                        // or raw auth exception messages.
                                        .authenticationEntryPoint(
                                                (request, response, authException) ->
                                                        secureErrorResponses.write(
                                                                request,
                                                                response,
                                                                HttpStatus.UNAUTHORIZED,
                                                                "UNAUTHORIZED",
                                                                "Authentication is required"))
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) -> {
                                                    Authentication authentication =
                                                            SecurityContextHolder.getContext()
                                                                    .getAuthentication();
                                                    boolean anonymous =
                                                            authentication == null
                                                                    || !authentication
                                                                            .isAuthenticated()
                                                                    || authentication
                                                                            instanceof
                                                                            AnonymousAuthenticationToken;
                                                    if (anonymous) {
                                                        secureErrorResponses.write(
                                                                request,
                                                                response,
                                                                HttpStatus.UNAUTHORIZED,
                                                                "UNAUTHORIZED",
                                                                "Authentication is required");
                                                    } else {
                                                        secureErrorResponses.write(
                                                                request,
                                                                response,
                                                                HttpStatus.FORBIDDEN,
                                                                "ACCESS_DENIED",
                                                                "Role is not allowed to perform this action");
                                                    }
                                                }))
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configures Spring Security HTTP response headers for a stateless JSON API (item 545).
     *
     * <p>HSTS is disabled here on purpose: production HSTS is applied by {@link
     * HttpsEnforcementFilter} only on HTTPS / forwarded-HTTPS responses so plain HTTP health probes
     * are not branded with HSTS incorrectly.
     */
    static void configureSecurityHeaders(
            org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<?>
                    headers) {
        // Spring Security 6.5: permissionsPolicy(Customizer) returns PermissionsPolicyConfig;
        // use permissionsPolicyHeader(...) to keep chaining on HeadersConfigurer.
        headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                .permissionsPolicyHeader(
                        permissions ->
                                permissions.policy(ApiSecurityHeadersFilter.VALUE_PERMISSIONS))
                .contentSecurityPolicy(
                        csp -> csp.policyDirectives(ApiSecurityHeadersFilter.VALUE_CSP))
                .cacheControl(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts.disable());
    }

    /**
     * CORS configuration (KB item 540).
     *
     * <p>Development defaults to local Vite origins. Production requires an explicit {@code
     * app.cors.allowed-origins} / {@code CORS_ALLOWED_ORIGINS} list — no wildcards, no empty list,
     * and no localhost fallbacks.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            org.springframework.core.env.Environment environment) {
        List<String> allowedOrigins = resolveAllowedOrigins(environment);
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);
        // Cache preflight for production browsers (seconds).
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Resolves and validates allowed CORS origins for the active profile (item 540).
     *
     * <p>Package-visible for unit tests.
     */
    List<String> resolveAllowedOrigins(org.springframework.core.env.Environment environment) {
        boolean production = isProductionProfile(environment);
        String configured = environment.getProperty("app.cors.allowed-origins");
        if (production) {
            return validateProductionOrigins(parseOrigins(configured));
        }
        if (configured == null || configured.isBlank()) {
            return List.of("http://localhost:5173", "http://127.0.0.1:5173");
        }
        return parseOrigins(configured);
    }

    static boolean isProductionProfile(org.springframework.core.env.Environment environment) {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }

    /**
     * Production CORS rules (items 540–541 / acceptance 556): explicit HTTPS origins only.
     *
     * @throws IllegalStateException when origins are missing, wildcarded, or unsafe for production
     */
    static List<String> validateProductionOrigins(List<String> origins) {
        if (origins == null || origins.isEmpty()) {
            throw new IllegalStateException(
                    "Production CORS requires app.cors.allowed-origins / CORS_ALLOWED_ORIGINS "
                            + "with at least one explicit frontend origin");
        }
        for (String origin : origins) {
            if (origin == null || origin.isBlank()) {
                throw new IllegalStateException(
                        "Production CORS origin entries must not be blank");
            }
            if ("*".equals(origin) || origin.contains("*")) {
                throw new IllegalStateException(
                        "Production CORS must not use wildcard origins (found: " + origin + ")");
            }
            String lower = origin.toLowerCase();
            if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
                throw new IllegalStateException(
                        "Production CORS must not allow localhost origins (found: " + origin + ")");
            }
            // Item 541: production frontends must use HTTPS origins (not plain http).
            if (!lower.startsWith("https://")) {
                throw new IllegalStateException(
                        "Production CORS origins must use HTTPS (found: " + origin + ")");
            }
        }
        return List.copyOf(origins);
    }

    static List<String> parseOrigins(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    private static String[] roles(SystemRoleName... roles) {
        return Arrays.stream(roles).map(SystemRoleName::name).toArray(String[]::new);
    }
}
