package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("545 Backend security headers")
class ApiSecurityHeadersFilterTests {

    @Test
    void appliesApiSecurityHeadersToResponse() throws Exception {
        ApiSecurityHeadersFilter filter = new ApiSecurityHeadersFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_CONTENT_TYPE_OPTIONS))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_NOSNIFF);
        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_FRAME_OPTIONS))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_FRAME_DENY);
        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_REFERRER_POLICY))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_REFERRER);
        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_PERMISSIONS_POLICY))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_PERMISSIONS);
        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_CSP))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_CSP);
        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_CROSS_DOMAIN))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_CROSS_DOMAIN);
        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_CACHE_CONTROL))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_CACHE_CONTROL);
        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_PRAGMA))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_PRAGMA);
        // HSTS is not set by this filter (production HTTPS filter owns HSTS).
        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doesNotOverwriteExistingHeaderValues() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader(ApiSecurityHeadersFilter.HEADER_FRAME_OPTIONS, "SAMEORIGIN");

        ApiSecurityHeadersFilter.applySecurityHeaders(response);

        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_FRAME_OPTIONS))
                .isEqualTo("SAMEORIGIN");
        assertThat(response.getHeader(ApiSecurityHeadersFilter.HEADER_CONTENT_TYPE_OPTIONS))
                .isEqualTo(ApiSecurityHeadersFilter.VALUE_NOSNIFF);
    }

    @Test
    void cspIsRestrictiveForJsonApi() {
        assertThat(ApiSecurityHeadersFilter.VALUE_CSP)
                .contains("default-src 'none'")
                .contains("frame-ancestors 'none'")
                .doesNotContain("unsafe-inline");
    }
}
