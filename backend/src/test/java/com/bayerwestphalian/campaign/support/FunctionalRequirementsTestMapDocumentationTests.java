package com.bayerwestphalian.campaign.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Sprint 16 item 620 — Map tests to KB functional requirements.
 *
 * Locks existence and key content of the functional requirements test map
 * without executing the mapped suites.
 */
class FunctionalRequirementsTestMapDocumentationTests {

    private static final Path DOC = Path.of("../docs/testing/functional-requirements-test-map.md");

    private static final List<String> REQUIRED_IDS =
            List.of(
                    "FR-001",
                    "FR-005",
                    "FR-010",
                    "FR-020",
                    "FR-030",
                    "FR-034",
                    "FR-040",
                    "FR-046",
                    "FR-050",
                    "FR-062",
                    "FR-070",
                    "FR-079",
                    "FR-080",
                    "FR-089",
                    "FR-090",
                    "FR-097",
                    "FR-100",
                    "FR-110",
                    "AI-001",
                    "AI-006");

    @Test
    void documentsFunctionalRequirementsTestMapForItem620() throws Exception {
        String documentation = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Functional Requirements Test Map")
                .contains("item **620**")
                .contains("Map tests to KB functional requirements")
                .contains("functionalRequirementsTestMap.ts")
                .contains("do not run any tests")
                .contains("## Auth and RBAC (FR-001–FR-005)")
                .contains("## Campaign lifecycle (FR-050–FR-062)")
                .contains("## AI-assisted features (AI-001–AI-006)")
                .contains("## Acceptance (item 620)")
                .contains("## Coverage summary");

        for (String id : REQUIRED_IDS) {
            assertThat(documentation).as("missing requirement id %s", id).contains(id);
        }
    }

    @Test
    void documentsHappyPathAndRelatedSprint16Items() throws Exception {
        String documentation = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("FR-001 → FR-011 → FR-033 → FR-041 → FR-077 → FR-050 → FR-059 → FR-060 → FR-100")
                .contains("**621**")
                .contains("**622**")
                .contains("**670**")
                .contains("AuthControllerTests")
                .contains("EligibilityServiceTests")
                .contains("CampaignCanBeApprovedTests")
                .contains("happy-path");
    }
}
