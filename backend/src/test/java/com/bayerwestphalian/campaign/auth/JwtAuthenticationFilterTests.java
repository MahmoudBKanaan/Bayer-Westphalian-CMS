package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.api.SecureErrorResponses;
import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    @Mock private JwtService jwtService;

    private SecureErrorResponses secureErrorResponses;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        secureErrorResponses = new SecureErrorResponses(objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leavesSecurityContextEmptyWhenBearerTokenIsMissing() throws Exception {
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, secureErrorResponses);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(jwtService);
    }

    @Test
    void authenticatesValidBearerAccessToken() throws Exception {
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, secureErrorResponses);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009901");
        JwtTokenClaims claims =
                new JwtTokenClaims(
                        userId,
                        "admin@bayer-westphalian.test",
                        List.of(SystemRoleName.ADMIN, SystemRoleName.BI_ANALYST),
                        JwtTokenType.ACCESS,
                        "bayer-westphalian-campaign-platform-test",
                        Instant.parse("2026-07-04T12:00:00Z"),
                        Instant.parse("2026-07-04T12:15:00Z"),
                        "access-token-id");
        when(jwtService.validateToken("access-token", JwtTokenType.ACCESS)).thenReturn(claims);

        filter.doFilter(request, response, filterChain);

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedPrincipal principal = (AuthenticatedPrincipal) authentication.getPrincipal();

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("admin@bayer-westphalian.test");
        assertThat(principal.roles())
                .containsExactly(SystemRoleName.ADMIN, SystemRoleName.BI_ANALYST);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN", "ROLE_BI_ANALYST");
        assertThat(response.getStatus()).isEqualTo(200);
        verify(jwtService).validateToken("access-token", JwtTokenType.ACCESS);
    }

    @Test
    void rejectsInvalidBearerAccessTokenWithSecureJsonErrorResponse() throws Exception {
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, secureErrorResponses);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        request.addHeader("Authorization", "Bearer invalid-token");
        request.addHeader("X-Request-Id", "req-jwt-538");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        when(jwtService.validateToken("invalid-token", JwtTokenType.ACCESS))
                .thenThrow(new UnauthorizedException("Invalid token signature details"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("message").asText()).isEqualTo("Authentication is required");
        assertThat(body.get("requestId").asText()).isEqualTo("req-jwt-538");
        // Item 538: do not echo internal token validation messages to clients.
        assertThat(response.getContentAsString()).doesNotContain("Invalid token signature details");
        assertThat(response.getContentAsString()).doesNotContain("stackTrace");
    }
}
