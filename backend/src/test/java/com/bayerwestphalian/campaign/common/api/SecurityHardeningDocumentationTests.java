package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** KB item 560: security hardening documentation remains available as production evidence. */
@DisplayName("560 Security hardening documentation")
class SecurityHardeningDocumentationTests {

    private static final Path SECURITY_HARDENING_DOC =
            Path.of("../docs/architecture/security-hardening.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsSecureErrorAndProductionStackTracePolicies() throws Exception {
        String documentation = Files.readString(SECURITY_HARDENING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Security Hardening Documentation")
                .contains("560")
                .contains("Secure Error Responses")
                .contains("ErrorResponse")
                .contains("GlobalExceptionHandler")
                .contains("SecureErrorResponses")
                .contains("Hide Stack Traces in Production")
                .contains("include-stacktrace: never")
                .contains("ProductionErrorSafetyConfiguration")
                .contains("INTERNAL_ERROR")
                .contains("Unexpected server error");
    }

    @Test
    void documentsProductionCorsHttpsAndHeaders() throws Exception {
        String documentation = Files.readString(SECURITY_HARDENING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Production CORS Configuration")
                .contains("CORS_ALLOWED_ORIGINS")
                .contains("Wildcards")
                .contains("localhost")
                .contains("https://")
                .contains("HTTPS Production Requirement")
                .contains("HttpsEnforcementFilter")
                .contains("X-Forwarded-Proto")
                .contains("Backend Security Headers")
                .contains("X-Content-Type-Options")
                .contains("X-Frame-Options")
                .contains("Content-Security-Policy")
                .contains("Strict-Transport-Security");
    }

    @Test
    void documentsProductionConfigValidationSecretsRateLimitsAndSafeLogging() throws Exception {
        String documentation = Files.readString(SECURITY_HARDENING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Environment Variable Validation")
                .contains("ProductionEnvironmentPostProcessor")
                .contains("DB_URL")
                .contains("JWT_SECRET")
                .contains("Secret Presence Validation")
                .contains("SecretPresenceValidator")
                .contains("Missing secret fails startup")
                .contains("Login Rate Limiting")
                .contains("LoginAttemptTracker")
                .contains("Retry-After")
                .contains("API Error Logging Without Leaking Secrets")
                .contains("SafeApiErrorLogger")
                .contains("password")
                .contains("token")
                .contains("secret");
    }

    @Test
    void documentationIndexLinksSecurityHardeningDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index).contains("architecture/security-hardening.md");
    }
}
