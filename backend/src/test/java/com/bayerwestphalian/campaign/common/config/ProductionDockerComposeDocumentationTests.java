package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("717 Create production Docker Compose file")
class ProductionDockerComposeDocumentationTests {

    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path GUIDE = Path.of("../docs/deployment/production-compose.md");

    @Test
    void productionComposeDefinesCompleteHealthyStack() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("item 717")
                .contains("postgres:")
                .contains("backend:")
                .contains("frontend:")
                .contains("SPRING_PROFILES_ACTIVE: prod")
                .contains("condition: service_healthy")
                .contains("/actuator/health")
                .contains("/healthz")
                .contains("restart: unless-stopped");
    }

    @Test
    void productionComposeRequiresSecretsAndPersistsState() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("DB_PASSWORD is required")
                .contains("JWT_SECRET is required")
                .contains("CORS_ALLOWED_ORIGINS is required")
                .contains("bwc_postgres_prod_data:/var/lib/postgresql/data")
                .contains("bwc_consent_evidence:/app/data/consent-evidence")
                .contains("internal: true")
                .contains("no-new-privileges:true")
                .contains("read_only: true")
                .doesNotContain("JWT_SECRET=change-me")
                .doesNotContain("DB_PASSWORD=bwc_app");
    }

    @Test
    void productionComposeGuideExplainsSafeOperation() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("Sprint 18 item 717")
                .contains("docker-compose.prod.yml")
                .contains("uncommitted env file")
                .contains("only service publishing an HTTP host port")
                .contains("ProductionDockerComposeDocumentationTests");
    }
}
