package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("766 Incident notes")
class FinalIncidentNotesDocumentationTests {

    private static final Path NOTES = Path.of("../docs/deployment/incident-response-notes.md");

    @Test
    void notesDefineSafeTraceableLiveIncidentRecording() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Sprint 18 item 766")
                .contains("Live Note Quality")
                .contains("one chronological UTC timeline")
                .contains("OBSERVATION")
                .contains("ACTION")
                .contains("DECISION")
                .contains("RESULT")
                .contains("do not silently rewrite history")
                .contains("No critical action may be left without an owner");
    }

    @Test
    void notesRequireHumanApprovalClosureAndPostIncidentActions() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("human-approved target")
                .contains("Post-Incident Review")
                .contains("actual RPO/RTO")
                .contains("priority, owner, due date, verification method")
                .contains("Track them to completion")
                .contains("Do not delete incident or audit evidence")
                .contains("not production incident payloads");
    }
}
