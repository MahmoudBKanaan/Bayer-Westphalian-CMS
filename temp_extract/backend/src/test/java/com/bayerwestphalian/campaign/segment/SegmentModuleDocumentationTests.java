package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 202: Segmentation module documentation exists and covers package boundary, API, filters,
 * preview eligibility, authorization, audit, frontend boundary, and FR-070–079 evidence.
 */
class SegmentModuleDocumentationTests {

    private static final Path SEGMENTATION_MODULE_DOC =
            Path.of("../docs/modules/segmentation-module.md");

    @Test
    void documentsSegmentationModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(SEGMENTATION_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Segmentation Module Documentation")
                .contains("com.bayerwestphalian.campaign.segment")
                .contains("Segment")
                .contains("SegmentCriteria")
                .contains("SegmentRepository")
                .contains("SegmentCriteriaRepository")
                .contains("SegmentService")
                .contains("SegmentController")
                .contains("SegmentVisibility")
                .contains("SegmentOperator")
                .contains("SegmentJoinOperator")
                .contains("SegmentAgeGroupSupport")
                .contains("SegmentLocationSupport")
                .contains("SegmentCustomerTypeSupport")
                .contains("SegmentProductOwnershipSupport")
                .contains("SegmentPaymentHistorySupport")
                .contains("SegmentBehaviorStatusSupport")
                .contains("SegmentConsentStatusSupport")
                .contains("SegmentProductExpirationSupport")
                .contains("SegmentCriteriaLogicSupport")
                .contains("SegmentExclusionReasonSummary")
                .contains("EligibilityService")
                .contains("/api/segments")
                .contains("/api/segments/{id}")
                .contains("/api/segments/preview");
    }

    @Test
    void documentsFunctionalRequirementsAndFilterCategories() throws Exception {
        String documentation = Files.readString(SEGMENTATION_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("FR-070")
                .contains("FR-071")
                .contains("FR-072")
                .contains("FR-073")
                .contains("FR-074")
                .contains("FR-075")
                .contains("FR-076")
                .contains("FR-077")
                .contains("FR-078")
                .contains("FR-079")
                .contains("age_group")
                .contains("city")
                .contains("country")
                .contains("address_line")
                .contains("customer_type")
                .contains("product_type")
                .contains("product_id")
                .contains("ownership_status")
                .contains("payment_status")
                .contains("days_overdue")
                .contains("expiring_within_months")
                .contains("DUE")
                .contains("PAID")
                .contains("OVERDUE")
                .contains("DEFAULT_RISK")
                .contains("3 / 6 / 12");
    }

    @Test
    void documentsAndOrLogicVisibilityAndCriteriaModel() throws Exception {
        String documentation = Files.readString(SEGMENTATION_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("PRIVATE")
                .contains("TEAM")
                .contains("GLOBAL")
                .contains("join_operator")
                .contains("EQUALS")
                .contains("NOT_EQUALS")
                .contains("CONTAINS")
                .contains("IN")
                .contains("BETWEEN")
                .contains("BEFORE")
                .contains("AFTER")
                .contains("AND")
                .contains("OR")
                .contains("left-to-right")
                .contains("default to `AND`")
                .contains("owner_user_id");
    }

    @Test
    void documentsPreviewEligibilityCountsAndProductionGate() throws Exception {
        String documentation = Files.readString(SEGMENTATION_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("SegmentPreviewView")
                .contains("totalAudienceCount")
                .contains("eligibleCount")
                .contains("excludedCount")
                .contains("exclusionReasonSummary")
                .contains("eligibleCount + excludedCount = totalAudienceCount")
                .contains("evaluateForSegmentPreview")
                .contains("FR-054")
                .contains("FR-055")
                .contains("BR-001")
                .contains("BR-002")
                .contains("BR-003")
                .contains("BR-006")
                .contains("Production gate")
                .contains("criteria-only")
                .contains("eligibility-rules.md")
                .contains("never treat criteria-only matches as a final campaign audience");
    }

    @Test
    void documentsAuthorizationAuditAndRoleEvidence() throws Exception {
        String documentation = Files.readString(SEGMENTATION_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("canCreateSegments")
                .contains("canManageSegments")
                .contains("canReadSegments")
                .contains("canPreviewSegments")
                .contains("SegmentCreateAccess")
                .contains("ADMIN")
                .contains("CAMPAIGN_MANAGER")
                .contains("BI_ANALYST")
                .contains("COMPLIANCE_OFFICER")
                .contains("BI Analyst cannot edit segment unless allowed")
                .contains("item 200")
                .contains("Campaign Manager can create reusable segment")
                .contains("item 201")
                .contains("CREATE")
                .contains("UPDATE")
                .contains("DELETE")
                .contains("entity type: `segments`")
                .contains("backend role authorization");
    }

    @Test
    void documentsFrontendBoundaryAndDownstreamUse() throws Exception {
        String documentation = Files.readString(SEGMENTATION_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("SegmentsPage")
                .contains("SegmentCriteriaBuilder")
                .contains("SegmentPreviewResults")
                .contains("ExclusionReasonSummaryPanel")
                .contains("SegmentInsightPanel")
                .contains("frontend/src/api/segments.ts")
                .contains("criteriaFields.ts")
                .contains("exclusionReasons.ts")
                .contains("segmentInsights.ts")
                .contains("permissions.ts")
                .contains("Downstream Use")
                .contains("Campaign builder")
                .contains("Audience size")
                .contains("BI reporting");
    }

    @Test
    void documentsKbEvidenceChecklist() throws Exception {
        String documentation = Files.readString(SEGMENTATION_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Evidence")
                .contains("Users can segment by age group")
                .contains("Campaign Managers (and Admins) can create and save reusable segments")
                .contains("Criteria can be combined with AND and OR logic")
                .contains("Preview returns total audience size plus eligible and excluded counts")
                .contains("Preview always applies `EligibilityService`")
                .contains("Exclusion reason summaries")
                .contains("BI Analyst cannot edit segments unless also granted a manage role")
                .contains("Saved segment create/update/delete produce audit logs")
                .contains("Unauthorized roles cannot create or mutate protected segment workflows");
    }

    @Test
    void documentationIndexLinksSegmentationModuleDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/segmentation-module.md");
    }
}
