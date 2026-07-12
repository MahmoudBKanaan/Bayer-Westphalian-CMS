package com.bayerwestphalian.campaign.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies backend security headers appropriate for a JSON API (KB item 545).
 *
 * <p>Complements Spring Security {@code headers()} configuration. HSTS remains production-only via
 * {@link HttpsEnforcementFilter} (item 541) so local HTTP health checks are not forced onto HTTPS
 * header semantics.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class ApiSecurityHeadersFilter extends OncePerRequestFilter {

    public static final String HEADER_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String HEADER_FRAME_OPTIONS = "X-Frame-Options";
    public static final String HEADER_REFERRER_POLICY = "Referrer-Policy";
    public static final String HEADER_PERMISSIONS_POLICY = "Permissions-Policy";
    public static final String HEADER_CSP = "Content-Security-Policy";
    public static final String HEADER_CROSS_DOMAIN = "X-Permitted-Cross-Domain-Policies";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_PRAGMA = "Pragma";

    public static final String VALUE_NOSNIFF = "nosniff";
    public static final String VALUE_FRAME_DENY = "DENY";
    public static final String VALUE_REFERRER = "no-referrer";
    public static final String VALUE_PERMISSIONS =
            "camera=(), microphone=(), geolocation=(), payment=(), usb=()";
    /** Restrictive CSP for a pure API (no HTML document to render). */
    public static final String VALUE_CSP =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";
    public static final String VALUE_CROSS_DOMAIN = "none";
    public static final String VALUE_CACHE_CONTROL = "no-store, no-cache, must-revalidate, max-age=0";
    public static final String VALUE_PRAGMA = "no-cache";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        applySecurityHeaders(response);
        filterChain.doFilter(request, response);
    }

    /**
     * Sets API security headers if not already present (package-visible for tests).
     */
    static void applySecurityHeaders(HttpServletResponse response) {
        setIfAbsent(response, HEADER_CONTENT_TYPE_OPTIONS, VALUE_NOSNIFF);
        setIfAbsent(response, HEADER_FRAME_OPTIONS, VALUE_FRAME_DENY);
        setIfAbsent(response, HEADER_REFERRER_POLICY, VALUE_REFERRER);
        setIfAbsent(response, HEADER_PERMISSIONS_POLICY, VALUE_PERMISSIONS);
        setIfAbsent(response, HEADER_CSP, VALUE_CSP);
        setIfAbsent(response, HEADER_CROSS_DOMAIN, VALUE_CROSS_DOMAIN);
        setIfAbsent(response, HEADER_CACHE_CONTROL, VALUE_CACHE_CONTROL);
        setIfAbsent(response, HEADER_PRAGMA, VALUE_PRAGMA);
    }

    private static void setIfAbsent(HttpServletResponse response, String name, String value) {
        if (!response.containsHeader(name)) {
            response.setHeader(name, value);
        }
    }
}
