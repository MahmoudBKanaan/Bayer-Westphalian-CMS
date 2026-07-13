package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionIncidentResponseNotesDocumentationTests {

    private static final Path NOTES = Path.of("../docs/deployment/incident-response-notes.md");

    @Test
    void notesDefineSeverityOwnershipAndImmediateResponse() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Sprint 18 item 740")
                .contains("SEV-1 Critical")
                .contains("SEV-4 Low")
                .contains("Incident commander")
                .contains("Security/privacy/compliance owner")
                .contains("System Auditor / scribe")
                .contains("First 15 Minutes")
                .contains("INC-YYYYMMDD-NNN");
    }

    @Test
    void notesCoverCriticalScenariosAndUnsafeActions() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Secret or credential exposure")
                .contains("Unauthorized access or suspected personal-data exposure")
                .contains("Consent, do-not-contact, eligibility, or unintended sending failure")
                .contains("Database corruption, migration failure, or data loss")
                .contains("Never run `flyway clean`")
                .contains("Do not automatically")
                .contains("Do not invent statutory notification deadlines");
    }

    @Test
    void notesRequireSafeEvidenceRecoveryValidationAndHumanApproval() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Safe Evidence Collection")
                .contains("Never paste `Authorization`")
                .contains("all Critical item 737")
                .contains("human-approved")
                .contains("Incident Record Template")
                .contains("Actual RPO / RTO")
                .contains("not production incident payloads");
    }

    @Test
    void notesAreIndexedAndCrossLinkedFromOperationalControls() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);
        String rollback =
                Files.readString(
                        Path.of("../docs/deployment/rollback-plan.md"), StandardCharsets.UTF_8);
        String security =
                Files.readString(
                        Path.of("../docs/deployment/production-security-checklist.md"),
                        StandardCharsets.UTF_8);

        assertThat(index)
                .contains("deployment/incident-response-notes.md")
                .contains("item **740**");
        assertThat(rollback).contains("incident-response-notes.md").contains("item **740**");
        assertThat(security).contains("incident-response-notes.md").contains("item **740**");
    }
}
