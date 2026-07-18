package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("777 Write admin manual")
class AdministratorManualDocumentationTests {

    private static final Path MANUAL = Path.of("../docs/admin/admin-manual.md");

    @Test
    void manualCoversAccountRolePasswordSettingsAndAuditAdministration() throws Exception {
        String manual = DocumentationTestText.normalize(Files.readString(MANUAL, StandardCharsets.UTF_8));

        assertThat(manual)
                .contains("Item 777")
                .contains("Account lifecycle")
                .contains("Create an employee account")
                .contains("Disable an account")
                .contains("Role administration")
                .contains("Password reset and lockout")
                .contains("System settings")
                .contains("Audit review")
                .contains("First production administrator")
                .contains("Routine administration checklist");
    }

    @Test
    void manualEnforcesLeastPrivilegeAuditAndOperationalBoundaries() throws Exception {
        String manual = DocumentationTestText.normalize(Files.readString(MANUAL, StandardCharsets.UTF_8));

        assertThat(manual)
                .contains("Apply least privilege")
                .contains("Sensitive actions must be auditable")
                .contains("do not edit database tables")
                .contains("must not be used to approve one's own work")
                .contains("Do not weaken contact, retry, or uninterested safeguards")
                .contains("Admin UI is not a host/secret/database console")
                .contains("Never include passwords, hashes, JWTs");
    }
}
