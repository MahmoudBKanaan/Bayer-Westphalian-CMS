package com.bayerwestphalian.campaign.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

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
}
