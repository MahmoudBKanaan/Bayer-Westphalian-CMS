package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.common.exception.ApplicationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;

/**
 * Raised when login is blocked by the rate-limit / lockout strategy (KB item 544).
 *
 * <p>Uses HTTP 429 so clients can distinguish lockout from invalid credentials (401).
 */
public class LoginLockoutException extends ApplicationException {

    public static final String CODE = "LOGIN_RATE_LIMITED";
    public static final String DEFAULT_MESSAGE =
            "Too many failed login attempts. Try again later";

    private final Instant lockedUntil;
    private final Long retryAfterSeconds;

    public LoginLockoutException(Instant lockedUntil, Long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, CODE, DEFAULT_MESSAGE);
        this.lockedUntil = lockedUntil;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    /**
     * Suggested seconds until the client may retry (for {@code Retry-After} header), or {@code null}
     * when unknown.
     */
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
