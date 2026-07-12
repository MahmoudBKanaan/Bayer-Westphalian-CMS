package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductModuleDocumentationTests {

    private static final Path PRODUCT_MODULE_DOC = Path.of("../docs/modules/product-module.md");

    @Test
    void documentsProductModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(PRODUCT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Product Module Documentation")
                .contains("com.bayerwestphalian.campaign.product")
                .contains("Product")
                .contains("ProductRepository")
                .contains("ProductService")
                .contains("ProductController")
                .contains("ProductOwnership")
                .contains("ProductOwnershipRepository")
                .contains("ProductOwnershipService")
                .contains("ProductOwnershipController")
                .contains("ProductChangeRequest")
                .contains("PaymentRecord")
                .contains("PaymentRecordRepository")
                .contains("PaymentRecordService")
                .contains("PaymentRecordController")
                .contains("/api/products")
                .contains("/api/product-ownerships")
                .contains("/api/product-change-requests")
                .contains("/api/payment-records");
    }

    @Test
    void documentsProductCatalogRulesSearchAndSoftDeleteEvidence() throws Exception {
        String documentation = Files.readString(PRODUCT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("term")
                .contains("productType")
                .contains("active")
                .contains("name` and `productType` are required")
                .contains("HOMEOWNER_INSURANCE")
                .contains("LIFE_INSURANCE")
                .contains("INVESTMENT_FUND")
                .contains("expirationPolicy")
                .contains("deletedAt")
                .contains("Soft-deleted products are excluded")
                .contains("PATCH")
                .contains("/api/products/{id}/disable")
                .contains("DELETE")
                .contains("/api/products/{id}");
    }

    @Test
    void documentsProductOwnershipPaymentAndChangeRequestWorkflows() throws Exception {
        String documentation = Files.readString(PRODUCT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Assign a product to a customer")
                .contains("Update ownership expiration and policy number")
                .contains("Create a product-change request")
                .contains("approve")
                .contains("reject")
                .contains("mark-implemented")
                .contains("Create a payment record")
                .contains("mark-paid")
                .contains("mark-overdue")
                .contains("increment-reminder")
                .contains("ProductsPage")
                .contains("ProductDetailsPage")
                .contains("ProductChangeRequestsPage")
                .contains("CustomerDetailsPage")
                .contains("product ownership tab")
                .contains("payment records tab");
    }

    @Test
    void documentsProductAuthorizationAuditAndKbEvidence() throws Exception {
        String documentation = Files.readString(PRODUCT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("PRODUCT_MANAGER")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("CAMPAIGN_MANAGER")
                .contains("BI_ANALYST")
                .contains("COMPLIANCE_OFFICER")
                .contains("Product changes create audit logs")
                .contains("product-audit-logging.md")
                .contains("Product Manager and Admin can create, edit, search, and disable products")
                .contains("Unauthorized roles cannot create or mutate protected product workflows")
                .contains("Products can be assigned to customers with expiration dates")
                .contains("Payment records can be created and tracked for customer profiles")
                .contains("backend role authorization");
    }

    @Test
    void documentsProductDownstreamUseForCampaignsSegmentationRemindersAndAnalytics()
            throws Exception {
        String documentation = Files.readString(PRODUCT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Downstream Use")
                .contains("product-expiration campaigns")
                .contains("segmentation")
                .contains("payment reminders")
                .contains("analytics")
                .contains("BR-023")
                .contains("3, 6, or 12 months")
                .contains("isExpiringWithinMonths")
                .contains("findExpiringBetween")
                .contains("findExpiringWithinMonths")
                .contains("ProductOwnershipSearchCriteria")
                .contains("expiringFrom")
                .contains("expiringTo")
                .contains("findDuePayments")
                .contains("findOverduePayments")
                .contains("incrementReminder")
                .contains("DEFAULT_RISK")
                .contains("product_ownership_id")
                .contains("Production gate")
                .contains("FR-073")
                .contains("FR-074")
                .contains("FR-076");
    }

    @Test
    void documentationIndexLinksProductModuleDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/product-module.md");
    }
}