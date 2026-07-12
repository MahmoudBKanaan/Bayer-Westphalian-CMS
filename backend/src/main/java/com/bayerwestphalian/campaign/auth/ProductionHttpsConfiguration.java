package com.bayerwestphalian.campaign.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables HTTPS property binding for all profiles (KB item 541).
 *
 * <p>Enforcement is performed by {@link HttpsEnforcementFilter}, which only blocks requests when
 * the {@code prod} profile is active and {@code app.security.https.required=true}. TLS is expected
 * to terminate at the reverse proxy (Caddy/nginx); the backend trusts {@code X-Forwarded-Proto}
 * via {@code server.forward-headers-strategy: framework}.
 */
@Configuration
@EnableConfigurationProperties(ProductionHttpsProperties.class)
public class ProductionHttpsConfiguration {}
