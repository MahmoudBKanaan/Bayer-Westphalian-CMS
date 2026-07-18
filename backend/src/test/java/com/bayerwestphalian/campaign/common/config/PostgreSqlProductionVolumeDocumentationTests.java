package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("720 Configure PostgreSQL production volume")
class PostgreSqlProductionVolumeDocumentationTests {

    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path ENV_TEMPLATE = Path.of(".env.production.example");
    private static final Path GUIDE = Path.of("../docs/deployment/postgres-production-volume.md");

    @Test
    void composeDefinesStablePersistentPostgresStorage() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("bwc_postgres_prod_data:/var/lib/postgresql/data")
                .contains("PGDATA: /var/lib/postgresql/data/pgdata")
                .contains("POSTGRES_INITDB_ARGS")
                .contains("--data-checksums --encoding=UTF8")
                .contains("name: ${POSTGRES_VOLUME_NAME:-bwc_postgres_prod_data}")
                .contains("driver: local")
                .contains("backup-required: \"true\"")
                .contains("stop_grace_period: ${POSTGRES_STOP_GRACE_PERIOD:-60s}")
                .contains("shm_size: ${POSTGRES_SHM_SIZE:-256m}");
    }

    @Test
    void postgresVolumeRemainsPrivateAndConfigurable() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);
        String postgresBlock =
                compose.substring(
                        compose.indexOf("  postgres:"), compose.indexOf("  backend:"));
        String template = Files.readString(ENV_TEMPLATE, StandardCharsets.UTF_8);

        assertThat(postgresBlock).doesNotContain("ports:");
        assertThat(compose).contains("database:").contains("internal: true");
        assertThat(template)
                .contains("POSTGRES_VOLUME_NAME=bwc_postgres_prod_data")
                .contains("POSTGRES_SHM_SIZE=256m")
                .contains("POSTGRES_STOP_GRACE_PERIOD=60s");
    }

    @Test
    void guideDocumentsLifecycleAndBackupSafety() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("Sprint 18 item 720")
                .contains("docker compose down -v")
                .contains("scheduled logical backups")
                .contains("SHOW data_checksums")
                .contains("PostgreSqlProductionVolumeDocumentationTests");
    }
}
