package com.bayerwestphalian.campaign.common.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String serviceName;

    public HealthController(
            @Value("${spring.application.name:bayer-westphalian-campaign-platform}")
                    String serviceName) {
        this.serviceName = serviceName;
    }

    @GetMapping
    HealthResponse health() {
        return HealthResponse.up(serviceName);
    }
}
