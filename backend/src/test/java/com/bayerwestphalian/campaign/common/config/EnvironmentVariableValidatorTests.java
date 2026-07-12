package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("542 Environment variable validation")
class EnvironmentVariableValidatorTests {

    @Nested
    class ProductionRequired {

        @Test
        void passesWhenAllProductionVariablesAreValid() {
            MockEnvironment environment = validProductionEnvironment();

            assertThat(EnvironmentVariableValidator.collectProductionErrors(environment)).isEmpty();
            assertThatCode(() -> EnvironmentVariableValidator.validateProduction(environment))
                    .doesNotThrowAnyException();
        }

        @Test
        void failsWhenDatabaseUrlMissing() {
            MockEnvironment environment = validProductionEnvironment();
            environment.setProperty("DB_URL", "");
            environment.setProperty("spring.datasource.url", "");

            List<String> errors = EnvironmentVariableValidator.collectProductionErrors(environment);

            assertThat(errors).anyMatch(error -> error.contains("DB_URL"));
        }

        @Test
        void failsWhenDatabaseUrlIsNotJdbc() {
            MockEnvironment environment = validProductionEnvironment();
            environment.setProperty("DB_URL", "postgresql://localhost/db");

            assertThat(EnvironmentVariableValidator.collectProductionErrors(environment))
                    .anyMatch(error -> error.contains("JDBC"));
        }

        @Test
        void failsWhenJwtSecretIsDevPlaceholder() {
            MockEnvironment environment = validProductionEnvironment();
            environment.setProperty("JWT_SECRET", "dev-only-change-me");
            environment.setProperty("app.security.jwt.secret", "dev-only-change-me");

            assertThat(EnvironmentVariableValidator.collectProductionErrors(environment))
                    .anyMatch(error -> error.contains("development placeholder"));
        }

        @Test
        void failsWhenJwtSecretTooShort() {
            MockEnvironment environment = validProductionEnvironment();
            environment.setProperty("JWT_SECRET", "short");
            environment.setProperty("app.security.jwt.secret", "short");

            assertThat(EnvironmentVariableValidator.collectProductionErrors(environment))
                    .anyMatch(error -> error.contains("at least 16"));
        }

        @Test
        void failsWhenCorsMissingHttpsOrigin() {
            MockEnvironment environment = validProductionEnvironment();
            environment.setProperty("CORS_ALLOWED_ORIGINS", "http://insecure.example.com");
            environment.setProperty("app.cors.allowed-origins", "http://insecure.example.com");

            assertThat(EnvironmentVariableValidator.collectProductionErrors(environment))
                    .anyMatch(error -> error.contains("https://"));
        }

        @Test
        void failsWhenCorsUsesWildcard() {
            MockEnvironment environment = validProductionEnvironment();
            environment.setProperty("CORS_ALLOWED_ORIGINS", "*");
            environment.setProperty("app.cors.allowed-origins", "*");

            assertThat(EnvironmentVariableValidator.collectProductionErrors(environment))
                    .anyMatch(error -> error.contains("wildcard"));
        }

        @Test
        void failsWhenPlaceholderUnresolved() {
            MockEnvironment environment = validProductionEnvironment();
            environment.setProperty("DB_URL", "${DB_URL}");
            environment.setProperty("spring.datasource.url", "${DB_URL}");

            assertThat(EnvironmentVariableValidator.collectProductionErrors(environment))
                    .anyMatch(error -> error.contains("unresolved"));
        }

        @Test
        void validateProductionThrowsAggregatedMessageWithoutSecretValues() {
            MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
            environment.setProperty("JWT_SECRET", "super-secret-value-xyz");

            assertThatThrownBy(() -> EnvironmentVariableValidator.validateProduction(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Production environment variable validation failed")
                    .hasMessageContaining("DB_URL")
                    .hasMessageNotContaining("super-secret-value-xyz");
        }

        @Test
        void errorMessagesNeverEchoPasswordValues() {
            MockEnvironment environment = validProductionEnvironment();
            environment.setProperty("DB_PASSWORD", "");
            environment.setProperty("spring.datasource.password", "");

            List<String> errors = EnvironmentVariableValidator.collectProductionErrors(environment);

            assertThat(errors).anyMatch(error -> error.contains("DB_PASSWORD"));
            assertThat(String.join(" ", errors)).doesNotContain("p@ssw0rd");
        }
    }

    @Nested
    class ProfileGating {

        @Test
        void validateIfProductionIsNoOpOutsideProd() {
            MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
            // Missing all prod vars — still OK for non-prod.
            assertThatCode(() -> EnvironmentVariableValidator.validateIfProduction(environment))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateIfProductionRunsOnProdProfile() {
            MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
            assertThatThrownBy(
                            () -> EnvironmentVariableValidator.validateIfProduction(environment))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void isProductionProfileDetectsProd() {
            assertThat(
                            EnvironmentVariableValidator.isProductionProfile(
                                    new MockEnvironment().withProperty("spring.profiles.active", "prod")))
                    .isTrue();
            assertThat(
                            EnvironmentVariableValidator.isProductionProfile(
                                    new MockEnvironment().withProperty("spring.profiles.active", "dev")))
                    .isFalse();
        }
    }

    @Nested
    class Checklist {

        @Test
        void productionRequiredEnvNamesMatchesOpsChecklist() {
            assertThat(EnvironmentVariableValidator.productionRequiredEnvNames())
                    .containsExactly(
                            "DB_URL",
                            "DB_USERNAME",
                            "DB_PASSWORD",
                            "JWT_SECRET",
                            "CORS_ALLOWED_ORIGINS");
        }
    }

    private static MockEnvironment validProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setProperty("DB_URL", "jdbc:postgresql://db:5432/bwc_campaign");
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://db:5432/bwc_campaign");
        environment.setProperty("DB_USERNAME", "bwc_app");
        environment.setProperty("spring.datasource.username", "bwc_app");
        environment.setProperty("DB_PASSWORD", "p@ssw0rd-not-in-errors");
        environment.setProperty("spring.datasource.password", "p@ssw0rd-not-in-errors");
        environment.setProperty("JWT_SECRET", "production-jwt-secret-32chars-min!!");
        environment.setProperty("app.security.jwt.secret", "production-jwt-secret-32chars-min!!");
        environment.setProperty("CORS_ALLOWED_ORIGINS", "https://campaign.example.com");
        environment.setProperty("app.cors.allowed-origins", "https://campaign.example.com");
        environment.setProperty("LOGIN_RATE_LIMIT_MAX_FAILURES", "5");
        environment.setProperty("app.security.login-rate-limit.max-failures", "5");
        environment.setProperty("LOGIN_RATE_LIMIT_FAILURE_WINDOW_MINUTES", "15");
        environment.setProperty("app.security.login-rate-limit.failure-window-minutes", "15");
        environment.setProperty("LOGIN_RATE_LIMIT_LOCKOUT_MINUTES", "15");
        environment.setProperty("app.security.login-rate-limit.lockout-minutes", "15");
        return environment;
    }
}
