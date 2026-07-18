package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionProductManagerCreateProductDocumentationTests {

    private static final Path SCRIPT =
            Path.of("../scripts/test-production-product-manager-create-product.ps1");
    private static final Path DOC =
            Path.of("../docs/deployment/product-manager-create-product-verification.md");

    @Test
    void verifierUsesProductManagerForCompleteProductLifecycle() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("PRODUCT_MANAGER")
                .contains("Post -Uri \"$origin/api/products\"")
                .contains("Get -Uri \"$origin/api/products/$productId\"")
                .contains("Patch -Uri \"$origin/api/products/$productId/disable\"")
                .contains("Delete -Uri \"$origin/api/products/$productId\"")
                .doesNotContain("AdminCleanupCredential");
    }

    @Test
    void verifierValidatesUuidPersistenceAndSafeSyntheticProduct() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("SMOKE-$suffix")
                .contains("productType = \"OTHER\"")
                .contains("price = 0.00")
                .contains("SMOKE_TEST_ONLY")
                .contains("StatusCode -ne 201")
                .contains("Guid]::TryParse")
                .contains("deletedAt");
    }

    @Test
    void verifierGuaranteesCleanupWithoutPrintingSensitiveMaterial() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("finally")
                .contains("during failure cleanup")
                .contains("$password = $null")
                .contains("$accessToken = $null")
                .contains("$syntheticName = $null")
                .doesNotContain("Write-Host $syntheticName")
                .doesNotContain("Write-Host $accessToken")
                .doesNotContain("Set-Content");
    }

    @Test
    void documentationRecordsBlockedRoleSpecificAuditableAcceptance() throws Exception {
        String doc = DocumentationTestText.normalize(Files.readString(DOC, StandardCharsets.UTF_8));

        assertThat(doc)
                .contains("Sprint 18 item 749")
                .contains("**BLOCKED**")
                .contains("No Product Manager credential was requested")
                .contains("active `PRODUCT_MANAGER` receives HTTP 201")
                .contains("create audit evidence exists")
                .contains("No Admin substitution is allowed")
                .contains("do not prove the role-specific deployed workflow");
    }
}
