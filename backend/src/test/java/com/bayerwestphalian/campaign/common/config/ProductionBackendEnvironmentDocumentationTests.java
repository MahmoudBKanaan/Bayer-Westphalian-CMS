package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("718 Configure production backend environment")
class ProductionBackendEnvironmentDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path GUIDE = Path.of("../docs/deployment/production-backend-environment.md");

    @Test
    void productionProfileUsesValidatedSchemaAndRestrictedOperationalEndpoints() throws Exception {
        String profile = Files.readString(PROFILE, StandardCharsets.UTF_8);

        assertThat(profile)
                .contains("ddl-auto: validate")
                .contains("clean-disabled: true")
                .contains("shutdown: graceful")
                .contains("maximum-pool-size: ${DB_POOL_MAX_SIZE:20}")
                .contains("include: health,info")
                .contains("show-details: never")
                .contains("enabled: ${OPENAPI_ENABLED:false}")
                .contains("enabled: ${SWAGGER_UI_ENABLED:false}")
                .contains("include-stacktrace: never");
    }

    @Test
    void productionTemplateListsRequiredValuesWithoutEmbeddingSecrets() throws Exception {
        String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

        assertThat(template)
                .contains("item 718")
                .contains("SPRING_PROFILES_ACTIVE=prod")
                .contains("DB_PASSWORD=")
                .contains("JWT_SECRET=")
                .contains("CORS_ALLOWED_ORIGINS=")
                .contains("OPENAPI_ENABLED=false")
                .contains("PROVIDER_REAL_SENDING_ENABLED=false")
                .doesNotContain("JWT_SECRET=change")
                .doesNotContain("DB_PASSWORD=bwc_app");
    }

    @Test
    void composeAndGuideUseTheProductionBackendContract() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("SPRING_PROFILES_ACTIVE: prod")
                .contains("DB_POOL_MAX_SIZE")
                .contains("OPENAPI_ENABLED")
                .contains("SHUTDOWN_TIMEOUT");
        assertThat(guide)
                .contains("Sprint 18 item 718")
                .contains("SecretPresenceValidator")
                .contains("fails startup safely")
                .contains("ProductionBackendEnvironmentDocumentationTests");
    }
}
