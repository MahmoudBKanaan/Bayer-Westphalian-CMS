package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("542 ProductionEnvironmentPostProcessor")
class ProductionEnvironmentPostProcessorTests {

    private final ProductionEnvironmentPostProcessor processor =
            new ProductionEnvironmentPostProcessor();

    @Test
    void skipsValidationWhenNotProduction() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        assertThatCode(
                        () ->
                                processor.postProcessEnvironment(
                                        environment, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void failsWhenProductionMissingRequiredVariables() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        assertThatThrownBy(
                        () ->
                                processor.postProcessEnvironment(
                                        environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production environment variable validation failed");
    }

    @Test
    void acceptsValidProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setProperty("DB_URL", "jdbc:postgresql://db:5432/bwc");
        environment.setProperty("DB_USERNAME", "bwc");
        environment.setProperty("DB_PASSWORD", "secret-password");
        environment.setProperty("JWT_SECRET", "production-jwt-secret-32chars-min!!");
        environment.setProperty("CORS_ALLOWED_ORIGINS", "https://app.example.com");

        assertThatCode(
                        () ->
                                processor.postProcessEnvironment(
                                        environment, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void failsWhenProductionJwtSecretTooShortForSecretPresenceRules() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setProperty("DB_URL", "jdbc:postgresql://db:5432/bwc");
        environment.setProperty("DB_USERNAME", "bwc");
        environment.setProperty("DB_PASSWORD", "secret-password");
        // 16+ chars passes basic env checks in isolation for length in 542, but 543 requires 32.
        environment.setProperty("JWT_SECRET", "sixteen-char-sec!");
        environment.setProperty("CORS_ALLOWED_ORIGINS", "https://app.example.com");

        assertThatThrownBy(
                        () ->
                                processor.postProcessEnvironment(
                                        environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secret presence");
    }

    @Test
    @DisplayName("555 Missing secret fails startup with safe configuration error")
    void failsStartupWhenConditionalProductionSecretIsMissingWithoutLeakingValues() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setProperty("DB_URL", "jdbc:postgresql://db:5432/bwc");
        environment.setProperty("DB_USERNAME", "bwc");
        environment.setProperty("DB_PASSWORD", "secret-password");
        environment.setProperty("JWT_SECRET", "production-jwt-secret-32chars-min!!");
        environment.setProperty("CORS_ALLOWED_ORIGINS", "https://app.example.com");
        environment.setProperty("PROVIDER_REAL_SENDING_ENABLED", "true");
        environment.setProperty("EMAIL_PROVIDER_MODE", "smtp");
        environment.setProperty("SMTP_PASSWORD", "");

        assertThatThrownBy(
                        () ->
                                processor.postProcessEnvironment(
                                        environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production secret presence validation failed")
                .hasMessageContaining("SMTP_PASSWORD")
                .hasMessageContaining("required")
                .hasMessageNotContaining("production-jwt-secret-32chars-min")
                .hasMessageNotContaining("secret-password");
    }
}
