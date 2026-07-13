package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("767 v1.0 release notes")
class FinalV1ReleaseNotesDocumentationTests {

    private static final Path NOTES = Path.of("../docs/releases/v1.0-draft.md");

    @Test
    void notesDescribeScopeAudienceCompatibilityAndLimitations() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Sprint 18 item **767**")
                .contains("DRAFT - NOT RELEASED")
                .contains("Intended Audience And Support Boundary")
                .contains("first `v1.0` production milestone")
                .contains("No backward database migration is provided")
                .contains("AI outputs are decision support")
                .contains("Production Operations Guide");
    }

    @Test
    void notesDoNotClaimReleaseWithoutExactEvidenceAndHumanApproval() throws Exception {
        String notes = Files.readString(NOTES, StandardCharsets.UTF_8);

        assertThat(notes)
                .contains("Unauthorized restricted access")
                .contains("Items 760-766 guides reviewed")
                .contains("Complete items 737/763 execution")
                .contains("Preparing item 767 does not itself authorize a tag or release")
                .contains("approved `main` commit")
                .contains("Final commit | `<final-main-sha>`")
                .contains("Current readiness | **BLOCKED**");
    }
}
