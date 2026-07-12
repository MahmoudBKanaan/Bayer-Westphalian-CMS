package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * KB item 543 secret presence validation.
 *
 * <p>Sprint 16 critical restatement: item <b>665</b> — {@link MissingSecretsAreDetectedTests}.
 */
@DisplayName("543 Secret presence validation")
class SecretPresenceValidatorTests {

    @Nested
    class ProductionSecrets {

        @Test
        void passesWhenJwtAndDatabaseSecretsAreStrong() {
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
        void failsWhenJwtSecretMissing() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("JWT_SECRET", "");
            environment.setProperty("app.security.jwt.secret", "");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("JWT_SECRET") && error.contains("required"));
        }

        @Test
        @DisplayName("555 Missing secret fails startup or shows safe configuration error")
        void missingSecretThrowsSafeConfigurationError() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("JWT_SECRET", "");
            environment.setProperty("app.security.jwt.secret", "");
            environment.setProperty("DB_PASSWORD", "database-secret-should-not-leak");
            environment.setProperty("spring.datasource.password", "database-secret-should-not-leak");

            assertThatThrownBy(() -> SecretPresenceValidator.validateProductionSecrets(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Production secret presence validation failed")
                    .hasMessageContaining("JWT_SECRET")
                    .hasMessageContaining("required")
                    .hasMessageNotContaining("database-secret-should-not-leak")
                    .hasMessageNotContaining("production-jwt-secret");
        }

        @Test
        void failsWhenJwtSecretShorterThan32Characters() {
            MockEnvironment environment = validSecretsEnvironment();
            // 16 chars — passes env var min length in item 542 but not secret presence min (32).
            environment.setProperty("JWT_SECRET", "sixteen-char-sec!");
            environment.setProperty("app.security.jwt.secret", "sixteen-char-sec!");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("JWT_SECRET") && error.contains("32"));
        }

        @Test
        void failsWhenJwtSecretIsPlaceholder() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("JWT_SECRET", "changeme-please-use-real-value");
            environment.setProperty("app.security.jwt.secret", "changeme-please-use-real-value");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("JWT_SECRET") && error.contains("placeholder"));
        }

        @Test
        void failsWhenDatabasePasswordMissing() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("DB_PASSWORD", "");
            environment.setProperty("spring.datasource.password", "");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("DB_PASSWORD"));
        }

        @Test
        void failsWhenDatabasePasswordTooShort() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("DB_PASSWORD", "short");
            environment.setProperty("spring.datasource.password", "short");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("DB_PASSWORD") && error.contains("8"));
        }

        @Test
        void requiresSmtpPasswordWhenRealSmtpSendingEnabled() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "true");
            environment.setProperty("app.providers.real-sending-enabled", "true");
            environment.setProperty("EMAIL_PROVIDER_MODE", "smtp");
            environment.setProperty("app.providers.email.mode", "smtp");
            environment.setProperty("SMTP_PASSWORD", "");
            environment.setProperty("app.providers.email.smtp-password", "");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("SMTP_PASSWORD"));
        }

        @Test
        void requiresSmsApiKeyWhenRealSmsProviderEnabled() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "true");
            environment.setProperty("SMS_PROVIDER_MODE", "provider");
            environment.setProperty("SMS_API_KEY", "");
            environment.setProperty("app.providers.sms.api-key", "");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment))
                    .anyMatch(error -> error.contains("SMS_API_KEY"));
        }

        @Test
        void doesNotRequireProviderSecretsWhenRealSendingDisabled() {
            MockEnvironment environment = validSecretsEnvironment();
            environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "false");
            environment.setProperty("EMAIL_PROVIDER_MODE", "smtp");
            environment.setProperty("SMTP_PASSWORD", "");

            assertThat(SecretPresenceValidator.collectProductionSecretErrors(environment)).isEmpty();
        }

        @Test
        void thrownMessageDoesNotContainSecretValue() {
            MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
            environment.setProperty("JWT_SECRET", "actual-super-secret-value-should-not-leak!!");
            environment.setProperty("DB_PASSWORD", "");

            assertThatThrownBy(
                            () -> SecretPresenceValidator.validateProductionSecrets(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Production secret presence validation failed")
                    .hasMessageNotContaining("actual-super-secret-value-should-not-leak");
        }
    }

    @Nested
    class ProfileGating {

        @Test
        void validateIfProductionIsNoOpOutsideProd() {
            MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
            assertThatCode(() -> SecretPresenceValidator.validateIfProduction(environment))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateIfProductionEnforcesOnProd() {
            MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
            assertThatThrownBy(() -> SecretPresenceValidator.validateIfProduction(environment))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Helpers {

        @Test
        void isForbiddenPlaceholderDetectsCommonValues() {
            assertThat(SecretPresenceValidator.isForbiddenPlaceholder("dev-only-change-me"))
                    .isTrue();
            assertThat(SecretPresenceValidator.isForbiddenPlaceholder("password")).isTrue();
            assertThat(SecretPresenceValidator.isForbiddenPlaceholder("changeme-xyz")).isTrue();
            assertThat(
                            SecretPresenceValidator.isForbiddenPlaceholder(
                                    "production-jwt-secret-32chars-min!!"))
                    .isFalse();
        }

        @Test
        void productionRequiredSecretNames() {
            assertThat(SecretPresenceValidator.productionRequiredSecretNames())
                    .containsExactly("JWT_SECRET", "DB_PASSWORD");
        }
    }

    private static MockEnvironment validSecretsEnvironment() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setProperty("JWT_SECRET", "production-jwt-secret-32chars-min!!");
        environment.setProperty("app.security.jwt.secret", "production-jwt-secret-32chars-min!!");
        environment.setProperty("DB_PASSWORD", "db-pass-ok");
        environment.setProperty("spring.datasource.password", "db-pass-ok");
        environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "false");
        return environment;
    }
}
