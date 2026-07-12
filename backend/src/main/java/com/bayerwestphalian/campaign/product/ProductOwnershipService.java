package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductOwnershipService {

    private final ProductOwnershipRepository productOwnershipRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AuthorizationExpressions authorizationExpressions;
    private final AuditService auditService;

    public ProductOwnershipService(
            ProductOwnershipRepository productOwnershipRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            AuthorizationExpressions authorizationExpressions,
            AuditService auditService) {
        this.productOwnershipRepository = productOwnershipRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.authorizationExpressions = authorizationExpressions;
        this.auditService = auditService;
    }

    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductOwnershipView assignProduct(CreateProductOwnershipCommand command) {
        validateAssignCommand(command);
        Product product = findAssignableProduct(command.productId());
        Customer customer = findCustomer(command.customerId());

        ProductOwnership ownership =
                ProductOwnership.create(
                        customer, product, command.startDate(), command.expirationDate());
        String policyNumber = normalize(command.policyNumber());
        if (policyNumber != null) {
            ownership.recordPolicyNumber(policyNumber);
        }

        ProductOwnership savedOwnership = productOwnershipRepository.save(ownership);
        auditService.logCreate(
                currentActorUserId(),
                "product_ownerships",
                savedOwnership.getId(),
                ownershipAuditPayload(savedOwnership));

        return ProductOwnershipView.from(savedOwnership);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'PRODUCT_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public ProductOwnershipView updateOwnership(
            UUID ownershipId, UpdateProductOwnershipCommand command) {
        validateOwnershipId(ownershipId);
        validateUpdateCommand(command);
        ProductOwnership ownership = findOwnership(ownershipId);
        Map<String, ?> oldValue = ownershipAuditPayload(ownership);

        if (command.expirationDate() != null) {
            ownership.updateExpirationDate(command.expirationDate());
        }
        String policyNumber = normalize(command.policyNumber());
        if (policyNumber != null) {
            ownership.recordPolicyNumber(policyNumber);
        }

        ProductOwnership savedOwnership = productOwnershipRepository.save(ownership);
        auditService.logUpdate(
                currentActorUserId(),
                "product_ownerships",
                savedOwnership.getId(),
                oldValue,
                ownershipAuditPayload(savedOwnership));

        return ProductOwnershipView.from(savedOwnership);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'PRODUCT_MANAGER', "
                    + "'COMPLIANCE_OFFICER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', "
                    + "'EXECUTIVE_VIEWER')")
    @Transactional(readOnly = true)
    public List<ProductOwnershipView> findExpiringWithinMonths(int months) {
        validateMonths(months);
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusMonths(months);

        return productOwnershipRepository.findExpiringBetween(today, endDate).stream()
                .filter(ownership -> ownership.isExpiringWithinMonths(months))
                .map(ProductOwnershipView::from)
                .toList();
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'PRODUCT_MANAGER', "
                    + "'COMPLIANCE_OFFICER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', "
                    + "'EXECUTIVE_VIEWER')")
    @Transactional(readOnly = true)
    public List<ProductOwnershipView> listCustomerProducts(UUID customerId) {
        validateCustomerId(customerId);

        return productOwnershipRepository.findByCustomerId(customerId).stream()
                .map(ProductOwnershipView::from)
                .toList();
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .filter(customer -> !customer.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private Product findAssignableProduct(UUID productId) {
        Product product =
                productRepository
                        .findById(productId)
                        .filter(candidate -> !candidate.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!product.isActive()) {
            throw new ValidationException(
                    "Product ownership validation failed",
                    List.of("productId: product must be active"));
        }
        return product;
    }

    private ProductOwnership findOwnership(UUID ownershipId) {
        return productOwnershipRepository
                .findById(ownershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ownership", ownershipId));
    }

    private void validateAssignCommand(CreateProductOwnershipCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Product ownership validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("customerId", command.customerId()),
                                required("productId", command.productId()),
                                required("startDate", command.startDate()),
                                validateExpirationDate(
                                        command.startDate(), command.expirationDate()),
                                validateLength("policyNumber", command.policyNumber(), 100))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Product ownership validation failed", errors);
        }
    }

    private void validateUpdateCommand(UpdateProductOwnershipCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Product ownership validation failed", List.of("command: is required"));
        }
        if (command.expirationDate() == null && !StringUtils.hasText(command.policyNumber())) {
            throw new ValidationException(
                    "Product ownership validation failed",
                    List.of("command: must include expirationDate or policyNumber"));
        }
        if (StringUtils.hasText(command.policyNumber()) && command.policyNumber().length() > 100) {
            throw new ValidationException(
                    "Product ownership validation failed",
                    List.of("policyNumber: must be at most 100 characters"));
        }
    }

    private void validateOwnershipId(UUID ownershipId) {
        if (ownershipId == null) {
            throw new ValidationException(
                    "Product ownership validation failed", List.of("ownershipId: is required"));
        }
    }

    private void validateCustomerId(UUID customerId) {
        if (customerId == null) {
            throw new ValidationException(
                    "Product ownership validation failed", List.of("customerId: is required"));
        }
    }

    private void validateMonths(int months) {
        if (months < 0) {
            throw new ValidationException(
                    "Product ownership validation failed",
                    List.of("months: must be greater than or equal to 0"));
        }
    }

    private String validateExpirationDate(LocalDate startDate, LocalDate expirationDate) {
        if (startDate != null && expirationDate != null && expirationDate.isBefore(startDate)) {
            return "expirationDate: must be on or after startDate";
        }
        return "";
    }

    private String required(String fieldName, Object value) {
        return value == null ? fieldName + ": must not be null" : "";
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

    private UUID currentActorUserId() {
        return authorizationExpressions.isAuthenticated()
                ? authorizationExpressions.currentUserId()
                : null;
    }

    private Map<String, ?> ownershipAuditPayload(ProductOwnership ownership) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", ownership.getCustomer().getId());
        payload.put("productId", ownership.getProduct().getId());
        payload.put("policyNumber", ownership.getPolicyNumber());
        payload.put("startDate", ownership.getStartDate());
        payload.put("expirationDate", ownership.getExpirationDate());
        payload.put("status", ownership.getStatus().name());
        return payload;
    }
}
