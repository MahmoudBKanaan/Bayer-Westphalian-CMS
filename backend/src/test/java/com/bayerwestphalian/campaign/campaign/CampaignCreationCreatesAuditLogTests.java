package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditLogRepository;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB item 233: campaign creation writes an immutable CREATE audit log for entity type {@code
 * campaigns}.
 */
@ExtendWith(MockitoExtension.class)
class CampaignCreationCreatesAuditLogTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID SEGMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignProductRepository campaignProductRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private AuditService auditService;
    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
        lenient()
                .when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        campaignService =
                new CampaignService(
                        campaignRepository,
                        campaignProductRepository,
                        segmentRepository,
                        productRepository,
                        userRepository,
                        authorizationExpressions,
                        auditService);
    }

    @Test
    void createCampaignPersistsCreateAuditLogWithDraftPayload() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Segment segment = segment(owner);
        Product product = product();
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(
                        invocation -> {
                            Campaign campaign = invocation.getArgument(0);
                            setId(campaign, CAMPAIGN_ID);
                            return campaign;
                        });
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID))
                .thenReturn(List.of(CampaignProduct.link(campaignWithId(owner, segment), product)));

        campaignService.createCampaign(
                new CreateCampaignCommand(
                        "Life renewal outreach",
                        "Promote life insurance renewals",
                        SEGMENT_ID,
                        CampaignChannel.EMAIL,
                        "Renew your cover",
                        "Dear customer, ...",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        List.of(PRODUCT_ID)));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo(CampaignService.AUDIT_ENTITY_TYPE);
        assertThat(auditLog.getEntityType()).isEqualTo("campaigns");
        assertThat(auditLog.getEntityId()).isEqualTo(CAMPAIGN_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(OWNER_ID);
        assertThat(auditLog.getOldValue()).isNull();
        assertThat(auditLog.getNewValue())
                .containsEntry("id", CAMPAIGN_ID.toString())
                .containsEntry("name", "Life renewal outreach")
                .containsEntry("objective", "Promote life insurance renewals")
                .containsEntry("status", "DRAFT")
                .containsEntry("ownerUserId", OWNER_ID.toString())
                .containsEntry("segmentId", SEGMENT_ID.toString())
                .containsEntry("channel", "EMAIL")
                .containsEntry("messageSubject", "Renew your cover")
                .containsEntry("messageBody", "Dear customer, ...")
                .containsEntry("startDate", "2026-09-01")
                .containsEntry("endDate", "2026-09-30")
                .containsEntry("rejectionReason", null)
                .containsEntry("complianceReviewNotes", null);
        assertThat(castList(auditLog.getNewValue().get("productIds")))
                .containsExactly(PRODUCT_ID.toString());
    }

    @Test
    void createCampaignPersistsCreateAuditLogForMinimalDraft() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(
                        invocation -> {
                            Campaign campaign = invocation.getArgument(0);
                            setId(campaign, CAMPAIGN_ID);
                            return campaign;
                        });
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        campaignService.createCampaign(
                new CreateCampaignCommand(
                        "Minimal draft",
                        "Minimal objective",
                        null,
                        CampaignChannel.PHONE,
                        null,
                        null,
                        null,
                        null,
                        List.of()));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo("campaigns");
        assertThat(auditLog.getEntityId()).isEqualTo(CAMPAIGN_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(OWNER_ID);
        assertThat(auditLog.getOldValue()).isNull();
        assertThat(auditLog.getNewValue())
                .containsEntry("name", "Minimal draft")
                .containsEntry("status", "DRAFT")
                .containsEntry("channel", "PHONE")
                .containsEntry("segmentId", null);
        assertThat(castList(auditLog.getNewValue().get("productIds"))).isEmpty();
    }

    @Test
    void failedCreateValidationDoesNotWriteAuditLog() {
        assertThatThrownBy(
                        () ->
                                campaignService.createCampaign(
                                        new CreateCampaignCommand(
                                                " ", null, null, null, null, null, null, null,
                                                List.of())))
                .isInstanceOf(ValidationException.class);

        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void auditEntityTypeConstantIsCampaignsForKbTraceability() {
        assertThat(CampaignService.AUDIT_ENTITY_TYPE).isEqualTo("campaigns");
    }

    private AuditLog captureSavedAuditLog() {
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        return auditLogCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object value) {
        return (List<String>) value;
    }

    private Campaign campaignWithId(User owner, Segment segment) throws Exception {
        Campaign campaign =
                Campaign.create(
                        "Life renewal outreach",
                        "Promote life insurance renewals",
                        owner,
                        segment,
                        CampaignChannel.EMAIL);
        setId(campaign, CAMPAIGN_ID);
        return campaign;
    }

    private User user(UUID id, String name) throws Exception {
        User user =
                User.create(name.toLowerCase().replace(' ', '.') + "@test", "$2a$10$hash", name);
        setId(user, id);
        return user;
    }

    private Segment segment(User owner) throws Exception {
        Segment segment =
                Segment.create(
                        "Munich prospects", "Location audience", owner, SegmentVisibility.TEAM);
        setId(segment, SEGMENT_ID);
        return segment;
    }

    private Product product() throws Exception {
        Product product =
                Product.create(
                        "Life Protection", ProductType.LIFE_INSURANCE, new BigDecimal("99.00"), 12);
        setId(product, PRODUCT_ID);
        return product;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
