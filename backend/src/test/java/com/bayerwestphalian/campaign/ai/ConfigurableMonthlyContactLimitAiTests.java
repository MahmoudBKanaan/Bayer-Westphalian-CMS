package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Item 535: AI-006 duplicate-contact risk uses the Admin-configured monthly contact limit from
 * system settings (BR-011 advisory warning).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("535 Configurable monthly contact limit (AI-006)")
class ConfigurableMonthlyContactLimitAiTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000535");

    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductOwnershipRepository ownershipRepository;
    @Mock private PaymentRecordRepository paymentRecordRepository;
    @Mock private ContactEventRepository contactEventRepository;
    @Mock private AiRecommendationRepository aiRecommendationRepository;
    @Mock private SystemSettingsService systemSettingsService;

    private AiRecommendationService service;

    @BeforeEach
    void setUp() {
        service =
                new AiRecommendationService(
                        customerRepository,
                        productRepository,
                        ownershipRepository,
                        paymentRecordRepository,
                        contactEventRepository,
                        aiRecommendationRepository,
                        systemSettingsService);
    }

    @Test
    void detectDuplicateRiskUsesConfiguredMonthlyLimitInViewAndExplanation() {
        when(systemSettingsService.monthlyContactLimit()).thenReturn(5);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(contactEventRepository.countRecentCustomerMarketingContacts(any(), any()))
                .thenReturn(5L);
        when(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DuplicateContactRiskView view =
                service.detectDuplicateRisk(new DuplicateContactRiskRequest(CUSTOMER_ID, null));

        assertThat(view.riskDetected()).isTrue();
        assertThat(view.monthlyContactLimit()).isEqualTo(5);
        assertThat(view.contactsInCurrentMonth()).isEqualTo(5);
        assertThat(view.explanation()).contains("monthly limit=5");
        assertThat(view.warning()).contains("BR-011 monthly contact limit");
        verify(systemSettingsService).monthlyContactLimit();
    }

    @Test
    void detectDuplicateRiskDoesNotFlagWhenBelowRaisedConfiguredLimit() {
        when(systemSettingsService.monthlyContactLimit()).thenReturn(10);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(contactEventRepository.countRecentCustomerMarketingContacts(any(), any()))
                .thenReturn(3L);
        when(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DuplicateContactRiskView view =
                service.detectDuplicateRisk(new DuplicateContactRiskRequest(CUSTOMER_ID, null));

        assertThat(view.riskDetected()).isFalse();
        assertThat(view.monthlyContactLimit()).isEqualTo(10);
        assertThat(view.warning()).contains("No duplicate-contact risk detected");
        verify(systemSettingsService).monthlyContactLimit();
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ai", "Limit");
        try {
            Field id = BaseEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(customer, CUSTOMER_ID);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return customer;
    }
}
