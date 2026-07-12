package com.bayerwestphalian.campaign.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BeneficiaryModuleDocumentationTests {

    private static final Path BENEFICIARY_MODULE_DOC =
            Path.of("../docs/modules/beneficiary-module.md");

    @Test
    void documentsBeneficiaryModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(BENEFICIARY_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Beneficiary Module Documentation")
                .contains("com.bayerwestphalian.campaign.beneficiary")
                .contains("Beneficiary")
                .contains("BeneficiaryRepository")
                .contains("BeneficiaryService")
                .contains("BeneficiaryController")
                .contains("/api/beneficiaries")
                .contains("GET")
                .contains("POST")
                .contains("PUT")
                .contains("DELETE")
                .contains("policyholderCustomerId")
                .contains("beneficiaryCustomerId")
                .contains("guardianConsentRequired");
    }

    @Test
    void documentsBeneficiaryRelationshipAndGuardianConsentRules() throws Exception {
        String documentation = Files.readString(BENEFICIARY_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("policyholderCustomerId`, `beneficiaryCustomerId`, and `relationship`")
                .contains("must be different records")
                .contains("must exist and must not be soft-deleted")
                .contains("Duplicate links are rejected")
                .contains("database unique constraint")
                .contains("guardianName")
                .contains("guardianEmail")
                .contains("guardianConsentRequired")
                .contains("Guardian email format")
                .contains("guardian consent is required")
                .contains("clears the guardian consent requirement");
    }

    @Test
    void documentsBeneficiaryAuthorizationAndKbEvidence() throws Exception {
        String documentation = Files.readString(BENEFICIARY_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("COMPLIANCE_OFFICER")
                .contains("CAMPAIGN_MANAGER")
                .contains("BI_ANALYST")
                .contains("SYSTEM_AUDITOR")
                .contains("A beneficiary can be linked to a policyholder customer")
                .contains("The guardian consent required flag is saved")
                .contains("Unauthorized roles cannot modify beneficiary relationships");
    }

    @Test
    void documentationIndexLinksBeneficiaryModuleDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/beneficiary-module.md");
    }
}
