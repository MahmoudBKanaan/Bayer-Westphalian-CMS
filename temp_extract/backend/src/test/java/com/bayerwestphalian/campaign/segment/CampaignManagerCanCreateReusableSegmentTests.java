package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthenticatedPrincipal;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.auth.method.SegmentCreateAccess;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 201 / FR-077 acceptance: Campaign Manager can create reusable segment.
 *
 * <p>Proves that a Campaign Manager (and Admin) can save a named audience definition with
 * visibility and criteria, that the definition is owned and reloadable for later reuse, and that
 * create is denied for roles without segment create permission.
 */
class CampaignManagerCanCreateReusableSegmentTests {

    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final UUID CRITERION_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000211");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000201");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T10:30:00Z");

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ServiceAcceptance {

        @Mock private SegmentRepository segmentRepository;
        @Mock private SegmentCriteriaRepository segmentCriteriaRepository;
        @Mock private CustomerRepository customerRepository;
        @Mock private ProductOwnershipRepository productOwnershipRepository;
        @Mock private PaymentRecordRepository paymentRecordRepository;
        @Mock private ConsentRepository consentRepository;
        @Mock private UserRepository userRepository;
        @Mock private AuthorizationExpressions authorizationExpressions;
        @Mock private ConsentService consentService;
        @Mock private EligibilityService eligibilityService;
        @Mock private AuditService auditService;

        private SegmentService segmentService;

        @BeforeEach
        void setUp() {
            segmentService =
                    new SegmentService(
                            segmentRepository,
                            segmentCriteriaRepository,
                            customerRepository,
                            productOwnershipRepository,
                            paymentRecordRepository,
                            consentRepository,
                            userRepository,
                            authorizationExpressions,
                            consentService,
                            eligibilityService,
                            auditService);
            lenient()
                    .when(eligibilityService.evaluateForSegmentPreview(any(UUID.class)))
                    .thenReturn(EligibilityDecision.included());
            lenient().when(authorizationExpressions.isAuthenticated()).thenReturn(true);
            lenient().when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        }

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void campaignManagerCreatesReusableSegmentWithNameVisibilityAndCriteria()
                throws Exception {
            User owner = campaignManagerOwner();
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(segmentRepository.save(any(Segment.class)))
                    .thenAnswer(
                            invocation -> {
                                Segment segment = invocation.getArgument(0);
                                setId(segment, SEGMENT_ID);
                                return segment;
                            });

            SegmentView view =
                    segmentService.createSegment(
                            new CreateSegmentCommand(
                                    "  Expiring life policies  ",
                                    "  Reusable CM audience for renewals  ",
                                    SegmentVisibility.TEAM,
                                    List.of(
                                            new CreateSegmentCriteriaCommand(
                                                    "city",
                                                    SegmentOperator.EQUALS,
                                                    " Munich ",
                                                    "location",
                                                    SegmentJoinOperator.AND),
                                            new CreateSegmentCriteriaCommand(
                                                    "product_expiration_months",
                                                    SegmentOperator.EQUALS,
                                                    "6",
                                                    "expiration",
                                                    SegmentJoinOperator.AND))));

            ArgumentCaptor<Segment> segmentCaptor = ArgumentCaptor.forClass(Segment.class);
            verify(segmentRepository).save(segmentCaptor.capture());
            Segment saved = segmentCaptor.getValue();

            assertThat(saved.getName()).isEqualTo("Expiring life policies");
            assertThat(saved.getDescription()).isEqualTo("Reusable CM audience for renewals");
            assertThat(saved.getOwner().getId()).isEqualTo(OWNER_ID);
            assertThat(saved.getVisibility()).isEqualTo(SegmentVisibility.TEAM);
            assertThat(saved.getCriteria()).hasSize(2);
            assertThat(saved.getCriteria().getFirst().getFieldName()).isEqualTo("city");
            assertThat(saved.getCriteria().getFirst().getValue()).isEqualTo("Munich");
            // product_expiration_months is canonicalized for storage (FR-076 filter field).
            assertThat(saved.getCriteria().get(1).getFieldName())
                    .isEqualTo("expiring_within_months");
            assertThat(saved.getCriteria().get(1).getValue()).isEqualTo("6");
            assertThat(saved.getCriteria().get(1).getJoinOperator())
                    .isEqualTo(SegmentJoinOperator.AND);

            assertThat(view.id()).isEqualTo(SEGMENT_ID);
            assertThat(view.name()).isEqualTo("Expiring life policies");
            assertThat(view.ownerUserId()).isEqualTo(OWNER_ID);
            assertThat(view.visibility()).isEqualTo(SegmentVisibility.TEAM);
            assertThat(view.criteria()).hasSize(2);
            assertThat(view.criteria().get(1).fieldName()).isEqualTo("expiring_within_months");
            assertThat(view.ownerFullName()).isEqualTo("Campaign Manager");
        }

        @ParameterizedTest(name = "visibility {0} is saved as reusable")
        @EnumSource(SegmentVisibility.class)
        void campaignManagerCanCreateReusableSegmentForEveryVisibility(
                SegmentVisibility visibility) throws Exception {
            User owner = campaignManagerOwner();
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(segmentRepository.save(any(Segment.class)))
                    .thenAnswer(
                            invocation -> {
                                Segment segment = invocation.getArgument(0);
                                setId(segment, SEGMENT_ID);
                                return segment;
                            });

            SegmentView view =
                    segmentService.createSegment(
                            new CreateSegmentCommand(
                                    "Reusable " + visibility.name(),
                                    "Saved for later campaign selection",
                                    visibility,
                                    List.of(
                                            new CreateSegmentCriteriaCommand(
                                                    "customer_type",
                                                    SegmentOperator.EQUALS,
                                                    "PROSPECT",
                                                    "type",
                                                    SegmentJoinOperator.AND))));

            ArgumentCaptor<Segment> segmentCaptor = ArgumentCaptor.forClass(Segment.class);
            verify(segmentRepository).save(segmentCaptor.capture());
            assertThat(segmentCaptor.getValue().getVisibility()).isEqualTo(visibility);
            assertThat(view.visibility()).isEqualTo(visibility);
            assertThat(view.criteria()).hasSize(1);
            assertThat(view.criteria().getFirst().fieldName()).isEqualTo("customer_type");
        }

        @Test
        void nullVisibilityDefaultsToPrivateReusableSegment() throws Exception {
            User owner = campaignManagerOwner();
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(segmentRepository.save(any(Segment.class)))
                    .thenAnswer(
                            invocation -> {
                                Segment segment = invocation.getArgument(0);
                                setId(segment, SEGMENT_ID);
                                return segment;
                            });

            SegmentView view =
                    segmentService.createSegment(
                            new CreateSegmentCommand(
                                    "Private draft audience",
                                    null,
                                    null,
                                    List.of(
                                            new CreateSegmentCriteriaCommand(
                                                    "age_group",
                                                    SegmentOperator.EQUALS,
                                                    "26_40",
                                                    null,
                                                    null))));

            ArgumentCaptor<Segment> segmentCaptor = ArgumentCaptor.forClass(Segment.class);
            verify(segmentRepository).save(segmentCaptor.capture());
            assertThat(segmentCaptor.getValue().getVisibility())
                    .isEqualTo(SegmentVisibility.PRIVATE);
            assertThat(view.visibility()).isEqualTo(SegmentVisibility.PRIVATE);
            assertThat(segmentCaptor.getValue().getCriteria().getFirst().getJoinOperator())
                    .isEqualTo(SegmentJoinOperator.AND);
        }

        @Test
        void createdReusableSegmentCanBeReloadedById() throws Exception {
            User owner = campaignManagerOwner();
            Segment persisted =
                    Segment.create(
                            "Team renewal audience",
                            "Reusable by campaign managers",
                            owner,
                            SegmentVisibility.TEAM);
            setId(persisted, SEGMENT_ID);
            persisted.addCriteria(
                    "city", SegmentOperator.EQUALS, "Berlin", "location", SegmentJoinOperator.AND);

            when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
            when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(persisted));

            SegmentView loaded = segmentService.findById(SEGMENT_ID);

            assertThat(loaded.id()).isEqualTo(SEGMENT_ID);
            assertThat(loaded.name()).isEqualTo("Team renewal audience");
            assertThat(loaded.visibility()).isEqualTo(SegmentVisibility.TEAM);
            assertThat(loaded.ownerUserId()).isEqualTo(OWNER_ID);
            assertThat(loaded.criteria()).hasSize(1);
            assertThat(loaded.criteria().getFirst().value()).isEqualTo("Berlin");
        }

        @Test
        void createdReusableSegmentAppearsInSegmentSearchForReuse() throws Exception {
            User owner = campaignManagerOwner();
            Segment teamSegment =
                    Segment.create(
                            "Munich prospects",
                            "Reusable location segment",
                            owner,
                            SegmentVisibility.TEAM);
            setId(teamSegment, SEGMENT_ID);
            teamSegment.addCriteria(
                    "city", SegmentOperator.EQUALS, "Munich", "location", SegmentJoinOperator.AND);

            when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
            when(segmentRepository.findByOwner(OWNER_ID)).thenReturn(List.of(teamSegment));

            List<SegmentView> results =
                    segmentService.searchSegments(
                            new SegmentSearchCriteria("Munich", OWNER_ID, null));

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().id()).isEqualTo(SEGMENT_ID);
            assertThat(results.getFirst().name()).isEqualTo("Munich prospects");
            assertThat(results.getFirst().visibility()).isEqualTo(SegmentVisibility.TEAM);
            assertThat(results.getFirst().criteria()).hasSize(1);
        }

        @Test
        void createReusableSegmentWritesAuditLog() throws Exception {
            User owner = campaignManagerOwner();
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
            when(segmentRepository.save(any(Segment.class)))
                    .thenAnswer(
                            invocation -> {
                                Segment segment = invocation.getArgument(0);
                                setId(segment, SEGMENT_ID);
                                return segment;
                            });

            segmentService.createSegment(
                    new CreateSegmentCommand(
                            "Audited reusable segment",
                            "Create must be audited",
                            SegmentVisibility.GLOBAL,
                            List.of(
                                    new CreateSegmentCriteriaCommand(
                                            "consent_status",
                                            SegmentOperator.EQUALS,
                                            "GRANTED",
                                            "consent",
                                            SegmentJoinOperator.AND))));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(auditService)
                    .logCreate(
                            eq(OWNER_ID),
                            eq(SegmentService.AUDIT_ENTITY_TYPE),
                            eq(SEGMENT_ID),
                            payloadCaptor.capture());
            assertThat(payloadCaptor.getValue())
                    .containsEntry("name", "Audited reusable segment")
                    .containsEntry("visibility", "GLOBAL")
                    .containsEntry("ownerUserId", OWNER_ID.toString())
                    .containsEntry("criteriaCount", 1);
        }

        @Test
        void createSegmentMethodUsesSegmentCreateAccessForCampaignManagerPath() throws Exception {
            Method createSegment =
                    SegmentService.class.getMethod("createSegment", CreateSegmentCommand.class);
            SegmentCreateAccess createAccess = createSegment.getAnnotation(SegmentCreateAccess.class);
            assertThat(createAccess).isNotNull();

            PreAuthorize preAuthorize = SegmentCreateAccess.class.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).isEqualTo("@authz.canCreateSegments()");
        }

        @Test
        void canCreateSegmentsExpressionAllowsCampaignManager() {
            AuthorizationExpressions authz = new AuthorizationExpressions();
            authenticate(SystemRoleName.CAMPAIGN_MANAGER);
            assertThat(authz.canCreateSegments()).isTrue();
            assertThat(authz.canManageSegments()).isTrue();
        }

        @Test
        void canCreateSegmentsExpressionAllowsAdmin() {
            AuthorizationExpressions authz = new AuthorizationExpressions();
            authenticate(SystemRoleName.ADMIN);
            assertThat(authz.canCreateSegments()).isTrue();
        }

        @Test
        void canCreateSegmentsExpressionDeniesBiAnalystAlone() {
            AuthorizationExpressions authz = new AuthorizationExpressions();
            authenticate(SystemRoleName.BI_ANALYST);
            assertThat(authz.canCreateSegments()).isFalse();
        }

        private User campaignManagerOwner() throws Exception {
            User owner = User.create("campaign.manager@bayer-westphalian.test", "hash", "Campaign Manager");
            setId(owner, OWNER_ID);
            return owner;
        }

        private void authenticate(SystemRoleName role) {
            AuthenticatedPrincipal principal =
                    new AuthenticatedPrincipal(
                            OWNER_ID,
                            role.name().toLowerCase() + "@bayer-westphalian.test",
                            List.of(role));
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    "access-token",
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
        }

        private static void setId(BaseEntity entity, UUID id) throws Exception {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        }
    }

    @Nested
    @WebMvcTest(controllers = SegmentController.class)
    @Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
    class HttpAcceptance {

        @Autowired private MockMvc mockMvc;

        @MockBean private SegmentService segmentService;

        @MockBean private JwtService jwtService;

        @MockBean(name = "jpaMappingContext")
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void campaignManagerCanCreateReusableSegmentViaPostApi() throws Exception {
            when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                    .thenReturn(campaignManagerClaims());
            when(segmentService.createSegment(any(CreateSegmentCommand.class)))
                    .thenReturn(segmentView(SegmentVisibility.TEAM));

            mockMvc.perform(
                            post("/api/segments")
                                    .header("Authorization", "Bearer campaign-manager-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(createReusableSegmentPayload("TEAM")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Segment created"))
                    .andExpect(jsonPath("$.data.id").value(SEGMENT_ID.toString()))
                    .andExpect(jsonPath("$.data.name").value("Expiring life policies"))
                    .andExpect(jsonPath("$.data.visibility").value("TEAM"))
                    .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                    .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"))
                    .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"))
                    .andExpect(
                            jsonPath("$.data.criteria[1].fieldName")
                                    .value("product_expiration_months"));

            ArgumentCaptor<CreateSegmentCommand> commandCaptor =
                    ArgumentCaptor.forClass(CreateSegmentCommand.class);
            verify(segmentService).createSegment(commandCaptor.capture());
            CreateSegmentCommand command = commandCaptor.getValue();
            assertThat(command.name()).isEqualTo("Expiring life policies");
            assertThat(command.description()).isEqualTo("Reusable CM audience for renewals");
            assertThat(command.visibility()).isEqualTo(SegmentVisibility.TEAM);
            assertThat(command.criteria()).hasSize(2);
            assertThat(command.criteria().getFirst().fieldName()).isEqualTo("city");
            assertThat(command.criteria().get(1).fieldName())
                    .isEqualTo("product_expiration_months");
        }

        @Test
        void campaignManagerCanCreateGlobalReusableSegmentViaPostApi() throws Exception {
            when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                    .thenReturn(campaignManagerClaims());
            when(segmentService.createSegment(any(CreateSegmentCommand.class)))
                    .thenReturn(segmentView(SegmentVisibility.GLOBAL));

            mockMvc.perform(
                            post("/api/segments")
                                    .header("Authorization", "Bearer campaign-manager-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(createReusableSegmentPayload("GLOBAL")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.visibility").value("GLOBAL"))
                    .andExpect(jsonPath("$.data.name").value("Expiring life policies"));

            ArgumentCaptor<CreateSegmentCommand> commandCaptor =
                    ArgumentCaptor.forClass(CreateSegmentCommand.class);
            verify(segmentService).createSegment(commandCaptor.capture());
            assertThat(commandCaptor.getValue().visibility()).isEqualTo(SegmentVisibility.GLOBAL);
        }

        @Test
        void createdReusableSegmentCanBeLoadedViaGetApi() throws Exception {
            when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                    .thenReturn(campaignManagerClaims());
            when(segmentService.createSegment(any(CreateSegmentCommand.class)))
                    .thenReturn(segmentView(SegmentVisibility.TEAM));
            when(segmentService.findById(SEGMENT_ID)).thenReturn(segmentView(SegmentVisibility.TEAM));

            mockMvc.perform(
                            post("/api/segments")
                                    .header("Authorization", "Bearer campaign-manager-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(createReusableSegmentPayload("TEAM")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(SEGMENT_ID.toString()));

            mockMvc.perform(
                            get("/api/segments/{id}", SEGMENT_ID)
                                    .header("Authorization", "Bearer campaign-manager-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Segment loaded"))
                    .andExpect(jsonPath("$.data.name").value("Expiring life policies"))
                    .andExpect(jsonPath("$.data.visibility").value("TEAM"))
                    .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"));

            verify(segmentService).createSegment(any(CreateSegmentCommand.class));
            verify(segmentService).findById(SEGMENT_ID);
        }

        @Test
        void adminCanAlsoCreateReusableSegmentViaPostApi() throws Exception {
            when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                    .thenReturn(adminClaims());
            when(segmentService.createSegment(any(CreateSegmentCommand.class)))
                    .thenReturn(segmentView(SegmentVisibility.PRIVATE));

            mockMvc.perform(
                            post("/api/segments")
                                    .header("Authorization", "Bearer admin-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(createReusableSegmentPayload("PRIVATE")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Segment created"));

            verify(segmentService).createSegment(any(CreateSegmentCommand.class));
        }

        @Test
        void biAnalystCannotCreateReusableSegmentViaPostApi() throws Exception {
            when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

            mockMvc.perform(
                            post("/api/segments")
                                    .header("Authorization", "Bearer bi-analyst-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(createReusableSegmentPayload("TEAM")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("Segment created"))));
        }

        @Test
        void productManagerCannotCreateReusableSegmentViaPostApi() throws Exception {
            when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));

            mockMvc.perform(
                            post("/api/segments")
                                    .header("Authorization", "Bearer product-manager-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(createReusableSegmentPayload("TEAM")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("Segment created"))));
        }

        @Test
        void securityConfigurationSegmentCreateRolesIncludeCampaignManager() {
            assertThat(SecurityConfiguration.SEGMENT_CREATE_ROLES)
                    .containsExactly(
                            SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
        }

        private static JwtTokenClaims campaignManagerClaims() {
            return new JwtTokenClaims(
                    OWNER_ID,
                    "campaign.manager@bayer-westphalian.test",
                    List.of(SystemRoleName.CAMPAIGN_MANAGER));
        }

        private static JwtTokenClaims adminClaims() {
            return new JwtTokenClaims(
                    UUID.fromString("10000000-0000-0000-0000-000000009901"),
                    "admin@bayer-westphalian.test",
                    List.of(SystemRoleName.ADMIN));
        }

        private static JwtTokenClaims roleClaims(SystemRoleName role) {
            return new JwtTokenClaims(
                    UUID.fromString("10000000-0000-0000-0000-000000009902"),
                    role.name().toLowerCase().replace('_', '.') + "@bayer-westphalian.test",
                    List.of(role));
        }

        private static String createReusableSegmentPayload(String visibility) {
            return """
                    {
                      "name": "Expiring life policies",
                      "description": "Reusable CM audience for renewals",
                      "visibility": "%s",
                      "criteria": [
                        {
                          "fieldName": "city",
                          "operator": "EQUALS",
                          "value": "Munich",
                          "logicalGroup": "location",
                          "joinOperator": "AND"
                        },
                        {
                          "fieldName": "product_expiration_months",
                          "operator": "EQUALS",
                          "value": "6",
                          "logicalGroup": "expiration",
                          "joinOperator": "AND"
                        }
                      ]
                    }
                    """
                    .formatted(visibility);
        }

        private static SegmentView segmentView(SegmentVisibility visibility) {
            return new SegmentView(
                    SEGMENT_ID,
                    "Expiring life policies",
                    "Reusable CM audience for renewals",
                    OWNER_ID,
                    "Campaign Manager",
                    visibility,
                    List.of(
                            new SegmentCriteriaView(
                                    CRITERION_ID,
                                    SEGMENT_ID,
                                    "city",
                                    SegmentOperator.EQUALS,
                                    "Munich",
                                    "location",
                                    SegmentJoinOperator.AND),
                            new SegmentCriteriaView(
                                    UUID.fromString("42000000-0000-0000-0000-000000000212"),
                                    SEGMENT_ID,
                                    "product_expiration_months",
                                    SegmentOperator.EQUALS,
                                    "6",
                                    "expiration",
                                    SegmentJoinOperator.AND)),
                    CREATED_AT,
                    UPDATED_AT);
        }
    }
}
