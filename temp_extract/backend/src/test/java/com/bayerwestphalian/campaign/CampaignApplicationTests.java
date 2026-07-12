package com.bayerwestphalian.campaign;

import com.bayerwestphalian.campaign.audit.AuditLogRepository;
import com.bayerwestphalian.campaign.beneficiary.BeneficiaryRepository;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.ProductChangeRequestRepository;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.segment.SegmentCriteriaRepository;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.user.RoleRepository;
import com.bayerwestphalian.campaign.user.UserRepository;
import com.bayerwestphalian.campaign.user.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        properties = {
            "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
class CampaignApplicationTests {

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean private AuditLogRepository auditLogRepository;

    @MockBean private BeneficiaryRepository beneficiaryRepository;

    @MockBean private ConsentRepository consentRepository;

    @MockBean private CustomerRepository customerRepository;

    @MockBean private PaymentRecordRepository paymentRecordRepository;

    @MockBean private ProductChangeRequestRepository productChangeRequestRepository;

    @MockBean private ProductOwnershipRepository productOwnershipRepository;

    @MockBean private ProductRepository productRepository;

    @MockBean private SegmentCriteriaRepository segmentCriteriaRepository;

    @MockBean private SegmentRepository segmentRepository;

    @MockBean private JdbcTemplate jdbcTemplate;

    @MockBean private RoleRepository roleRepository;

    @MockBean private UserRepository userRepository;

    @MockBean private UserRoleRepository userRoleRepository;

    @Test
    void contextLoads() {}
}
