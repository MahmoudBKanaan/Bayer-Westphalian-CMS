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
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class ProductChangeRequestServiceTests {

    private static final UUID REQUEST_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000101");
    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");

    @Mock private ProductChangeRequestRepository productChangeRequestRepository;

    @Mock private ProductRepository productRepository;

    @Mock private UserRepository userRepository;

    @Mock private AuthorizationExpressions authorizationExpressions;

    @Mock private AuditService auditService;

    private ProductChangeRequestService productChangeRequestService;

    @BeforeEach
    void setUp() {
        productChangeRequestService =
                new ProductChangeRequestService(
                        productChangeRequestRepository,
                        productRepository,
                        userRepository,
                        authorizationExpressions,
                        auditService);
    }

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorizeWithExpression(
                "createRequest",
                new Class<?>[] {CreateProductChangeRequestCommand.class},
                "@authz.canManageProducts()");
        assertPreAuthorizeWithExpression(
                "updateRequest",
                new Class<?>[] {UUID.class, UpdateProductChangeRequestCommand.class},
                "@authz.canManageProducts()");
        assertPreAuthorizeWithExpression(
                "approveRequest", new Class<?>[] {UUID.class}, "@authz.canManageProducts()");
        assertPreAuthorizeWithExpression(
                "rejectRequest", new Class<?>[] {UUID.class}, "@authz.canManageProducts()");
        assertPreAuthorizeWithExpression(
                "markImplemented", new Class<?>[] {UUID.class}, "@authz.canManageProducts()");
        assertPreAuthorize("listRequests", ProductChangeRequestSearchCriteria.class);
    }

    @Test
    void createsOpenProductChangeRequestAndAuditsCreation() throws Exception {
        Product product = product();
        User requester = requester();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(requester));
        when(productChangeRequestRepository.save(any(ProductChangeRequest.class)))
                .thenAnswer(
                        invocation -> {
                            ProductChangeRequest request = invocation.getArgument(0);
                            setId(request, REQUEST_ID);
                            return request;
                        });

        ProductChangeRequestView view =
                productChangeRequestService.createRequest(
                        new CreateProductChangeRequestCommand(
                                PRODUCT_ID,
                                ProductChangeType.PRICE_CHANGE,
                                " Adjust monthly price for the new tariff. "));

        ArgumentCaptor<ProductChangeRequest> requestCaptor =
                ArgumentCaptor.forClass(ProductChangeRequest.class);
        verify(productChangeRequestRepository).save(requestCaptor.capture());
        ProductChangeRequest saved = requestCaptor.getValue();
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getRequestedBy()).isSameAs(requester);
        assertThat(saved.getRequestType()).isEqualTo(ProductChangeType.PRICE_CHANGE);
        assertThat(saved.getDescription())
                .isEqualTo("Adjust monthly price for the new tariff.");
        assertThat(saved.getStatus()).isEqualTo(ProductChangeStatus.OPEN);
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.requestedByFullName()).isEqualTo("Product Manager");
        assertThat(view.status()).isEqualTo(ProductChangeStatus.OPEN);
        verify(auditService)
                .logCreate(
                        eq(USER_ID),
                        eq("product_change_requests"),
                        eq(REQUEST_ID),
                        eq(
                                Map.of(
                                        "productId",
                                        PRODUCT_ID,
                                        "requestType",
                                        "PRICE_CHANGE",
                                        "description",
                                        "Adjust monthly price for the new tariff.",
                                        "status",
                                        "OPEN",
                                        "requestedByUserId",
                                        USER_ID)));
    }

    @Test
    void validatesCreateProductChangeRequestCommand() {
        assertThatThrownBy(() -> productChangeRequestService.createRequest(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product change request validation failed");

        assertThatThrownBy(
                        () ->
                                productChangeRequestService.createRequest(
                                        new CreateProductChangeRequestCommand(
                                                null, null, " ")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product change request validation failed");
    }

    @Test
    void updatesOpenRequestDescriptionAndAuditsChange() throws Exception {
        ProductChangeRequest request = openRequest();
        when(productChangeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(productChangeRequestRepository.save(request)).thenReturn(request);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);

        ProductChangeRequestView view =
                productChangeRequestService.updateRequest(
                        REQUEST_ID,
                        new UpdateProductChangeRequestCommand(
                                " Use the updated 6-month expiration reminder policy. "));

        assertThat(view.description())
                .isEqualTo("Use the updated 6-month expiration reminder policy.");
        verify(productChangeRequestRepository).save(request);
        verify(auditService)
                .logUpdate(
                        eq(USER_ID),
                        eq("product_change_requests"),
                        eq(REQUEST_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void rejectsUpdateForNonOpenRequests() throws Exception {
        ProductChangeRequest approvedRequest = openRequest();
        approvedRequest.approve();
        when(productChangeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(approvedRequest));

        assertThatThrownBy(
                        () ->
                                productChangeRequestService.updateRequest(
                                        REQUEST_ID,
                                        new UpdateProductChangeRequestCommand(
                                                "Updated description.")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product change request validation failed");
    }

    @Test
    void rejectsUpdateWithoutDescription() {
        assertThatThrownBy(
                        () ->
                                productChangeRequestService.updateRequest(
                                        REQUEST_ID, new UpdateProductChangeRequestCommand(" ")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product change request validation failed");
    }

    @Test
    void approvesOpenRequestAndAuditsStatusChange() throws Exception {
        ProductChangeRequest request = openRequest();
        when(productChangeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(productChangeRequestRepository.save(request)).thenReturn(request);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);

        ProductChangeRequestView view = productChangeRequestService.approveRequest(REQUEST_ID);

        assertThat(view.status()).isEqualTo(ProductChangeStatus.APPROVED);
        verify(productChangeRequestRepository).save(request);
        verify(auditService)
                .logApproval(
                        eq(USER_ID),
                        eq("product_change_requests"),
                        eq(REQUEST_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void tracksProductChangeRequestFromOpenToApprovedToImplemented() throws Exception {
        ProductChangeRequest request = openRequest();
        when(productChangeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(productChangeRequestRepository.save(request)).thenReturn(request);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);

        ProductChangeRequestView approvedView = productChangeRequestService.approveRequest(REQUEST_ID);
        assertThat(approvedView.status()).isEqualTo(ProductChangeStatus.APPROVED);

        ProductChangeRequestView implementedView =
                productChangeRequestService.markImplemented(REQUEST_ID);
        assertThat(implementedView.status()).isEqualTo(ProductChangeStatus.IMPLEMENTED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logUpdate(
                        eq(USER_ID),
                        eq("product_change_requests"),
                        eq(REQUEST_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());

        assertThat(castAuditPayload(oldValueCaptor.getValue())).containsEntry("status", "APPROVED");
        assertThat(castAuditPayload(newValueCaptor.getValue())).containsEntry("status", "IMPLEMENTED");
    }

    @Test
    void approvesOpenRequestAndAuditsStatusTransitionFromOpenToApproved() throws Exception {
        ProductChangeRequest request = openRequest();
        when(productChangeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(productChangeRequestRepository.save(request)).thenReturn(request);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);

        ProductChangeRequestView view = productChangeRequestService.approveRequest(REQUEST_ID);

        assertThat(view.status()).isEqualTo(ProductChangeStatus.APPROVED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logApproval(
                        eq(USER_ID),
                        eq("product_change_requests"),
                        eq(REQUEST_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());

        assertThat(castAuditPayload(oldValueCaptor.getValue())).containsEntry("status", "OPEN");
        assertThat(castAuditPayload(newValueCaptor.getValue())).containsEntry("status", "APPROVED");
    }

    @Test
    void rejectsOpenRequestAndAuditsStatusChange() throws Exception {
        ProductChangeRequest request = openRequest();
        when(productChangeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(productChangeRequestRepository.save(request)).thenReturn(request);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);

        ProductChangeRequestView view = productChangeRequestService.rejectRequest(REQUEST_ID);

        assertThat(view.status()).isEqualTo(ProductChangeStatus.REJECTED);
        verify(auditService)
                .logRejection(
                        eq(USER_ID),
                        eq("product_change_requests"),
                        eq(REQUEST_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void marksApprovedRequestAsImplemented() throws Exception {
        ProductChangeRequest request = openRequest();
        request.approve();
        when(productChangeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(productChangeRequestRepository.save(request)).thenReturn(request);

        ProductChangeRequestView view = productChangeRequestService.markImplemented(REQUEST_ID);

        assertThat(view.status()).isEqualTo(ProductChangeStatus.IMPLEMENTED);
    }

    @Test
    void rejectsInvalidWorkflowTransitions() throws Exception {
        ProductChangeRequest approvedRequest = openRequest();
        approvedRequest.approve();
        when(productChangeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(approvedRequest));

        assertThatThrownBy(() -> productChangeRequestService.approveRequest(REQUEST_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product change request validation failed");

        ProductChangeRequest openRequest = openRequest();
        when(productChangeRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(openRequest));

        assertThatThrownBy(() -> productChangeRequestService.markImplemented(REQUEST_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product change request validation failed");
    }

    @Test
    void listsRequestsWithKbFilters() throws Exception {
        ProductChangeRequest openRequest = openRequest();
        ProductChangeRequest approvedRequest = openRequest();
        approvedRequest.approve();
        when(productChangeRequestRepository.findByProductId(PRODUCT_ID))
                .thenReturn(List.of(openRequest, approvedRequest));

        List<ProductChangeRequestView> views =
                productChangeRequestService.listRequests(
                        new ProductChangeRequestSearchCriteria(PRODUCT_ID, ProductChangeStatus.OPEN));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).requestType()).isEqualTo(ProductChangeType.PRICE_CHANGE);
        assertThat(views.get(0).status()).isEqualTo(ProductChangeStatus.OPEN);
        verify(productChangeRequestRepository).findByProductId(PRODUCT_ID);
    }

    @Test
    void rejectsMissingRequestOrProduct() throws Exception {
        UUID missingRequestId = UUID.fromString("42000000-0000-0000-0000-000000000099");
        when(productChangeRequestRepository.findById(missingRequestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productChangeRequestService.approveRequest(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product change request validation failed");

        assertThatThrownBy(() -> productChangeRequestService.approveRequest(missingRequestId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product change request was not found: " + missingRequestId);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                productChangeRequestService.createRequest(
                                        new CreateProductChangeRequestCommand(
                                                PRODUCT_ID,
                                                ProductChangeType.STATUS_CHANGE,
                                                "Deactivate legacy tariff.")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product was not found: " + PRODUCT_ID);
    }

    private static void assertPreAuthorize(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = ProductChangeRequestService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = ProductChangeRequestService.class.getMethod(methodName, parameterTypes);

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
        setId(product, PRODUCT_ID);
        return product;
    }

    private static User requester() throws Exception {
        User user =
                User.create(
                        "product.manager@bayer-westphalian.test",
                        "$2a$10$product-manager",
                        "Product Manager");
        setId(user, USER_ID);
        return user;
    }

    private static ProductChangeRequest openRequest() throws Exception {
        ProductChangeRequest request =
                ProductChangeRequest.create(
                        product(),
                        requester(),
                        ProductChangeType.PRICE_CHANGE,
                        "Adjust monthly price for the new tariff.");
        setId(request, REQUEST_ID);
        return request;
    }

    private static void setId(Product product, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(product, id);
    }

    private static void setId(User user, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }

    private static void setId(ProductChangeRequest request, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(request, id);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castAuditPayload(Map<String, ?> payload) {
        return (Map<String, Object>) payload;
    }
}
