package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 203: Segment criteria guide documents field catalog, operators, AND/OR join semantics,
 * recipes, and FR-070–078 evidence.
 */
class SegmentCriteriaGuideDocumentationTests {

    private static final Path SEGMENT_CRITERIA_GUIDE =
            Path.of("../docs/modules/segment-criteria-guide.md");

    @Test
    void documentsCriteriaShapeOperatorsAndJoinSemantics() throws Exception {
        String documentation = Files.readString(SEGMENT_CRITERIA_GUIDE, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Segment Criteria Guide")
                .contains("fieldName")
                .contains("operator")
                .contains("value")
                .contains("logicalGroup")
                .contains("joinOperator")
                .contains("EQUALS")
                .contains("NOT_EQUALS")
                .contains("CONTAINS")
                .contains("IN")
                .contains("BETWEEN")
                .contains("BEFORE")
                .contains("AFTER")
                .contains("SegmentOperator")
                .contains("left-to-right")
                .contains("SegmentCriteriaLogicSupport")
                .contains("Default **`AND`**")
                .contains("FR-078");
    }

    @Test
    void documentsAgeLocationTypeAndOwnershipFields() throws Exception {
        String documentation = Files.readString(SEGMENT_CRITERIA_GUIDE, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("FR-070")
                .contains("FR-071")
                .contains("FR-072")
                .contains("FR-073")
                .contains("age_group")
                .contains("MINOR")
                .contains("18_25")
                .contains("26_40")
                .contains("41_60")
                .contains("60_PLUS")
                .contains("city")
                .contains("country")
                .contains("address_line")
                .contains("location` → `city")
                .contains("customer_type")
                .contains("CUSTOMER")
                .contains("PROSPECT")
                .contains("BENEFICIARY")
                .contains("product_type")
                .contains("product_id")
                .contains("ownership_status")
                .contains("HOMEOWNER_INSURANCE")
                .contains("LIFE_INSURANCE")
                .contains("ACTIVE")
                .contains("EXPIRED")
                .contains("CANCELLED");
    }

    @Test
    void documentsPaymentBehaviorConsentAndExpirationFields() throws Exception {
        String documentation = Files.readString(SEGMENT_CRITERIA_GUIDE, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("FR-074")
                .contains("FR-075")
                .contains("FR-076")
                .contains("payment_status")
                .contains("payment_history")
                .contains("reminder_count")
                .contains("days_overdue")
                .contains("default_risk")
                .contains("DUE")
                .contains("PAID")
                .contains("OVERDUE")
                .contains("DEFAULT_RISK")
                .contains("PENDING")
                .contains("status")
                .contains("interest")
                .contains("source")
                .contains("do_not_contact")
                .contains("consent_status")
                .contains("consent_type")
                .contains("has_valid_marketing_consent")
                .contains("opt_out")
                .contains("has_valid_guardian_consent")
                .contains("MARKETING_EMAIL")
                .contains("GUARDIAN")
                .contains("expiring_within_months")
                .contains("expiration_date")
                .contains("is_expiring")
                .contains("3")
                .contains("6")
                .contains("12")
                .contains("product_expiration_months");
    }

    @Test
    void documentsAndOrExamplesAndMixedEvaluation() throws Exception {
        String documentation = Files.readString(SEGMENT_CRITERIA_GUIDE, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("AND examples")
                .contains("OR examples")
                .contains("Mixed AND / OR")
                .contains("(PROSPECT AND Munich) OR Berlin")
                .contains("Munich prospects")
                .contains("intersection")
                .contains("union")
                .contains("no operator precedence beyond sequential fold");
    }

    @Test
    void documentsUiApiWorkflowsRecipesAndCommonMistakes() throws Exception {
        String documentation = Files.readString(SEGMENT_CRITERIA_GUIDE, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Building Criteria in the UI")
                .contains("Criteria builder")
                .contains("criteriaFields.ts")
                .contains("SegmentCriteriaBuilder")
                .contains("Building Criteria via API")
                .contains("POST /api/segments")
                .contains("PUT /api/segments/{id}")
                .contains("POST /api/segments/preview")
                .contains("CreateSegmentCriteriaCommand")
                .contains("Worked Recipes")
                .contains("Munich prospects for life insurance renewal outreach")
                .contains("Overdue payers in Munich or Berlin")
                .contains("Common Mistakes")
                .contains("Treating criteria match as final audience")
                .contains("Wrong payment statuses")
                .contains("EligibilityService");
    }

    @Test
    void documentsEvidenceAndRelatedModuleLinks() throws Exception {
        String documentation = Files.readString(SEGMENT_CRITERIA_GUIDE, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Evidence")
                .contains("FR-070–076")
                .contains("AND (default) and OR left-to-right")
                .contains("segmentation-module.md")
                .contains("eligibility-rules.md")
                .contains("role-based-access.md")
                .contains("criteria matching is not final contact permission without eligibility");
    }

    @Test
    void documentationIndexLinksSegmentCriteriaGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/segment-criteria-guide.md");
    }

    @Test
    void segmentationModuleDocumentationLinksCriteriaGuide() throws Exception {
        String moduleDoc =
                Files.readString(
                        Path.of("../docs/modules/segmentation-module.md"), StandardCharsets.UTF_8);

        assertThat(moduleDoc).contains("segment-criteria-guide.md");
    }
}
