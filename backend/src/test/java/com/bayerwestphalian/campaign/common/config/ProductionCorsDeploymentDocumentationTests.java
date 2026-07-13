package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("723 Configure production CORS")
class ProductionCorsDeploymentDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path GUIDE = Path.of("../docs/deployment/production-cors.md");

    @Test
    void productionDeploymentRequiresExplicitCorsOrigins() throws Exception {
        assertThat(Files.readString(PROFILE, StandardCharsets.UTF_8))
                .contains("allowed-origins: ${CORS_ALLOWED_ORIGINS}")
                .doesNotContain("CORS_ALLOWED_ORIGINS:http://localhost");
        assertThat(Files.readString(COMPOSE, StandardCharsets.UTF_8))
                .contains("CORS_ALLOWED_ORIGINS is required");
        assertThat(Files.readString(TEMPLATE, StandardCharsets.UTF_8))
                .contains("Exact comma-separated HTTPS origins")
                .contains("CORS_ALLOWED_ORIGINS=");
    }

    @Test
    void guideDefinesCanonicalAllowListAndRuntimePolicy() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 723")
                .contains("ProductionCorsOrigins")
                .contains("no path, trailing slash, query string, fragment, or user info")
                .contains("Credentials are enabled only alongside the explicit origin list")
                .contains("ProductionCorsDeploymentDocumentationTests");
    }
}
