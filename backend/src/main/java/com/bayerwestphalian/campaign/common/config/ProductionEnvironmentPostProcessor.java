package com.bayerwestphalian.campaign.common.config;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails production startup when mandatory environment variables or secrets are missing or invalid
 * (items 542–543).
 *
 * <p>Runs during environment preparation (before the application context is fully created) so
 * misconfigured deployments fail fast with a clear message.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class ProductionEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (!isProd(environment)) {
            return;
        }
        EnvironmentVariableValidator.validateProduction(environment);
        SecretPresenceValidator.validateProductionSecrets(environment);
    }

    private static boolean isProd(ConfigurableEnvironment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                        .anyMatch(profile -> "prod".equalsIgnoreCase(profile))
                || Arrays.stream(environment.getDefaultProfiles())
                        .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }
}
