package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

@ExtendWith(MockitoExtension.class)
class ProductDisableSoftDeleteEndpointTests {

    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

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
    void exposesKbProductDisableEndpoint() throws Exception {
        Method disableMethod = ProductController.class.getMethod("disableProduct", UUID.class);

        assertThat(disableMethod.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(disableMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{id}/disable");
    }

    @Test
    void exposesKbProductSoftDeleteEndpoint() throws Exception {
        Method deleteMethod = ProductController.class.getMethod("deleteProduct", UUID.class);

        assertThat(deleteMethod.isAnnotationPresent(DeleteMapping.class)).isTrue();
        assertThat(deleteMethod.getAnnotation(DeleteMapping.class).value())
                .containsExactly("/{id}");
    }

    @Test
    void disableEndpointDeactivatesProductWithoutSoftDeleting() throws Exception {
        when(productService.deactivateProduct(PRODUCT_ID)).thenReturn(disabledProductView());

        mockMvc.perform(patch("/api/products/{id}/disable", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product disabled"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.deleted").value(false));

        verify(productService).deactivateProduct(PRODUCT_ID);
    }

    @Test
    void deleteEndpointSoftDeletesProduct() throws Exception {
        when(productService.softDeleteProduct(PRODUCT_ID)).thenReturn(deletedProductView());

        mockMvc.perform(delete("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted"))
                .andExpect(jsonPath("$.data.deleted").value(true));

        verify(productService).softDeleteProduct(PRODUCT_ID);
    }

    private static ProductView disabledProductView() {
        return new ProductView(
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "Coverage for beneficiaries",
                new BigDecimal("129.99"),
                24,
                "EXPIRES_AT_TERM_END",
                false,
                false,
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-04T12:00:00Z"),
                null);
    }

    private static ProductView deletedProductView() {
        return new ProductView(
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "Coverage for beneficiaries",
                new BigDecimal("129.99"),
                24,
                "EXPIRES_AT_TERM_END",
                false,
                true,
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-04T12:00:00Z"),
                Instant.parse("2026-07-04T12:00:00Z"));
    }
}
