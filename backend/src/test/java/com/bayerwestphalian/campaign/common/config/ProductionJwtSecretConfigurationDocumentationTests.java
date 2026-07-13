package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("724 Configure secure JWT/session secret")
class ProductionJwtSecretConfigurationDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path GENERATOR = Path.of("../scripts/New-ProductionJwtSecret.ps1");
    private static final Path GUIDE = Path.of("../docs/deployment/jwt-secret.md");

    @Test
    void productionRequiresRuntimeJwtSecretWithoutCommittedFallback() throws Exception {
        assertThat(Files.readString(PROFILE, StandardCharsets.UTF_8))
                .contains("secret: ${JWT_SECRET}")
                .doesNotContain("JWT_SECRET:dev-only-change-me");
        assertThat(Files.readString(COMPOSE, StandardCharsets.UTF_8))
                .contains("JWT_SECRET: ${JWT_SECRET:?JWT_SECRET is required}")
                .doesNotContain("ARG JWT_SECRET");
        assertThat(Files.readString(TEMPLATE, StandardCharsets.UTF_8))
                .contains("JWT_SECRET=")
                .contains("never reuse DB_PASSWORD")
                .doesNotContain("JWT_SECRET=change");
    }

    @Test
    void generatorUsesCryptographicRandomBytesAndSafeMinimum() throws Exception {
        String generator = Files.readString(GENERATOR, StandardCharsets.UTF_8);

        assertThat(generator)
                .contains("item 724")
                .contains("RandomNumberGenerator")
                .contains("[ValidateRange(32, 256)]")
                .contains("[int]$ByteLength = 48")
                .contains("Refusing to write a production secret inside the Git repository")
                .contains("Refusing to overwrite an existing secret file")
                .contains("Keep this file outside Git");
    }

    @Test
    void guideDocumentsStorageRotationAndSessionImpact() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 724")
                .contains("SEC-004")
                .contains("must not equal `DB_PASSWORD`")
                .contains("invalidates every access and refresh token")
                .contains("There is no dual-key grace period")
                .contains("ProductionJwtSecretConfigurationDocumentationTests");
    }
}
