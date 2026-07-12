package com.bayerwestphalian.campaign.settings;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** KB item 561: system settings documentation remains available as Admin/settings evidence. */
@DisplayName("561 System settings documentation")
class SystemSettingsDocumentationTests {

    private static final Path SYSTEM_SETTINGS_DOC = Path.of("../docs/modules/system-settings.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsAdminScreenBoundaryAndRestSurface() throws Exception {
        assertThat(SYSTEM_SETTINGS_DOC).exists();
        String content = Files.readString(SYSTEM_SETTINGS_DOC, StandardCharsets.UTF_8);

        assertThat(content)
                .contains("System Settings Documentation")
                .contains("534")
                .contains("561")
                .contains("com.bayerwestphalian.campaign.settings")
                .contains("system_settings")
                .contains("GET/PUT /api/system-settings")
                .contains("SystemSettingsPage.tsx")
                .contains("SystemSettingsView")
                .contains("Admin")
                .contains("All other roles")
                .contains("@PreAuthorize(\"@authz.canManageSystemSettings()\")");
    }

    @Test
    void documentsConfigurableFieldsRangesAndDefaults() throws Exception {
        String content = Files.readString(SYSTEM_SETTINGS_DOC, StandardCharsets.UTF_8);

        assertThat(content)
                .contains("monthlyContactLimit")
                .contains("sendRetryLimit")
                .contains("uninterestedExclusionDays")
                .contains("monthly_contact_limit")
                .contains("send_retry_limit")
                .contains("uninterested_exclusion_days")
                .contains("1–100")
                .contains("1–20")
                .contains("1–3650")
                .contains("app.contact.monthly-limit")
                .contains("app.contact.retry-limit")
                .contains("app.contact.uninterested-exclusion-days");
    }

    @Test
    void documentsRuntimeConsumersAndNoRestartBehavior() throws Exception {
        String content = Files.readString(SYSTEM_SETTINGS_DOC, StandardCharsets.UTF_8);

        assertThat(content)
                .contains("535")
                .contains("EligibilityService")
                .contains("AiRecommendationService.detectDuplicateRisk")
                .contains("536")
                .contains("SendRetryService")
                .contains("537")
                .contains("uninterested")
                .contains("applied immediately")
                .contains("applied on the next outbound send")
                .contains("SystemSettingsService.monthlyContactLimit()")
                .contains("SystemSettingsService.sendRetryLimit()")
                .contains("SystemSettingsService.uninterestedExclusionDays()")
                .contains("seed/default");
    }

    @Test
    void documentationIndexLinksSystemSettingsDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index).contains("modules/system-settings.md");
    }
}
