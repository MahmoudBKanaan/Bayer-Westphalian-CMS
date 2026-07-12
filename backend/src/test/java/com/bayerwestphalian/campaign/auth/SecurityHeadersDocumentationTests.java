package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("545 Security headers documentation")
class SecurityHeadersDocumentationTests {

    @Test
    void securityConfigurationConfiguresHeaders() throws Exception {
        Path source =
                Paths.get(
                        "src/main/java/com/bayerwestphalian/campaign/auth/SecurityConfiguration.java");
        String content = Files.readString(source, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("configureSecurityHeaders")
                .contains("contentTypeOptions")
                .contains("frameOptions")
                .contains("contentSecurityPolicy")
                .contains("httpStrictTransportSecurity");
    }

    @Test
    void securityHardeningDocDescribesBackendSecurityHeaders() throws Exception {
        Path doc = Path.of("..", "docs", "architecture", "security-hardening.md");
        assertThat(doc).exists();
        String content = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("545")
                .contains("X-Content-Type-Options")
                .contains("X-Frame-Options")
                .contains("Content-Security-Policy")
                .contains("ApiSecurityHeadersFilter");
    }
}
