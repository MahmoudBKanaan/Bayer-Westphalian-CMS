package com.bayerwestphalian.campaign.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * In-memory login rate limiting and temporary lockout strategy (KB item 544).
 *
 * <p>Tracks failed attempts per normalized email (and optional client IP). After {@code
 * max-failures} within the failure window, the principal is locked for {@code lockout-minutes}.
 * Successful login clears the counter. Configuration: {@code app.security.login-rate-limit.*}.
 */
@Service
public class LoginAttemptTracker {

    static final int DEFAULT_MAX_FAILURES = 5;
    static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
    static final Duration DEFAULT_LOCKOUT = Duration.ofMinutes(15);
    static final String MAX_FAILURES_PROPERTY = "app.security.login-rate-limit.max-failures";
    static final String FAILURE_WINDOW_MINUTES_PROPERTY =
            "app.security.login-rate-limit.failure-window-minutes";
    static final String LOCKOUT_MINUTES_PROPERTY = "app.security.login-rate-limit.lockout-minutes";

    private final Clock clock;
    private final int maxFailures;
    private final Duration failureWindow;
    private final Duration lockoutDuration;
    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptTracker(
            @Value("${app.security.login-rate-limit.max-failures:5}") int maxFailures,
            @Value("${app.security.login-rate-limit.failure-window-minutes:15}")
                    long failureWindowMinutes,
            @Value("${app.security.login-rate-limit.lockout-minutes:15}") long lockoutMinutes) {
        this(
                Clock.systemUTC(),
                maxFailures,
                Duration.ofMinutes(failureWindowMinutes),
                Duration.ofMinutes(lockoutMinutes));
    }

    LoginAttemptTracker(
            Clock clock, int maxFailures, Duration failureWindow, Duration lockoutDuration) {
        this.clock = clock;
        this.maxFailures = Math.max(1, maxFailures);
        this.failureWindow = failureWindow == null ? DEFAULT_WINDOW : failureWindow;
        this.lockoutDuration = lockoutDuration == null ? DEFAULT_LOCKOUT : lockoutDuration;
    }

    /** Ensures the email is not currently locked out. */
    public void ensureAllowed(String email) {
        ensureAllowed(email, null);
    }

    /**
     * Ensures the login principal is not locked out.
     *
     * @param email account email (normalized)
     * @param clientIp optional client IP for composite keying
     * @throws LoginLockoutException when lockout is active
     */
    public void ensureAllowed(String email, String clientIp) {
        String key = key(email, clientIp);
        if (key == null) {
            return;
        }

        AttemptState state = attempts.get(key);
        if (state == null) {
            return;
        }

        Instant now = clock.instant();
        if (state.isLocked(now)) {
            throw lockoutException(state, now);
        }
        if (state.isExpired(now, failureWindow)) {
            attempts.remove(key, state);
        }
    }

    public void recordFailure(String email) {
        recordFailure(email, null);
    }

    public void recordFailure(String email, String clientIp) {
        String key = key(email, clientIp);
        if (key == null) {
            return;
        }

        Instant now = clock.instant();
        attempts.compute(
                key,
                (ignored, state) -> {
                    AttemptState current =
                            state == null || state.isExpired(now, failureWindow)
                                    ? AttemptState.firstFailure(now)
                                    : state.nextFailure();
                    return current.failureCount() >= maxFailures
                            ? current.lockUntil(now.plus(lockoutDuration))
                            : current;
                });
    }

    public void recordSuccess(String email) {
        recordSuccess(email, null);
    }

    public void recordSuccess(String email, String clientIp) {
        String key = key(email, clientIp);
        if (key != null) {
            attempts.remove(key);
        }
        // Also clear email-only key if IP-scoped key was used, and vice versa.
        String emailOnly = key(email, null);
        if (emailOnly != null && clientIp != null) {
            attempts.remove(emailOnly);
        }
    }

    /** Returns remaining lockout duration for diagnostics, or empty if not locked. */
    public Optional<Duration> remainingLockout(String email, String clientIp) {
        String key = key(email, clientIp);
        if (key == null) {
            return Optional.empty();
        }
        AttemptState state = attempts.get(key);
        if (state == null) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        if (!state.isLocked(now)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, state.lockedUntil()));
    }

    public int getMaxFailures() {
        return maxFailures;
    }

    public Duration getFailureWindow() {
        return failureWindow;
    }

    public Duration getLockoutDuration() {
        return lockoutDuration;
    }

    private LoginLockoutException lockoutException(AttemptState state, Instant now) {
        Instant lockedUntil = state.lockedUntil();
        Long retryAfter =
                lockedUntil == null
                        ? null
                        : Math.max(1L, Duration.between(now, lockedUntil).getSeconds());
        return new LoginLockoutException(lockedUntil, retryAfter);
    }

    static String key(String email, String clientIp) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(clientIp)) {
            return normalizedEmail;
        }
        return normalizedEmail + "|" + clientIp.trim();
    }

    private record AttemptState(int failureCount, Instant firstFailureAt, Instant lockedUntil) {

        static AttemptState firstFailure(Instant now) {
            return new AttemptState(1, now, null);
        }

        AttemptState nextFailure() {
            return new AttemptState(failureCount + 1, firstFailureAt, lockedUntil);
        }

        AttemptState lockUntil(Instant lockoutEnd) {
            return new AttemptState(failureCount, firstFailureAt, lockoutEnd);
        }

        boolean isLocked(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        boolean isExpired(Instant now, Duration failureWindow) {
            return !now.isBefore(firstFailureAt.plus(failureWindow))
                    && (lockedUntil == null || !now.isBefore(lockedUntil));
        }
    }
}
