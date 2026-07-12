package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("541 HTTPS production configuration and documentation")
class ProductionHttpsConfigurationDocumentationTests {

    @Test
    void productionYmlRequiresHttpsPolicy() throws Exception {
        String yaml =
                Files.readString(
                        Path.of("src/main/resources/application-prod.yml"), StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("https:")
                .contains("required: ${HTTPS_REQUIRED:true}")
                .contains("hsts-enabled:")
                .contains("forward-headers-strategy: framework");
    }

    @Test
    void baseYmlDoesNotForceHttpsOutsideProd() throws Exception {
        String yaml =
                Files.readString(
                        Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertThat(yaml).contains("required: ${HTTPS_REQUIRED:false}");
    }

    @Test
    void securityHardeningDocDescribesHttpsRequirement() throws Exception {
        Path doc = Path.of("..", "docs", "architecture", "security-hardening.md");
        assertThat(doc).exists();
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("541")
                .contains("HTTPS")
                .contains("X-Forwarded-Proto")
                .contains("HttpsEnforcementFilter");
    }

    @Test
    void dockerReadmeMentionsHttpsForProduction() throws Exception {
        Path doc = Path.of("..", "docker", "README.md");
        assertThat(doc).exists();
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content).containsIgnoringCase("HTTPS");
    }
}
