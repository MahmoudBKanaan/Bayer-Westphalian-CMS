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
                .contains("campaign name, objective, target segment, product, message, schedule, and owner")
                .contains("Approve campaigns")
                .contains("Reject campaigns")
                .contains("Request changes")
                .contains("add review notes")
                .contains("cannot launch before Compliance Officer approval")
                .contains("Unauthorized roles cannot approve")
                .contains("consent withdrawals")
                .contains("campaign approval and rejection history")
                .contains("audit-log ready")
                .contains("Backend authorization is authoritative")
                .contains("403 Forbidden")
                .contains("Validation failures");
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
                .contains("FR-059")
                .contains("BR-005")
                .contains("COMP-006")
                .contains("TC-011");
    }
}
