package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConsentModuleDocumentationTests {

    private static final Path CONSENT_MODULE_DOC = Path.of("../docs/modules/consent-module.md");

    @Test
    void documentsConsentModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(CONSENT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Consent Module Documentation")
                .contains("com.bayerwestphalian.campaign.consent")
                .contains("ConsentRecord")
                .contains("ConsentRepository")
                .contains("ConsentService")
                .contains("ConsentController")
                .contains("ConsentType")
                .contains("ConsentStatus")
                .contains("/api/consents")
                .contains("/api/consents/status")
                .contains("/api/consents/eligibility")
                .contains("/api/consents/withdraw")
                .contains("GET")
                .contains("POST")
                .contains("customerId")
                .contains("consentType")
                .contains("status")
                .contains("validOnly");
    }

    @Test
    void documentsConsentRulesOptOutGuardianConsentAndEvidence() throws Exception {
        String documentation = Files.readString(CONSENT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("consentType`, `status`, `purpose`, `source`")
                .contains("MARKETING_EMAIL")
                .contains("MARKETING_PHONE")
                .contains("MARKETING_SMS")
                .contains("GUARDIAN")
                .contains("DATA_PROCESSING")
                .contains("GIVEN")
                .contains("WITHDRAWN")
                .contains("REQUIRED")
                .contains("EXPIRED")
                .contains("REJECTED")
                .contains("not expired")
                .contains("not withdrawn")
                .contains("doNotContact = true")
                .contains("opted out")
                .contains("guardian consent")
                .contains("evidenceFileUrl")
                .contains("grantedAt")
                .contains("withdrawnAt")
                .contains("expiresAt")
                .contains("recorder information");
    }

    @Test
    void documentsConsentAuthorizationAuditAndKbEvidence() throws Exception {
        String documentation = Files.readString(CONSENT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("COMPLIANCE_OFFICER")
                .contains("CAMPAIGN_MANAGER")
                .contains("SYSTEM_AUDITOR")
                .contains("Audit")
                .contains("consent creation")
                .contains("consent changes")
                .contains("consent withdrawal")
                .contains("Consent changes create audit logs")
                .contains("Customers with withdrawn consent are excluded")
                .contains("Customers with marketing opt-outs are excluded")
                .contains("Minor beneficiaries without guardian consent are excluded")
                .contains("Valid guardian consent allows eligibility checks to continue")
                .contains("Exclusion reasons are returned and stored-ready")
                .contains("Unauthorized roles cannot approve compliance-controlled consent");
    }

    @Test
    void documentationIndexLinksConsentModuleDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/consent-module.md");
    }
}
