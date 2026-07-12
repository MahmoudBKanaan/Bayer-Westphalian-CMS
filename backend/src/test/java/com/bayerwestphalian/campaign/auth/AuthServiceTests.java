package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import com.bayerwestphalian.campaign.user.Role;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import com.bayerwestphalian.campaign.user.UserRole;
import com.bayerwestphalian.campaign.user.UserRoleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Auth service unit tests. Sprint 16 critical restatement of disabled-login: item <b>659</b> —
 * {@link DisabledUserCannotLogInTests}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    private static final String EMAIL = "advisor@bayer-westphalian.test";
    private static final String RAW_PASSWORD = "StrongPassword!2026";
    private static final String PASSWORD_HASH = "$2a$10$examplehash";

    @Mock private UserRepository userRepository;

    @Mock private UserRoleRepository userRoleRepository;

    @Mock private PasswordHashingService passwordHashingService;

    @Mock private JwtService jwtService;

    @Mock private LoginAttemptTracker loginAttemptTracker;

    @InjectMocks private AuthService authService;

    @Test
    void validatesActiveUserCredentialsWithBCryptHash() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        User authenticatedUser = authService.validateCredentials(EMAIL, RAW_PASSWORD);

        assertThat(authenticatedUser).isSameAs(user);
        verify(passwordHashingService).matches(RAW_PASSWORD, PASSWORD_HASH);
        verify(loginAttemptTracker).ensureAllowed(eq(EMAIL), isNull());
        verify(loginAttemptTracker).recordSuccess(eq(EMAIL), isNull());
    }

    @Test
    void rejectsUnknownOrInvalidCredentialsWithGenericMessage() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.validateCredentials(EMAIL, RAW_PASSWORD))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(passwordHashingService, never()).matches(RAW_PASSWORD, PASSWORD_HASH);
        verify(loginAttemptTracker).recordFailure(eq(EMAIL), isNull());
    }

    @Test
    void rejectsThrottledLoginBeforeCredentialLookup() {
        doThrow(new LoginLockoutException(java.time.Instant.now().plusSeconds(60), 60L))
                .when(loginAttemptTracker)
                .ensureAllowed(eq(EMAIL), isNull());

        assertThatThrownBy(() -> authService.validateCredentials(EMAIL, RAW_PASSWORD))
                .isInstanceOf(LoginLockoutException.class)
                .hasMessage(LoginLockoutException.DEFAULT_MESSAGE);

        verify(userRepository, never()).findByEmailIgnoreCase(EMAIL);
        verify(passwordHashingService, never()).matches(RAW_PASSWORD, PASSWORD_HASH);
    }

    @Test
    void recordsLoginFailureWhenPasswordDoesNotMatch() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.validateCredentials(EMAIL, RAW_PASSWORD))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(loginAttemptTracker).recordFailure(eq(EMAIL), isNull());
        verify(loginAttemptTracker, never()).recordSuccess(eq(EMAIL), isNull());
    }

    @Test
    void passesClientIpToLoginAttemptTracker() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        authService.validateCredentials(EMAIL, RAW_PASSWORD, "203.0.113.10");

        verify(loginAttemptTracker).ensureAllowed(EMAIL, "203.0.113.10");
        verify(loginAttemptTracker).recordSuccess(EMAIL, "203.0.113.10");
    }

    @Test
    void rejectsDisabledUsersAfterCredentialValidation() {
        User user = user();
        user.disable();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.validateCredentials(EMAIL, RAW_PASSWORD))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User account is not active");
    }

    @Test
    void loginSessionRejectsDisabledUsersWithoutIssuingTokens() {
        User user = user();
        user.disable();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.loginSession(EMAIL, RAW_PASSWORD))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User account is not active");

        verify(userRepository, never()).save(user);
        verify(userRoleRepository, never()).findByIdUserId(user.getId());
    }

    @Test
    void loginRecordsLastLoginAndReturnsAuthenticatedUserView() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);

        AuthenticatedUser authenticatedUser = authService.login(EMAIL, RAW_PASSWORD);

        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(authenticatedUser.email()).isEqualTo(EMAIL);
        assertThat(authenticatedUser.fullName()).isEqualTo("Advisor User");
        verify(userRepository).save(user);
    }

    @Test
    void loginSessionIssuesJwtPairWithAssignedRoles() {
        User user = user();
        Role role = role(SystemRoleName.ADMIN);
        JwtTokenPair tokenPair =
                new JwtTokenPair(
                        "access-token",
                        Instant.parse("2026-07-03T12:15:00Z"),
                        "refresh-token",
                        Instant.parse("2026-07-10T12:00:00Z"));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHashingService.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(userRoleRepository.findByIdUserId(user.getId()))
                .thenReturn(List.of(UserRole.assign(user, role, null)));
        when(jwtService.issueTokenPair(user, List.of(SystemRoleName.ADMIN))).thenReturn(tokenPair);

        AuthenticatedSession session = authService.loginSession(EMAIL, RAW_PASSWORD);

        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(session.user().email()).isEqualTo(EMAIL);
        assertThat(session.tokens()).isSameAs(tokenPair);
        verify(jwtService).issueTokenPair(user, List.of(SystemRoleName.ADMIN));
    }

    @Test
    void returnsCurrentUserById() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009901");
        User user = user();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthenticatedUser currentUser = authService.getCurrentUser(userId);

        assertThat(currentUser.email()).isEqualTo(EMAIL);
    }

    @Test
    void rejectsMissingCurrentUserContext() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication is required");
    }

    @Test
    void throwsWhenCurrentUserDoesNotExist() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009902");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User was not found: " + userId);
    }

    @Test
    void validatesLogoutUserContext() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009903");
        User user = user();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        authService.logout(userId);

        verify(userRepository).findById(userId);
    }

    @Test
    void logoutSessionValidatesBearerAccessTokenAndUserContext() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009907");
        User user = user();
        JwtTokenClaims claims =
                new JwtTokenClaims(
                        userId,
                        EMAIL,
                        List.of(SystemRoleName.ADMIN),
                        JwtTokenType.ACCESS,
                        "bayer-westphalian-campaign-platform-test",
                        Instant.parse("2026-07-03T12:00:00Z"),
                        Instant.parse("2026-07-03T12:15:00Z"),
                        "access-token-id");
        when(jwtService.validateToken("access-token", JwtTokenType.ACCESS)).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        authService.logoutSession("Bearer access-token");

        verify(jwtService).validateToken("access-token", JwtTokenType.ACCESS);
        verify(userRepository).findById(userId);
    }

    @Test
    void logoutSessionRejectsMissingBearerAccessToken() {
        assertThatThrownBy(() -> authService.logoutSession(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Bearer access token is required");
        assertThatThrownBy(() -> authService.logoutSession("Basic access-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Bearer access token is required");
        assertThatThrownBy(() -> authService.logoutSession("Bearer "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Bearer access token is required");
    }

    @Test
    void logoutSessionRejectsDisabledUsers() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009908");
        User user = user();
        user.disable();
        JwtTokenClaims claims =
                new JwtTokenClaims(
                        userId,
                        EMAIL,
                        List.of(SystemRoleName.ADMIN),
                        JwtTokenType.ACCESS,
                        "bayer-westphalian-campaign-platform-test",
                        Instant.parse("2026-07-03T12:00:00Z"),
                        Instant.parse("2026-07-03T12:15:00Z"),
                        "access-token-id");
        when(jwtService.validateToken("access-token", JwtTokenType.ACCESS)).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.logoutSession("Bearer access-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User account is not active");
    }

    @Test
    void getCurrentSessionUserValidatesBearerAccessTokenAndReturnsSafeUserView() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009909");
        User user = user();
        JwtTokenClaims claims =
                new JwtTokenClaims(
                        userId,
                        EMAIL,
                        List.of(SystemRoleName.ADMIN),
                        JwtTokenType.ACCESS,
                        "bayer-westphalian-campaign-platform-test",
                        Instant.parse("2026-07-03T12:00:00Z"),
                        Instant.parse("2026-07-03T12:15:00Z"),
                        "access-token-id");
        when(jwtService.validateToken("access-token", JwtTokenType.ACCESS)).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthenticatedUser authenticatedUser =
                authService.getCurrentSessionUser("Bearer access-token");

        assertThat(authenticatedUser.email()).isEqualTo(EMAIL);
        assertThat(authenticatedUser.fullName()).isEqualTo("Advisor User");
        verify(jwtService).validateToken("access-token", JwtTokenType.ACCESS);
        verify(userRepository).findById(userId);
    }

    @Test
    void getCurrentSessionUserRejectsMissingBearerAccessToken() {
        assertThatThrownBy(() -> authService.getCurrentSessionUser(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Bearer access token is required");
    }

    @Test
    void getCurrentSessionUserRejectsDisabledUsers() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009910");
        User user = user();
        user.disable();
        JwtTokenClaims claims =
                new JwtTokenClaims(
                        userId,
                        EMAIL,
                        List.of(SystemRoleName.ADMIN),
                        JwtTokenType.ACCESS,
                        "bayer-westphalian-campaign-platform-test",
                        Instant.parse("2026-07-03T12:00:00Z"),
                        Instant.parse("2026-07-03T12:15:00Z"),
                        "access-token-id");
        when(jwtService.validateToken("access-token", JwtTokenType.ACCESS)).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.getCurrentSessionUser("Bearer access-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User account is not active");
    }

    @Test
    void refreshTokenValidatesJwtAndReturnsCurrentUser() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009904");
        User user = user();
        JwtTokenClaims claims =
                new JwtTokenClaims(
                        userId,
                        EMAIL,
                        List.of(),
                        JwtTokenType.REFRESH,
                        "bayer-westphalian-campaign-platform-test",
                        Instant.parse("2026-07-03T12:00:00Z"),
                        Instant.parse("2026-07-10T12:00:00Z"),
                        "refresh-token-id");
        when(jwtService.validateToken("refresh-token", JwtTokenType.REFRESH)).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthenticatedUser authenticatedUser = authService.refreshToken("refresh-token");

        assertThat(authenticatedUser.email()).isEqualTo(EMAIL);
        verify(jwtService).validateToken("refresh-token", JwtTokenType.REFRESH);
    }

    @Test
    void refreshSessionValidatesJwtAndIssuesRotatedTokenPair() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009905");
        User user = user();
        Role role = role(SystemRoleName.ADMIN);
        JwtTokenPair tokenPair =
                new JwtTokenPair(
                        "new-access-token",
                        Instant.parse("2026-07-03T12:15:00Z"),
                        "new-refresh-token",
                        Instant.parse("2026-07-10T12:00:00Z"));
        JwtTokenClaims claims =
                new JwtTokenClaims(
                        userId,
                        EMAIL,
                        List.of(),
                        JwtTokenType.REFRESH,
                        "bayer-westphalian-campaign-platform-test",
                        Instant.parse("2026-07-03T12:00:00Z"),
                        Instant.parse("2026-07-10T12:00:00Z"),
                        "refresh-token-id");
        when(jwtService.validateToken("refresh-token", JwtTokenType.REFRESH)).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByIdUserId(user.getId()))
                .thenReturn(List.of(UserRole.assign(user, role, null)));
        when(jwtService.issueTokenPair(user, List.of(SystemRoleName.ADMIN))).thenReturn(tokenPair);

        AuthenticatedSession session = authService.refreshSession("refresh-token");

        assertThat(session.user().email()).isEqualTo(EMAIL);
        assertThat(session.tokens()).isSameAs(tokenPair);
        verify(jwtService).validateToken("refresh-token", JwtTokenType.REFRESH);
        verify(jwtService).issueTokenPair(user, List.of(SystemRoleName.ADMIN));
    }

    @Test
    void refreshSessionRejectsDisabledUsers() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009906");
        User user = user();
        user.disable();
        JwtTokenClaims claims =
                new JwtTokenClaims(
                        userId,
                        EMAIL,
                        List.of(),
                        JwtTokenType.REFRESH,
                        "bayer-westphalian-campaign-platform-test",
                        Instant.parse("2026-07-03T12:00:00Z"),
                        Instant.parse("2026-07-10T12:00:00Z"),
                        "refresh-token-id");
        when(jwtService.validateToken("refresh-token", JwtTokenType.REFRESH)).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshSession("refresh-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User account is not active");
    }

    @Test
    void rejectsBlankRefreshToken() {
        assertThatThrownBy(() -> authService.refreshToken(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token is required");
    }

    @Test
    void rejectsBlankRefreshSessionToken() {
        assertThatThrownBy(() -> authService.refreshSession(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token is required");
    }

    private static User user() {
        return User.create(EMAIL, PASSWORD_HASH, "Advisor User");
    }

    private static Role role(SystemRoleName roleName) {
        return Role.create(
                roleName,
                "Admin",
                "Manages users, roles, settings, and full system configuration",
                "Manage users, assign roles, manage settings, view audit logs",
                true);
    }
}
