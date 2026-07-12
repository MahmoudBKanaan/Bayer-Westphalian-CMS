package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** KB item 562: System Auditor guide section remains available as role evidence. */
@DisplayName("562 System Auditor guide section")
class SystemAuditorUserGuideDocumentationTests {

    private static final Path SYSTEM_AUDITOR_GUIDE =
            Path.of("../docs/user-guides/system-auditor-guide.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsSystemAuditorRoleScopeAndLeastPrivilege() throws Exception {
        String guide = Files.readString(SYSTEM_AUDITOR_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("System Auditor User Guide")
                .contains("562")
                .contains("SYSTEM_AUDITOR")
                .contains("audit logs")
                .contains("consent history")
                .contains("campaign approval history")
                .contains("user activity history")
                .contains("audit-report exports")
                .contains("read-only")
                .contains("cannot")
                .contains("Create, edit, or disable users")
                .contains("Create, approve, reject, launch, or edit campaigns")
                .contains("Edit or delete audit log entries");
    }

    @Test
    void documentsAuditLogWorkflowFiltersAndEntityHistory() throws Exception {
        String guide = Files.readString(SYSTEM_AUDITOR_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("/audit")
                .contains("GET /api/audit-logs")
                .contains("GET /api/audit-logs/entity-history")
                .contains("actorUserId")
                .contains("action")
                .contains("entityType")
                .contains("entityId")
                .contains("createdFrom")
                .contains("createdTo")
                .contains("Apply filters")
                .contains("Reset");
    }

    @Test
    void documentsAuditEvidenceAndExportWorkflow() throws Exception {
        String guide = Files.readString(SYSTEM_AUDITOR_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("WITHDRAW_CONSENT")
                .contains("APPROVE")
                .contains("REJECT")
                .contains("LAUNCH")
                .contains("EXPORT_REPORT")
                .contains("ReportService.exportAuditReport")
                .contains("audit-report surface")
                .contains("separate from campaign performance report export")
                .contains("campaigns cannot launch before compliance approval");
    }

    @Test
    void documentationIndexLinksSystemAuditorGuide() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index).contains("user-guides/system-auditor-guide.md");
    }
}
