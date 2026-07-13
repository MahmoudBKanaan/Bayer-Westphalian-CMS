package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTests {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void preservesSafeRequestIdInResponseAndLoggingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "request-729");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        FilterChain chain =
                (ignoredRequest, ignoredResponse) ->
                        requestIdInChain.set(
                                MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(requestIdInChain).hasValue("request-729");
        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .isEqualTo("request-729");
        assertThat(request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTRIBUTE))
                .isEqualTo("request-729");
        assertThat(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeOrMissingRequestIds() {
        assertThat(RequestCorrelationFilter.resolveRequestId("unsafe value\nforged"))
                .matches("[0-9a-f-]{36}");
        assertThat(RequestCorrelationFilter.resolveRequestId(null)).matches("[0-9a-f-]{36}");
        assertThat(RequestCorrelationFilter.resolveRequestId("x".repeat(129)))
                .matches("[0-9a-f-]{36}");
    }
}
