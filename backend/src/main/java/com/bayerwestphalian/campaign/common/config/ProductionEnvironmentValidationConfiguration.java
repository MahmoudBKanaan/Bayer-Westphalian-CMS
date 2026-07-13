package com.bayerwestphalian.campaign.common.config;

import com.bayerwestphalian.campaign.user.AdminBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Secondary production environment and secret validation after the context starts (items 542–543).
 *
 * <p>Complements {@link ProductionEnvironmentPostProcessor} so validation also runs when the post
 * processor registration is unavailable in certain test slices, and logs a short success line
 * without printing secret values.
 */
@Configuration
@Profile("prod")
public class ProductionEnvironmentValidationConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(ProductionEnvironmentValidationConfiguration.class);

    @Bean
    @Order(0)
    ApplicationRunner productionEnvironmentVariableValidationRunner(Environment environment) {
        return (ApplicationArguments args) -> {
            EnvironmentVariableValidator.validateProduction(environment);
            log.info(
                    "Production environment variable validation passed for required keys: {}",
                    EnvironmentVariableValidator.productionRequiredEnvNames());
        };
    }

    @Bean
    @Order(1)
    ApplicationRunner productionSecretPresenceValidationRunner(Environment environment) {
        return (ApplicationArguments args) -> {
            SecretPresenceValidator.validateProductionSecrets(environment);
            log.info(
                    "Production secret presence validation passed for required secrets: {}",
                    SecretPresenceValidator.productionRequiredSecretNames());
        };
    }

    @Bean
    @Order(2)
    ApplicationRunner productionAdminBootstrapRunner(
            Environment environment, AdminBootstrapService adminBootstrapService) {
        return args -> {
            if (!environment.getProperty("ADMIN_BOOTSTRAP_ENABLED", Boolean.class, false)) {
                log.info("Production admin bootstrap is disabled");
                return;
            }

            String email = requiredBootstrapValue(environment, "ADMIN_BOOTSTRAP_EMAIL").trim();
            String password = requiredBootstrapValue(environment, "ADMIN_BOOTSTRAP_PASSWORD");
            String fullName =
                    environment
                            .getProperty(
                                    "ADMIN_BOOTSTRAP_FULL_NAME", "Production Administrator")
                            .trim();
            if (!email.contains("@") || email.toLowerCase().endsWith("@bayer-westphalian.test")) {
                throw new IllegalStateException(
                        "ADMIN_BOOTSTRAP_EMAIL must be a valid non-test email address");
            }
            if (password.length() < 16
                    || !password.matches(".*[A-Z].*")
                    || !password.matches(".*[a-z].*")
                    || !password.matches(".*[0-9].*")) {
                throw new IllegalStateException(
                        "ADMIN_BOOTSTRAP_PASSWORD must have at least 16 characters with upper-case, lower-case, and numeric characters");
            }
            if (!StringUtils.hasText(fullName)) {
                throw new IllegalStateException("ADMIN_BOOTSTRAP_FULL_NAME must not be blank");
            }

            var result = adminBootstrapService.bootstrap(email, password, fullName);
            if (result.created()) {
                log.warn(
                        "Production bootstrap administrator created for {}. Disable bootstrap and remove its password secret now.",
                        result.email());
            } else {
                log.info(
                        "Production bootstrap skipped because account {} already exists; credentials were not changed",
                        result.email());
            }
        };
    }

    private static String requiredBootstrapValue(Environment environment, String name) {
        String value = environment.getProperty(name);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " is required when ADMIN_BOOTSTRAP_ENABLED=true");
        }
        return value;
    }
}
