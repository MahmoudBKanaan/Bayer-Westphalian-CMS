package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PaymentRecordDocumentationTests {

    private static final Path PAYMENT_RECORD_DOC = Path.of("../docs/modules/payment-records.md");

    @Test
    void documentsPaymentRecordModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(PAYMENT_RECORD_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Payment Record Documentation")
                .contains("com.bayerwestphalian.campaign.product")
                .contains("PaymentRecord")
                .contains("PaymentRecordRepository")
                .contains("PaymentRecordService")
                .contains("PaymentRecordController")
                .contains("/api/payment-records")
                .contains("GET")
                .contains("POST")
                .contains("PUT")
                .contains("PATCH")
                .contains("mark-paid")
                .contains("mark-overdue")
                .contains("increment-reminder")
                .contains("payment_records")
                .contains("product_ownership_id");
    }

    @Test
    void documentsPaymentRecordCreateStatusAndReminderRules() throws Exception {
        String documentation = Files.readString(PAYMENT_RECORD_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("customerId`, `productOwnershipId`, `dueDate`, and `amountDue` are required")
                .contains("must be greater than or equal to `0.00`")
                .contains("must belong to the specified customer")
                .contains("DUE")
                .contains("PAID")
                .contains("OVERDUE")
                .contains("DEFAULT_RISK")
                .contains("reminderCount")
                .contains("markPaid()")
                .contains("markOverdue()")
                .contains("incrementReminder()")
                .contains("updateDetails()")
                .contains("BR-024")
                .contains("BR-020")
                .contains("BR-021")
                .contains("BR-022")
                .contains("calculateDaysOverdue()")
                .contains("isDefaultRisk()")
                .contains("payment_records_due_status_idx");
    }

    @Test
    void documentsPaymentRecordAuthorizationAuditAndFrontendBoundary() throws Exception {
        String documentation = Files.readString(PAYMENT_RECORD_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Spring Security")
                .contains("method-level authorization")
                .contains("ADMIN")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("CAMPAIGN_MANAGER")
                .contains("BI_ANALYST")
                .contains("COMPLIANCE_OFFICER")
                .contains("SYSTEM_AUDITOR")
                .contains("AuditService")
                .contains("product-audit-logging.md")
                .contains("payment records tab")
                .contains("frontend/src/api/paymentRecords.ts")
                .contains("CustomerDetailsPage.tsx")
                .contains("backend role authorization");
    }

    @Test
    void documentsPaymentRecordDownstreamUseAndKbEvidence() throws Exception {
        String documentation = Files.readString(PAYMENT_RECORD_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Downstream Use")
                .contains("payment reminders")
                .contains("segmentation")
                .contains("default-risk")
                .contains("analytics")
                .contains("FR-074")
                .contains("AI-004")
                .contains("product-module.md")
                .contains("product-ownership.md")
                .contains("A payment record can be created for a customer-owned product")
                .contains("A payment record can be updated while unpaid")
                .contains("A payment record can be marked paid")
                .contains("Payment records can be listed on the customer profile")
                .contains("Paid payments are excluded from further reminder increments")
                .contains("Unauthorized roles cannot create or mutate protected payment workflows")
                .contains("Payment record changes create audit logs");
    }

    @Test
    void documentationIndexLinksPaymentRecordDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/payment-records.md");
    }
}
