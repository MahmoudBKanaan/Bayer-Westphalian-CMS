package com.bayerwestphalian.campaign.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Sprint 16 item 622 — Map tests to non-functional requirements.
 *
 * Locks existence and key content of the NFR test map without executing the
 * mapped suites.
 */
class NonFunctionalRequirementsTestMapDocumentationTests {

    private static final Path DOC =
            Path.of("../docs/testing/non-functional-requirements-test-map.md");

    private static final List<String> REQUIRED_IDS =
            List.of(
                    "NFR-001",
                    "NFR-002",
                    "NFR-003",
                    "NFR-004",
                    "NFR-005",
                    "NFR-006",
                    "NFR-007",
                    "NFR-008",
                    "NFR-009",
                    "NFR-010",
                    "NFR-011",
                    "NFR-012",
                    "NFR-013",
                    "NFR-014");

    @Test
    void documentsNonFunctionalRequirementsTestMapForItem622() throws Exception {
        String documentation = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Non-Functional Requirements Test Map")
                .contains("item **622**")
                .contains("Map tests to non-functional requirements")
                .contains("nonFunctionalRequirementsTestMap.ts")
                .contains("do not run any tests")
                .contains("SecurityHardeningDocumentationTests")
                .contains("## Security and privacy (NFR-001–NFR-002)")
                .contains("## Usability and accessibility (NFR-005, NFR-011)")
                .contains("## Data integrity, backup, and observability (NFR-012–NFR-014)")
                .contains("## Critical and run-item crosswalk")
                .contains("## Acceptance (item 622)")
                .contains("## Coverage summary");

        for (String id : REQUIRED_IDS) {
            assertThat(documentation).as("missing NFR id %s", id).contains(id);
        }
    }

    @Test
    void documentsAsymmetricNfrsAndSprint16RunItems() throws Exception {
        String documentation = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("frontend-primary")
                .contains("no backup UI")
                .contains("**638**")
                .contains("**639**")
                .contains("**640**")
                .contains("**666**")
                .contains("**664**")
                .contains("**665**")
                .contains("functional-requirements-test-map.md")
                .contains("business-rules-test-map.md")
                .contains("ProtectedEndpointSecurityTests")
                .contains("HealthEndpointIntegrationTests");
    }
}
