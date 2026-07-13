package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("780 Export OpenAPI/Swagger documentation")
class OpenApiExportDocumentationTests {

    private static final Path EXPORT = Path.of("../docs/api/openapi.json");
    private static final Path SCRIPT = Path.of("../scripts/export-openapi.ps1");

    @Test
    void exportedDocumentIsOpenApiAndContainsCriticalPaths() throws Exception {
        String export = Files.readString(EXPORT, StandardCharsets.UTF_8);

        assertThat(export)
                .contains("\"openapi\"")
                .contains("Bayer-Westphalian Campaign Management Platform API")
                .contains("/api/auth/login")
                .contains("/api/customers")
                .contains("/api/products")
                .contains("/api/segments")
                .contains("/api/campaigns")
                .contains("/api/analytics/dashboard")
                .contains("/api/audit-logs");
    }

    @Test
    void exporterIsLocalAtomicAndValidatesBeforePublishing() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("localhost")
                .contains("127.0.0.1")
                .contains("openapi")
                .contains("requiredPaths")
                .contains(".partial")
                .contains("Move-Item")
                .contains("UTF8Encoding")
                .contains("export production documentation inside its approved boundary");
    }
}
