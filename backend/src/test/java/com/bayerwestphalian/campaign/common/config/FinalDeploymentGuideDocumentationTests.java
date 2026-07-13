package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("775 Write deployment guide")
class FinalDeploymentGuideDocumentationTests {

    private static final Path GUIDE = Path.of("../docs/deployment/production-deployment-guide.md");

    @Test
    void guideDefinesSupportedModelAndRepeatableDeploymentPaths() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Item 775")
                .contains("Supported deployment model")
                .contains("First deployment versus update")
                .contains("Select immutable artifacts")
                .contains("BACKEND_IMAGE")
                .contains("FRONTEND_IMAGE")
                .contains("Do not use `latest`")
                .contains("docker-compose.prod.yml")
                .contains("Operator completion checklist");
    }

    @Test
    void guideRequiresSecurityRecoverySmokeAndReleaseApproval() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("item 770")
                .contains("Green CI alone is insufficient")
                .contains("Provider policy is approved real configuration or explicitly disabled")
                .contains("matching consent-evidence recovery point")
                .contains("Full production smoke checklist")
                .contains("critical role workflows")
                .contains("deployment `BLOCKED`");
    }
}
