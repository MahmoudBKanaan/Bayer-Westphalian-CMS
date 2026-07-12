package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("538 Secure error response configuration and documentation")
class SecureErrorConfigurationDocumentationTests {

    @Test
    void applicationYmlDisablesStackTracesAndExceptionDetailsOnErrorPages() throws Exception {
        String yaml =
                Files.readString(
                        Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("include-stacktrace: never")
                .contains("include-message: never")
                .contains("include-binding-errors: never")
                .contains("include-exception: false");
    }

    @Test
    void productionYmlReinforcesSecureErrorPageSettings() throws Exception {
        String yaml =
                Files.readString(
                        Path.of("src/main/resources/application-prod.yml"),
                        StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("include-stacktrace: never")
                .contains("include-message: never")
                .contains("include-exception: false");
    }

    @Test
    void securityHardeningDocDescribesSecureErrorResponsesAndProductionStackPolicy()
            throws Exception {
        Path doc = Path.of("..", "docs", "architecture", "security-hardening.md");
        assertThat(doc).exists();
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("538")
                .contains("539")
                .contains("ErrorResponse")
                .contains("ProductionErrorSafetyConfiguration")
                .contains("stack")
                .containsIgnoringCase("secure");
    }
}
