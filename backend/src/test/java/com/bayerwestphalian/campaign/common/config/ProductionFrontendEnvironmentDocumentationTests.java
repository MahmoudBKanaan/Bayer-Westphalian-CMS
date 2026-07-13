package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("719 Configure production frontend environment")
class ProductionFrontendEnvironmentDocumentationTests {

    private static final Path DOCKERFILE = Path.of("../frontend/Dockerfile");
    private static final Path TEMPLATE = Path.of("../frontend/.env.production.example");
    private static final Path NGINX = Path.of("../frontend/nginx.docker.conf");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path GUIDE =
            Path.of("../docs/deployment/production-frontend-environment.md");

    @Test
    void productionImageReceivesSafeCompileTimeConfiguration() throws Exception {
        String dockerfile = Files.readString(DOCKERFILE, StandardCharsets.UTF_8);
        String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);

        assertThat(dockerfile)
                .contains("Item 719")
                .contains("ARG VITE_API_BASE_URL=/api")
                .contains("ARG VITE_APP_ENV=prod");
        assertThat(template)
                .contains("item 719")
                .contains("VITE_API_BASE_URL=/api")
                .contains("VITE_APP_ENV=prod")
                .contains("Never place secrets here")
                .doesNotContain("JWT_SECRET")
                .doesNotContain("DB_PASSWORD");
        assertThat(compose)
                .contains("VITE_API_BASE_URL: ${VITE_API_BASE_URL:-/api}")
                .contains("VITE_APP_ENV: prod");
    }

    @Test
    void nginxUsesProductionCachingAndSecurityHeaders() throws Exception {
        String nginx = Files.readString(NGINX, StandardCharsets.UTF_8);

        assertThat(nginx)
                .contains("server_tokens off")
                .contains("X-Content-Type-Options")
                .contains("X-Frame-Options")
                .contains("Cache-Control \"no-store\"")
                .contains("max-age=31536000, immutable")
                .contains("try_files $uri $uri/ /index.html");
    }

    @Test
    void guideDocumentsPublicConfigurationBoundary() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 719")
                .contains("compile-time browser configuration")
                .contains("not runtime secrets")
                .contains("ProductionFrontendEnvironmentDocumentationTests");
    }
}
