package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecipientPreviewDocumentationTests {

    private static final Path RECIPIENT_PREVIEW_DOC =
            Path.of("../docs/modules/recipient-preview.md");

    @Test
    void documentsRecipientPreviewWorkflowAndApiContract() throws Exception {
        String documentation = Files.readString(RECIPIENT_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Recipient Preview Documentation")
                .contains("campaign-scoped audience review")
                .contains("EligibilityService")
                .contains("campaign_recipients")
                .contains("GET /api/campaigns/{id}/recipients/preview")
                .contains("GET /api/campaigns/{id}/recipients/eligible")
                .contains("GET /api/campaigns/{id}/recipients/excluded")
                .contains("GET /api/campaigns/{id}/recipients/summary")
                .contains("eligibilityStatus")
                .contains("exclusionReason")
                .contains("eligibilityExplanation");
    }

    @Test
    void documentsRecipientPreviewUiAndLaunchBoundary() throws Exception {
        String documentation = Files.readString(RECIPIENT_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Eligible tab")
                .contains("Excluded tab")
                .contains("exclusion reason summary panel")
                .contains("launch button")
                .contains("launch confirmation dialog")
                .contains("launch result")
                .contains("APPROVED")
                .contains("contact_events")
                .contains("campaign metrics")
                .contains("launch audit log");
    }

    @Test
    void documentsRecipientPreviewKbRulesAndAuthorization() throws Exception {
        String documentation = Files.readString(RECIPIENT_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("FR-054")
                .contains("FR-055")
                .contains("BR-001")
                .contains("BR-002")
                .contains("BR-003")
                .contains("BR-006")
                .contains("BR-007")
                .contains("BR-010")
                .contains("BR-011")
                .contains("do_not_contact")
                .contains("marketing opt-out")
                .contains("guardian consent")
                .contains("Monthly contact limit")
                .contains("Campaign Manager")
                .contains("Compliance Officer")
                .contains("Product Manager")
                .contains("cannot launch campaigns");
    }

    @Test
    void documentationIndexLinksRecipientPreviewDocumentation() throws Exception {
        String index = Files.readString(Path.of("../docs/README.md"), StandardCharsets.UTF_8);

        assertThat(index).contains("modules/recipient-preview.md");
    }
}
