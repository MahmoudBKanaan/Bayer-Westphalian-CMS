package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EligibilityRulesDocumentationTests {

    private static final Path ELIGIBILITY_RULES_DOC =
            Path.of("../docs/architecture/eligibility-rules.md");

    @Test
    void documentsEligibilityBoundaryDependenciesAndAuthorization() throws Exception {
        String documentation = Files.readString(ELIGIBILITY_RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Eligibility Rules Documentation")
                .contains("com.bayerwestphalian.campaign.campaign")
                .contains("EligibilityService")
                .contains("EligibilityDecision")
                .contains("EligibilityExclusionReason")
                .contains("EligibilityResponse")
                .contains("CustomerRepository")
                .contains("ConsentService")
                .contains("campaign_recipients")
                .contains("contact_events")
                .contains("beneficiaries")
                .contains("campaigns.channel")
                .contains("Spring Security")
                .contains("ADMIN")
                .contains("CAMPAIGN_MANAGER")
                .contains("COMPLIANCE_OFFICER");
    }

    @Test
    void documentsKbEligibilityEvaluationOrderAndConsentMapping() throws Exception {
        String documentation = Files.readString(ELIGIBILITY_RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("doNotContact = true")
                .contains("withdrawn or rejected marketing consent")
                .contains("valid required consent")
                .contains("guardian consent is required but not valid")
                .contains("duplicate recipients")
                .contains("monthly marketing contact limit")
                .contains("EMAIL")
                .contains("MARKETING_EMAIL")
                .contains("SMS")
                .contains("MARKETING_SMS")
                .contains("PHONE")
                .contains("MARKETING_PHONE")
                .contains("IN_APP")
                .contains("DATA_PROCESSING")
                .contains("Unsupported campaign channels fail validation");
    }

    @Test
    void documentsStableExclusionReasonsAndRecipientStorageFields() throws Exception {
        String documentation = Files.readString(ELIGIBILITY_RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("DO_NOT_CONTACT")
                .contains("Customer has do-not-contact enabled")
                .contains("MARKETING_OPT_OUT")
                .contains("Customer has withdrawn or rejected marketing consent")
                .contains("INVALID_CONSENT")
                .contains("Customer does not have valid required consent")
                .contains("DUPLICATE_CAMPAIGN_RECIPIENT")
                .contains("Customer is already assigned to this campaign")
                .contains("MONTHLY_CONTACT_LIMIT")
                .contains("Customer has reached the monthly marketing contact limit")
                .contains("campaign_recipients.exclusion_reason")
                .contains("campaign_recipients.eligibility_explanation")
                .contains("status `ELIGIBLE`")
                .contains("status `EXCLUDED`");
    }

    @Test
    void documentsDuplicateContactPrevention() throws Exception {
        String documentation = Files.readString(ELIGIBILITY_RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Duplicate-Contact Prevention")
                .contains("same campaign more than once")
                .contains("campaign_recipients")
                .contains("(campaign_id, customer_id)")
                .contains("DUPLICATE_CAMPAIGN_RECIPIENT")
                .contains("CampaignRecipientService.generateRecipients")
                .contains("deduplicates repeated candidate customer ids")
                .contains("campaign_recipients_campaign_customer_unique")
                .contains("Launch reads only stored `ELIGIBLE` recipients")
                .contains("contact_events")
                .contains("different campaign");
    }

    @Test
    void documentsKbRequirementTraceabilityForEligibilityRules() throws Exception {
        String documentation = Files.readString(ELIGIBILITY_RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("BR-001")
                .contains("BR-002")
                .contains("BR-003")
                .contains("BR-006")
                .contains("BR-007")
                .contains("BR-010")
                .contains("BR-011")
                .contains("FR-034")
                .contains("FR-054")
                .contains("FR-055")
                .contains("FR-056")
                .contains("FR-097");
    }

    @Test
    void documentationIndexLinksEligibilityRulesDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("architecture/eligibility-rules.md");
    }
}
