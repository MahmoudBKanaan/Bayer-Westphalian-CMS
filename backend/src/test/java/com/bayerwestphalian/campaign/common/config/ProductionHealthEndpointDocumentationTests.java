package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("731 Enable health endpoint")
class ProductionHealthEndpointDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path NGINX = Path.of("../docker/nginx/nginx.conf");
    private static final Path GUIDE = Path.of("../docs/deployment/health-endpoints.md");

    @Test
    void productionDefinesSafeLivenessAndReadinessGroups() throws Exception {
        String profile = Files.readString(PROFILE, StandardCharsets.UTF_8);

        assertThat(profile)
                .contains("include: health,info")
                .contains("add-additional-paths: true")
                .contains("show-details: never")
                .contains("show-components: never")
                .contains("include: livenessState,ping")
                .contains("include: readinessState,db,diskSpace,consentEvidenceStorage");
    }

    @Test
    void composeAndProxyUseReadinessWhilePublishingLivenessSeparately() throws Exception {
        assertThat(Files.readString(COMPOSE, StandardCharsets.UTF_8))
                .contains("/actuator/health/readiness || exit 1");
        String nginx = Files.readString(NGINX, StandardCharsets.UTF_8);
        assertThat(nginx)
                .contains("location = /livez")
                .contains("/actuator/health/liveness")
                .contains("location = /readyz")
                .contains("location = /healthz")
                .contains("/actuator/health/readiness");
    }

    @Test
    void guideDocumentsMeaningSecurityAndOperations() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 731")
                .contains("excludes external dependencies")
                .contains("do not expose database URLs")
                .contains("Health status is operational evidence")
                .contains("ProductionHealthEndpointDocumentationTests");
    }
}
