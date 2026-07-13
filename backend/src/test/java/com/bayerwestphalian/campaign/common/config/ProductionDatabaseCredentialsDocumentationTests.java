package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("725 Configure database credentials through environment variables")
class ProductionDatabaseCredentialsDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path GENERATOR =
            Path.of("../scripts/New-ProductionDatabasePassword.ps1");
    private static final Path GUIDE = Path.of("../docs/deployment/database-credentials.md");

    @Test
    void profileAndComposeRequireSeparateDatabaseEnvironmentValues() throws Exception {
        String profile = Files.readString(PROFILE, StandardCharsets.UTF_8);
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);

        assertThat(profile)
                .contains("url: ${DB_URL}")
                .contains("username: ${DB_USERNAME}")
                .contains("password: ${DB_PASSWORD}");
        assertThat(compose)
                .contains("DB_URL: ${DB_URL:?DB_URL is required}")
                .contains("DB_USERNAME: ${DB_USERNAME:?DB_USERNAME is required}")
                .contains("DB_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}")
                .contains("POSTGRES_USER: ${DB_USERNAME:?DB_USERNAME is required}")
                .contains("POSTGRES_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}");
    }

    @Test
    void templatesAndGeneratorDoNotEmbedCredentials() throws Exception {
        String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        String generator = Files.readString(GENERATOR, StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Keep credentials separate from DB_URL")
                .contains("DB_USERNAME=bwc_app")
                .contains("DB_PASSWORD=")
                .doesNotContain("DB_PASSWORD=bwc_app");
        assertThat(generator)
                .contains("item 725")
                .contains("RandomNumberGenerator")
                .contains(
                        "Refusing to write production database credentials inside the Git repository")
                .contains("Refusing to overwrite an existing credential file");
    }

    @Test
    void guideDocumentsProvisioningAndCoordinatedRotation() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 725")
                .contains("dedicated least-privilege role")
                .contains("Keep credentials out of `DB_URL`")
                .contains("Rotate the database role and deployment secret as one operation")
                .contains("ProductionDatabaseCredentialsDocumentationTests");
    }
}
