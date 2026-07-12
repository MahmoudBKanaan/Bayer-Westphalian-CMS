package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("542 Environment variable validation documentation")
class EnvironmentVariableValidationDocumentationTests {

    @Test
    void environmentPostProcessorIsRegistered() throws Exception {
        Path registration =
                Path.of(
                        "src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor");
        assertThat(registration).exists();
        String content = Files.readString(registration, StandardCharsets.UTF_8);
        assertThat(content)
                .contains(
                        "com.bayerwestphalian.campaign.common.config.ProductionEnvironmentPostProcessor");
    }

    @Test
    void securityHardeningDocDescribesEnvironmentValidation() throws Exception {
        Path doc = Path.of("..", "docs", "architecture", "security-hardening.md");
        assertThat(doc).exists();
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("542")
                .contains("EnvironmentVariableValidator")
                .contains("DB_URL")
                .contains("JWT_SECRET")
                .contains("CORS_ALLOWED_ORIGINS");
    }

    @Test
    void securityHardeningDocDescribesSecretPresenceValidation() throws Exception {
        Path doc = Path.of("..", "docs", "architecture", "security-hardening.md");
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("543")
                .contains("SecretPresenceValidator")
                .contains("secret presence")
                .contains("32");
    }

    @Test
    void backendReadmeListsProductionRequiredVariables() throws Exception {
        Path readme = Path.of("README.md");
        assertThat(readme).exists();
        String content = Files.readString(readme, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("DB_URL")
                .contains("JWT_SECRET")
                .contains("CORS_ALLOWED_ORIGINS");
    }
}
