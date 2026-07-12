package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Product catalog service (KB E10 / FR-040–FR-043).
 *
 * <p>Item 527: every successful product create, update, deactivate, and soft-delete writes an
 * immutable audit row on entity type {@code products} via {@link AuditService} with the acting
 * Product Manager/Admin principal and before/after payloads.
 */
@Service
public class ProductService {

    /** KB audit entity type for product catalog rows. */
    public static final String AUDIT_ENTITY_TYPE = "products";

    private final ProductRepository productRepository;
    private final AuthorizationExpressions authorizationExpressions;
    private final AuditService auditService;

    public ProductService(
            ProductRepository productRepository,
            AuthorizationExpressions authorizationExpressions,
            AuditService auditService) {
        this.productRepository = productRepository;
        this.authorizationExpressions = authorizationExpressions;
        this.auditService = auditService;
    }

    /**
     * Creates a product (FR-041).
     *
     * <p>Item 527: writes {@code CREATE} on {@code products} with actor and product payload.
     */
    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductView createProduct(CreateProductCommand command) {
        validateCreateCommand(command);

        Product product =
                Product.create(
                        command.name().trim(),
                        command.productType(),
                        command.price(),
                        command.durationMonths());
        product.updateDetails(
                command.name().trim(),
                command.productType(),
                normalize(command.description()),
                command.durationMonths(),
                normalize(command.expirationPolicy()));
        if (command.price() != null) {
            product.updatePricing(command.price());
        }

        Product savedProduct = productRepository.save(product);
        // Item 527: log product creation.
        auditService.logCreate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                savedProduct.getId(),
                productAuditPayload(savedProduct));

        return ProductView.from(savedProduct);
    }

    /**
     * Updates product details / active flag (FR-042).
     *
     * <p>Item 527: writes {@code UPDATE} with before/after product payloads.
     */
    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductView updateProduct(UUID productId, UpdateProductCommand command) {
        validateProductId(productId);
        validateUpdateCommand(command);
        Product product = findProduct(productId);
        Map<String, Object> oldValue = productAuditPayload(product);

        product.updateDetails(
                command.name().trim(),
                command.productType(),
                normalize(command.description()),
                command.durationMonths(),
                normalize(command.expirationPolicy()));
        product.updatePricing(command.price());
        applyActiveState(product, command.active());

        Product savedProduct = productRepository.save(product);
        // Item 527: log product change.
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                savedProduct.getId(),
                oldValue,
                productAuditPayload(savedProduct));

        return ProductView.from(savedProduct);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'PRODUCT_MANAGER', "
                    + "'COMPLIANCE_OFFICER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', "
                    + "'EXECUTIVE_VIEWER')")
    @Transactional(readOnly = true)
    public ProductView findById(UUID productId) {
        validateProductId(productId);
        return ProductView.from(findProduct(productId));
    }

    /**
     * Soft-deletes a product (FR-043). Rows remain with {@code deletedAt}; hard delete is not MVP.
     *
     * <p>Item 527: writes {@code DELETE} with before/after {@code deleted} flags.
     */
    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductView softDeleteProduct(UUID productId) {
        validateProductId(productId);
        Product product = findProduct(productId);
        Map<String, Object> oldValue = productAuditPayload(product);

        product.softDelete();
        Product savedProduct = productRepository.save(product);
        // Item 527: log product soft delete.
        auditService.logDelete(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                savedProduct.getId(),
                oldValue,
                productAuditPayload(savedProduct));

        return ProductView.from(savedProduct);
    }

    /**
     * Deactivates a product without soft-delete (FR-043 disable path).
     *
     * <p>Item 527: writes {@code UPDATE} with {@code active} true → false.
     */
    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductView deactivateProduct(UUID productId) {
        validateProductId(productId);
        Product product = findProduct(productId);
        Map<String, Object> oldValue = productAuditPayload(product);

        product.deactivate();
        Product savedProduct = productRepository.save(product);
        // Item 527: log product disable.
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                savedProduct.getId(),
                oldValue,
                productAuditPayload(savedProduct));

        return ProductView.from(savedProduct);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'PRODUCT_MANAGER', "
                    + "'COMPLIANCE_OFFICER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', "
                    + "'EXECUTIVE_VIEWER')")
    @Transactional(readOnly = true)
    public List<ProductView> searchProducts(ProductSearchCriteria criteria) {
        ProductSearchCriteria normalized = normalize(criteria);

        return loadCandidates(normalized).stream()
                .filter(product -> !product.isDeleted())
                .filter(product -> matches(product, normalized))
                .map(ProductView::from)
                .toList();
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'PRODUCT_MANAGER', "
                    + "'COMPLIANCE_OFFICER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', "
                    + "'EXECUTIVE_VIEWER')")
    @Transactional(readOnly = true)
    public List<ProductView> findActiveProducts() {
        return productRepository.findActive().stream().map(ProductView::from).toList();
    }

    private List<Product> loadCandidates(ProductSearchCriteria criteria) {
        if (StringUtils.hasText(criteria.term())) {
            return productRepository.searchByNameOrType(criteria.term());
        }
        if (criteria.productType() != null) {
            return productRepository.findByType(criteria.productType());
        }
        if (Boolean.TRUE.equals(criteria.active())) {
            return productRepository.findActive();
        }
        return productRepository.findAll();
    }

    private boolean matches(Product product, ProductSearchCriteria criteria) {
        return matchesProductType(product, criteria.productType())
                && matchesActive(product, criteria.active());
    }

    private boolean matchesProductType(Product product, ProductType productType) {
        return productType == null || product.getProductType() == productType;
    }

    private boolean matchesActive(Product product, Boolean active) {
        return active == null || product.isActive() == active;
    }

    private ProductSearchCriteria normalize(ProductSearchCriteria criteria) {
        if (criteria == null) {
            return new ProductSearchCriteria(null, null, null);
        }
        return new ProductSearchCriteria(
                normalize(criteria.term()), criteria.productType(), criteria.active());
    }

    private Product findProduct(UUID productId) {
        return productRepository
                .findById(productId)
                .filter(product -> !product.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private void applyActiveState(Product product, Boolean active) {
        if (active == null) {
            return;
        }
        if (active) {
            product.activate();
        } else {
            product.deactivate();
        }
    }

    private void validateCreateCommand(CreateProductCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Product validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("name", command.name()),
                                required("productType", command.productType()),
                                validatePrice("price", command.price()),
                                validateDurationMonths("durationMonths", command.durationMonths()),
                                validateLength("expirationPolicy", command.expirationPolicy(), 100))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Product validation failed", errors);
        }
    }

    private void validateUpdateCommand(UpdateProductCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Product validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("name", command.name()),
                                required("productType", command.productType()),
                                validatePrice("price", command.price()),
                                validateDurationMonths("durationMonths", command.durationMonths()),
                                validateLength("expirationPolicy", command.expirationPolicy(), 100))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Product validation failed", errors);
        }
    }

    private void validateProductId(UUID productId) {
        if (productId == null) {
            throw new ValidationException(
                    "Product validation failed", List.of("productId: is required"));
        }
    }

    private String required(String fieldName, String value) {
        return StringUtils.hasText(value) ? "" : fieldName + ": must not be blank";
    }

    private String required(String fieldName, Object value) {
        return value == null ? fieldName + ": must not be null" : "";
    }

    private String validatePrice(String fieldName, BigDecimal price) {
        if (price != null && price.signum() < 0) {
            return fieldName + ": must be greater than or equal to 0.00";
        }
        return "";
    }

    private String validateDurationMonths(String fieldName, Integer durationMonths) {
        if (durationMonths != null && durationMonths < 1) {
            return fieldName + ": must be greater than 0";
        }
        return "";
    }

    private String validateLength(String fieldName, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            return fieldName + ": must be at most " + maxLength + " characters";
        }
        return "";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Product audit payload for item 527 (never includes secrets). */
    private Map<String, Object> productAuditPayload(Product product) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (product.getId() != null) {
            payload.put("id", product.getId().toString());
        }
        payload.put("name", product.getName());
        payload.put("productType", product.getProductType().name());
        putIfPresent(payload, "description", product.getDescription());
        if (product.getPrice() != null) {
            payload.put("price", product.getPrice());
        }
        if (product.getDurationMonths() != null) {
            payload.put("durationMonths", product.getDurationMonths());
        }
        putIfPresent(payload, "expirationPolicy", product.getExpirationPolicy());
        payload.put("active", product.getActive());
        payload.put("deleted", product.isDeleted());
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private UUID currentActorUserId() {
        return authorizationExpressions.isAuthenticated()
                ? authorizationExpressions.currentUserId()
                : null;
    }
}
