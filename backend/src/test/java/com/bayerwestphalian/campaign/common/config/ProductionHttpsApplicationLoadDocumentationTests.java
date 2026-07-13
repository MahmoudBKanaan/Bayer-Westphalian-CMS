package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionHttpsApplicationLoadDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-https.ps1");
    private static final Path HTTPS_DOC = Path.of("../docs/deployment/https.md");

    @Test
    void verifierRequiresTrustedHttpsApplicationAndReadiness() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("BaseUrl must use https")
                .contains("%{http_code}")
                .contains("React application root")
                .contains("strict-transport-security")
                .contains("30[178]")
                .contains("/readyz")
                .doesNotContain("curl.exe -k")
                .doesNotContain("--insecure");
    }

    @Test
    void verifierUsesTemporaryEvidenceAndAlwaysRemovesIt() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("GetTempPath")
                .contains("finally")
                .contains("Remove-Item")
                .contains("HTTPS verification request failed")
                .doesNotContain("Authorization")
                .doesNotContain("Cookie");
    }

    @Test
    void documentationRecordsItem744AsBlockedUntilDeploymentExists() throws Exception {
        String doc = Files.readString(HTTPS_DOC, StandardCharsets.UTF_8);

        assertThat(doc)
                .contains("Application Load Verification (Item 744)")
                .contains("test-production-https.ps1")
                .contains("does not use `curl -k`")
                .contains("**BLOCKED**")
                .contains("`https://localhost/` is unreachable")
                .contains("Vite server on HTTP port 5173 is not production HTTPS evidence");
    }
}
