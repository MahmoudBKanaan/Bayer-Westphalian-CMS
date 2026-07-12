package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductChangeRequestService {

    private final ProductChangeRequestRepository productChangeRequestRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuthorizationExpressions authorizationExpressions;
    private final AuditService auditService;

    public ProductChangeRequestService(
            ProductChangeRequestRepository productChangeRequestRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions,
            AuditService auditService) {
        this.productChangeRequestRepository = productChangeRequestRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.authorizationExpressions = authorizationExpressions;
        this.auditService = auditService;
    }

    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductChangeRequestView createRequest(CreateProductChangeRequestCommand command) {
        validateCreateCommand(command);
        Product product = findProduct(command.productId());

        ProductChangeRequest request =
                ProductChangeRequest.create(
                        product,
                        resolveCurrentUser(),
                        command.requestType(),
                        command.description().trim());
        ProductChangeRequest savedRequest = productChangeRequestRepository.save(request);
        auditService.logCreate(
                currentActorUserId(),
                "product_change_requests",
                savedRequest.getId(),
                requestAuditPayload(savedRequest));

        return ProductChangeRequestView.from(savedRequest);
    }

    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductChangeRequestView updateRequest(
            UUID requestId, UpdateProductChangeRequestCommand command) {
        validateRequestId(requestId);
        validateUpdateCommand(command);
        ProductChangeRequest request = findRequest(requestId);
        ensureOpenForUpdate(request);
        Map<String, ?> oldValue = requestAuditPayload(request);

        request.updateDescription(command.description().trim());

        ProductChangeRequest savedRequest = productChangeRequestRepository.save(request);
        auditService.logUpdate(
                currentActorUserId(),
                "product_change_requests",
                savedRequest.getId(),
                oldValue,
                requestAuditPayload(savedRequest));

        return ProductChangeRequestView.from(savedRequest);
    }

    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductChangeRequestView approveRequest(UUID requestId) {
        return transitionRequest(
                requestId,
                ProductChangeStatus.OPEN,
                ProductChangeStatus.APPROVED,
                ProductChangeRequest::approve);
    }

    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductChangeRequestView rejectRequest(UUID requestId) {
        return transitionRequest(
                requestId,
                ProductChangeStatus.OPEN,
                ProductChangeStatus.REJECTED,
                ProductChangeRequest::reject);
    }

    @PreAuthorize("@authz.canManageProducts()")
    @Transactional
    public ProductChangeRequestView markImplemented(UUID requestId) {
        return transitionRequest(
                requestId,
                ProductChangeStatus.APPROVED,
                ProductChangeStatus.IMPLEMENTED,
                ProductChangeRequest::markImplemented);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'PRODUCT_MANAGER', "
                    + "'COMPLIANCE_OFFICER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', "
                    + "'EXECUTIVE_VIEWER')")
    @Transactional(readOnly = true)
    public List<ProductChangeRequestView> listRequests(
            ProductChangeRequestSearchCriteria criteria) {
        ProductChangeRequestSearchCriteria normalized = normalize(criteria);

        return loadCandidates(normalized).stream()
                .filter(request -> matches(request, normalized))
                .map(ProductChangeRequestView::from)
                .toList();
    }

    private ProductChangeRequestView transitionRequest(
            UUID requestId,
            ProductChangeStatus requiredStatus,
            ProductChangeStatus targetStatus,
            Consumer<ProductChangeRequest> transition) {
        validateRequestId(requestId);
        ProductChangeRequest request = findRequest(requestId);
        ensureStatus(request, requiredStatus, targetStatus);
        Map<String, ?> oldValue = requestAuditPayload(request);

        transition.accept(request);

        ProductChangeRequest savedRequest = productChangeRequestRepository.save(request);
        auditWorkflowTransition(
                requiredStatus,
                targetStatus,
                savedRequest.getId(),
                oldValue,
                requestAuditPayload(savedRequest));

        return ProductChangeRequestView.from(savedRequest);
    }

    private void auditWorkflowTransition(
            ProductChangeStatus requiredStatus,
            ProductChangeStatus targetStatus,
            UUID requestId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        UUID actorUserId = currentActorUserId();
        if (requiredStatus == ProductChangeStatus.OPEN
                && targetStatus == ProductChangeStatus.APPROVED) {
            auditService.logApproval(
                    actorUserId, "product_change_requests", requestId, oldValue, newValue);
            return;
        }
        if (requiredStatus == ProductChangeStatus.OPEN
                && targetStatus == ProductChangeStatus.REJECTED) {
            auditService.logRejection(
                    actorUserId, "product_change_requests", requestId, oldValue, newValue);
            return;
        }
        auditService.logUpdate(
                actorUserId, "product_change_requests", requestId, oldValue, newValue);
    }

    private List<ProductChangeRequest> loadCandidates(ProductChangeRequestSearchCriteria criteria) {
        if (criteria.productId() != null) {
            return productChangeRequestRepository.findByProductId(criteria.productId());
        }
        if (criteria.status() != null) {
            return productChangeRequestRepository.findByStatus(criteria.status());
        }
        return productChangeRequestRepository.findAll();
    }

    private boolean matches(
            ProductChangeRequest request, ProductChangeRequestSearchCriteria criteria) {
        return matchesProduct(request, criteria.productId())
                && matchesStatus(request, criteria.status());
    }

    private boolean matchesProduct(ProductChangeRequest request, UUID productId) {
        return productId == null || Objects.equals(request.getProduct().getId(), productId);
    }

    private boolean matchesStatus(ProductChangeRequest request, ProductChangeStatus status) {
        return status == null || request.getStatus() == status;
    }

    private ProductChangeRequestSearchCriteria normalize(
            ProductChangeRequestSearchCriteria criteria) {
        if (criteria == null) {
            return new ProductChangeRequestSearchCriteria(null, null);
        }
        return criteria;
    }

    private Product findProduct(UUID productId) {
        return productRepository
                .findById(productId)
                .filter(product -> !product.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private ProductChangeRequest findRequest(UUID requestId) {
        return productChangeRequestRepository
                .findById(requestId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product change request", requestId));
    }

    private User resolveCurrentUser() {
        if (!authorizationExpressions.isAuthenticated()) {
            return null;
        }
        return userRepository.findById(authorizationExpressions.currentUserId()).orElse(null);
    }

    private UUID currentActorUserId() {
        return authorizationExpressions.isAuthenticated()
                ? authorizationExpressions.currentUserId()
                : null;
    }

    private void validateUpdateCommand(UpdateProductChangeRequestCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Product change request validation failed", List.of("command: is required"));
        }
        if (!StringUtils.hasText(command.description())) {
            throw new ValidationException(
                    "Product change request validation failed",
                    List.of("description: must not be blank"));
        }
    }

    private void ensureOpenForUpdate(ProductChangeRequest request) {
        if (request.getStatus() != ProductChangeStatus.OPEN) {
            throw new ValidationException(
                    "Product change request validation failed",
                    List.of("status: only OPEN requests can be updated"));
        }
    }

    private void validateCreateCommand(CreateProductChangeRequestCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Product change request validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("productId", command.productId()),
                                required("requestType", command.requestType()),
                                required("description", command.description()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Product change request validation failed", errors);
        }
    }

    private void validateRequestId(UUID requestId) {
        if (requestId == null) {
            throw new ValidationException(
                    "Product change request validation failed", List.of("requestId: is required"));
        }
    }

    private void ensureStatus(
            ProductChangeRequest request,
            ProductChangeStatus requiredStatus,
            ProductChangeStatus targetStatus) {
        if (request.getStatus() != requiredStatus) {
            throw new ValidationException(
                    "Product change request validation failed",
                    List.of(
                            "status: request must be "
                                    + requiredStatus
                                    + " before transitioning to "
                                    + targetStatus));
        }
    }

    private String required(String fieldName, Object value) {
        return value == null ? fieldName + ": must not be null" : "";
    }

    private String required(String fieldName, String value) {
        return StringUtils.hasText(value) ? "" : fieldName + ": must not be blank";
    }

    private Map<String, ?> requestAuditPayload(ProductChangeRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", request.getProduct().getId());
        payload.put("requestType", request.getRequestType().name());
        payload.put("description", request.getDescription());
        payload.put("status", request.getStatus().name());
        User requestedBy = request.getRequestedBy();
        if (requestedBy != null) {
            payload.put("requestedByUserId", requestedBy.getId());
        }
        return payload;
    }
}
