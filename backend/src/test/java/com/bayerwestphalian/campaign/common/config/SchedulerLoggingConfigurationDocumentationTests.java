package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("730 Configure scheduler logging")
class SchedulerLoggingConfigurationDocumentationTests {

    private static final Path BASE_PROFILE = Path.of("src/main/resources/application.yml");
    private static final Path PROD_PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path GUIDE = Path.of("../docs/deployment/scheduler-logging.md");

    @Test
    void productionConfiguresSchedulerZoneLevelAndMdcField() throws Exception {
        assertThat(Files.readString(BASE_PROFILE, StandardCharsets.UTF_8))
                .contains("processing-zone: \"${REMINDER_PROCESSING_ZONE:UTC}\"");
        assertThat(Files.readString(PROD_PROFILE, StandardCharsets.UTF_8))
                .contains("ReminderProcessingScheduler: ${LOG_LEVEL_SCHEDULER:INFO}")
                .contains("schedulerRunId=%X{schedulerRunId:-none}");
        assertThat(Files.readString(COMPOSE, StandardCharsets.UTF_8))
                .contains("REMINDER_PROCESSING_ZONE: ${REMINDER_PROCESSING_ZONE:-UTC}")
                .contains("LOG_LEVEL_SCHEDULER: ${LOG_LEVEL_SCHEDULER:-INFO}");
        assertThat(Files.readString(TEMPLATE, StandardCharsets.UTF_8))
                .contains("REMINDER_PROCESSING_ZONE=UTC")
                .contains("LOG_LEVEL_SCHEDULER=INFO");
    }

    @Test
    void guideDocumentsEventsQueriesAndAlertConditions() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("Sprint 18 item 730")
                .contains("schedulerEvent=run_failed")
                .contains("failedCount")
                .contains("absent for more than the expected cron interval")
                .contains("SchedulerLoggingConfigurationDocumentationTests");
    }
}
