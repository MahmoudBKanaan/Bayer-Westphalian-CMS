package com.bayerwestphalian.campaign.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Sprint 16 item 639 — performance smoke documentation lock (KB NFR-003).
 */
class PerformanceSmokeDocumentationTests {

    private static final Path DOC = Path.of("../docs/testing/performance-smoke.md");

    @Test
    void documentsPerformanceSmokeForItem639AndNfr003() throws Exception {
        String documentation = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Performance Smoke Checks")
                .contains("item **639**")
                .contains("NFR-003")
                .contains("under 1 second")
                .contains("search")
                .contains("dashboard")
                .contains("PerformanceSmokeTests")
                .contains("performanceSmoke.ts")
                .contains("do not run any tests")
                .contains("## Acceptance (item 639)");
    }

    @Test
    void documentsBudgetsAndDatasetSizing() throws Exception {
        String documentation = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("1000")
                .contains("PROJECT_DATASET_SIZE")
                .contains("DASHBOARD_CAMPAIGN_COUNT")
                .contains("customer")
                .contains("product")
                .contains("AnalyticsService");
    }
}
