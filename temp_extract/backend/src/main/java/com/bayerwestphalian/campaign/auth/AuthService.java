package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import com.bayerwestphalian.campaign.user.UserRoleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordHashingService passwordHashingService;
    private final JwtService jwtService;
    private final LoginAttemptTracker loginAttemptTracker;

    public AuthService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordHashingService passwordHashingService,
            JwtService jwtService,
            LoginAttemptTracker loginAttemptTracker) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordHashingService = passwordHashingService;
        this.jwtService = jwtService;
        this.loginAttemptTracker = loginAttemptTracker;
    }

    @Transactional
    public AuthenticatedUser login(String email, String rawPassword) {
        User user = validateCredentials(email, rawPassword);

        user.recordLogin(Instant.now());
        return AuthenticatedUser.from(userRepository.save(user));
    }

    @Transactional
    public AuthenticatedSession loginSession(String email, String rawPassword) {
        User user = validateCredentials(email, rawPassword);

        user.recordLogin(Instant.now());
        User savedUser = userRepository.save(user);
        List<SystemRoleName> roles = assignedRoles(savedUser);

        return new AuthenticatedSession(
                AuthenticatedUser.from(savedUser), jwtService.issueTokenPair(savedUser, roles));
    }

    public AuthenticatedUser refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException("Refresh token is required");
        }
        JwtTokenClaims claims = jwtService.validateToken(refreshToken, JwtTokenType.REFRESH);
        return AuthenticatedUser.from(findActiveUser(claims.userId()));
    }

    @Transactional(readOnly = true)
    public AuthenticatedSession refreshSession(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException("Refresh token is required");
        }

        JwtTokenClaims claims = jwtService.validateToken(refreshToken, JwtTokenType.REFRESH);
        User user = findActiveUser(claims.userId());
        List<SystemRoleName> roles = assignedRoles(user);

        return new AuthenticatedSession(
                AuthenticatedUser.from(user), jwtService.issueTokenPair(user, roles));
    }

    public void logout(UUID userId) {
        getCurrentUser(userId);
    }

    @Transactional(readOnly = true)
    public void logoutSession(String authorizationHeader) {
        String accessToken = bearerToken(authorizationHeader);
        JwtTokenClaims claims = jwtService.validateToken(accessToken, JwtTokenType.ACCESS);

        findActiveUser(claims.userId());
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser getCurrentSessionUser(String authorizationHeader) {
        String accessToken = bearerToken(authorizationHeader);
        JwtTokenClaims claims = jwtService.validateToken(accessToken, JwtTokenType.ACCESS);

        return AuthenticatedUser.from(findActiveUser(claims.userId()));
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser getCurrentUser(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        return AuthenticatedUser.from(findUser(userId));
    }

    @Transactional(readOnly = true)
    public User validateCredentials(String email, String rawPassword) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(rawPassword)) {
            throw invalidCredentials();
        }

        loginAttemptTracker.ensureAllowed(email);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !passwordHashingService.matches(rawPassword, user.getPasswordHash())) {
            loginAttemptTracker.recordFailure(email);
            throw invalidCredentials();
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is not active");
        }

        loginAttemptTracker.recordSuccess(email);
        return user;
    }

    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid email or password");
    }

    private String bearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)
                || !authorizationHeader.startsWith("Bearer ")
                || !StringUtils.hasText(authorizationHeader.substring(7))) {
            throw new UnauthorizedException("Bearer access token is required");
        }
        return authorizationHeader.substring(7);
    }

    private User findActiveUser(UUID userId) {
        User user = findUser(userId);
        if (!user.isActive()) {
            throw new UnauthorizedException("User account is not active");
        }
        return user;
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private List<SystemRoleName> assignedRoles(User user) {
        return userRoleRepository.findByIdUserId(user.getId()).stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();
    }
}
