package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductChangesCreateAuditLogDocumentationTests {

    private static final Path PRODUCT_AUDIT_DOC =
            Path.of("../docs/modules/product-audit-logging.md");

    @Test
    void documentsProductAuditBoundaryAndServices() throws Exception {
        String documentation = Files.readString(PRODUCT_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Product Audit Logging Documentation")
                .contains("com.bayerwestphalian.campaign.product")
                .contains("com.bayerwestphalian.campaign.audit")
                .contains("ProductService")
                .contains("ProductOwnershipService")
                .contains("PaymentRecordService")
                .contains("ProductChangeRequestService")
                .contains("AuditService")
                .contains("AuditLog")
                .contains("AuditLogRepository")
                .contains("AuditController")
                .contains("/api/audit-logs");
    }

    @Test
    void documentsAuditedEntityTypesActionsAndPayloadFields() throws Exception {
        String documentation = Files.readString(PRODUCT_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("products")
                .contains("product_ownerships")
                .contains("payment_records")
                .contains("product_change_requests")
                .contains("CREATE")
                .contains("UPDATE")
                .contains("DELETE")
                .contains("APPROVE")
                .contains("REJECT")
                .contains("name")
                .contains("productType")
                .contains("customerId")
                .contains("productOwnershipId")
                .contains("requestType")
                .contains("status")
                .contains("requestedByUserId")
                .contains("old and new workflow status values");
    }

    @Test
    void documentsProductAuditAuthorizationAndKbEvidence() throws Exception {
        String documentation = Files.readString(PRODUCT_AUDIT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("PRODUCT_MANAGER")
                .contains("COMPLIANCE_OFFICER")
                .contains("SYSTEM_AUDITOR")
                .contains(
                        "Product create, edit, disable, and soft-delete actions create audit logs")
                .contains("Product ownership assignment and ownership updates create audit logs")
                .contains("Payment record create and payment status updates create audit logs")
                .contains(
                        "Product-change request create, update, approve, reject, and implement"
                                + " actions create audit logs")
                .contains("Product changes create audit logs")
                .contains("Item 527")
                .contains("audit log API");
    }

    @Test
    void documentationIndexLinksProductAuditLoggingDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/product-audit-logging.md");
    }
}
