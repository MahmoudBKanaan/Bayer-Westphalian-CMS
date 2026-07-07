package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.common.exception.UnauthorizedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

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
    }

    @Test
    void locksNormalizedEmailAfterMaximumFailures() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(" advisor@bayer-westphalian.test ");
        tracker.recordFailure("ADVISOR@BAYER-WESTPHALIAN.TEST");

        assertThatThrownBy(() -> tracker.ensureAllowed("advisor@bayer-westphalian.test"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Too many failed login attempts. Try again later");
    }

    @Test
    void successfulLoginClearsFailureHistory() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordSuccess(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);

        tracker.ensureAllowed(EMAIL);
    }

    @Test
    void expiredFailureWindowAllowsNewLoginAttempt() {
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        clock.advance(WINDOW.plus(LOCKOUT).plusSeconds(1));

        tracker.ensureAllowed(EMAIL);
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
