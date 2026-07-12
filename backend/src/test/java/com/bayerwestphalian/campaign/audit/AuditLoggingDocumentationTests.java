package com.bayerwestphalian.campaign.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** KB item 559: audit logging documentation remains available as E22 evidence. */
@DisplayName("559 Audit logging documentation")
class AuditLoggingDocumentationTests {

    private static final Path AUDIT_LOGGING_DOC = Path.of("../docs/modules/audit-logging.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsAuditLoggingBoundaryAccessAndReadOnlyApi() throws Exception {
        String documentation = Files.readString(AUDIT_LOGGING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Audit Logging Documentation")
                .contains("E22")
                .contains("COMP-008")
                .contains("559")
                .contains("com.bayerwestphalian.campaign.audit")
                .contains("Audit Log screen is **read-only**")
                .contains("GET /api/audit-logs")
                .contains("GET /api/audit-logs/entity-history")
                .contains("Admin")
                .contains("Compliance Officer")
                .contains("System Auditor")
                .contains("Other roles")
                .contains("not exposed over the API or UI");
    }

    @Test
    void documentsAuditFiltersAndEntityHistory() throws Exception {
        String documentation = Files.readString(AUDIT_LOGGING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Audit Filters")
                .contains("actorUserId")
                .contains("action")
                .contains("entityType")
                .contains("entityId")
                .contains("createdFrom")
                .contains("createdTo")
                .contains("getEntityHistory")
                .contains("AuditService.listAuditLogs(AuditLogSearchCriteria)")
                .contains("AuditController");
    }

    @Test
    void documentsSensitiveActionsAndPayloadShape() throws Exception {
        String documentation = Files.readString(AUDIT_LOGGING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Sensitive Actions Logged")
                .contains("Users")
                .contains("Consent")
                .contains("Products")
                .contains("Campaigns")
                .contains("Reports")
                .contains("CREATE")
                .contains("WITHDRAW_CONSENT")
                .contains("APPROVE")
                .contains("LAUNCH")
                .contains("EXPORT_REPORT")
                .contains("AuditLogView")
                .contains("oldValue")
                .contains("newValue")
                .contains("ipAddress")
                .contains("createdAt");
    }

    @Test
    void documentationIndexLinksAuditLoggingDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index).contains("modules/audit-logging.md");
    }
}
