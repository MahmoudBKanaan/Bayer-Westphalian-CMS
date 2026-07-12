package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@DisplayName("544 AuthController client IP for lockout")
class AuthControllerClientIpTests {

    @Test
    void prefersFirstXForwardedForHop() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.1");
        request.setRemoteAddr("10.0.0.1");

        assertThat(AuthController.clientIp(request)).isEqualTo("203.0.113.50");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");

        assertThat(AuthController.clientIp(request)).isEqualTo("198.51.100.20");
    }
}
