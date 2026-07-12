package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class ProductControllerTests {

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
    void exposesProductApiRoute() {
        assertThat(ProductController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(ProductController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/products");
    }

    @Test
    void searchesProductsByTermOnly() throws Exception {
        when(productService.searchProducts(any(ProductSearchCriteria.class)))
                .thenReturn(List.of(productView()));

        mockMvc.perform(get("/api/products").param("term", "protection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Products loaded"))
                .andExpect(jsonPath("$.data[0].name").value("Life Protection"));

        ArgumentCaptor<ProductSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).searchProducts(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isEqualTo("protection");
        assertThat(criteriaCaptor.getValue().productType()).isNull();
        assertThat(criteriaCaptor.getValue().active()).isNull();
    }

    @Test
    void searchesProductsByTypeOnly() throws Exception {
        when(productService.searchProducts(any(ProductSearchCriteria.class)))
                .thenReturn(List.of(productView()));

        mockMvc.perform(get("/api/products").param("productType", "LIFE_INSURANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productType").value("LIFE_INSURANCE"));

        ArgumentCaptor<ProductSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).searchProducts(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isNull();
        assertThat(criteriaCaptor.getValue().productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(criteriaCaptor.getValue().active()).isNull();
    }

    @Test
    void searchesProductsByActiveOnly() throws Exception {
        when(productService.searchProducts(any(ProductSearchCriteria.class)))
                .thenReturn(List.of(productView()));

        mockMvc.perform(get("/api/products").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].active").value(true));

        ArgumentCaptor<ProductSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).searchProducts(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isNull();
        assertThat(criteriaCaptor.getValue().productType()).isNull();
        assertThat(criteriaCaptor.getValue().active()).isTrue();
    }

    @Test
    void searchesProductsWithoutFilters() throws Exception {
        when(productService.searchProducts(any(ProductSearchCriteria.class)))
                .thenReturn(List.of(productView()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(PRODUCT_ID.toString()));

        ArgumentCaptor<ProductSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).searchProducts(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isNull();
        assertThat(criteriaCaptor.getValue().productType()).isNull();
        assertThat(criteriaCaptor.getValue().active()).isNull();
    }

    @Test
    void rejectsOversizedProductSearchTerm() throws Exception {
        mockMvc.perform(get("/api/products").param("term", "x".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void listsProductsWithKbFilters() throws Exception {
        when(productService.searchProducts(any(ProductSearchCriteria.class)))
                .thenReturn(List.of(productView()));

        mockMvc.perform(
                        get("/api/products")
                                .param("term", "life")
                                .param("productType", "LIFE_INSURANCE")
                                .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Products loaded"))
                .andExpect(jsonPath("$.data[0].id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Life Protection"))
                .andExpect(jsonPath("$.data[0].productType").value("LIFE_INSURANCE"))
                .andExpect(jsonPath("$.data[0].active").value(true));

        ArgumentCaptor<ProductSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).searchProducts(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isEqualTo("life");
        assertThat(criteriaCaptor.getValue().productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(criteriaCaptor.getValue().active()).isTrue();
    }

    @Test
    void getsProductById() throws Exception {
        when(productService.findById(PRODUCT_ID)).thenReturn(productView());

        mockMvc.perform(get("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product loaded"))
                .andExpect(jsonPath("$.data.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Life Protection"))
                .andExpect(jsonPath("$.data.price").value(129.99))
                .andExpect(jsonPath("$.data.durationMonths").value(24));

        verify(productService).findById(PRODUCT_ID);
    }

    @Test
    void createsProduct() throws Exception {
        when(productService.createProduct(any(CreateProductCommand.class)))
                .thenReturn(productView());

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Life Protection",
                                          "productType": "LIFE_INSURANCE",
                                          "description": "Coverage for beneficiaries",
                                          "price": 129.99,
                                          "durationMonths": 24,
                                          "expirationPolicy": "EXPIRES_AT_TERM_END"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product created"))
                .andExpect(jsonPath("$.data.name").value("Life Protection"))
                .andExpect(jsonPath("$.data.productType").value("LIFE_INSURANCE"))
                .andExpect(jsonPath("$.data.description").value("Coverage for beneficiaries"))
                .andExpect(jsonPath("$.data.price").value(129.99))
                .andExpect(jsonPath("$.data.durationMonths").value(24))
                .andExpect(jsonPath("$.data.expirationPolicy").value("EXPIRES_AT_TERM_END"))
                .andExpect(jsonPath("$.data.active").value(true));

        ArgumentCaptor<CreateProductCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateProductCommand.class);
        verify(productService).createProduct(commandCaptor.capture());
        assertThat(commandCaptor.getValue().name()).isEqualTo("Life Protection");
        assertThat(commandCaptor.getValue().productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(commandCaptor.getValue().description()).isEqualTo("Coverage for beneficiaries");
        assertThat(commandCaptor.getValue().price()).isEqualByComparingTo("129.99");
        assertThat(commandCaptor.getValue().durationMonths()).isEqualTo(24);
        assertThat(commandCaptor.getValue().expirationPolicy()).isEqualTo("EXPIRES_AT_TERM_END");
    }

    @Test
    void rejectsInvalidCreateProductRequest() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void updatesProduct() throws Exception {
        when(productService.updateProduct(any(UUID.class), any(UpdateProductCommand.class)))
                .thenReturn(updatedProductView());

        mockMvc.perform(
                        put("/api/products/{id}", PRODUCT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Life Protection Plus",
                                          "productType": "LIFE_INSURANCE",
                                          "description": "Expanded beneficiary coverage",
                                          "price": 149.50,
                                          "durationMonths": 36,
                                          "expirationPolicy": "AUTO_RENEW",
                                          "active": false
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product updated"))
                .andExpect(jsonPath("$.data.name").value("Life Protection Plus"))
                .andExpect(jsonPath("$.data.price").value(149.50))
                .andExpect(jsonPath("$.data.durationMonths").value(36))
                .andExpect(jsonPath("$.data.expirationPolicy").value("AUTO_RENEW"))
                .andExpect(jsonPath("$.data.active").value(false));

        ArgumentCaptor<UpdateProductCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateProductCommand.class);
        verify(productService).updateProduct(eq(PRODUCT_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().name()).isEqualTo("Life Protection Plus");
        assertThat(commandCaptor.getValue().productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(commandCaptor.getValue().description())
                .isEqualTo("Expanded beneficiary coverage");
        assertThat(commandCaptor.getValue().price()).isEqualByComparingTo("149.50");
        assertThat(commandCaptor.getValue().durationMonths()).isEqualTo(36);
        assertThat(commandCaptor.getValue().expirationPolicy()).isEqualTo("AUTO_RENEW");
        assertThat(commandCaptor.getValue().active()).isFalse();
    }

    @Test
    void rejectsInvalidUpdateProductRequest() throws Exception {
        mockMvc.perform(
                        put("/api/products/{id}", PRODUCT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": " ",
                                          "productType": "LIFE_INSURANCE"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/products/" + PRODUCT_ID))
                .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
    }

    @Test
    void disablesProduct() throws Exception {
        when(productService.deactivateProduct(PRODUCT_ID)).thenReturn(disabledProductView());

        mockMvc.perform(patch("/api/products/{id}/disable", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product disabled"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.deleted").value(false));

        verify(productService).deactivateProduct(PRODUCT_ID);
    }

    @Test
    void mapsMissingProductOnDisableToNotFoundResponse() throws Exception {
        when(productService.deactivateProduct(PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

        mockMvc.perform(patch("/api/products/{id}/disable", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/products/" + PRODUCT_ID + "/disable"));
    }

    @Test
    void deletesProduct() throws Exception {
        when(productService.softDeleteProduct(PRODUCT_ID)).thenReturn(deletedProductView());

        mockMvc.perform(delete("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product deleted"))
                .andExpect(jsonPath("$.data.deleted").value(true));

        verify(productService).softDeleteProduct(PRODUCT_ID);
    }

    @Test
    void mapsMissingProductToNotFoundResponse() throws Exception {
        when(productService.findById(PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

        mockMvc.perform(get("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/products/" + PRODUCT_ID));
    }

    @Test
    void mapsMissingProductOnUpdateToNotFoundResponse() throws Exception {
        when(productService.updateProduct(any(UUID.class), any(UpdateProductCommand.class)))
                .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

        mockMvc.perform(
                        put("/api/products/{id}", PRODUCT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Life Protection Plus",
                                          "productType": "LIFE_INSURANCE",
                                          "price": 149.50,
                                          "durationMonths": 36
                                        }
                                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/products/" + PRODUCT_ID));
    }

    @Test
    void mapsMissingProductOnDeleteToNotFoundResponse() throws Exception {
        when(productService.softDeleteProduct(PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

        mockMvc.perform(delete("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/products/" + PRODUCT_ID));
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

    private static ProductView updatedProductView() {
        return new ProductView(
                PRODUCT_ID,
                "Life Protection Plus",
                ProductType.LIFE_INSURANCE,
                "Expanded beneficiary coverage",
                new BigDecimal("149.50"),
                36,
                "AUTO_RENEW",
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
