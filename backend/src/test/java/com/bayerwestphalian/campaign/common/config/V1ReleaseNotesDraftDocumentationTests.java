package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V1ReleaseNotesDraftDocumentationTests {

    private static final Path RELEASE_NOTES = Path.of("../docs/releases/v1.0-draft.md");

    @Test
    void draftIsClearlyUnreleasedAndUsesEvidencePlaceholders() throws Exception {
        String notes = Files.readString(RELEASE_NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("DRAFT - NOT RELEASED")
                .contains("Sprint 18 item **742**")
                .contains("Production-ready MVP")
                .contains("<final-main-sha>")
                .contains("@sha256:<digest>")
                .contains("Current readiness | **BLOCKED**")
                .contains("working changes are not release evidence");
    }

    @Test
    void draftSummarizesProductSecurityAndOperationalScope() throws Exception {
        String notes = Files.readString(RELEASE_NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Customers, Products, And Consent")
                .contains("Segments And Campaigns")
                .contains("Communications, Reminders, And Follow-Up")
                .contains("Analytics, AI, And Audit")
                .contains("Security And Production Operations")
                .contains("EligibilityService")
                .contains("human approval")
                .contains("PostgreSQL 16")
                .contains("SHA-256");
    }

    @Test
    void draftRequiresAllCriticalReleaseEvidenceBeforePublication() throws Exception {
        String notes = Files.readString(RELEASE_NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Release Gate - Must Be Complete Before Publication")
                .contains("CI on exact final SHA")
                .contains("Database backup")
                .contains("Non-production restore")
                .contains("Production smoke test")
                .contains("**BLOCKED** - see item 738 report")
                .contains("no Critical gate may be")
                .contains("Never move or")
                .contains("force-update a published tag");
    }

    @Test
    void draftIsIndexedAndLinkedFromReleaseTaggingGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);
        String tagging =
                Files.readString(
                        Path.of("../docs/deployment/release-tagging.md"),
                        StandardCharsets.UTF_8);

        assertThat(index).contains("releases/v1.0-draft.md").contains("item **742**");
        assertThat(tagging).contains("../releases/v1.0-draft.md").contains("item **742**");
    }
}
