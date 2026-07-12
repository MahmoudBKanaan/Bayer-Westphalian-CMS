package com.bayerwestphalian.campaign.common.api;

import java.time.Instant;

public record HealthResponse(String status, String service, Instant timestamp) {

    public static HealthResponse up(String service) {
        return new HealthResponse("UP", service, Instant.now());
    }
}
