package com.bayerwestphalian.campaign.consent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Readiness signal for the production consent-evidence storage mount. */
@Component("consentEvidenceStorageHealthIndicator")
@Profile("prod")
@ConditionalOnProperty(
        prefix = "app.providers.file-storage",
        name = "mode",
        havingValue = "filesystem")
public class ConsentEvidenceStorageHealthIndicator implements HealthIndicator {

    private final Path storageRoot;

    public ConsentEvidenceStorageHealthIndicator(
            @Value("${app.providers.file-storage.local-path}") String configuredRoot) {
        this.storageRoot = Path.of(configuredRoot).toAbsolutePath().normalize();
    }

    @Override
    public Health health() {
        try {
            Files.createDirectories(storageRoot);
            if (!Files.isDirectory(storageRoot) || !Files.isWritable(storageRoot)) {
                return Health.down().withDetail("reason", "storage_not_writable").build();
            }
            return Health.up().build();
        } catch (IOException | RuntimeException exception) {
            return Health.down().withDetail("reason", "storage_unavailable").build();
        }
    }
}
