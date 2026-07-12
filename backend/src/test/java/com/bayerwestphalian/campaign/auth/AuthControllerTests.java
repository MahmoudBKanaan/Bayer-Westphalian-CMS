package com.bayerwestphalian.campaign.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import com.bayerwestphalian.campaign.user.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    private static final String LOGIN_JSON =
            """
            {
              "email": "advisor@bayer-westphalian.test",
              "password": "StrongPassword!2026"
            }
            """;

    private static final String REFRESH_JSON =
            """
            {
              "refreshToken": "refresh-token"
            }
            """;

    @Mock private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AuthController(authService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void loginEndpointReturnsAuthenticatedSession() throws Exception {
        AuthenticatedSession session =
                new AuthenticatedSession(
                        new AuthenticatedUser(
                                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                                "advisor@bayer-westphalian.test",
                                "Advisor User",
                                UserStatus.ACTIVE,
                                Instant.parse("2026-07-03T12:00:00Z")),
                        new JwtTokenPair(
                                "access-token",
                                Instant.parse("2026-07-03T12:15:00Z"),
                                "refresh-token",
                                Instant.parse("2026-07-10T12:00:00Z")));
        when(authService.loginSession(
                        eq("advisor@bayer-westphalian.test"),
                        eq("StrongPassword!2026"),
                        any()))
                .thenReturn(session);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.user.email").value("advisor@bayer-westphalian.test"))
                .andExpect(jsonPath("$.data.user.fullName").value("Advisor User"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.tokens.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.tokens.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());

        verify(authService)
                .loginSession(eq("advisor@bayer-westphalian.test"), eq("StrongPassword!2026"), any());
    }

    @Test
    void loginEndpointReturnsValidationErrorsForInvalidRequest() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void loginEndpointReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.loginSession(
                        eq("advisor@bayer-westphalian.test"),
                        eq("StrongPassword!2026"),
                        any()))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginEndpointReturnsUnauthorizedForDisabledUser() throws Exception {
        when(authService.loginSession(
                        eq("advisor@bayer-westphalian.test"),
                        eq("StrongPassword!2026"),
                        any()))
                .thenThrow(new UnauthorizedException("User account is not active"));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("User account is not active"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void loginEndpointReturnsTooManyRequestsWhenLoginAttemptsAreThrottled() throws Exception {
        when(authService.loginSession(
                        eq("advisor@bayer-westphalian.test"),
                        eq("StrongPassword!2026"),
                        any()))
                .thenThrow(new LoginLockoutException(Instant.now().plusSeconds(120), 120L));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(LOGIN_JSON)
                                .with(
                                        request -> {
                                            request.setRemoteAddr("198.51.100.10");
                                            return request;
                                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(LoginLockoutException.CODE))
                .andExpect(
                        jsonPath("$.message")
                                .value(LoginLockoutException.DEFAULT_MESSAGE))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(header().string("Retry-After", "120"));

        verify(authService)
                .loginSession(
                        "advisor@bayer-westphalian.test", "StrongPassword!2026", "198.51.100.10");
    }

    @Test
    void refreshEndpointReturnsRotatedAuthenticatedSession() throws Exception {
        AuthenticatedSession session =
                new AuthenticatedSession(
                        new AuthenticatedUser(
                                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                                "advisor@bayer-westphalian.test",
                                "Advisor User",
                                UserStatus.ACTIVE,
                                Instant.parse("2026-07-03T12:00:00Z")),
                        new JwtTokenPair(
                                "new-access-token",
                                Instant.parse("2026-07-03T12:15:00Z"),
                                "new-refresh-token",
                                Instant.parse("2026-07-10T12:00:00Z")));
        when(authService.refreshSession("refresh-token")).thenReturn(session);

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REFRESH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.user.email").value("advisor@bayer-westphalian.test"))
                .andExpect(jsonPath("$.data.tokens.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.tokens.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());

        verify(authService).refreshSession("refresh-token");
    }

    @Test
    void refreshEndpointReturnsValidationErrorsForInvalidRequest() throws Exception {
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/auth/refresh"));
    }

    @Test
    void refreshEndpointReturnsUnauthorizedForInvalidRefreshToken() throws Exception {
        when(authService.refreshSession("refresh-token"))
                .thenThrow(new UnauthorizedException("Invalid token"));

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REFRESH_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }

    @Test
    void logoutEndpointReturnsSuccessfulResponse() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                                .header("Authorization", "Bearer access-token")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService).logoutSession("Bearer access-token");
    }

    @Test
    void logoutEndpointReturnsUnauthorizedWhenBearerTokenIsMissing() throws Exception {
        doThrow(new UnauthorizedException("Bearer access token is required"))
                .when(authService)
                .logoutSession(null);

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Bearer access token is required"));
    }

    @Test
    void logoutEndpointReturnsUnauthorizedForInvalidAccessToken() throws Exception {
        doThrow(new UnauthorizedException("Invalid token"))
                .when(authService)
                .logoutSession("Bearer access-token");

        mockMvc.perform(
                        post("/api/auth/logout")
                                .header("Authorization", "Bearer access-token")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }

    @Test
    void meEndpointReturnsCurrentAuthenticatedUser() throws Exception {
        AuthenticatedUser user =
                new AuthenticatedUser(
                        UUID.fromString("10000000-0000-0000-0000-000000009901"),
                        "advisor@bayer-westphalian.test",
                        "Advisor User",
                        UserStatus.ACTIVE,
                        Instant.parse("2026-07-03T12:00:00Z"));
        when(authService.getCurrentSessionUser("Bearer access-token")).thenReturn(user);

        mockMvc.perform(
                        get("/api/auth/me")
                                .header("Authorization", "Bearer access-token")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current user loaded"))
                .andExpect(jsonPath("$.data.email").value("advisor@bayer-westphalian.test"))
                .andExpect(jsonPath("$.data.fullName").value("Advisor User"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(authService).getCurrentSessionUser("Bearer access-token");
    }

    @Test
    void meEndpointReturnsUnauthorizedWhenBearerTokenIsMissing() throws Exception {
        when(authService.getCurrentSessionUser(null))
                .thenThrow(new UnauthorizedException("Bearer access token is required"));

        mockMvc.perform(get("/api/auth/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Bearer access token is required"));
    }

    @Test
    void meEndpointReturnsUnauthorizedForInvalidAccessToken() throws Exception {
        when(authService.getCurrentSessionUser("Bearer access-token"))
                .thenThrow(new UnauthorizedException("Invalid token"));

        mockMvc.perform(
                        get("/api/auth/me")
                                .header("Authorization", "Bearer access-token")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }
}
