package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("729 Configure production logging")
class ProductionLoggingConfigurationDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path GUIDE = Path.of("../docs/deployment/production-logging.md");

    @Test
    void productionUsesCorrelatedKeyValueConsoleLogs() throws Exception {
        String profile = Files.readString(PROFILE, StandardCharsets.UTF_8);

        assertThat(profile)
                .contains("root: ${LOG_LEVEL_ROOT:INFO}")
                .contains("com.bayerwestphalian.campaign: ${LOG_LEVEL_APPLICATION:INFO}")
                .contains("org.hibernate.SQL: ${LOG_LEVEL_HIBERNATE_SQL:WARN}")
                .contains("requestId=%X{requestId:-none}")
                .contains("service=${spring.application.name}")
                .contains("message=%msg");
    }

    @Test
    void composeRotatesLogsForEveryProductionService() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("x-production-logging: &production-logging")
                .contains("driver: local")
                .contains("max-size: ${LOG_MAX_SIZE:-10m}")
                .contains("max-file: \"${LOG_MAX_FILES:-5}\"");
        assertThat(count(compose, "logging: *production-logging")).isEqualTo(5);
    }

    @Test
    void templateAndGuideDocumentSafeOperations() throws Exception {
        String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(template)
                .contains("LOG_LEVEL_APPLICATION=INFO")
                .contains("LOG_MAX_SIZE=10m")
                .contains("LOG_MAX_FILES=5");
        assertThat(guide)
                .contains("Sprint 18 item 729")
                .contains("Unsafe, oversized, or newline-containing IDs are replaced")
                .contains("Operational logs do not replace immutable application `AuditLog`")
                .contains("ProductionLoggingConfigurationDocumentationTests");
    }

    private static int count(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
    }
}
