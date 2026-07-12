package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import com.bayerwestphalian.campaign.user.UserRoleRepository;
import com.bayerwestphalian.campaign.user.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Sprint 16 critical test item <b>659</b>: Disabled user cannot log in.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code FR-001} — Login is only for active employee accounts
 *   <li>{@code NFR-001} — Security: disabled accounts must not obtain sessions
 *   <li>{@code FR-005} — Admin can disable users; disabled status blocks authentication
 *   <li>Authentication design: disabled or locked users are rejected before a session is issued
 * </ul>
 *
 * <p>Enforcement: {@link AuthService#validateCredentials} checks {@link User#isActive()} after
 * password match; {@link AuthService#findActiveUser} re-checks on refresh / session operations.
 *
 * <p>Companion coverage: {@link AuthServiceTests}, {@link AuthControllerTests}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("659 Disabled user cannot log in")
class DisabledUserCannotLogInTests {

    private static final String EMAIL = "disabled.user@bayer-westphalian.test";
    private static final String RAW_PASSWORD = "StrongPassword!2026";
    private static final String PASSWORD_HASH = "$2a$10$examplehash";
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000659");
    private static final String NOT_ACTIVE_MESSAGE = "User account is not active";

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordHashingService passwordHashingService;
    @Mock private JwtService jwtService;
    @Mock private LoginAttemptTracker loginAttemptTracker;
    @Mock private AuthService authServiceForController;

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(
                        userRepository,
                        userRoleRepository,
                        passwordHashingService,
                        jwtService,
                        loginAttemptTracker);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AuthController(authServiceForController))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Nested
    @DisplayName("Domain: DISABLED is not active")
    class DomainStatus {

        @Test
        void disabledUserIsNotActive() {
            User user = activeUser();
            assertThat(user.isActive()).isTrue();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

            user.disable();

            assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(user.isActive()).isFalse();
        }

        @Test
        void lockedUserIsNotActiveEither() {
            User user = activeUser();
            user.lock();

            assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
            assertThat(user.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("AuthService: credential validation and login session")
    class CredentialAndLogin {

        @Test
        void validateCredentialsRejectsDisabledUserEvenWhenPasswordMatches() {
            User user = disabledUser();
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
            when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

            assertThatThrownBy(() -> authService.validateCredentials(EMAIL, RAW_PASSWORD))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(NOT_ACTIVE_MESSAGE);

            verify(loginAttemptTracker).ensureAllowed(eq(EMAIL), isNull());
            verify(loginAttemptTracker, never()).recordSuccess(eq(EMAIL), isNull());
            verify(jwtService, never()).issueTokenPair(any(), any());
        }

        @Test
        void loginSessionDoesNotIssueTokensOrRecordLastLoginForDisabledUser() {
            User user = disabledUser();
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
            when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

            assertThatThrownBy(() -> authService.loginSession(EMAIL, RAW_PASSWORD))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(NOT_ACTIVE_MESSAGE);

            assertThat(user.getLastLoginAt()).isNull();
            verify(userRepository, never()).save(user);
            verify(userRoleRepository, never()).findByIdUserId(any());
            verify(jwtService, never()).issueTokenPair(any(), any());
        }

        @Test
        void loginDoesNotPersistLastLoginForDisabledUser() {
            User user = disabledUser();
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
            when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

            assertThatThrownBy(() -> authService.login(EMAIL, RAW_PASSWORD))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(NOT_ACTIVE_MESSAGE);

            verify(userRepository, never()).save(any());
        }

        @Test
        void activeUserCanStillAuthenticate() {
            User user = activeUser();
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
            when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

            User authenticated = authService.validateCredentials(EMAIL, RAW_PASSWORD);

            assertThat(authenticated).isSameAs(user);
            assertThat(authenticated.isActive()).isTrue();
            verify(loginAttemptTracker).recordSuccess(eq(EMAIL), isNull());
        }
    }

    @Nested
    @DisplayName("AuthService: session operations re-check active status")
    class SessionRevalidation {

        @Test
        void refreshSessionRejectsDisabledUser() {
            User user = disabledUser();
            JwtTokenClaims claims = refreshClaims();
            when(jwtService.validateToken("refresh-token", JwtTokenType.REFRESH)).thenReturn(claims);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.refreshSession("refresh-token"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(NOT_ACTIVE_MESSAGE);

            verify(jwtService, never()).issueTokenPair(any(), any());
        }

        @Test
        void getCurrentSessionUserRejectsDisabledUser() {
            User user = disabledUser();
            JwtTokenClaims claims = accessClaims();
            when(jwtService.validateToken("access-token", JwtTokenType.ACCESS)).thenReturn(claims);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.getCurrentSessionUser("Bearer access-token"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(NOT_ACTIVE_MESSAGE);
        }
    }

    @Nested
    @DisplayName("HTTP: POST /api/auth/login returns 401 for disabled accounts")
    class LoginEndpoint {

        @Test
        void loginEndpointReturnsUnauthorizedWhenAccountNotActive() throws Exception {
            when(authServiceForController.loginSession(
                            eq(EMAIL), eq(RAW_PASSWORD), any()))
                    .thenThrow(new UnauthorizedException(NOT_ACTIVE_MESSAGE));

            String body =
                    """
                    {"email":"%s","password":"%s"}
                    """
                            .formatted(EMAIL, RAW_PASSWORD);

            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value(NOT_ACTIVE_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }
    }

    private static User activeUser() {
        return User.create(EMAIL, PASSWORD_HASH, "Disabled User Test");
    }

    private static User disabledUser() {
        User user = activeUser();
        user.disable();
        return user;
    }

    private static JwtTokenClaims accessClaims() {
        return new JwtTokenClaims(
                USER_ID,
                EMAIL,
                List.of(SystemRoleName.CUSTOMER_SERVICE_AGENT),
                JwtTokenType.ACCESS,
                "bayer-westphalian-campaign-platform-test",
                Instant.parse("2026-07-12T12:00:00Z"),
                Instant.parse("2026-07-12T12:15:00Z"),
                "access-token-id-659");
    }

    private static JwtTokenClaims refreshClaims() {
        return new JwtTokenClaims(
                USER_ID,
                EMAIL,
                List.of(SystemRoleName.CUSTOMER_SERVICE_AGENT),
                JwtTokenType.REFRESH,
                "bayer-westphalian-campaign-platform-test",
                Instant.parse("2026-07-12T12:00:00Z"),
                Instant.parse("2026-07-19T12:00:00Z"),
                "refresh-token-id-659");
    }
}
