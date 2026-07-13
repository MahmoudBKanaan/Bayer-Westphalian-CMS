package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("782 Create final demo script")
class FinalDemoScriptDocumentationTests {

    private static final Path SCRIPT = Path.of("../docs/demo/final-demo-script.md");

    @Test
    void scriptDefinesTimedKbJourneyWithDeterministicAnchors() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("Item 782")
                .contains("Target duration is **20 minutes**")
                .contains("Preflight")
                .contains("Timed walkthrough")
                .contains("customer/consent -> product -> segment -> campaign -> human approval -> launch")
                .contains("20000000-0000-0000-0000-000000000101")
                .contains("40000000-0000-0000-0000-000000000101")
                .contains("50000000-0000-0000-0000-000000000101")
                .contains("Post-demo cleanup and evidence")
                .contains("Presenter checklist");
    }

    @Test
    void scriptPreservesConsentHumanApprovalProviderAndReleaseSafety() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("PROVIDER_REAL_SENDING_ENABLED=false")
                .contains("Hard no-go conditions")
                .contains("EligibilityService")
                .contains("AI cannot approve")
                .contains("Safety gate")
                .contains("Read-only fallback")
                .contains("Never edit database rows")
                .contains("item 770 evidence gate")
                .contains("does not change the blocked production release status");
    }
}
