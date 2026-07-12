package com.bayerwestphalian.campaign.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Sprint 16 item 621 — Map tests to KB business rules.
 *
 * Locks existence and key content of the business rules test map without
 * executing the mapped suites.
 */
class BusinessRulesTestMapDocumentationTests {

    private static final Path DOC = Path.of("../docs/testing/business-rules-test-map.md");

    private static final List<String> REQUIRED_IDS =
            List.of(
                    "BR-001",
                    "BR-002",
                    "BR-003",
                    "BR-004",
                    "BR-005",
                    "BR-006",
                    "BR-007",
                    "BR-010",
                    "BR-011",
                    "BR-012",
                    "BR-013",
                    "BR-014",
                    "BR-020",
                    "BR-021",
                    "BR-022",
                    "BR-023",
                    "BR-024",
                    "BR-030",
                    "BR-031",
                    "BR-032",
                    "BR-033",
                    "BR-034");

    @Test
    void documentsBusinessRulesTestMapForItem621() throws Exception {
        String documentation = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Business Rules Test Map")
                .contains("item **621**")
                .contains("Map tests to KB business rules")
                .contains("businessRulesTestMap.ts")
                .contains("do not run any tests")
                .contains("EligibilityServiceTests")
                .contains("## Eligibility and consent (BR-001–BR-004)")
                .contains("## Campaign lifecycle constraints (BR-030–BR-033)")
                .contains("## Critical test crosswalk (items 647–665)")
                .contains("## Acceptance (item 621)")
                .contains("## Coverage summary");

        for (String id : REQUIRED_IDS) {
            assertThat(documentation).as("missing business rule id %s", id).contains(id);
        }
    }

    @Test
    void documentsCriticalCrosswalkAndGateRuleSets() throws Exception {
        String documentation = Files.readString(DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("**647**")
                .contains("**648**")
                .contains("**660**")
                .contains("**674**")
                .contains("BR-001, BR-002, BR-003, BR-010, BR-011, BR-013, BR-014")
                .contains("BR-005, BR-032")
                .contains("functional-requirements-test-map.md")
                .contains("PaymentReminderNotSentIfPaymentCompletedTests")
                .contains("CampaignServiceTests");
    }
}
