package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER =
            Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> JSON_MAP = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final String issuer;
    private final String secret;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    @Autowired
    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${app.security.jwt.refresh-token-days}") long refreshTokenDays) {
        this(
                objectMapper,
                issuer,
                secret,
                Duration.ofMinutes(accessTokenMinutes),
                Duration.ofDays(refreshTokenDays),
                Clock.systemUTC());
    }

    JwtService(
            ObjectMapper objectMapper,
            String issuer,
            String secret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            Clock clock) {
        this.objectMapper = objectMapper;
        this.issuer = issuer;
        this.secret = secret;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    public JwtTokenPair issueTokenPair(User user, Collection<SystemRoleName> roles) {
        Instant issuedAt = Instant.now(clock);
        Instant accessExpiresAt = issuedAt.plus(accessTokenTtl);
        Instant refreshExpiresAt = issuedAt.plus(refreshTokenTtl);

        return new JwtTokenPair(
                generateToken(user, roles, JwtTokenType.ACCESS, issuedAt, accessExpiresAt),
                accessExpiresAt,
                generateToken(user, List.of(), JwtTokenType.REFRESH, issuedAt, refreshExpiresAt),
                refreshExpiresAt);
    }

    public String generateAccessToken(User user, Collection<SystemRoleName> roles) {
        Instant issuedAt = Instant.now(clock);
        return generateToken(
                user, roles, JwtTokenType.ACCESS, issuedAt, issuedAt.plus(accessTokenTtl));
    }

    public String generateRefreshToken(User user) {
        Instant issuedAt = Instant.now(clock);
        return generateToken(
                user, List.of(), JwtTokenType.REFRESH, issuedAt, issuedAt.plus(refreshTokenTtl));
    }

    public JwtTokenClaims validateToken(String token, JwtTokenType expectedType) {
        String[] parts = splitToken(token);
        String signedContent = parts[0] + "." + parts[1];

        if (!MessageDigest.isEqual(sign(signedContent), decode(parts[2]))) {
            throw invalidToken();
        }

        Map<String, Object> payload = readJson(parts[1]);
        JwtTokenClaims claims = toClaims(payload);

        if (!issuer.equals(claims.issuer())) {
            throw invalidToken();
        }
        if (expectedType != null && claims.tokenType() != expectedType) {
            throw new UnauthorizedException("Unexpected token type");
        }
        if (!claims.expiresAt().isAfter(Instant.now(clock))) {
            throw new UnauthorizedException("Token has expired");
        }

        return claims;
    }

    public UUID extractUserId(String token) {
        return validateToken(token, null).userId();
    }

    public List<SystemRoleName> extractRoles(String accessToken) {
        return validateToken(accessToken, JwtTokenType.ACCESS).roles();
    }

    private String generateToken(
            User user,
            Collection<SystemRoleName> roles,
            JwtTokenType tokenType,
            Instant issuedAt,
            Instant expiresAt) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id is required for JWT generation");
        }

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload =
                Map.of(
                        "iss", issuer,
                        "sub", user.getId().toString(),
                        "email", user.getEmail(),
                        "roles", roleNames(roles),
                        "typ", tokenType.name(),
                        "iat", issuedAt.getEpochSecond(),
                        "exp", expiresAt.getEpochSecond(),
                        "jti", UUID.randomUUID().toString());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signedContent = headerPart + "." + payloadPart;

        return signedContent + "." + BASE64_URL_ENCODER.encodeToString(sign(signedContent));
    }

    private List<String> roleNames(Collection<SystemRoleName> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream().map(SystemRoleName::name).sorted().toList();
    }

    private JwtTokenClaims toClaims(Map<String, Object> payload) {
        try {
            return new JwtTokenClaims(
                    UUID.fromString((String) payload.get("sub")),
                    (String) payload.get("email"),
                    rolesFrom(payload.get("roles")),
                    JwtTokenType.valueOf((String) payload.get("typ")),
                    (String) payload.get("iss"),
                    Instant.ofEpochSecond(((Number) payload.get("iat")).longValue()),
                    Instant.ofEpochSecond(((Number) payload.get("exp")).longValue()),
                    (String) payload.get("jti"));
        } catch (RuntimeException exception) {
            throw invalidToken();
        }
    }

    private List<SystemRoleName> rolesFrom(Object roles) {
        if (!(roles instanceof List<?> roleValues)) {
            return List.of();
        }
        return roleValues.stream().map(String.class::cast).map(SystemRoleName::valueOf).toList();
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JWT payload could not be serialized", exception);
        }
    }

    private Map<String, Object> readJson(String tokenPart) {
        try {
            return objectMapper.readValue(decode(tokenPart), JSON_MAP);
        } catch (RuntimeException | IOException exception) {
            throw invalidToken();
        }
    }

    private String[] splitToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw invalidToken();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw invalidToken();
        }
        return parts;
    }

    private byte[] sign(String content) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT signature could not be generated", exception);
        }
    }

    private byte[] decode(String tokenPart) {
        try {
            return BASE64_URL_DECODER.decode(tokenPart);
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private static UnauthorizedException invalidToken() {
        return new UnauthorizedException("Invalid token");
    }
}
