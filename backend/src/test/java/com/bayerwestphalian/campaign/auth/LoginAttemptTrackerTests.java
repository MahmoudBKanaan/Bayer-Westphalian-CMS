package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("544 Login rate limiting / lockout strategy")
class LoginAttemptTrackerTests {

    private static final String EMAIL = "Advisor@Bayer-Westphalian.Test";
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT = Duration.ofMinutes(15);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-07-04T12:00:00Z"));
    private final LoginAttemptTracker tracker = new LoginAttemptTracker(clock, 3, WINDOW, LOCKOUT);

    @Test
    void exposesConfigurableRateLimitPlaceholderDefaults() {
        assertThat(LoginAttemptTracker.MAX_FAILURES_PROPERTY)
                .isEqualTo("app.security.login-rate-limit.max-failures");
        assertThat(LoginAttemptTracker.FAILURE_WINDOW_MINUTES_PROPERTY)
                .isEqualTo("app.security.login-rate-limit.failure-window-minutes");
        assertThat(LoginAttemptTracker.LOCKOUT_MINUTES_PROPERTY)
                .isEqualTo("app.security.login-rate-limit.lockout-minutes");
        assertThat(LoginAttemptTracker.DEFAULT_MAX_FAILURES).isEqualTo(5);
        assertThat(LoginAttemptTracker.DEFAULT_WINDOW).isEqualTo(Duration.ofMinutes(15));
        assertThat(LoginAttemptTracker.DEFAULT_LOCKOUT).isEqualTo(Duration.ofMinutes(15));
        assertThat(tracker.getMaxFailures()).isEqualTo(3);
        assertThat(tracker.getFailureWindow()).isEqualTo(WINDOW);
        assertThat(tracker.getLockoutDuration()).isEqualTo(LOCKOUT);
    }

    @Test
    void locksNormalizedEmailAfterMaximumFailures() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(" advisor@bayer-westphalian.test ");
        tracker.recordFailure("ADVISOR@BAYER-WESTPHALIAN.TEST");

        try {
            tracker.ensureAllowed("advisor@bayer-westphalian.test");
            throw new AssertionError("Expected LoginLockoutException");
        } catch (LoginLockoutException lockout) {
            assertThat(lockout.getMessage()).isEqualTo(LoginLockoutException.DEFAULT_MESSAGE);
            assertThat(lockout.getCode()).isEqualTo(LoginLockoutException.CODE);
            assertThat(lockout.getStatus().value()).isEqualTo(429);
            assertThat(lockout.getRetryAfterSeconds()).isNotNull().isPositive();
            assertThat(lockout.getLockedUntil()).isAfter(clock.instant());
        }
    }

    @Test
    void successfulLoginClearsFailureHistory() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordSuccess(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);

        assertThatCode(() -> tracker.ensureAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void expiredFailureWindowAllowsNewLoginAttempt() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        clock.advance(WINDOW.plus(LOCKOUT).plusSeconds(1));

        assertThatCode(() -> tracker.ensureAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void lockoutExpiresAfterConfiguredDuration() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);

        assertThatThrownBy(() -> tracker.ensureAllowed(EMAIL))
                .isInstanceOf(LoginLockoutException.class);

        clock.advance(LOCKOUT.plusSeconds(1));
        assertThatCode(() -> tracker.ensureAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void clientIpScopesAttemptsSeparately() {
        tracker.recordFailure(EMAIL, "10.0.0.1");
        tracker.recordFailure(EMAIL, "10.0.0.1");
        tracker.recordFailure(EMAIL, "10.0.0.1");

        assertThatThrownBy(() -> tracker.ensureAllowed(EMAIL, "10.0.0.1"))
                .isInstanceOf(LoginLockoutException.class);

        // Different IP still allowed under its own counter.
        assertThatCode(() -> tracker.ensureAllowed(EMAIL, "10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void remainingLockoutReflectsActiveLock() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);

        assertThat(tracker.remainingLockout(EMAIL, null)).isPresent();
        assertThat(tracker.remainingLockout(EMAIL, null).orElseThrow().getSeconds())
                .isBetween(1L, LOCKOUT.getSeconds());
    }

    @Test
    void keyNormalizesEmailAndOptionalIp() {
        assertThat(LoginAttemptTracker.key("  A@B.C  ", null)).isEqualTo("a@b.c");
        assertThat(LoginAttemptTracker.key("A@B.C", " 1.2.3.4 "))
                .isEqualTo("a@b.c|1.2.3.4");
        assertThat(LoginAttemptTracker.key("  ", "1.2.3.4")).isNull();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
