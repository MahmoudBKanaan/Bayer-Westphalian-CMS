package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.consent.ConsentRecord;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentStatus;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers(disabledWithoutDocker = true)
@Import({
    SegmentService.class,
    SegmentController.class,
    GlobalExceptionHandler.class,
    EligibilityService.class,
    AuditService.class
})
class SegmentControllerIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_segment_controller_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private MockMvc mockMvc;

    @Autowired private TestEntityManager entityManager;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    @MockBean private ConsentService consentService;

    private User owner;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        owner = persistUser("segment-controller-owner");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), any(ConsentType.class), anyBoolean()))
                .thenReturn(true);
        when(consentService.isCommunicationEligible(any(Customer.class), any(ConsentType.class)))
                .thenReturn(true);
    }

    @Test
    void createsSegmentWritesAuditLogThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Controller audit segment",
                                                  "description": "Audit via API",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "city",
                                                      "operator": "EQUALS",
                                                      "value": "Munich",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.name").value("Controller audit segment"))
                        .andReturn();

        String segmentId =
                JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        List<AuditLog> logs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :entityType "
                                        + "and a.entityId = :entityId and a.action = :action",
                                AuditLog.class)
                        .setParameter("entityType", "segments")
                        .setParameter("entityId", UUID.fromString(segmentId))
                        .setParameter("action", "CREATE")
                        .getResultList();

        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getActorUserId()).isEqualTo(owner.getId());
        assertThat(logs.getFirst().getNewValue()).containsEntry("name", "Controller audit segment");
        assertThat(logs.getFirst().getNewValue()).containsEntry("criteriaCount", 1);
    }

    @Test
    void createsAndLoadsSegmentThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Munich prospects",
                                                  "description": "Customers located in Munich",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "city",
                                                      "operator": "EQUALS",
                                                      "value": "Munich",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.message").value("Segment created"))
                        .andExpect(jsonPath("$.data.name").value("Munich prospects"))
                        .andExpect(jsonPath("$.data.ownerUserId").value(owner.getId().toString()))
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"))
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.id").value(segmentId.toString()))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"));
    }

    @Test
    void createsReusableSegmentWithDefaultPrivateVisibility() throws Exception {
        mockMvc.perform(
                        post("/api/segments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Saved renewal audience",
                                          "description": "Reusable segment without explicit visibility"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Segment created"))
                .andExpect(jsonPath("$.data.name").value("Saved renewal audience"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.ownerUserId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.data.criteria").isEmpty());
    }

    @Test
    void createsSegmentWithCombinedAndOrCriteria() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);

        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Prospects or Munich customers",
                                                  "description": "Combined AND/OR audience rules",
                                                  "visibility": "GLOBAL",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "customer_type",
                                                      "operator": "EQUALS",
                                                      "value": "PROSPECT",
                                                      "joinOperator": "AND"
                                                    },
                                                    {
                                                      "fieldName": "city",
                                                      "operator": "EQUALS",
                                                      "value": "Munich",
                                                      "joinOperator": "OR"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("customer_type"))
                        .andExpect(jsonPath("$.data.criteria[0].joinOperator").value("AND"))
                        .andExpect(jsonPath("$.data.criteria[1].fieldName").value("city"))
                        .andExpect(jsonPath("$.data.criteria[1].joinOperator").value("OR"))
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria.length()").value(2))
                .andExpect(jsonPath("$.data.criteria[1].value").value("Munich"));
    }

    @Test
    void loadsSegmentDetailsWithFullCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Detail audience",
                                                  "description": "Segment for detail endpoint",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "city",
                                                      "operator": "EQUALS",
                                                      "value": "Hamburg",
                                                      "logicalGroup": "location",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.id").value(segmentId.toString()))
                .andExpect(jsonPath("$.data.name").value("Detail audience"))
                .andExpect(jsonPath("$.data.description").value("Segment for detail endpoint"))
                .andExpect(jsonPath("$.data.ownerUserId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.data.ownerFullName").value(owner.getFullName()))
                .andExpect(jsonPath("$.data.visibility").value("TEAM"))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("Hamburg"))
                .andExpect(jsonPath("$.data.criteria[0].logicalGroup").value("location"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    void loadsGlobalSegmentDetailsForNonOwner() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Shared audience",
                                                  "visibility": "GLOBAL"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        UUID otherUserId = UUID.fromString("10000000-0000-0000-0000-000000009999");
        when(authorizationExpressions.currentUserId()).thenReturn(otherUserId);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Shared audience"))
                .andExpect(jsonPath("$.data.visibility").value("GLOBAL"));
    }

    @Test
    void adminCanLoadPrivateSegmentOwnedByAnotherUser() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Private owner audience",
                                                  "visibility": "PRIVATE"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        UUID otherUserId = UUID.fromString("10000000-0000-0000-0000-000000009998");
        when(authorizationExpressions.currentUserId()).thenReturn(otherUserId);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(true);

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Private owner audience"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.ownerUserId").value(owner.getId().toString()));
    }

    @Test
    void blocksPrivateSegmentDetailsForNonOwnerThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Restricted audience",
                                                  "visibility": "PRIVATE"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        UUID otherUserId = UUID.fromString("10000000-0000-0000-0000-000000009997");
        when(authorizationExpressions.currentUserId()).thenReturn(otherUserId);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + segmentId));
    }

    @Test
    void listsSegmentsThroughController() throws Exception {
        mockMvc.perform(
                        post("/api/segments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Berlin customers",
                                          "description": "Customers located in Berlin",
                                          "visibility": "GLOBAL",
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Berlin",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/segments").param("term", "Berlin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segments loaded"))
                .andExpect(jsonPath("$.data[0].name").value("Berlin customers"))
                .andExpect(jsonPath("$.data[0].visibility").value("GLOBAL"));
    }

    @Test
    void updatesSegmentMetadataWithoutReplacingCriteria() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Initial audience",
                                                  "visibility": "PRIVATE",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "city",
                                                      "operator": "EQUALS",
                                                      "value": "Munich",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(
                        put("/api/segments/{id}", segmentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Renamed audience",
                                          "description": "Updated description only",
                                          "visibility": "TEAM"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment updated"))
                .andExpect(jsonPath("$.data.name").value("Renamed audience"))
                .andExpect(jsonPath("$.data.description").value("Updated description only"))
                .andExpect(jsonPath("$.data.visibility").value("TEAM"))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"));
    }

    @Test
    void adminCanUpdateSegmentOwnedByAnotherUser() throws Exception {
        User otherOwner = persistUser("segment-other-owner");
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(true);

        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Owned by campaign manager",
                                                  "visibility": "PRIVATE"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        when(authorizationExpressions.currentUserId()).thenReturn(otherOwner.getId());

        mockMvc.perform(
                        put("/api/segments/{id}", segmentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Admin refined audience",
                                          "description": "Updated by admin",
                                          "visibility": "GLOBAL"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment updated"))
                .andExpect(jsonPath("$.data.name").value("Admin refined audience"))
                .andExpect(jsonPath("$.data.visibility").value("GLOBAL"))
                .andExpect(jsonPath("$.data.ownerUserId").value(owner.getId().toString()));
    }

    @Test
    void deletesSegmentAndRemovesItFromSearchResults() throws Exception {
        MvcResult disposableResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Disposable audience",
                                                  "visibility": "GLOBAL"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        mockMvc.perform(
                        post("/api/segments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Keep audience",
                                          "visibility": "GLOBAL"
                                        }
                                        """))
                .andExpect(status().isCreated());

        UUID disposableSegmentId =
                UUID.fromString(
                        JsonPath.read(
                                disposableResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(delete("/api/segments/{id}", disposableSegmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment deleted"));

        mockMvc.perform(get("/api/segments").param("term", "Keep"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Keep audience"));

        mockMvc.perform(get("/api/segments/{id}", disposableSegmentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void adminCanDeleteSegmentOwnedByAnotherUser() throws Exception {
        User otherOwner = persistUser("segment-delete-other-owner");
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(true);

        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Delete by admin",
                                                  "visibility": "PRIVATE"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        when(authorizationExpressions.currentUserId()).thenReturn(otherOwner.getId());

        mockMvc.perform(delete("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment deleted"));

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void blocksNonOwnerDeleteThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Owned audience",
                                                  "visibility": "PRIVATE"
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        UUID otherUserId = UUID.fromString("10000000-0000-0000-0000-000000009999");
        when(authorizationExpressions.currentUserId()).thenReturn(otherUserId);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);

        mockMvc.perform(delete("/api/segments/{id}", segmentId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + segmentId));
    }

    @Test
    void updatesAndDeletesSegmentThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Prospect audience",
                                                  "description": "Active prospects",
                                                  "visibility": "PRIVATE",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "customer_type",
                                                      "operator": "EQUALS",
                                                      "value": "PROSPECT",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(
                        put("/api/segments/{id}", segmentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Refined prospect audience",
                                          "description": "Prospects in Munich",
                                          "visibility": "TEAM",
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment updated"))
                .andExpect(jsonPath("$.data.name").value("Refined prospect audience"))
                .andExpect(jsonPath("$.data.visibility").value("TEAM"))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"));

        mockMvc.perform(delete("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment deleted"));

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void previewsSegmentAudienceThroughController() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].city").value("Munich"));
    }

    @Test
    void previewsSegmentWithCombinedAndOrCriteriaThroughController() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        persistCustomer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);

        // Left-to-right: joinOperator on each criterion after the first links to the prior result.
        // PROSPECT OR city=Munich => Lena (prospect) + Anna (Munich customer).
        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "customer_type",
                                              "operator": "EQUALS",
                                              "value": "PROSPECT",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "OR"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(2))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(2));
    }

    @Test
    void previewsSegmentWithPureOrCityChainThroughController() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        persistCustomer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Berlin",
                                              "joinOperator": "OR"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Hamburg",
                                              "joinOperator": "OR"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(3))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(3));
    }

    @Test
    void createsSegmentWithOrJoinOperatorThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Munich or Berlin audience",
                                                  "description": "KB FR-078 OR criteria segment",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "city",
                                                      "operator": "EQUALS",
                                                      "value": "Munich",
                                                      "logicalGroup": "location",
                                                      "joinOperator": "AND"
                                                    },
                                                    {
                                                      "fieldName": "city",
                                                      "operator": "EQUALS",
                                                      "value": "Berlin",
                                                      "logicalGroup": "location",
                                                      "joinOperator": "OR"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].joinOperator").value("AND"))
                        .andExpect(jsonPath("$.data.criteria[1].joinOperator").value("OR"))
                        .andExpect(jsonPath("$.data.criteria[1].value").value("Berlin"))
                        .andReturn();

        String segmentId =
                JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[1].joinOperator").value("OR"));
    }

    @Test
    void previewsAudienceFilteredByOwnedProductTypeThroughController() throws Exception {
        Customer lifeCustomer = persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer prospectCustomer =
                persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product homeProduct = persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);
        persistOwnership(lifeCustomer, lifeProduct);
        persistOwnership(prospectCustomer, homeProduct);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "product_type",
                                              "operator": "EQUALS",
                                              "value": "LIFE_INSURANCE",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Tom"));
    }

    @Test
    void previewsAudienceWithoutOwnedHomeownerInsuranceThroughController() throws Exception {
        Customer withHomeInsurance =
                persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer withoutHomeInsurance =
                persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Product homeProduct = persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);
        persistOwnership(withHomeInsurance, homeProduct);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "product_ownership",
                                              "operator": "NOT_EQUALS",
                                              "value": "HOMEOWNER_INSURANCE",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"));
    }

    @Test
    void createsSegmentWithProductOwnershipCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Life insurance owners",
                                                  "description": "Customers with active life insurance ownership",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "owned_product_type",
                                                      "operator": "EQUALS",
                                                      "value": "life_insurance",
                                                      "logicalGroup": "product",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("product_type"))
                        .andExpect(jsonPath("$.data.criteria[0].value").value("LIFE_INSURANCE"))
                        .andReturn();

        String segmentId =
                JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("product_type"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("LIFE_INSURANCE"));
    }

    @Test
    void previewsAudienceFilteredByPaymentStatusThroughController() throws Exception {
        Customer overdueCustomer =
                persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer paidCustomer = persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product autoProduct = persistProduct("Auto Cover", ProductType.AUTO_INSURANCE);
        ProductOwnership overdueOwnership = persistOwnership(overdueCustomer, lifeProduct);
        ProductOwnership paidOwnership = persistOwnership(paidCustomer, autoProduct);
        persistOverduePayment(overdueCustomer, overdueOwnership);
        persistPaidPayment(paidCustomer, paidOwnership);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "payment_status",
                                              "operator": "EQUALS",
                                              "value": "OVERDUE",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Tom"));
    }

    @Test
    void previewsAudienceWithoutDefaultRiskPaymentHistoryThroughController() throws Exception {
        Customer defaultRiskCustomer =
                persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer paidCustomer = persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Product homeProduct = persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);
        Product lifeProduct = persistProduct("Life Cover", ProductType.LIFE_INSURANCE);
        ProductOwnership riskOwnership = persistOwnership(defaultRiskCustomer, homeProduct);
        ProductOwnership paidOwnership = persistOwnership(paidCustomer, lifeProduct);
        persistDefaultRiskPayment(defaultRiskCustomer, riskOwnership);
        persistPaidPayment(paidCustomer, paidOwnership);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "payment_history",
                                              "operator": "NOT_EQUALS",
                                              "value": "DEFAULT_RISK",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"));
    }

    @Test
    void createsSegmentWithPaymentHistoryCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Default risk payers",
                                                  "description": "Customers with high reminder escalation",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "payment_history",
                                                      "operator": "EQUALS",
                                                      "value": "default_risk",
                                                      "logicalGroup": "payment",
                                                      "joinOperator": "AND"
                                                    },
                                                    {
                                                      "fieldName": "reminder_count",
                                                      "operator": "AFTER",
                                                      "value": "2",
                                                      "logicalGroup": "payment",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("payment_status"))
                        .andExpect(jsonPath("$.data.criteria[0].value").value("DEFAULT_RISK"))
                        .andExpect(jsonPath("$.data.criteria[1].fieldName").value("reminder_count"))
                        .andExpect(jsonPath("$.data.criteria[1].value").value("2"))
                        .andReturn();

        String segmentId =
                JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("payment_status"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("DEFAULT_RISK"))
                .andExpect(jsonPath("$.data.criteria[1].fieldName").value("reminder_count"));
    }

    @Test
    void previewsAudienceFilteredByAgeGroupThroughController() throws Exception {
        Customer youngProspect =
                persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        youngProspect.updateDemographics(
                null, com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_18_25);
        entityManager.persistAndFlush(youngProspect);

        Customer matureCustomer =
                persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        matureCustomer.updateDemographics(
                null, com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_41_60);
        entityManager.persistAndFlush(matureCustomer);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "age_group",
                                              "operator": "EQUALS",
                                              "value": "AGE_18_25",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].ageGroup").value("AGE_18_25"));
    }

    @Test
    void previewsSegmentWithTotalAudienceCountThroughController() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        persistCustomer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(2))
                .andExpect(jsonPath("$.data.eligibleCount").value(2))
                .andExpect(jsonPath("$.data.excludedCount").value(0))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(2));
    }

    @Test
    void previewsSegmentWithEligibleCountThroughController() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer blocked = persistCustomer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        blocked.markDoNotContact();
        entityManager.persistAndFlush(blocked);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(2))
                .andExpect(jsonPath("$.data.eligibleCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"));
    }

    @Test
    void previewsSegmentWithExcludedCountThroughController() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer blockedOne = persistCustomer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        blockedOne.markDoNotContact();
        entityManager.persistAndFlush(blockedOne);
        Customer blockedTwo = persistCustomer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        blockedTwo.markDoNotContact();
        entityManager.persistAndFlush(blockedTwo);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(3))
                .andExpect(jsonPath("$.data.eligibleCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(2))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(1))
                .andExpect(jsonPath("$.data.exclusionReasonSummary.length()").value(1))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[0].code").value("DO_NOT_CONTACT"))
                .andExpect(jsonPath("$.data.exclusionReasonSummary[0].count").value(2));
    }

    @Test
    void previewsSegmentWithExclusionReasonSummaryThroughController() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer blocked = persistCustomer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        blocked.markDoNotContact();
        entityManager.persistAndFlush(blocked);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[0].code").value("DO_NOT_CONTACT"))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[0].message")
                                .value("Customer has do-not-contact enabled"))
                .andExpect(jsonPath("$.data.exclusionReasonSummary[0].count").value(1));
    }

    @Test
    void previewsAudienceFilteredByCityThroughController() throws Exception {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "logicalGroup": "location",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].city").value("Munich"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].country").value("Germany"));
    }

    @Test
    void previewsAudienceFilteredByCountryThroughController() throws Exception {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Anna", "Weber", "Vienna", "Austria", CustomerType.CUSTOMER);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "country",
                                              "operator": "EQUALS",
                                              "value": "Germany",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].country").value("Germany"));
    }

    @Test
    void createsSegmentWithLocationCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Southern Germany audience",
                                                  "description": "Customers in Munich and Stuttgart",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "location",
                                                      "operator": "IN",
                                                      "value": "Munich, Stuttgart",
                                                      "logicalGroup": "location",
                                                      "joinOperator": "AND"
                                                    },
                                                    {
                                                      "fieldName": "country",
                                                      "operator": "EQUALS",
                                                      "value": "Germany",
                                                      "logicalGroup": "location",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"))
                        .andExpect(jsonPath("$.data.criteria[0].value").value("Munich,Stuttgart"))
                        .andExpect(jsonPath("$.data.criteria[1].fieldName").value("country"))
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].logicalGroup").value("location"))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"));
    }

    @Test
    void previewsAudienceFilteredByCustomerTypeThroughController() throws Exception {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "customer_type",
                                              "operator": "EQUALS",
                                              "value": "PROSPECT",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].customerType").value("PROSPECT"));
    }

    @Test
    void createsSegmentWithCustomerTypeCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Prospects and beneficiaries",
                                                  "description": "Non-policyholder audience",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "type",
                                                      "operator": "IN",
                                                      "value": "prospect, BENEFICIARY",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("customer_type"))
                        .andExpect(
                                jsonPath("$.data.criteria[0].value").value("PROSPECT,BENEFICIARY"))
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("customer_type"))
                .andExpect(jsonPath("$.data.criteria[0].operator").value("IN"));
    }

    @Test
    void rejectsUnsupportedCustomerTypeCriterionThroughController() throws Exception {
        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "customer_type",
                                              "operator": "EQUALS",
                                              "value": "LEAD",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments/preview"));
    }

    @Test
    void previewsAudienceFilteredByBehaviorStatusThroughController() throws Exception {
        Customer interested =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        interested.changeStatus(CustomerStatus.INTERESTED);
        interested.recordSource("LIFE_INSURANCE_BENEFICIARY");
        entityManager.persistAndFlush(interested);

        Customer uninterested =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        uninterested.changeStatus(CustomerStatus.UNINTERESTED);
        entityManager.persistAndFlush(uninterested);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "status",
                                              "operator": "EQUALS",
                                              "value": "INTERESTED",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].status").value("INTERESTED"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"));
    }

    @Test
    void previewsAudienceFilteredByDoNotContactAndInterestThroughController() throws Exception {
        Customer contactable =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        contactable.recordSource("LIFE_INSURANCE_BENEFICIARY");
        entityManager.persistAndFlush(contactable);

        Customer blocked =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        blocked.markDoNotContact();
        blocked.recordSource("LIFE_INSURANCE_BENEFICIARY");
        entityManager.persistAndFlush(blocked);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "do_not_contact",
                                              "operator": "EQUALS",
                                              "value": "false",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "interest",
                                              "operator": "CONTAINS",
                                              "value": "LIFE_INSURANCE",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].doNotContact").value(false));
    }

    @Test
    void createsSegmentWithBehaviorStatusCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Interested life-insurance beneficiaries",
                                                  "description": "Behavior and interest based audience",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "behavior",
                                                      "operator": "IN",
                                                      "value": "interested, converted",
                                                      "logicalGroup": "behavior",
                                                      "joinOperator": "AND"
                                                    },
                                                    {
                                                      "fieldName": "interests",
                                                      "operator": "CONTAINS",
                                                      "value": "LIFE_INSURANCE",
                                                      "logicalGroup": "behavior",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("status"))
                        .andExpect(
                                jsonPath("$.data.criteria[0].value").value("INTERESTED,CONVERTED"))
                        .andExpect(jsonPath("$.data.criteria[1].fieldName").value("source"))
                        .andReturn();

        String segmentId =
                JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("status"))
                .andExpect(jsonPath("$.data.criteria[1].fieldName").value("source"));
    }

    @Test
    void rejectsUnsupportedBehaviorStatusCriterionThroughController() throws Exception {
        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "status",
                                              "operator": "EQUALS",
                                              "value": "WARM_LEAD",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments/preview"));
    }

    @Test
    void previewsAudienceFilteredByConsentStatusThroughController() throws Exception {
        Customer consented =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer optedOut =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        persistGivenMarketingConsent(consented);
        persistWithdrawnMarketingConsent(optedOut);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "consent_status",
                                              "operator": "EQUALS",
                                              "value": "GIVEN",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"));
    }

    @Test
    void previewsAudienceWithValidMarketingConsentThroughController() throws Exception {
        Customer consented =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer optedOut =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        persistGivenMarketingConsent(consented);
        persistWithdrawnMarketingConsent(optedOut);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "marketing_consent",
                                              "operator": "EQUALS",
                                              "value": "true",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "opt_out",
                                              "operator": "EQUALS",
                                              "value": "false",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"));
    }

    @Test
    void createsSegmentWithConsentStatusCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Consented marketing audience",
                                                  "description": "Customers with valid marketing consent and no opt-out",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "consent",
                                                      "operator": "EQUALS",
                                                      "value": "given",
                                                      "logicalGroup": "consent",
                                                      "joinOperator": "AND"
                                                    },
                                                    {
                                                      "fieldName": "has_valid_marketing_consent",
                                                      "operator": "EQUALS",
                                                      "value": "true",
                                                      "logicalGroup": "consent",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("consent_status"))
                        .andExpect(jsonPath("$.data.criteria[0].value").value("GIVEN"))
                        .andExpect(
                                jsonPath("$.data.criteria[1].fieldName")
                                        .value("has_valid_marketing_consent"))
                        .andReturn();

        String segmentId =
                JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("consent_status"))
                .andExpect(
                        jsonPath("$.data.criteria[1].fieldName")
                                .value("has_valid_marketing_consent"));
    }

    @Test
    void rejectsUnsupportedConsentStatusCriterionThroughController() throws Exception {
        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "consent_status",
                                              "operator": "EQUALS",
                                              "value": "PENDING",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments/preview"));
    }

    @Test
    void previewsAudienceFilteredByProductExpirationThroughController() throws Exception {
        Customer expiringSoon = persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer farFuture = persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product autoProduct = persistProduct("Auto Cover", ProductType.AUTO_INSURANCE);
        persistOwnershipExpiringInMonths(expiringSoon, lifeProduct, 2);
        persistOwnershipExpiringInMonths(farFuture, autoProduct, 18);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "expiring_within_months",
                                              "operator": "EQUALS",
                                              "value": "3",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Tom"));
    }

    @Test
    void createsSegmentWithProductExpirationCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Policies expiring within six months",
                                                  "description": "KB product-expiration segment for BR-023 windows",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "product_expiration",
                                                      "operator": "EQUALS",
                                                      "value": "6",
                                                      "logicalGroup": "product",
                                                      "joinOperator": "AND"
                                                    },
                                                    {
                                                      "fieldName": "is_expiring",
                                                      "operator": "EQUALS",
                                                      "value": "true",
                                                      "logicalGroup": "product",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(
                                jsonPath("$.data.criteria[0].fieldName")
                                        .value("expiring_within_months"))
                        .andExpect(jsonPath("$.data.criteria[0].value").value("6"))
                        .andExpect(jsonPath("$.data.criteria[1].fieldName").value("is_expiring"))
                        .andExpect(jsonPath("$.data.criteria[1].value").value("true"))
                        .andReturn();

        String segmentId =
                JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("expiring_within_months"))
                .andExpect(jsonPath("$.data.criteria[1].fieldName").value("is_expiring"));
    }

    @Test
    void rejectsUnsupportedProductExpirationCriterionThroughController() throws Exception {
        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "expiring_within_months",
                                              "operator": "EQUALS",
                                              "value": "soon",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments/preview"));
    }

    @Test
    void rejectsUnsupportedAgeGroupCriterionThroughController() throws Exception {
        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "age_group",
                                              "operator": "EQUALS",
                                              "value": "TEENAGER",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments/preview"));
    }

    @Test
    void createsSegmentWithAgeGroupCriteriaThroughController() throws Exception {
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/segments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "name": "Young adult audience",
                                                  "description": "Customers aged 18-25",
                                                  "visibility": "TEAM",
                                                  "criteria": [
                                                    {
                                                      "fieldName": "age_group",
                                                      "operator": "IN",
                                                      "value": "18_25,AGE_26_40",
                                                      "logicalGroup": "demographics",
                                                      "joinOperator": "AND"
                                                    }
                                                  ]
                                                }
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.criteria[0].fieldName").value("age_group"))
                        .andExpect(jsonPath("$.data.criteria[0].value").value("18_25,26_40"))
                        .andReturn();

        UUID segmentId =
                UUID.fromString(
                        JsonPath.read(
                                createResult.getResponse().getContentAsString(), "$.data.id"));

        mockMvc.perform(get("/api/segments/{id}", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criteria[0].logicalGroup").value("demographics"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("18_25,26_40"));
    }

    @Test
    void previewsEmptyCriteriaAsFullActiveAudienceThroughController() throws Exception {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);

        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": []
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(2))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(2));
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@segment-controller-integration.test",
                        "{noop}password",
                        "Segment Controller Integration User");
        return entityManager.persistAndFlush(user);
    }

    private Customer persistCustomer(
            String firstName, String lastName, String city, CustomerType customerType) {
        return persistCustomerWithLocation(firstName, lastName, city, "Germany", customerType);
    }

    private Customer persistCustomerWithLocation(
            String firstName,
            String lastName,
            String city,
            String country,
            CustomerType customerType) {
        Customer customer = Customer.create(customerType, firstName, lastName);
        customer.updateAddress(null, city, country);
        customer.changeStatus(CustomerStatus.ACTIVE);
        return entityManager.persistAndFlush(customer);
    }

    private Product persistProduct(String name, ProductType productType) {
        Product product = Product.create(name, productType, new BigDecimal("99.00"), 12);
        return entityManager.persistAndFlush(product);
    }

    private ProductOwnership persistOwnershipExpiringInMonths(
            Customer customer, Product product, int months) {
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.now().minusMonths(6),
                        LocalDate.now().plusMonths(months));
        return entityManager.persistAndFlush(ownership);
    }

    private ProductOwnership persistOwnership(Customer customer, Product product) {
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.now().minusMonths(6),
                        LocalDate.now().plusYears(1));
        return entityManager.persistAndFlush(ownership);
    }

    private PaymentRecord persistOverduePayment(Customer customer, ProductOwnership ownership) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership,
                        LocalDate.now().minusDays(10),
                        new BigDecimal("120.00"));
        payment.markOverdue();
        return entityManager.persistAndFlush(payment);
    }

    private PaymentRecord persistPaidPayment(Customer customer, ProductOwnership ownership) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.now().minusDays(5), new BigDecimal("80.00"));
        payment.markPaid(new BigDecimal("80.00"), Instant.now());
        return entityManager.persistAndFlush(payment);
    }

    private PaymentRecord persistDefaultRiskPayment(Customer customer, ProductOwnership ownership) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership,
                        LocalDate.now().minusDays(30),
                        new BigDecimal("200.00"));
        payment.incrementReminder();
        payment.incrementReminder();
        payment.incrementReminder();
        return entityManager.persistAndFlush(payment);
    }

    private ConsentRecord persistGivenMarketingConsent(Customer customer) {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        return entityManager.persistAndFlush(consent);
    }

    private ConsentRecord persistWithdrawnMarketingConsent(Customer customer) {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        consent.withdraw(Instant.now());
        return entityManager.persistAndFlush(consent);
    }
}
