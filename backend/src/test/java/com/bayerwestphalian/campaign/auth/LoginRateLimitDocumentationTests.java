package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("544 Login rate limit documentation")
class LoginRateLimitDocumentationTests {

    @Test
    void applicationYmlDefinesLoginRateLimitSettings() throws Exception {
        String yaml =
                Files.readString(
                        Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("login-rate-limit:")
                .contains("max-failures:")
                .contains("failure-window-minutes:")
                .contains("lockout-minutes:")
                .contains("LOGIN_RATE_LIMIT_MAX_FAILURES")
                .contains("LOGIN_RATE_LIMIT_LOCKOUT_MINUTES");
    }

    @Test
    void securityHardeningDocDescribesLoginLockoutStrategy() throws Exception {
        Path doc = Path.of("..", "docs", "architecture", "security-hardening.md");
        assertThat(doc).exists();
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("544")
                .contains("LoginAttemptTracker")
                .contains("LOGIN_RATE_LIMITED")
                .contains("Retry-After");
    }

    @Test
    void authenticationDesignMentionsLockout() throws Exception {
        Path doc = Path.of("..", "docs", "architecture", "authentication-design.md");
        assertThat(doc).exists();
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content).containsIgnoringCase("lockout").contains("544");
    }
}
