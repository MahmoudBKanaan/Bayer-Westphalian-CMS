package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.common.config.EnvironmentVariableValidator;
import com.bayerwestphalian.campaign.common.config.SecretPresenceValidator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** KB item 566: sensitive actions are auditable and production failures are safe. */
@DisplayName("566 Sensitive actions auditable and production safety")
class SensitiveAuditAndProductionSafetyAcceptanceTests {

    @Test
    void sensitiveActionAuditEvidenceCoversRequiredBusinessChanges() throws Exception {
        String auditDoc =
                Files.readString(
                        Path.of("../docs/modules/audit-logging.md"), StandardCharsets.UTF_8);

        assertThat(auditDoc)
                .contains("566")
                .contains("Sensitive Actions Logged")
                .contains("Users")
                .contains("CREATE")
                .contains("role assign")
                .contains("DISABLE_USER")
                .contains("Consent")
                .contains("WITHDRAW_CONSENT")
                .contains("OPT_OUT")
                .contains("Products")
                .contains("Campaigns")
                .contains("SUBMIT")
                .contains("APPROVE")
                .contains("REJECT")
                .contains("LAUNCH")
                .contains("Reports")
                .contains("EXPORT_REPORT")
                .contains("read-only");
    }

    @Test
    void productionErrorSanitizationDoesNotExposeSecretsStackTracesOrInternalDetails() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("timestamp", "2026-07-12T12:00:00Z");
        raw.put("status", 500);
        raw.put("error", "Internal Server Error");
        raw.put("requestId", "req-566");
        raw.put("message", "database password was super-secret-db-value");
        raw.put("exception", "java.lang.IllegalStateException");
        raw.put("trace", "java.lang.IllegalStateException\n\tat com.example.SecretService.run");
        raw.put("stackTrace", "stack frames should never reach clients");
        raw.put("errors", "binding dump");
        raw.put("path", "/api/internal");

        Map<String, Object> sanitized =
                ProductionErrorSafetyConfiguration.sanitizeForProduction(raw);

        assertThat(sanitized)
                .containsEntry("status", 500)
                .containsEntry("error", "Internal Server Error")
                .containsEntry("requestId", "req-566")
                .doesNotContainKeys(
                        "message", "exception", "trace", "stackTrace", "errors", "path");
        assertThat(sanitized.toString())
                .doesNotContain("super-secret-db-value")
                .doesNotContain("IllegalStateException")
                .doesNotContain("com.example.SecretService")
                .doesNotContain("stack frames");
    }

    @Test
    void productionConfigurationFailuresNameKeysWithoutLeakingSecretValues() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setProperty("JWT_SECRET", "actual-jwt-secret-value-that-must-not-leak!!");
        environment.setProperty(
                "app.security.jwt.secret", "actual-jwt-secret-value-that-must-not-leak!!");
        environment.setProperty("DB_PASSWORD", "actual-db-secret-that-must-not-leak");
        environment.setProperty(
                "spring.datasource.password", "actual-db-secret-that-must-not-leak");
        environment.setProperty("CORS_ALLOWED_ORIGINS", "*");
        environment.setProperty("app.cors.allowed-origins", "*");

        assertThatThrownBy(() -> EnvironmentVariableValidator.validateProduction(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production environment variable validation failed")
                .hasMessageContaining("DB_URL")
                .hasMessageContaining("DB_USERNAME")
                .hasMessageContaining("CORS_ALLOWED_ORIGINS")
                .hasMessageContaining("wildcards")
                .hasMessageNotContaining("actual-jwt-secret-value-that-must-not-leak")
                .hasMessageNotContaining("actual-db-secret-that-must-not-leak");
    }

    @Test
    void missingProductionSecretsFailSafelyWithoutEchoingConfiguredSecrets() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setProperty("JWT_SECRET", "");
        environment.setProperty("app.security.jwt.secret", "");
        environment.setProperty("DB_PASSWORD", "configured-db-secret-must-not-leak");
        environment.setProperty("spring.datasource.password", "configured-db-secret-must-not-leak");
        environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "true");
        environment.setProperty("EMAIL_PROVIDER_MODE", "smtp");
        environment.setProperty("SMS_PROVIDER_MODE", "provider");
        environment.setProperty("SMS_API_KEY", "configured-sms-secret-must-not-leak");

        assertThatThrownBy(() -> SecretPresenceValidator.validateProductionSecrets(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production secret presence validation failed")
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("SMTP_PASSWORD")
                .hasMessageNotContaining("configured-db-secret-must-not-leak")
                .hasMessageNotContaining("configured-sms-secret-must-not-leak");
    }
}
