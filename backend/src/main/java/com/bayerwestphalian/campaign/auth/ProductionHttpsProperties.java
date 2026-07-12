package com.bayerwestphalian.campaign.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Production HTTPS policy (KB item 541).
 *
 * <p>Bound from {@code app.security.https.*}. Defaults favor requiring HTTPS in production while
 * allowing reverse-proxy termination (see {@code X-Forwarded-Proto} and {@code
 * server.forward-headers-strategy}).
 */
@ConfigurationProperties(prefix = "app.security.https")
public class ProductionHttpsProperties {

    /**
     * When true (production default), non-health requests must arrive as HTTPS or with a trusted
     * {@code X-Forwarded-Proto: https} header from the reverse proxy.
     */
    private boolean required = true;

    /**
     * When true, emit {@code Strict-Transport-Security} on secure responses (item 541; complements
     * later security-header work).
     */
    private boolean hstsEnabled = true;

    /** HSTS max-age in seconds (default 1 year). */
    private long hstsMaxAgeSeconds = 31_536_000L;

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isHstsEnabled() {
        return hstsEnabled;
    }

    public void setHstsEnabled(boolean hstsEnabled) {
        this.hstsEnabled = hstsEnabled;
    }

    public long getHstsMaxAgeSeconds() {
        return hstsMaxAgeSeconds;
    }

    public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) {
        this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
    }
}
