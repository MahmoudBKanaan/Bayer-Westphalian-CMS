package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

/**
 * Sprint 16 critical test item <b>665</b>: Missing secrets are detected.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code NFR-001} — production must not run with missing or weak secrets
 *   <li>Item 543 — secret presence validation on startup
 *   <li>Item 555 — missing secret fails startup with a safe configuration error (names keys, never
 *       values)
 * </ul>
 *
 * <p>Enforcement: {@link SecretPresenceValidator} via {@link ProductionEnvironmentPostProcessor}
 * and {@code productionSecretPresenceValidationRunner}.
 *
 * <p>Companion: {@link SecretPresenceValidatorTests}, {@link ProductionEnvironmentPostProcessorTests}.
 */
@DisplayName("665 Missing secrets are detected")
class MissingSecretsAreDetectedTests {

    private static final Path SECURITY_HARDENING_DOC =
            Path.of("../docs/architecture/security-hardening.md");
    private static final Path PRODUCTION_CHECKLIST_DOC =
            Path.of("../docs/deployment/production-security-checklist.md");

    private static final String STRONG_JWT = "production-jwt-secret-32chars-min!!";
    private static final String STRONG_DB_PASSWORD = "db-pass-ok-secret";

    @Nested
    @DisplayName("SecretPresenceValidator detects missing and unsafe secrets")
    class Validator {

        @Test
        void passesWhenRequiredSecretsArePresentAndStrong() {
            MockEnvironment environment = validSecretsEnvironment();

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .isEmpty();
            assertThatCode(
                            () ->
                                    SecretPresenceValidator.validateProductionSecrets(
                                            environment))
                    .doesNotThrowAnyException();
        }

        @Test
        void detectsMissingJwtSecret() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("JWT_SECRET", "");
            environment.setProperty("app.security.jwt.secret", "");

            List<String> errors =
                    SecretPresenceValidator.collectProductionSecretErrors(environment);
            assertThat(errors)
                    .anyMatch(error -> error.contains("JWT_SECRET") && error.contains("required"));
        }

        @Test
        void detectsMissingDatabasePassword() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("DB_PASSWORD", "");
            environment.setProperty("spring.datasource.password", "");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("DB_PASSWORD"));
        }

        @Test
        void detectsPlaceholderAndShortJwtSecrets() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("JWT_SECRET", "dev-only-change-me");
            environment.setProperty("app.security.jwt.secret", "dev-only-change-me");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(
                            error ->
                                    error.contains("JWT_SECRET")
                                            && error.contains("placeholder"));

            environment.setProperty("JWT_SECRET", "sixteen-char-sec!");
            environment.setProperty("app.security.jwt.secret", "sixteen-char-sec!");
            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("JWT_SECRET") && error.contains("32"));
        }

        @Test
        void missingSecretThrowsSafeConfigurationErrorWithoutLeakingValues() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("JWT_SECRET", "");
            environment.setProperty("app.security.jwt.secret", "");
            environment.setProperty("DB_PASSWORD", "database-secret-should-not-leak-665");
            environment.setProperty(
                    "spring.datasource.password", "database-secret-should-not-leak-665");

            assertThatThrownBy(
                            () -> SecretPresenceValidator.validateProductionSecrets(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Production secret presence validation failed")
                    .hasMessageContaining("JWT_SECRET")
                    .hasMessageContaining("required")
                    .hasMessageNotContaining("database-secret-should-not-leak-665")
                    .hasMessageNotContaining(STRONG_JWT);
        }

        @Test
        void requiresProviderSecretsOnlyWhenRealSendingEnabled() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "true");
            environment.setProperty("EMAIL_PROVIDER_MODE", "smtp");
            environment.setProperty("SMTP_PASSWORD", "");
            environment.setProperty("app.providers.email.smtp-password", "");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("SMTP_PASSWORD"));

            environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "false");
            environment.setProperty("EMAIL_PROVIDER_MODE", "smtp");
            environment.setProperty("SMTP_PASSWORD", "");
            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .isEmpty();
        }

        @Test
        void productionRequiredSecretNamesAreJwtAndDbPassword() {
            assertThat(SecretPresenceValidator.productionRequiredSecretNames())
                    .containsExactly("JWT_SECRET", "DB_PASSWORD");
            assertThat(SecretPresenceValidator.MIN_JWT_SECRET_LENGTH).isEqualTo(32);
            assertThat(SecretPresenceValidator.MIN_DB_PASSWORD_LENGTH).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Profile gating and startup post-processor")
    class Startup {

        @Test
        void validateIfProductionIsNoOpOutsideProdAndEnforcesOnProd() {
            MockEnvironment dev =
                    new MockEnvironment().withProperty("spring.profiles.active", "dev");
            assertThatCode(() -> SecretPresenceValidator.validateIfProduction(dev))
                    .doesNotThrowAnyException();

            MockEnvironment prod =
                    new MockEnvironment().withProperty("spring.profiles.active", "prod");
            assertThatThrownBy(() -> SecretPresenceValidator.validateIfProduction(prod))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("secret presence");
        }

        @Test
        void productionEnvironmentPostProcessorFailsWhenSecretPresenceRulesFailWithoutLeakingValues() {
            ProductionEnvironmentPostProcessor processor =
                    new ProductionEnvironmentPostProcessor();
            MockEnvironment environment =
                    new MockEnvironment().withProperty("spring.profiles.active", "prod");
            environment.setProperty("DB_URL", "jdbc:postgresql://db:5432/bwc");
            environment.setProperty("DB_USERNAME", "bwc");
            environment.setProperty("DB_PASSWORD", "secret-password-value-must-not-appear");
            // Passes item 542 length (≥16) but fails item 543 secret presence (≥32).
            environment.setProperty("JWT_SECRET", "sixteen-char-sec!");
            environment.setProperty("CORS_ALLOWED_ORIGINS", "https://app.example.com");

            assertThatThrownBy(
                            () ->
                                    processor.postProcessEnvironment(
                                            environment, new SpringApplication()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("secret presence")
                    .hasMessageContaining("JWT_SECRET")
                    .hasMessageNotContaining("secret-password-value-must-not-appear")
                    .hasMessageNotContaining("sixteen-char-sec!");
        }

        @Test
        void productionEnvironmentPostProcessorAcceptsValidSecrets() {
            ProductionEnvironmentPostProcessor processor =
                    new ProductionEnvironmentPostProcessor();
            MockEnvironment environment =
                    new MockEnvironment().withProperty("spring.profiles.active", "prod");
            environment.setProperty("DB_URL", "jdbc:postgresql://db:5432/bwc");
            environment.setProperty("DB_USERNAME", "bwc");
            environment.setProperty("DB_PASSWORD", STRONG_DB_PASSWORD);
            environment.setProperty("JWT_SECRET", STRONG_JWT);
            environment.setProperty("CORS_ALLOWED_ORIGINS", "https://app.example.com");

            assertThatCode(
                            () ->
                                    processor.postProcessEnvironment(
                                            environment, new SpringApplication()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Documentation")
    class Docs {

        @Test
        void securityHardeningAndChecklistDocumentMissingSecretDetection() throws Exception {
            assertThat(SECURITY_HARDENING_DOC).exists();
            String hardening = Files.readString(SECURITY_HARDENING_DOC, StandardCharsets.UTF_8);
            assertThat(hardening)
                    .contains("665")
                    .contains("MissingSecretsAreDetectedTests")
                    .contains("SecretPresenceValidator")
                    .contains("JWT_SECRET")
                    .contains("DB_PASSWORD");

            assertThat(PRODUCTION_CHECKLIST_DOC).exists();
            String checklist =
                    Files.readString(PRODUCTION_CHECKLIST_DOC, StandardCharsets.UTF_8);
            assertThat(checklist)
                    .contains("SecretPresenceValidator")
                    .contains("JWT_SECRET")
                    .containsIgnoringCase("secret");
        }
    }

    private static MockEnvironment validSecretsEnvironment() {
        MockEnvironment environment =
                new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setProperty("JWT_SECRET", STRONG_JWT);
        environment.setProperty("app.security.jwt.secret", STRONG_JWT);
        environment.setProperty("DB_PASSWORD", STRONG_DB_PASSWORD);
        environment.setProperty("spring.datasource.password", STRONG_DB_PASSWORD);
        environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "false");
        return environment;
    }
}
