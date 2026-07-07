package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTests {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000009901");
    private static final Instant NOW = Instant.parse("2026-07-03T12:00:00Z");
    private static final String ISSUER = "bayer-westphalian-campaign-platform-test";
    private static final String SECRET = "test-secret-with-enough-entropy-for-hmac-signing";

    private final JwtService jwtService =
            new JwtService(
                    new ObjectMapper(),
                    ISSUER,
                    SECRET,
                    Duration.ofMinutes(15),
                    Duration.ofDays(7),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void issuesAccessTokenWithKbUserAndRoleClaims() throws Exception {
        User user = user();

        String token =
                jwtService.generateAccessToken(
                        user, List.of(SystemRoleName.BI_ANALYST, SystemRoleName.ADMIN));
        JwtTokenClaims claims = jwtService.validateToken(token, JwtTokenType.ACCESS);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(claims.userId()).isEqualTo(USER_ID);
        assertThat(claims.email()).isEqualTo("advisor@bayer-westphalian.test");
        assertThat(claims.roles()).containsExactly(SystemRoleName.ADMIN, SystemRoleName.BI_ANALYST);
        assertThat(claims.tokenType()).isEqualTo(JwtTokenType.ACCESS);
        assertThat(claims.issuer()).isEqualTo(ISSUER);
        assertThat(claims.issuedAt()).isEqualTo(NOW);
        assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(claims.tokenId()).isNotBlank();
    }

    @Test
    void issuesRefreshTokenWithoutRoleClaims() throws Exception {
        User user = user();

        String token = jwtService.generateRefreshToken(user);
        JwtTokenClaims claims = jwtService.validateToken(token, JwtTokenType.REFRESH);

        assertThat(claims.userId()).isEqualTo(USER_ID);
        assertThat(claims.roles()).isEmpty();
        assertThat(claims.tokenType()).isEqualTo(JwtTokenType.REFRESH);
        assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void issuesTokenPairWithSeparateAccessAndRefreshExpirations() throws Exception {
        User user = user();

        JwtTokenPair tokenPair = jwtService.issueTokenPair(user, List.of(SystemRoleName.ADMIN));

        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotBlank();
        assertThat(tokenPair.accessToken()).isNotEqualTo(tokenPair.refreshToken());
        assertThat(tokenPair.accessTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(tokenPair.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void extractsUserIdAndRolesFromValidatedToken() throws Exception {
        User user = user();

        String token = jwtService.generateAccessToken(user, List.of(SystemRoleName.ADMIN));

        assertThat(jwtService.extractUserId(token)).isEqualTo(USER_ID);
        assertThat(jwtService.extractRoles(token)).containsExactly(SystemRoleName.ADMIN);
    }

    @Test
    void rejectsTamperedTokenSignature() throws Exception {
        String token = jwtService.generateAccessToken(user(), List.of(SystemRoleName.ADMIN));
        char replacement = token.endsWith("x") ? 'y' : 'x';
        String tamperedToken = token.substring(0, token.length() - 1) + replacement;

        assertThatThrownBy(() -> jwtService.validateToken(tamperedToken, JwtTokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid token");
    }

    @Test
    void rejectsUnexpectedTokenType() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(user());

        assertThatThrownBy(() -> jwtService.validateToken(refreshToken, JwtTokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Unexpected token type");
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        JwtService expiredTokenService =
                new JwtService(
                        new ObjectMapper(),
                        ISSUER,
                        SECRET,
                        Duration.ofMinutes(-1),
                        Duration.ofDays(7),
                        Clock.fixed(NOW, ZoneOffset.UTC));
        String token = expiredTokenService.generateAccessToken(user(), List.of());

        assertThatThrownBy(() -> expiredTokenService.validateToken(token, JwtTokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Token has expired");
    }

    @Test
    void rejectsMalformedOrBlankTokens() {
        assertThatThrownBy(() -> jwtService.validateToken(" ", JwtTokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid token");
        assertThatThrownBy(() -> jwtService.validateToken("not-a-jwt", JwtTokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid token");
    }

    @Test
    void requiresUserIdBeforeTokenGeneration() {
        User user =
                User.create("advisor@bayer-westphalian.test", "$2a$10$examplehash", "Advisor User");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> jwtService.generateAccessToken(user, List.of()))
                .withMessage("User id is required for JWT generation");
    }

    @Test
    void requiresConfiguredSecretBeforeSigning() throws Exception {
        JwtService serviceWithoutSecret =
                new JwtService(
                        new ObjectMapper(),
                        ISSUER,
                        " ",
                        Duration.ofMinutes(15),
                        Duration.ofDays(7),
                        Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatIllegalStateException()
                .isThrownBy(() -> serviceWithoutSecret.generateAccessToken(user(), List.of()))
                .withMessage("JWT secret must be configured");
    }

    private static User user() throws Exception {
        User user =
                User.create("advisor@bayer-westphalian.test", "$2a$10$examplehash", "Advisor User");
        Field id = BaseEntity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(user, USER_ID);
        return user;
    }
}
