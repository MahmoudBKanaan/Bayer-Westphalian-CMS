package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("546 Safe API error logging documentation")
class SafeApiErrorLoggingDocumentationTests {

    @Test
    void globalExceptionHandlerUsesSafeApiErrorLogger() throws Exception {
        Path source =
                Path.of(
                        "src/main/java/com/bayerwestphalian/campaign/common/api/GlobalExceptionHandler.java");
        String content = Files.readString(source, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("SafeApiErrorLogger")
                .contains("logApplicationError")
                .contains("logUnexpectedError")
                .contains("logValidationError");
    }

    @Test
    void securityHardeningDocDescribesSafeErrorLogging() throws Exception {
        Path doc = Path.of("..", "docs", "architecture", "security-hardening.md");
        assertThat(doc).exists();
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("546")
                .contains("SafeApiErrorLogger")
                .contains("[REDACTED]")
                .containsIgnoringCase("secret");
    }
}
