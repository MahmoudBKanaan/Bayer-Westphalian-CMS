package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("721 Configure reverse proxy with Nginx or Caddy")
class ProductionReverseProxyDocumentationTests {

    private static final Path NGINX = Path.of("../docker/nginx/nginx.conf");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path GUIDE = Path.of("../docs/deployment/reverse-proxy.md");

    @Test
    void nginxRoutesApiFrontendAndHealthWithoutStrippingApiPrefix() throws Exception {
        String nginx = Files.readString(NGINX, StandardCharsets.UTF_8);

        assertThat(nginx)
                .contains("items 721-722")
                .contains("server backend:8080")
                .contains("server frontend:80")
                .contains("location /api/")
                .contains("proxy_pass http://backend_upstream;")
                .contains("location / {")
                .contains("proxy_pass http://frontend_upstream;")
                .contains("location = /healthz")
                .contains("location = /proxy-healthz");
    }

    @Test
    void nginxForwardsTrustedRequestMetadataAndUsesBoundedSettings() throws Exception {
        String nginx = Files.readString(NGINX, StandardCharsets.UTF_8);

        assertThat(nginx)
                .contains("X-Forwarded-For $proxy_add_x_forwarded_for")
                .contains("X-Forwarded-Proto https")
                .contains("X-Forwarded-Host $host")
                .contains("X-Forwarded-Port 443")
                .contains("client_max_body_size 20m")
                .contains("proxy_connect_timeout 10s")
                .contains("server_tokens off");
    }

    @Test
    void composePublishesOnlyTheReverseProxyApplicationPort() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);
        String backend = serviceBlock(compose, "backend", "frontend");
        String frontend = serviceBlock(compose, "frontend", "reverse-proxy");
        String proxy =
                compose.substring(
                        compose.indexOf("  reverse-proxy:"), compose.indexOf("\nvolumes:"));

        assertThat(backend).doesNotContain("ports:");
        assertThat(frontend).doesNotContain("ports:");
        assertThat(proxy)
                .contains("image: nginx:1.27-alpine")
                .contains("${HTTP_PORT:-80}:8080")
                .contains("./docker/nginx/nginx.conf:/etc/nginx/nginx.conf:ro")
                .contains("condition: service_healthy")
                .contains("read_only: true");
    }

    @Test
    void guideDocumentsRoutingAndHttpsBoundary() throws Exception {
        String guide = DocumentationTestText.normalize(Files.readString(GUIDE, StandardCharsets.UTF_8));

        assertThat(guide)
                .contains("Sprint 18 item 721")
                .contains("only published HTTP entry point")
                .contains("Item **722** adds [HTTPS/TLS]")
                .contains("ProductionReverseProxyDocumentationTests");
    }

    private static String serviceBlock(String compose, String service, String nextSection) {
        int start = compose.indexOf("  " + service + ":");
        int end = compose.indexOf("  " + nextSection + ":", start + 1);
        return compose.substring(start, end);
    }
}
