package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("722 Configure HTTPS")
class ProductionHttpsConfigurationDocumentationTests {

    private static final Path NGINX = Path.of("../docker/nginx/nginx.conf");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path ENV_TEMPLATE = Path.of(".env.production.example");
    private static final Path GUIDE = Path.of("../docs/deployment/https.md");

    @Test
    void nginxTerminatesModernTlsAndRedirectsHttp() throws Exception {
        String nginx = Files.readString(NGINX, StandardCharsets.UTF_8);

        assertThat(nginx)
                .contains("items 721-722")
                .contains("listen 8080")
                .contains("return 301 https://$host$request_uri")
                .contains("listen 8443 ssl")
                .contains("ssl_certificate /etc/nginx/tls/fullchain.pem")
                .contains("ssl_certificate_key /etc/nginx/tls/privkey.pem")
                .contains("ssl_protocols TLSv1.2 TLSv1.3")
                .contains("ssl_session_tickets off")
                .contains("Strict-Transport-Security");
    }

    @Test
    void tlsProxyForwardsSecureSchemeToBackend() throws Exception {
        String nginx = Files.readString(NGINX, StandardCharsets.UTF_8);

        assertThat(nginx)
                .contains("proxy_set_header X-Forwarded-Proto https")
                .contains("proxy_set_header X-Forwarded-Port 443")
                .doesNotContain("ssl_protocols TLSv1 TLSv1.1");
    }

    @Test
    void composeRequiresReadOnlyCertificateMounts() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);
        String template = Files.readString(ENV_TEMPLATE, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("${HTTPS_PORT:-443}:8443")
                .contains("TLS_CERTIFICATE_PATH is required")
                .contains("TLS_PRIVATE_KEY_PATH is required")
                .contains("target: /etc/nginx/tls/fullchain.pem")
                .contains("target: /etc/nginx/tls/privkey.pem")
                .contains("read_only: true");
        assertThat(template)
                .contains("HTTPS_PORT=443")
                .contains("TLS_CERTIFICATE_PATH=")
                .contains("TLS_PRIVATE_KEY_PATH=")
                .doesNotContain("BEGIN PRIVATE KEY");
    }

    @Test
    void guideDocumentsCertificateOperationAndVerification() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 722")
                .contains("trusted CA")
                .contains("Certificate renewal")
                .contains("HTTP returns `301`")
                .contains("ProductionHttpsConfigurationDocumentationTests");
    }
}
