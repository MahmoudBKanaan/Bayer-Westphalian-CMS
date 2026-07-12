package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bayerwestphalian.campaign.common.api.SecureErrorResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("541 HTTPS production requirement")
class HttpsEnforcementFilterTests {

    @Mock private FilterChain filterChain;

    private ProductionHttpsProperties properties;
    private SecureErrorResponses secureErrorResponses;

    @BeforeEach
    void setUp() {
        properties = new ProductionHttpsProperties();
        properties.setRequired(true);
        properties.setHstsEnabled(true);
        properties.setHstsMaxAgeSeconds(31_536_000L);
        secureErrorResponses =
                new SecureErrorResponses(new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    void doesNotFilterWhenNotProductionProfile() throws Exception {
        HttpsEnforcementFilter filter =
                new HttpsEnforcementFilter(
                        properties, secureErrorResponses, new MockEnvironment().withProperty("spring.profiles.active", "dev"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(filter.shouldNotFilter(request)).isTrue();
        filter.doFilter(request, response, filterChain);
        // shouldNotFilter causes OncePerRequestFilter to skip doFilterInternal; chain still runs
        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    void rejectsInsecureApiRequestInProduction() throws Exception {
        HttpsEnforcementFilter filter = productionFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("HTTPS_REQUIRED");
        assertThat(response.getContentAsString()).contains("HTTPS is required");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void allowsHttpsServletRequestAndSetsHsts() throws Exception {
        HttpsEnforcementFilter filter = productionFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    void allowsXForwardedProtoHttpsFromReverseProxy() throws Exception {
        HttpsEnforcementFilter filter = productionFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/campaigns");
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsHealthEndpointsOverPlainHttpInProduction() throws Exception {
        HttpsEnforcementFilter filter = productionFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(filter.shouldNotFilter(request)).isTrue();
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(403);
    }

    @Test
    void allowsActuatorHealthOverPlainHttpInProduction() throws Exception {
        HttpsEnforcementFilter filter = productionFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void isHttpsRequestParsesForwardedProtoList() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https, http");
        assertThat(HttpsEnforcementFilter.isHttpsRequest(request)).isTrue();

        MockHttpServletRequest httpOnly = new MockHttpServletRequest();
        httpOnly.addHeader("X-Forwarded-Proto", "http");
        assertThat(HttpsEnforcementFilter.isHttpsRequest(httpOnly)).isFalse();
    }

    @Test
    void doesNotEnforceWhenHttpsRequiredIsFalse() throws Exception {
        properties.setRequired(false);
        HttpsEnforcementFilter filter =
                new HttpsEnforcementFilter(
                        properties,
                        secureErrorResponses,
                        new MockEnvironment().withProperty("spring.profiles.active", "prod"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(filter.shouldNotFilter(request)).isTrue();
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    private HttpsEnforcementFilter productionFilter() {
        return new HttpsEnforcementFilter(
                properties, secureErrorResponses, new MockEnvironment().withProperty("spring.profiles.active", "prod"));
    }
}
