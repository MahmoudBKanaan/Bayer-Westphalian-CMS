package com.bayerwestphalian.campaign.common.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/** Canonical production CORS origin validation shared by startup and Spring Security. */
public final class ProductionCorsOrigins {

    private ProductionCorsOrigins() {}

    public static List<String> validate(List<String> origins) {
        if (origins == null || origins.isEmpty()) {
            throw new IllegalStateException(
                    "Production CORS requires CORS_ALLOWED_ORIGINS with at least one explicit "
                            + "HTTPS origin");
        }

        for (String origin : origins) {
            validateOrigin(origin);
        }
        return List.copyOf(origins);
    }

    public static List<String> parseAndValidate(String configuredOrigins) {
        if (configuredOrigins == null || configuredOrigins.isBlank()) {
            return validate(List.of());
        }
        String[] entries = configuredOrigins.split(",", -1);
        List<String> origins = java.util.Arrays.stream(entries).map(String::trim).toList();
        return validate(origins);
    }

    private static void validateOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalStateException("Production CORS origin entries must not be blank");
        }
        if (origin.contains("*")) {
            throw new IllegalStateException("Production CORS must not use wildcard origins");
        }

        URI uri;
        try {
            uri = new URI(origin);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Production CORS origins must be valid URIs");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException("Production CORS origins must use HTTPS (https://)");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException("Production CORS origins must include an explicit host");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("localhost")
                || host.equals("127.0.0.1")
                || host.equals("::1")
                || host.equals("[::1]")
                || host.endsWith(".localhost")) {
            throw new IllegalStateException("Production CORS must not allow localhost origins");
        }
        if (uri.getPort() > 65535) {
            throw new IllegalStateException("Production CORS origin port must be valid");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalStateException("Production CORS origins must not contain user info");
        }
        if ((uri.getPath() != null && !uri.getPath().isEmpty())
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException(
                    "Production CORS entries must be origins only, without path, query, or fragment");
        }
    }
}
