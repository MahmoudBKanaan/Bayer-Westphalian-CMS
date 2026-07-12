package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductManagerUserGuideDocumentationTests {

    private static final Path PRODUCT_MANAGER_GUIDE =
            Path.of("../docs/user-guides/product-manager-guide.md");

    @Test
    void documentsProductManagerScopeAndDashboardWorkflow() throws Exception {
        String guide = Files.readString(PRODUCT_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Product Manager User Guide")
                .contains("PRODUCT_MANAGER")
                .contains("create, edit, search, and disable products")
                .contains("product-change requests")
                .contains("view customer profiles")
                .contains("campaigns linked to products")
                .contains("product performance")
                .contains("cannot manage employee users")
                .contains("cannot launch campaigns")
                .contains("product-related KPIs");
    }

    @Test
    void documentsProductManagerCatalogAndDetailsWorkflows() throws Exception {
        String guide = Files.readString(PRODUCT_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Products area")
                .contains("productType")
                .contains("active")
                .contains("durationMonths")
                .contains("expirationPolicy")
                .contains("HOMEOWNER_INSURANCE")
                .contains("LIFE_INSURANCE")
                .contains("INVESTMENT_FUND")
                .contains("/api/products")
                .contains("product-module.md")
                .contains("product details page")
                .contains("Edit price, duration, expiration rules, and status")
                .contains("Disable or soft-delete");
    }

    @Test
    void documentsProductManagerOwnershipAndChangeRequestWorkflows() throws Exception {
        String guide = Files.readString(PRODUCT_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("assign products to customers")
                .contains("startDate")
                .contains("expirationDate")
                .contains("policyNumber")
                .contains("product ownership tab")
                .contains("/api/product-ownerships")
                .contains("product-ownership.md")
                .contains("Product Change Requests")
                .contains("PRICE_CHANGE")
                .contains("DURATION_CHANGE")
                .contains("EXPIRATION_RULE_CHANGE")
                .contains("STATUS_CHANGE")
                .contains("OPEN")
                .contains("APPROVED")
                .contains("REJECTED")
                .contains("IMPLEMENTED")
                .contains("/api/product-change-requests");
    }

    @Test
    void documentsProductManagerAccessAuditAndErrorHandling() throws Exception {
        String guide = Files.readString(PRODUCT_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("read-only campaign and analytics")
                .contains("cannot submit, approve, or launch campaigns")
                .contains("Backend authorization is authoritative")
                .contains("403 Forbidden")
                .contains("Validation failures")
                .contains("Unauthorized roles cannot create products")
                .contains("auditable")
                .contains("product-audit-logging.md")
                .contains("Admin or Compliance Officer workflows");
    }

    @Test
    void documentsProductManagerKbTraceability() throws Exception {
        String guide = Files.readString(PRODUCT_MANAGER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("KB Traceability")
                .contains("Role description")
                .contains("Allowed functions")
                .contains("Screens")
                .contains("Dashboard")
                .contains("Products")
                .contains("Product Details")
                .contains("Product Change Requests")
                .contains("Campaigns")
                .contains("Analytics")
                .contains("Reports")
                .contains("FR-040")
                .contains("FR-041")
                .contains("FR-042")
                .contains("FR-043")
                .contains("FR-044")
                .contains("FR-045")
                .contains("TC-010");
    }

    @Test
    void documentationIndexLinksProductManagerGuide() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("user-guides/product-manager-guide.md");
    }
}
