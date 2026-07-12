package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.common.api.SecureErrorResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces HTTPS for production traffic (KB item 541).
 *
 * <p>Active only when the {@code prod} profile is enabled and {@code app.security.https.required}
 * is true. Accepts either a secure servlet request or a reverse-proxy indication via {@code
 * X-Forwarded-Proto: https}. Health endpoints remain reachable over plain HTTP for internal probes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    public static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";

    private final ProductionHttpsProperties httpsProperties;
    private final SecureErrorResponses secureErrorResponses;
    private final boolean productionProfile;

    public HttpsEnforcementFilter(
            ProductionHttpsProperties httpsProperties,
            SecureErrorResponses secureErrorResponses,
            org.springframework.core.env.Environment environment) {
        this.httpsProperties = httpsProperties;
        this.secureErrorResponses = secureErrorResponses;
        this.productionProfile = SecurityConfiguration.isProductionProfile(environment);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!productionProfile || !httpsProperties.isRequired()) {
            return true;
        }
        return isHealthPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isHttpsRequest(request)) {
            if (httpsProperties.isHstsEnabled()) {
                applyHsts(response);
            }
            filterChain.doFilter(request, response);
            return;
        }

        secureErrorResponses.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "HTTPS_REQUIRED",
                "HTTPS is required for this API in production");
    }

    /**
     * True when the request is TLS-terminated at the app or indicated secure by a trusted proxy
     * header.
     */
    static boolean isHttpsRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader(FORWARDED_PROTO_HEADER);
        if (forwardedProto == null || forwardedProto.isBlank()) {
            return false;
        }
        // First value wins when multiple proxies append: "https,http"
        String first = forwardedProto.split(",")[0].trim();
        return "https".equalsIgnoreCase(first);
    }

    static boolean isHealthPath(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return false;
        }
        String path = requestUri.toLowerCase(Locale.ROOT);
        return path.equals("/api/health")
                || path.startsWith("/api/health/")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/");
    }

    private void applyHsts(HttpServletResponse response) {
        long maxAge = Math.max(0L, httpsProperties.getHstsMaxAgeSeconds());
        response.setHeader(
                "Strict-Transport-Security", "max-age=" + maxAge + "; includeSubDomains");
    }
}
