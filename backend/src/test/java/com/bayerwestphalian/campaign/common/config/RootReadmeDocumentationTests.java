package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("773 Update README")
class RootReadmeDocumentationTests {

    private static final Path README = Path.of("../README.md");

    @Test
    void readmeIsACurrentProjectEntryPoint() throws Exception {
        String readme = Files.readString(README, StandardCharsets.UTF_8);

        assertThat(readme)
                .contains("Current Status")
                .contains("Core Capabilities")
                .contains("System Roles")
                .contains("Quick Start")
                .contains("Quality Gates")
                .contains("Architecture And API")
                .contains("Security And Data Handling")
                .contains("Complete documentation index")
                .doesNotContain("## Setup Plan");
    }

    @Test
    void readmeDoesNotMisrepresentProductionReadiness() throws Exception {
        String readme = Files.readString(README, StandardCharsets.UTF_8);

        assertThat(readme)
                .contains("v1.0 candidate")
                .contains("DRAFT - NOT RELEASED")
                .contains("item 770 release gate")
                .contains("A green CI run alone does not authorize production")
                .contains("Real email/SMS sending")
                .contains("not public customer signup");
    }
}
