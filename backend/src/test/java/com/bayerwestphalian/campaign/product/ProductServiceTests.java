package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Mock private ProductRepository productRepository;

    @Mock private AuthorizationExpressions authorizationExpressions;

    @Mock private AuditService auditService;

    @InjectMocks private ProductService productService;

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorizeWithExpression(
                "createProduct",
                new Class<?>[] {CreateProductCommand.class},
                "@authz.canManageProducts()");
        assertPreAuthorizeWithExpression(
                "updateProduct",
                new Class<?>[] {UUID.class, UpdateProductCommand.class},
                "@authz.canManageProducts()");
        assertPreAuthorize("findById", UUID.class);
        assertPreAuthorize("softDeleteProduct", UUID.class);
        assertPreAuthorizeWithExpression(
                "deactivateProduct", new Class<?>[] {UUID.class}, "@authz.canManageProducts()");
        assertPreAuthorize("searchProducts", ProductSearchCriteria.class);
        assertPreAuthorize("findActiveProducts");
    }

    @Test
    void createsProductFromKbCommandAndAuditsCreation() throws Exception {
        when(productRepository.save(any(Product.class)))
                .thenAnswer(
                        invocation -> {
                            Product product = invocation.getArgument(0);
                            setId(product, PRODUCT_ID);
                            return product;
                        });

        ProductView view =
                productService.createProduct(
                        new CreateProductCommand(
                                "  Life Protection  ",
                                ProductType.LIFE_INSURANCE,
                                " Coverage for beneficiaries ",
                                new BigDecimal("129.99"),
                                24,
                                " EXPIRES_AT_TERM_END "));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Life Protection");
        assertThat(saved.getProductType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(saved.getDescription()).isEqualTo("Coverage for beneficiaries");
        assertThat(saved.getPrice()).isEqualByComparingTo("129.99");
        assertThat(saved.getDurationMonths()).isEqualTo(24);
        assertThat(saved.getExpirationPolicy()).isEqualTo("EXPIRES_AT_TERM_END");
        assertThat(saved.isActive()).isTrue();
        assertThat(view.name()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        // Item 527: product create writes CREATE on products (actor may be null without principal).
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> createPayload = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logCreate(eq((UUID) null), eq("products"), eq(PRODUCT_ID), createPayload.capture());
        assertThat(createPayload.getValue())
                .containsEntry("id", PRODUCT_ID.toString())
                .containsEntry("name", "Life Protection")
                .containsEntry("productType", "LIFE_INSURANCE")
                .containsEntry("description", "Coverage for beneficiaries")
                .containsEntry("price", new BigDecimal("129.99"))
                .containsEntry("durationMonths", 24)
                .containsEntry("expirationPolicy", "EXPIRES_AT_TERM_END")
                .containsEntry("active", true)
                .containsEntry("deleted", false);
    }

    @Test
    void validatesCreateProductCommand() {
        assertThatThrownBy(() -> productService.createProduct(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product validation failed");

        assertThatThrownBy(
                        () ->
                                productService.createProduct(
                                        new CreateProductCommand(
                                                " ",
                                                null,
                                                null,
                                                new BigDecimal("-1.00"),
                                                0,
                                                "X".repeat(101))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product validation failed");
    }

    @Test
    void updatesProductDetailsAndActiveState() throws Exception {
        Product product = product();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductView view =
                productService.updateProduct(
                        PRODUCT_ID,
                        new UpdateProductCommand(
                                " Life Protection Plus ",
                                ProductType.LIFE_INSURANCE,
                                " Expanded beneficiary coverage ",
                                new BigDecimal("149.50"),
                                36,
                                " AUTO_RENEW ",
                                false));

        assertThat(view.name()).isEqualTo("Life Protection Plus");
        assertThat(view.description()).isEqualTo("Expanded beneficiary coverage");
        assertThat(view.price()).isEqualByComparingTo("149.50");
        assertThat(view.durationMonths()).isEqualTo(36);
        assertThat(view.expirationPolicy()).isEqualTo("AUTO_RENEW");
        assertThat(view.active()).isFalse();
        verify(productRepository).save(product);
        verify(auditService)
                .logUpdate(
                        eq((UUID) null),
                        eq("products"),
                        eq(PRODUCT_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void findsProductById() throws Exception {
        Product product = product();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        ProductView view = productService.findById(PRODUCT_ID);

        assertThat(view.id()).isEqualTo(PRODUCT_ID);
        assertThat(view.name()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(view.description()).isEqualTo("Coverage for beneficiaries");
        assertThat(view.price()).isEqualByComparingTo("129.99");
        assertThat(view.durationMonths()).isEqualTo(24);
        assertThat(view.expirationPolicy()).isEqualTo("EXPIRES_AT_TERM_END");
        assertThat(view.active()).isTrue();
        assertThat(view.deleted()).isFalse();
        verify(productRepository).findById(PRODUCT_ID);
    }

    @Test
    void softDeletesProductAndAuditsDeletion() throws Exception {
        Product product = product();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductView view = productService.softDeleteProduct(PRODUCT_ID);

        assertThat(view.deleted()).isTrue();
        assertThat(view.active()).isTrue();
        verify(productRepository).save(product);
        verify(auditService)
                .logDelete(
                        eq((UUID) null),
                        eq("products"),
                        eq(PRODUCT_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void deactivatesProductAndAuditsUpdate() throws Exception {
        Product product = product();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductView view = productService.deactivateProduct(PRODUCT_ID);

        assertThat(view.active()).isFalse();
        verify(productRepository).save(product);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logUpdate(
                        eq((UUID) null),
                        eq("products"),
                        eq(PRODUCT_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());

        assertThat(castAuditPayload(oldValueCaptor.getValue())).containsEntry("active", true);
        assertThat(castAuditPayload(newValueCaptor.getValue())).containsEntry("active", false);
    }

    @Test
    void searchesProductsByTypeOnly() throws Exception {
        Product lifeProduct = product();
        Product homeProduct =
                Product.create(
                        "Home Protection",
                        ProductType.HOMEOWNER_INSURANCE,
                        new BigDecimal("89.00"),
                        12);
        when(productRepository.findByType(ProductType.LIFE_INSURANCE))
                .thenReturn(List.of(lifeProduct, homeProduct));

        List<ProductView> views =
                productService.searchProducts(
                        new ProductSearchCriteria(null, ProductType.LIFE_INSURANCE, null));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        verify(productRepository).findByType(ProductType.LIFE_INSURANCE);
    }

    @Test
    void searchesActiveProductsOnly() throws Exception {
        Product activeProduct = product();
        Product inactiveProduct =
                Product.create("Retired Cover", ProductType.OTHER, new BigDecimal("10.00"), 6);
        inactiveProduct.deactivate();
        when(productRepository.findActive()).thenReturn(List.of(activeProduct, inactiveProduct));

        List<ProductView> views =
                productService.searchProducts(new ProductSearchCriteria(null, null, true));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).active()).isTrue();
        verify(productRepository).findActive();
    }

    @Test
    void returnsNonDeletedProductsWhenNoSearchFiltersProvided() throws Exception {
        Product activeProduct = product();
        Product deletedProduct =
                Product.create("Retired Cover", ProductType.OTHER, new BigDecimal("10.00"), 6);
        deletedProduct.softDelete();
        when(productRepository.findAll()).thenReturn(List.of(activeProduct, deletedProduct));

        List<ProductView> views =
                productService.searchProducts(new ProductSearchCriteria(null, null, null));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).name()).isEqualTo("Life Protection");
        verify(productRepository).findAll();
    }

    @Test
    void searchesProductsWithKbFilters() throws Exception {
        Product activeLifeProduct = product();
        Product inactiveHomeProduct =
                Product.create(
                        "Home Protection",
                        ProductType.HOMEOWNER_INSURANCE,
                        new BigDecimal("89.00"),
                        12);
        inactiveHomeProduct.deactivate();
        Product deletedProduct =
                Product.create("Retired Cover", ProductType.OTHER, new BigDecimal("10.00"), 6);
        deletedProduct.softDelete();
        when(productRepository.searchByNameOrType("life"))
                .thenReturn(List.of(activeLifeProduct, inactiveHomeProduct, deletedProduct));

        List<ProductView> views =
                productService.searchProducts(
                        new ProductSearchCriteria("  life  ", ProductType.LIFE_INSURANCE, true));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).name()).isEqualTo("Life Protection");
        assertThat(views.get(0).productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(views.get(0).active()).isTrue();
        verify(productRepository).searchByNameOrType("life");
    }

    @Test
    void findsActiveProductsFromRepository() throws Exception {
        Product activeProduct = product();
        when(productRepository.findActive()).thenReturn(List.of(activeProduct));

        List<ProductView> views = productService.findActiveProducts();

        assertThat(views).hasSize(1);
        assertThat(views.get(0).name()).isEqualTo("Life Protection");
        assertThat(views.get(0).active()).isTrue();
        verify(productRepository).findActive();
    }

    @Test
    void rejectsMissingOrSoftDeletedProducts() throws Exception {
        Product deletedProduct = product();
        deletedProduct.softDelete();
        UUID missingId = UUID.fromString("40000000-0000-0000-0000-000000000099");

        assertThatThrownBy(() -> productService.findById(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product validation failed");

        assertThatThrownBy(() -> productService.softDeleteProduct(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product validation failed");

        assertThatThrownBy(() -> productService.deactivateProduct(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product validation failed");

        when(productRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.findById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product was not found: " + missingId);
        assertThatThrownBy(() -> productService.softDeleteProduct(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product was not found: " + missingId);
        assertThatThrownBy(() -> productService.deactivateProduct(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product was not found: " + missingId);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(deletedProduct));
        assertThatThrownBy(() -> productService.findById(PRODUCT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product was not found: " + PRODUCT_ID);
        assertThatThrownBy(() -> productService.softDeleteProduct(PRODUCT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product was not found: " + PRODUCT_ID);
        assertThatThrownBy(() -> productService.deactivateProduct(PRODUCT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product was not found: " + PRODUCT_ID);
    }

    private static void assertPreAuthorize(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = ProductService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = ProductService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expectedExpression);
    }

    private static Product product() throws Exception {
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        product.updateDetails(
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "Coverage for beneficiaries",
                24,
                "EXPIRES_AT_TERM_END");
        setId(product, PRODUCT_ID);
        return product;
    }

    private static void setId(Product product, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(product, id);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castAuditPayload(Map<String, ?> payload) {
        return (Map<String, Object>) payload;
    }
}
