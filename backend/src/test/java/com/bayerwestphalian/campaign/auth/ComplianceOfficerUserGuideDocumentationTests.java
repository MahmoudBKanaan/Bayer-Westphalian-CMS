package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ComplianceOfficerUserGuideDocumentationTests {

    private static final Path COMPLIANCE_OFFICER_GUIDE =
            Path.of("../docs/user-guides/compliance-officer-guide.md");

    @Test
    void documentsComplianceOfficerScopeAndDashboardWorkflow() throws Exception {
        String guide = Files.readString(COMPLIANCE_OFFICER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Compliance Officer User Guide")
                .contains("COMPLIANCE_OFFICER")
                .contains("review consent, opt-outs, guardian consent")
                .contains("campaign eligibility")
                .contains("campaign approval")
                .contains("audit logs")
                .contains("compliance reports")
                .contains("consent alerts and pending approvals")
                .contains("opt-outs")
                .contains("withdrawn consent")
                .contains("missing guardian consent")
                .contains("invalid eligibility");
    }

    @Test
    void documentsComplianceOfficerConsentAndEligibilityWorkflows() throws Exception {
        String guide = Files.readString(COMPLIANCE_OFFICER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("current consent status")
                .contains("consent history")
                .contains("withdrawn consent")
                .contains("rejected consent")
                .contains("expired consent")
                .contains("guardian consent for beneficiaries")
                .contains("doNotContact")
                .contains("purpose, source, granted date, withdrawn date, expiration date")
                .contains("evidence URL")
                .contains("valid consent is missing")
                .contains("customer opted out")
                .contains("doNotContact = true")
                .contains("recipient preview")
                .contains("eligible and excluded recipients")
                .contains("exclusion reason codes")
                .contains("DO_NOT_CONTACT")
                .contains("MARKETING_OPT_OUT")
                .contains("INVALID_CONSENT")
                .contains("DUPLICATE_CAMPAIGN_RECIPIENT")
                .contains("MONTHLY_CONTACT_LIMIT")
                .contains("minor beneficiaries requiring guardian consent")
                .contains("stored on campaign recipient records");
    }

    @Test
    void documentsComplianceReviewAuditAccessAndErrorHandling() throws Exception {
        String guide = Files.readString(COMPLIANCE_OFFICER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Review submitted campaigns before launch")
                .contains(
                        "campaign name, objective, target segment, product, message, schedule, and owner")
                .contains("Approve campaigns")
                .contains("Reject campaigns")
                .contains("required formal rejection reason")
                .contains("rejectionReason")
                .contains("rejection_reason")
                .contains("Request changes")
                .contains("add review notes")
                .contains("complianceReviewNotes")
                .contains("compliance-review-notes")
                .contains("cannot launch before Compliance Officer approval")
                .contains("Unauthorized roles cannot approve")
                .contains("consent withdrawals")
                .contains("campaign creation history")
                .contains("action=CREATE")
                .contains("entityType=campaigns")
                .contains("campaign approval history")
                .contains("action=APPROVE")
                .contains("campaign rejection history")
                .contains("audit-log ready")
                .contains("Backend authorization is authoritative")
                .contains("403 Forbidden")
                .contains("Validation failures");
    }

    @Test
    void documentsComplianceOfficerCampaignReviewSteps() throws Exception {
        String guide = Files.readString(COMPLIANCE_OFFICER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Campaign Review Steps")
                .contains("Open the Compliance Review page")
                .contains("SUBMITTED")
                .contains("campaign owner is not the reviewer")
                .contains("campaign objective, channel, message subject, message body")
                .contains("selected products")
                .contains("selected segment")
                .contains("recipient preview totals")
                .contains("eligible recipients")
                .contains("excluded recipients")
                .contains("consent, opt-out, do-not-contact, guardian consent")
                .contains("duplicate-recipient")
                .contains("monthly")
                .contains("Approve the campaign only when the campaign is compliant")
                .contains("non-blank `rejectionReason` is required")
                .contains("complianceReviewNotes")
                .contains("APPROVE")
                .contains("REJECT")
                .contains("entityType=campaigns")
                .contains("POST /api/campaigns/{id}/approve")
                .contains("POST /api/campaigns/{id}/reject")
                .contains("PUT /api/campaigns/{id}/compliance-review-notes")
                .contains("GET /api/audit-logs");
    }

    @Test
    void documentationIndexLinksComplianceOfficerGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("user-guides/compliance-officer-guide.md");
    }

    @Test
    void documentsComplianceOfficerKbTraceability() throws Exception {
        String guide = Files.readString(COMPLIANCE_OFFICER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("KB Traceability")
                .contains("Role description")
                .contains("Allowed functions")
                .contains("Screens")
                .contains("Dashboard")
                .contains("Consent Management")
                .contains("Customer Details")
                .contains("Compliance Review")
                .contains("Recipient Preview")
                .contains("Campaigns")
                .contains("Audit Log")
                .contains("Reports")
                .contains("Compliance review steps")
                .contains("owner/reviewer separation")
                .contains("FR-059")
                .contains("BR-005")
                .contains("COMP-006")
                .contains("TC-011");
    }
}
