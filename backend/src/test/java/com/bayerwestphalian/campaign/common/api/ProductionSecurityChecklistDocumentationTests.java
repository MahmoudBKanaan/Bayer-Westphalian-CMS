package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** KB item 563: production security checklist remains available as go-live evidence. */
@DisplayName("563 Production security checklist")
class ProductionSecurityChecklistDocumentationTests {

    private static final Path CHECKLIST =
            Path.of("../docs/deployment/production-security-checklist.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsProductionProfileEnvironmentAndSecretReadiness() throws Exception {
        String checklist = Files.readString(CHECKLIST, StandardCharsets.UTF_8);

        assertThat(checklist)
                .contains("Production Security Checklist")
                .contains("563")
                .contains("SPRING_PROFILES_ACTIVE=prod")
                .contains("application-prod.yml")
                .contains("DB_URL")
                .contains("DB_USERNAME")
                .contains("DB_PASSWORD")
                .contains("JWT_SECRET")
                .contains("CORS_ALLOWED_ORIGINS")
                .contains("SMTP_PASSWORD")
                .contains("SMS_API_KEY")
                .contains("EnvironmentVariableValidator")
                .contains("SecretPresenceValidator")
                .contains("ProductionEnvironmentPostProcessor")
                .contains("must not print configured secret values");
    }

    @Test
    void documentsHttpsCorsErrorsHeadersAndLoggingControls() throws Exception {
        String checklist = Files.readString(CHECKLIST, StandardCharsets.UTF_8);

        assertThat(checklist)
                .contains("HTTPS_REQUIRED")
                .contains("X-Forwarded-Proto: https")
                .contains("Strict-Transport-Security")
                .contains("HttpsEnforcementFilter")
                .contains("Wildcards")
                .contains("localhost")
                .contains("http://")
                .contains("include-stacktrace: never")
                .contains("INTERNAL_ERROR")
                .contains("ProductionErrorSafetyConfiguration")
                .contains("LOGIN_RATE_LIMITED")
                .contains("Retry-After")
                .contains("X-Content-Type-Options")
                .contains("Content-Security-Policy")
                .contains("SafeApiErrorLogger")
                .contains("Authorization")
                .contains("bearer tokens");
    }

    @Test
    void documentsAuditAccountabilityAndGoNoGoEvidence() throws Exception {
        String checklist = Files.readString(CHECKLIST, StandardCharsets.UTF_8);

        assertThat(checklist)
                .contains("AuditLog")
                .contains("GET /api/audit-logs")
                .contains("ADMIN")
                .contains("COMPLIANCE_OFFICER")
                .contains("SYSTEM_AUDITOR")
                .contains("EXPORT_REPORT")
                .contains("System Auditor guide")
                .contains("Go / No-Go Evidence")
                .contains("Audit log access denial")
                .contains("System Auditor access to audit logs and audit export only")
                .contains("../architecture/security-hardening.md")
                .contains("../modules/audit-logging.md")
                .contains("../user-guides/system-auditor-guide.md");
    }

    @Test
    void documentationIndexLinksProductionSecurityChecklist() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index).contains("deployment/production-security-checklist.md");
    }
}
