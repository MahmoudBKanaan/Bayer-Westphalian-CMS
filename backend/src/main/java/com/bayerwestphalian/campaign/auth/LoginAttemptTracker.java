package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAttemptTracker {

    static final int DEFAULT_MAX_FAILURES = 5;
    static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
    static final Duration DEFAULT_LOCKOUT = Duration.ofMinutes(15);
    static final String MAX_FAILURES_PROPERTY = "app.security.login-rate-limit.max-failures";
    static final String FAILURE_WINDOW_MINUTES_PROPERTY =
            "app.security.login-rate-limit.failure-window-minutes";
    static final String LOCKOUT_MINUTES_PROPERTY = "app.security.login-rate-limit.lockout-minutes";

    private static final String LOCKOUT_MESSAGE = "Too many failed login attempts. Try again later";

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
        this.maxFailures = maxFailures;
        this.failureWindow = failureWindow;
        this.lockoutDuration = lockoutDuration;
    }

    public void ensureAllowed(String email) {
        String key = key(email);
        if (key == null) {
            return;
        }

        AttemptState state = attempts.get(key);
        if (state == null) {
            return;
        }

        Instant now = clock.instant();
        if (state.isLocked(now)) {
            throw new UnauthorizedException(LOCKOUT_MESSAGE);
        }
        if (state.isExpired(now, failureWindow)) {
            attempts.remove(key, state);
        }
    }

    public void recordFailure(String email) {
        String key = key(email);
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
        String key = key(email);
        if (key != null) {
            attempts.remove(key);
        }
    }

    private static String key(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
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
