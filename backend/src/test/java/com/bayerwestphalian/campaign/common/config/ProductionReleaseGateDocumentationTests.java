package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("770 Production release gate")
class ProductionReleaseGateDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/assert-production-release-gate.ps1");
    private static final Path TEMPLATE =
            Path.of("../config/production-release-evidence.example.json");
    private static final Path GUIDE = Path.of("../docs/deployment/production-release-gate.md");

    @Test
    void validatorRequiresEveryKbProductionGateForExactRelease() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("smokeTests")
                .contains("backups")
                .contains("securityConfiguration")
                .contains("environmentConfiguration")
                .contains("providerConfigurationPolicy")
                .contains("rollbackPlan")
                .contains("criticalWorkflows")
                .contains("evidence commit does not match")
                .contains("human approver are required")
                .contains("overall decision is not PASS")
                .contains("is not PASS");
    }

    @Test
    void exampleIsFailClosedAndGuideForbidsEvidenceByAssertion() throws Exception {
        String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(template)
                .contains("\"decision\": \"BLOCKED\"")
                .contains("\"status\": \"BLOCKED\"")
                .doesNotContain("\"decision\": \"PASS\"");
        assertThat(guide)
                .contains("Sprint 18 item 770")
                .contains("green CI alone is not production approval")
                .contains("all seven gate statuses are `PASS`")
                .contains("A validator failure blocks release")
                .contains("rather than editing a status to `PASS`");
    }
}
