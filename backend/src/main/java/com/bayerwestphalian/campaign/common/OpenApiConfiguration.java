package com.bayerwestphalian.campaign.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI bayerWestphalianOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bayer-Westphalian Campaign Management Platform API")
                        .version("0.1.0")
                        .description("""
                                Internal REST API for CRM, consent-aware segmentation, campaigns,
                                reminders, analytics, reports, audit, and AI-assisted recommendations.
                                """)
                        .contact(new Contact()
                                .name("Bayer-Westphalian Internal Project Team"))
                        .license(new License()
                                .name("Internal use only")));
    }
}
