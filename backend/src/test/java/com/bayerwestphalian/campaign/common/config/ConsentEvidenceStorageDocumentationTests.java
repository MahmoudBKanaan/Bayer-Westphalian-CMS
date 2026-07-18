package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("728 Configure file storage for consent evidence")
class ConsentEvidenceStorageDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path GUIDE = Path.of("../docs/deployment/consent-evidence-storage.md");

    @Test
    void productionConfiguresPersistentConsentEvidenceFilesystem() throws Exception {
        assertThat(Files.readString(PROFILE, StandardCharsets.UTF_8))
                .contains("mode: ${FILE_STORAGE_MODE:filesystem}")
                .contains("local-path: ${FILE_STORAGE_LOCAL_PATH:/app/data/consent-evidence}")
                .contains("max-bytes: ${FILE_STORAGE_MAX_BYTES:10485760}");
        assertThat(Files.readString(COMPOSE, StandardCharsets.UTF_8))
                .contains("bwc_consent_evidence:/app/data/consent-evidence")
                .contains("name: ${CONSENT_EVIDENCE_VOLUME_NAME:-bwc_consent_evidence}")
                .contains("com.bayer-westphalian.backup-required: \"true\"");
        assertThat(Files.readString(TEMPLATE, StandardCharsets.UTF_8))
                .contains("FILE_STORAGE_MODE=filesystem")
                .contains("FILE_STORAGE_MAX_BYTES=10485760")
                .contains("CONSENT_EVIDENCE_VOLUME_NAME=bwc_consent_evidence");
    }

    @Test
    void guideDocumentsSecurityPersistenceAndRecovery() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("Sprint 18 item 728")
                .contains("not public URLs")
                .contains("must not be served directly by Nginx")
                .contains("Back up the evidence volume together with the PostgreSQL logical backup")
                .contains("ConsentEvidenceStorageDocumentationTests");
    }
}
