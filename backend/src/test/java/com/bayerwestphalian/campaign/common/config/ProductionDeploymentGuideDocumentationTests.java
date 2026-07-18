package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("760 Production deployment guide")
class ProductionDeploymentGuideDocumentationTests {

    private static final Path GUIDE = Path.of("../docs/deployment/production-deployment-guide.md");

    @Test
    void guideDefinesAControlledEndToEndProductionDeployment() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("Sprint 18 item 760")
                .contains("required CI workflow passed")
                .contains("docker-compose.prod.yml")
                .contains("config --quiet")
                .contains("up -d --no-build")
                .contains("Flyway")
                .contains("production smoke test checklist")
                .contains("unauthorized page/API access")
                .contains("rollback plan")
                .contains("image digests");
    }

    @Test
    void guideProtectsSecretsAndPersistentProductionData() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("deployment secret manager")
                .contains("never commit private keys")
                .contains("must not expose passwords, tokens, API keys")
                .contains("Do not use `docker compose down -v`")
                .contains("pre-deployment backup")
                .contains("restore has been rehearsed in non-production");
    }
}
