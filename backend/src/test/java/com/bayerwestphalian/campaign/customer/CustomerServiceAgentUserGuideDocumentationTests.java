package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerServiceAgentUserGuideDocumentationTests {

    private static final Path CUSTOMER_SERVICE_AGENT_GUIDE =
            Path.of("../docs/user-guides/customer-service-agent-guide.md");

    @Test
    void documentsCustomerServiceAgentCustomerWorkflowsAndRestrictions() throws Exception {
        String guide = Files.readString(CUSTOMER_SERVICE_AGENT_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Customer Service Agent User Guide")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("search customer records")
                .contains("view customer details")
                .contains("create and update customer profiles")
                .contains("import customers from CSV")
                .contains("paginated customer and prospect list")
                .contains("Search by name, email, phone, city, country, and source")
                .contains("Filter by customer type, customer status, city, country")
                .contains("doNotContact")
                .contains("Soft delete is restricted to `ADMIN`")
                .contains("forbidden response");
    }

    @Test
    void documentsCustomerServiceAgentBeneficiaryAndCsvWorkflows() throws Exception {
        String guide = Files.readString(CUSTOMER_SERVICE_AGENT_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("beneficiaries tab")
                .contains("Link a beneficiary customer to a policyholder customer")
                .contains("Update the beneficiary relationship")
                .contains("guardian name")
                .contains("guardian email")
                .contains("guardianConsentRequired")
                .contains("Duplicate beneficiary links are rejected")
                .contains("POST /api/customers/import")
                .contains("Customer CSV Import Guide")
                .contains("Valid rows are imported")
                .contains("Invalid rows are rejected")
                .contains("lineNumber")
                .contains("field")
                .contains("message")
                .contains("value");
    }

    @Test
    void documentsCustomerServiceAgentConsentTabStatusDisplay() throws Exception {
        String guide = Files.readString(CUSTOMER_SERVICE_AGENT_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("consent tab")
                .contains("current consent status")
                .contains("valid or requires action")
                .contains("consent type, purpose, source")
                .contains("granted date, withdrawn date, expiration date")
                .contains("evidence URL")
                .contains("recorder information")
                .contains("Record new consent")
                .contains("Mark marketing opt-outs")
                .contains("withdraw consent")
                .contains("doNotContact")
                .contains("GIVEN")
                .contains("REQUIRED")
                .contains("WITHDRAWN")
                .contains("EXPIRED")
                .contains("REJECTED");
    }

    @Test
    void documentsCustomerServiceAgentAccessErrorsAndAuditExpectations() throws Exception {
        String guide = Files.readString(CUSTOMER_SERVICE_AGENT_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Backend authorization is authoritative")
                .contains("Frontend role-based controls")
                .contains("Missing authentication returns an unauthorized response")
                .contains("403 Forbidden")
                .contains("Validation failures")
                .contains("Customer creation")
                .contains("customer update")
                .contains("auditable")
                .contains("Admin workflows");
    }

    @Test
    void documentationIndexLinksCustomerServiceAgentGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("user-guides/customer-service-agent-guide.md");
    }
}
