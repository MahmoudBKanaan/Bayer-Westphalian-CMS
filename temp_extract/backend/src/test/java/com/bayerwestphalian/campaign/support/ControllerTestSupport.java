package com.bayerwestphalian.campaign.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public final class ControllerTestSupport {

    private ControllerTestSupport() {}

    public static ObjectMapper apiObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static MockMvc standaloneController(Object controller, Object... controllerAdvice) {
        var builder = MockMvcBuilders.standaloneSetup(controller);
        for (Object advice : controllerAdvice) {
            builder.setControllerAdvice(advice);
        }
        return builder
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(apiObjectMapper()))
                .build();
    }
}