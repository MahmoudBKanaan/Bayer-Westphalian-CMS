package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.auth.method.CampaignWriteAccess;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 243 / FR-050 / FR-057 acceptance: Campaign Manager can create draft campaign.
 *
 * <p>Proves that a Campaign Manager can create a named campaign that starts in {@code DRAFT}, is
 * owned by the authenticated Campaign Manager, and that create is authorized via {@link
 * CampaignWriteAccess} / {@code canManageCampaigns}.
 */
class CampaignManagerCanCreateDraftCampaignTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T10:30:00Z");

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Service {

        @Mock private CampaignRepository campaignRepository;
        @Mock private CampaignProductRepository campaignProductRepository;
        @Mock private SegmentRepository segmentRepository;
        @Mock private ProductRepository productRepository;
        @Mock private UserRepository userRepository;
        @Mock private AuthorizationExpressions authorizationExpressions;
        @Mock private AuditService auditService;

        private CampaignService campaignService;

        @BeforeEach
        void setUp() {
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
        void campaignManagerCreateCampaignProducesDraftOwnedByActor() throws Exception {
            User campaignManager = user(OWNER_ID, "Campaign Manager");
            when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(campaignManager));
            when(campaignRepository.save(any(Campaign.class)))
                    .thenAnswer(
                            invocation -> {
                                Campaign campaign = invocation.getArgument(0);
                                setId(campaign, CAMPAIGN_ID);
                                return campaign;
                            });
            when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

            CampaignView created =
                    campaignService.createCampaign(
                            new CreateCampaignCommand(
                                    "Life renewal outreach",
                                    "Promote life insurance renewals",
                                    null,
                                    CampaignChannel.EMAIL,
                                    "Renew your cover",
                                    "Dear customer, ...",
                                    LocalDate.of(2026, 9, 1),
                                    LocalDate.of(2026, 9, 30),
                                    List.of()));

            assertThat(created.status()).isEqualTo(CampaignStatus.DRAFT);
            assertThat(created.ownerUserId()).isEqualTo(OWNER_ID);
            assertThat(created.ownerFullName()).isEqualTo("Campaign Manager");
            assertThat(created.name()).isEqualTo("Life renewal outreach");
            assertThat(created.objective()).isEqualTo("Promote life insurance renewals");
            assertThat(created.channel()).isEqualTo(CampaignChannel.EMAIL);

            ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
            verify(campaignRepository).save(campaignCaptor.capture());
            Campaign saved = campaignCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(CampaignStatus.DRAFT);
            assertThat(saved.isDraft()).isTrue();
            assertThat(saved.canEdit()).isTrue();
            assertThat(saved.canLaunch()).isFalse();
            assertThat(saved.getOwnerUserId()).isEqualTo(OWNER_ID);
            assertThat(saved.getApprovedBy()).isNull();
            assertThat(saved.getRejectionReason()).isNull();

            verify(auditService)
                    .logCreate(
                            eq(OWNER_ID),
                            eq(CampaignService.AUDIT_ENTITY_TYPE),
                            eq(CAMPAIGN_ID),
                            any());
        }

        @Test
        void createCampaignIsProtectedByCampaignWriteAccess() throws Exception {
            Method create =
                    CampaignService.class.getMethod("createCampaign", CreateCampaignCommand.class);
            assertThat(create.isAnnotationPresent(CampaignWriteAccess.class)).isTrue();
            CampaignWriteAccess access = create.getAnnotation(CampaignWriteAccess.class);
            PreAuthorize preAuthorize = access.annotationType().getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize.value()).isEqualTo("@authz.canManageCampaigns()");
        }

        private User user(UUID id, String name) throws Exception {
            User user =
                    User.create(
                            name.toLowerCase().replace(' ', '.') + "@test", "$2a$10$hash", name);
            setId(user, id);
            return user;
        }

        private static void setId(Object entity, UUID id) throws Exception {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        }
    }

    @Nested
    @WebMvcTest(controllers = CampaignController.class)
    @Import({
        SecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        AuthorizationExpressions.class,
        GlobalExceptionHandler.class
    })
    class Http {

        @Autowired private MockMvc mockMvc;

        @MockBean private CampaignService campaignService;

        @MockBean private JwtService jwtService;

        @MockBean(name = "jpaMappingContext")
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Test
        void campaignManagerCanCreateDraftCampaign() throws Exception {
            when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                    .thenReturn(campaignManagerClaims());
            when(campaignService.createCampaign(any(CreateCampaignCommand.class)))
                    .thenReturn(draftOwnedByCampaignManager());

            mockMvc.perform(
                            post("/api/campaigns")
                                    .header("Authorization", "Bearer campaign-manager-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {
                                              "name": "Life renewal outreach",
                                              "objective": "Promote life insurance renewals",
                                              "channel": "EMAIL",
                                              "messageSubject": "Renew your cover",
                                              "messageBody": "Dear customer, ...",
                                              "startDate": "2026-09-01",
                                              "endDate": "2026-09-30"
                                            }
                                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Campaign created"))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                    .andExpect(jsonPath("$.data.ownerFullName").value("Campaign Manager"))
                    .andExpect(jsonPath("$.data.name").value("Life renewal outreach"));

            verify(campaignService).createCampaign(any(CreateCampaignCommand.class));
        }

        @Test
        void securityRolesIncludeCampaignManagerForCampaignWrite() {
            assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                    .contains(SystemRoleName.CAMPAIGN_MANAGER.name())
                    .contains(SystemRoleName.ADMIN.name());
        }

        private static JwtTokenClaims campaignManagerClaims() {
            return new JwtTokenClaims(
                    OWNER_ID,
                    "campaign.manager@bayer-westphalian.test",
                    List.of(SystemRoleName.CAMPAIGN_MANAGER));
        }

        private static CampaignView draftOwnedByCampaignManager() {
            return new CampaignView(
                    CAMPAIGN_ID,
                    "Life renewal outreach",
                    "Promote life insurance renewals",
                    CampaignStatus.DRAFT,
                    OWNER_ID,
                    "Campaign Manager",
                    null,
                    null,
                    CampaignChannel.EMAIL,
                    "Renew your cover",
                    "Dear customer, ...",
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    CREATED_AT,
                    UPDATED_AT);
        }
    }
}
