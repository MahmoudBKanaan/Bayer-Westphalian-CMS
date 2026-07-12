package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductOwnershipDocumentationTests {

    private static final Path PRODUCT_OWNERSHIP_DOC =
            Path.of("../docs/modules/product-ownership.md");

    @Test
    void documentsProductOwnershipModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(PRODUCT_OWNERSHIP_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Product Ownership Documentation")
                .contains("com.bayerwestphalian.campaign.product")
                .contains("ProductOwnership")
                .contains("ProductOwnershipRepository")
                .contains("ProductOwnershipService")
                .contains("ProductOwnershipController")
                .contains("/api/product-ownerships")
                .contains("GET")
                .contains("POST")
                .contains("customerId")
                .contains("product_ownerships")
                .contains("product_ownership_id");
    }

    @Test
    void documentsProductOwnershipAssignmentExpirationAndStatusRules() throws Exception {
        String documentation = Files.readString(PRODUCT_OWNERSHIP_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("customerId`, `productId`, and `startDate` are required")
                .contains("expirationDate")
                .contains("must be on or after `startDate`")
                .contains("policyNumber")
                .contains("must be at most 100 characters")
                .contains("Policy numbers are unique")
                .contains("must not be soft-deleted")
                .contains("must be active")
                .contains("ACTIVE")
                .contains("EXPIRED")
                .contains("CANCELLED")
                .contains("isExpiringWithinMonths")
                .contains("findExpiringBetween")
                .contains("3, 6, and 12 months")
                .contains("product_ownerships_expiration_after_start");
    }

    @Test
    void documentsProductOwnershipAuthorizationAuditAndFrontendBoundary() throws Exception {
        String documentation = Files.readString(PRODUCT_OWNERSHIP_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("PRODUCT_MANAGER")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("CAMPAIGN_MANAGER")
                .contains("BI_ANALYST")
                .contains("COMPLIANCE_OFFICER")
                .contains("AuditService")
                .contains("product-audit-logging.md")
                .contains("product ownership tab")
                .contains("frontend/src/api/productOwnerships.ts")
                .contains("CustomerDetailsPage.tsx")
                .contains("backend role authorization");
    }

    @Test
    void documentsProductOwnershipDownstreamUseAndKbEvidence() throws Exception {
        String documentation = Files.readString(PRODUCT_OWNERSHIP_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Downstream Use")
                .contains("product-expiration campaigns")
                .contains("segmentation")
                .contains("reminder scheduling")
                .contains("analytics")
                .contains("BR-023")
                .contains("FR-073")
                .contains("FR-076")
                .contains("product-module.md")
                .contains("A product can be assigned to a customer")
                .contains("Product ownership expiration date is saved")
                .contains("Ownership records can be listed on the customer profile")
                .contains("Inactive or soft-deleted products cannot be assigned")
                .contains("Unauthorized roles cannot assign product ownership")
                .contains("Ownership assignment and updates create audit logs");
    }

    @Test
    void documentationIndexLinksProductOwnershipDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/product-ownership.md");
    }
}