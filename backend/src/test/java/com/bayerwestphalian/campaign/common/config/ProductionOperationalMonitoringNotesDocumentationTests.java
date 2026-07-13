package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionOperationalMonitoringNotesDocumentationTests {

    private static final Path NOTES =
            Path.of("../docs/deployment/operational-monitoring-notes.md");

    @Test
    void monitoringNotesDescribeCurrentCapabilityAndExternalGapHonestly() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Sprint 18 item 741")
                .contains("NFR-004")
                .contains("NFR-014")
                .contains("does **not** bundle")
                .contains("project-scale fallback")
                .contains("not proof that the 99% target is met");
    }

    @Test
    void monitoringNotesDefineSignalsThresholdsOwnersAndAlertLifecycle() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Ownership And Alert Lifecycle")
                .contains("Public HTTPS availability")
                .contains("Backend readiness `/readyz`")
                .contains("HTTP 5xx ratio")
                .contains("BACKUP_HEALTH_MAX_AGE_MINUTES")
                .contains("Scheduler completion")
                .contains("Audit trail")
                .contains("TLS certificate")
                .contains("SEV-1")
                .contains("recovery window");
    }

    @Test
    void monitoringNotesCoverAvailabilityLogsBackupsCanariesAndSensitiveData() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("availability % = successful readiness observations")
                .contains("Customer/product search p95 above 1 second")
                .contains("Item 735")
                .contains("Item 736")
                .contains("Business And Compliance Canaries")
                .contains("EligibilityService")
                .contains("Never ingest passwords, JWTs")
                .contains("Do not make production mutations solely to keep a dashboard green");
    }

    @Test
    void monitoringNotesAreIndexedAndLinkedFromHealthAndIncidentRunbooks() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);
        String health =
                Files.readString(
                        Path.of("../docs/deployment/health-endpoints.md"),
                        StandardCharsets.UTF_8);
        String incident =
                Files.readString(
                        Path.of("../docs/deployment/incident-response-notes.md"),
                        StandardCharsets.UTF_8);

        assertThat(index)
                .contains("deployment/operational-monitoring-notes.md")
                .contains("item **741**");
        assertThat(health).contains("operational-monitoring-notes.md").contains("item **741**");
        assertThat(incident).contains("operational-monitoring-notes.md").contains("item **741**");
    }
}
