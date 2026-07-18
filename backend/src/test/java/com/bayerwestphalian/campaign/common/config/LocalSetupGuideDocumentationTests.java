package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("774 Write setup guide")
class LocalSetupGuideDocumentationTests {

    private static final Path GUIDE = Path.of("../docs/development/developer-setup.md");

    @Test
    void guideCoversReproducibleLocalSetupAndVerification() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("Item 774")
                .contains("java -version")
                .contains("docker compose version")
                .contains("docker compose up -d postgres")
                .contains("SPRING_PROFILES_ACTIVE")
                .contains("npm install")
                .contains("mvn spring-boot:run")
                .contains("Verify The Running Application")
                .contains("actuator/health/readiness")
                .contains("Setup Completion Checklist");
    }

    @Test
    void guideSeparatesLocalSetupFromProductionAndProtectsSecrets() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("development and test use only")
                .contains("does **not** automatically load `backend/.env`")
                .contains("Do not commit real `.env` files")
                .contains("never a production command")
                .contains("Do not reuse demo credentials")
                .contains("do not add plaintext credentials")
                .contains("production deployment guide");
    }
}
