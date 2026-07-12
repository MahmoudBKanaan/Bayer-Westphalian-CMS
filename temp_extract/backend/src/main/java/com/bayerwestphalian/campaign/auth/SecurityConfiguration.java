package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/health/**",
        "/actuator/health/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    static final String[] ADMIN_ROLES = roles(SystemRoleName.ADMIN);
    static final String[] ADMIN_OR_AGENT_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.CUSTOMER_SERVICE_AGENT);
    static final String[] PAYMENT_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT,
                    SystemRoleName.EXECUTIVE_VIEWER,
                    SystemRoleName.SYSTEM_AUDITOR);
    static final String[] AUTHORIZED_CUSTOMER_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT,
                    SystemRoleName.PRODUCT_MANAGER);
    static final String[] BENEFICIARY_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SYSTEM_AUDITOR);
    static final String[] BENEFICIARY_UPDATE_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.COMPLIANCE_OFFICER);
    static final String[] CONSENT_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SYSTEM_AUDITOR);
    static final String[] CONSENT_WRITE_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.COMPLIANCE_OFFICER);
    static final String[] ADMIN_OR_PRODUCT_MANAGER_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.PRODUCT_MANAGER);
    static final String[] PRODUCT_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.PRODUCT_MANAGER,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT,
                    SystemRoleName.EXECUTIVE_VIEWER);
    /** Campaign write/submit/launch (Admin + Campaign Manager; matches {@code canManageCampaigns}). */
    public static final String[] CAMPAIGN_MANAGER_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.CAMPAIGN_MANAGER);
    /** POST /api/segments — create reusable segments (KB: Campaign Manager + Admin). */
    public static final String[] SEGMENT_CREATE_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.CAMPAIGN_MANAGER);
    /**
     * PUT/DELETE /api/segments/** — edit and delete saved segments. BI Analyst is intentionally
     * excluded unless they also hold ADMIN or CAMPAIGN_MANAGER (item 200).
     */
    public static final String[] SEGMENT_MANAGE_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.CAMPAIGN_MANAGER);
    public static final String[] SEGMENT_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER);
    public static final String[] SEGMENT_PREVIEW_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST);
    public static final String[] CAMPAIGN_SEGMENT_PREVIEW_ROLES = SEGMENT_PREVIEW_ROLES;
    /** Campaign approve/reject (Admin + Compliance; matches {@code canApproveCampaigns}). */
    public static final String[] COMPLIANCE_ROLES =
            roles(SystemRoleName.ADMIN, SystemRoleName.COMPLIANCE_OFFICER);
    static final String[] CAMPAIGN_RECIPIENT_ROLES =
            roles(SystemRoleName.CAMPAIGN_MANAGER, SystemRoleName.COMPLIANCE_OFFICER);
    /** GET /api/campaigns/** list/details (matches {@code canReadCampaigns}). */
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
    /** GET /api/contact-events/** timeline/history reads. */
    public static final String[] CONTACT_EVENT_READ_ROLES = CAMPAIGN_READ_ROLES;
    static final String[] REMINDER_READ_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.CUSTOMER_SERVICE_AGENT,
                    SystemRoleName.SALES_AGENT,
                    SystemRoleName.SYSTEM_AUDITOR);
    static final String[] BI_CAMPAIGN_EXECUTIVE_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.MARKETING_ANALYST,
                    SystemRoleName.EXECUTIVE_VIEWER);
    static final String[] AI_RECOMMENDATION_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.CAMPAIGN_MANAGER,
                    SystemRoleName.BI_ANALYST,
                    SystemRoleName.PRODUCT_MANAGER,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.EXECUTIVE_VIEWER,
                    SystemRoleName.SYSTEM_AUDITOR);
    static final String[] AUDIT_ROLES =
            roles(
                    SystemRoleName.ADMIN,
                    SystemRoleName.COMPLIANCE_OFFICER,
                    SystemRoleName.SYSTEM_AUDITOR);

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
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
                                        .requestMatchers(
                                                HttpMethod.DELETE, "/api/beneficiaries/**")
                                        .hasAnyRole(ADMIN_OR_AGENT_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/beneficiaries/**")
                                        .hasAnyRole(BENEFICIARY_READ_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/consents/**")
                                        .hasAnyRole(CONSENT_WRITE_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/consents/**")
                                        .hasAnyRole(CONSENT_READ_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/contact-events/**")
                                        .hasAnyRole(CONTACT_EVENT_READ_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/products/**")
                                        .hasAnyRole(PRODUCT_READ_ROLES)
                                        .requestMatchers(
                                                HttpMethod.GET, "/api/product-ownerships/**")
                                        .hasAnyRole(PRODUCT_READ_ROLES)
                                        .requestMatchers(
                                                HttpMethod.GET, "/api/product-change-requests/**")
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
                                        .requestMatchers(HttpMethod.GET, "/api/payment-records/**")
                                        .hasAnyRole(PAYMENT_READ_ROLES)
                                        .requestMatchers(
                                                HttpMethod.POST, "/api/payment-records/**")
                                        .hasAnyRole(ADMIN_OR_AGENT_ROLES)
                                        .requestMatchers(HttpMethod.PUT, "/api/payment-records/**")
                                        .hasAnyRole(ADMIN_OR_AGENT_ROLES)
                                        .requestMatchers(
                                                HttpMethod.PATCH, "/api/payment-records/**")
                                        .hasAnyRole(ADMIN_OR_AGENT_ROLES)
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
                                                HttpMethod.GET, "/api/campaigns/*/recipients")
                                        .hasAnyRole(CAMPAIGN_RECIPIENT_ROLES)
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/campaigns/*/approve",
                                                "/api/campaigns/*/reject")
                                        .hasAnyRole(COMPLIANCE_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/campaigns/**")
                                        .hasAnyRole(CAMPAIGN_MANAGER_ROLES)
                                        .requestMatchers(
                                                HttpMethod.PUT,
                                                "/api/campaigns/*/compliance-review-notes")
                                        .hasAnyRole(COMPLIANCE_ROLES)
                                        .requestMatchers(HttpMethod.PUT, "/api/campaigns/**")
                                        .hasAnyRole(CAMPAIGN_MANAGER_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/campaigns/**")
                                        .hasAnyRole(CAMPAIGN_READ_ROLES)
                                        .requestMatchers(HttpMethod.POST, "/api/reminders/**")
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
                                        .requestMatchers(HttpMethod.POST, "/api/ai/campaign-copy")
                                        .hasAnyRole(CAMPAIGN_MANAGER_ROLES)
                                        .requestMatchers(HttpMethod.GET, "/api/ai/**")
                                        .hasAnyRole(AI_RECOMMENDATION_ROLES)
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/audit-logs",
                                                "/api/audit-logs/**")
                                        .hasAnyRole(AUDIT_ROLES)
                                        .anyRequest()
                                        .denyAll())
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            org.springframework.core.env.Environment environment) {
        String allowedOrigins =
                environment.getProperty(
                        "app.cors.allowed-origins", "http://localhost:5173,http://127.0.0.1:5173");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(commaSeparatedValues(allowedOrigins));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> commaSeparatedValues(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    private static String[] roles(SystemRoleName... roles) {
        return Arrays.stream(roles).map(SystemRoleName::name).toArray(String[]::new);
    }
}
