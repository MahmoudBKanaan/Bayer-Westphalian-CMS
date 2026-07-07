package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerModuleDocumentationTests {

    private static final Path CUSTOMER_MODULE_DOC = Path.of("../docs/modules/customer-module.md");

    @Test
    void documentsCustomerModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(CUSTOMER_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Customer Module Documentation")
                .contains("com.bayerwestphalian.campaign.customer")
                .contains("Customer")
                .contains("CustomerRepository")
                .contains("CustomerService")
                .contains("CustomerController")
                .contains("/api/customers")
                .contains("GET")
                .contains("POST")
                .contains("PUT")
                .contains("DELETE")
                .contains("/api/customers/import")
                .contains("beneficiary")
                .contains("policyholder");
    }

    @Test
    void documentsCustomerRulesSearchSoftDeleteAndImportEvidence() throws Exception {
        String documentation = Files.readString(CUSTOMER_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("term")
                .contains("customerType")
                .contains("status")
                .contains("city")
                .contains("country")
                .contains("contactable")
                .contains("page")
                .contains("size")
                .contains("customerType`, `firstName`, and `lastName` are required")
                .contains("Email and phone formats")
                .contains("doNotContact")
                .contains("Soft-deleted customers are excluded")
                .contains("Valid rows are imported")
                .contains("invalid rows are rejected")
                .contains("lineNumber")
                .contains("field")
                .contains("message")
                .contains("value");
    }

    @Test
    void documentsCustomerAuthorizationAndAuditRules() throws Exception {
        String documentation = Files.readString(CUSTOMER_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("COMPLIANCE_OFFICER")
                .contains("CAMPAIGN_MANAGER")
                .contains("BI_ANALYST")
                .contains("SALES_AGENT")
                .contains("Audit")
                .contains("create")
                .contains("update")
                .contains("soft-delete")
                .contains("backend role authorization");
    }

    @Test
    void documentationIndexLinksCustomerModuleDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/customer-module.md");
    }
}
