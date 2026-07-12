package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@ExtendWith(MockitoExtension.class)
class ProductSearchEndpointTests {

    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Mock private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ProductController(productService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void exposesKbProductSearchEndpointOnGetProducts() throws Exception {
        Method searchMethod = ProductController.class.getMethod("searchProducts", ProductSearchRequest.class);

        assertThat(searchMethod.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(searchMethod.getParameters()[0].isAnnotationPresent(ModelAttribute.class)).isTrue();
    }

    @Test
    void kbProductSearchEndpointSupportsNameTypeAndActiveFiltersTogether() throws Exception {
        when(productService.searchProducts(any(ProductSearchCriteria.class)))
                .thenReturn(List.of(productView()));

        mockMvc.perform(
                        get("/api/products")
                                .param("term", "  life protection  ")
                                .param("productType", "LIFE_INSURANCE")
                                .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Products loaded"))
                .andExpect(jsonPath("$.data[0].id").value(PRODUCT_ID.toString()));

        ArgumentCaptor<ProductSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).searchProducts(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isEqualTo("life protection");
        assertThat(criteriaCaptor.getValue().productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(criteriaCaptor.getValue().active()).isFalse();
    }

    private static ProductView productView() {
        return new ProductView(
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "Coverage for beneficiaries",
                new BigDecimal("129.99"),
                24,
                "EXPIRES_AT_TERM_END",
                true,
                false,
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-03T12:00:00Z"),
                null);
    }
}