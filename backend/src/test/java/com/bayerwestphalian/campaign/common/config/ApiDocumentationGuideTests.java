package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("779 Write API documentation")
class ApiDocumentationGuideTests {

    private static final Path GUIDE = Path.of("../docs/api/openapi.md");

    @Test
    void guideDocumentsApiConventionsSecurityAndErrorContracts() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Item 779")
                .contains("Authorization: Bearer")
                .contains("ApiResponse<T>")
                .contains("PageResponse<T>")
                .contains("ErrorResponse")
                .contains("validationErrors")
                .contains("requestId")
                .contains("401")
                .contains("403")
                .contains("RFC 4122 UUID")
                .contains("Production disables OpenAPI and Swagger by default");
    }

    @Test
    void guideCatalogsEveryMajorControllerFamilyAndCriticalSafeguards() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("/api/auth")
                .contains("/api/users")
                .contains("/api/customers")
                .contains("/api/consents")
                .contains("/api/products")
                .contains("/api/segments")
                .contains("/api/campaigns")
                .contains("/api/contact-events")
                .contains("/api/follow-up-tasks")
                .contains("/api/reminders")
                .contains("/api/analytics")
                .contains("/api/reports")
                .contains("/api/ai")
                .contains("/api/audit-logs")
                .contains("Only `APPROVED` campaigns can launch")
                .contains("AI cannot approve a campaign");
    }
}
