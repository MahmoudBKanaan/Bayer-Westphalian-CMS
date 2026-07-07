package com.bayerwestphalian.campaign.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ConflictException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTests {

    private static final UUID BENEFICIARY_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID POLICYHOLDER_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000101");
    private static final UUID BENEFICIARY_CUSTOMER_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000102");

    @Mock private BeneficiaryRepository beneficiaryRepository;

    @Mock private CustomerRepository customerRepository;

    @InjectMocks private BeneficiaryService beneficiaryService;

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorize("createBeneficiary", CreateBeneficiaryCommand.class);
        assertPreAuthorize("updateBeneficiary", UUID.class, UpdateBeneficiaryCommand.class);
        assertPreAuthorize("deleteBeneficiary", UUID.class);
        assertPreAuthorize("findById", UUID.class);
        assertPreAuthorize("searchBeneficiaries", BeneficiarySearchCriteria.class);
    }

    @Test
    void createsBeneficiaryLinkWithGuardianConsent() throws Exception {
        Customer policyholder =
                customer(POLICYHOLDER_ID, CustomerType.CUSTOMER, "Ada", "Policyholder");
        Customer beneficiaryCustomer =
                customer(BENEFICIARY_CUSTOMER_ID, CustomerType.BENEFICIARY, "Ben", "Beneficiary");
        when(beneficiaryRepository.existsByPolicyholderCustomerIdAndBeneficiaryCustomerId(
                        POLICYHOLDER_ID, BENEFICIARY_CUSTOMER_ID))
                .thenReturn(false);
        when(customerRepository.findById(POLICYHOLDER_ID)).thenReturn(Optional.of(policyholder));
        when(customerRepository.findById(BENEFICIARY_CUSTOMER_ID))
                .thenReturn(Optional.of(beneficiaryCustomer));
        when(beneficiaryRepository.save(any(Beneficiary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryView view =
                beneficiaryService.createBeneficiary(
                        new CreateBeneficiaryCommand(
                                POLICYHOLDER_ID,
                                BENEFICIARY_CUSTOMER_ID,
                                " Grandchild ",
                                " Guardian User ",
                                " guardian@bayer-westphalian.test ",
                                true));

        ArgumentCaptor<Beneficiary> beneficiaryCaptor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(beneficiaryRepository).save(beneficiaryCaptor.capture());
        Beneficiary saved = beneficiaryCaptor.getValue();
        assertThat(saved.getPolicyholderCustomer()).isSameAs(policyholder);
        assertThat(saved.getBeneficiaryCustomer()).isSameAs(beneficiaryCustomer);
        assertThat(saved.getRelationship()).isEqualTo("Grandchild");
        assertThat(saved.getGuardianName()).isEqualTo("Guardian User");
        assertThat(saved.getGuardianEmail()).isEqualTo("guardian@bayer-westphalian.test");
        assertThat(saved.isGuardianConsentRequired()).isTrue();
        assertThat(view.policyholderCustomerId()).isEqualTo(POLICYHOLDER_ID);
        assertThat(view.policyholderFullName()).isEqualTo("Ada Policyholder");
        assertThat(view.beneficiaryCustomerId()).isEqualTo(BENEFICIARY_CUSTOMER_ID);
        assertThat(view.beneficiaryFullName()).isEqualTo("Ben Beneficiary");
        assertThat(view.guardianConsentRequired()).isTrue();
        assertThat(view.hasGuardianRequirement()).isTrue();
    }

    @Test
    void rejectsDuplicateOrSelfBeneficiaryLinks() {
        when(beneficiaryRepository.existsByPolicyholderCustomerIdAndBeneficiaryCustomerId(
                        POLICYHOLDER_ID, BENEFICIARY_CUSTOMER_ID))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                beneficiaryService.createBeneficiary(
                                        new CreateBeneficiaryCommand(
                                                POLICYHOLDER_ID,
                                                BENEFICIARY_CUSTOMER_ID,
                                                "Grandchild",
                                                null,
                                                null,
                                                false)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Beneficiary link already exists");

        assertThatThrownBy(
                        () ->
                                beneficiaryService.createBeneficiary(
                                        new CreateBeneficiaryCommand(
                                                POLICYHOLDER_ID,
                                                POLICYHOLDER_ID,
                                                "Self",
                                                null,
                                                null,
                                                false)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Beneficiary validation failed");
    }

    @Test
    void updatesRelationshipAndGuardianRequirement() throws Exception {
        Beneficiary beneficiary = beneficiary();
        when(beneficiaryRepository.findById(BENEFICIARY_ID)).thenReturn(Optional.of(beneficiary));
        when(beneficiaryRepository.save(beneficiary)).thenReturn(beneficiary);

        BeneficiaryView view =
                beneficiaryService.updateBeneficiary(
                        BENEFICIARY_ID,
                        new UpdateBeneficiaryCommand(
                                "Grandchild - minor",
                                "Updated Guardian",
                                "updated.guardian@bayer-westphalian.test",
                                true));

        assertThat(view.relationship()).isEqualTo("Grandchild - minor");
        assertThat(view.guardianName()).isEqualTo("Updated Guardian");
        assertThat(view.guardianEmail()).isEqualTo("updated.guardian@bayer-westphalian.test");
        assertThat(view.guardianConsentRequired()).isTrue();
        verify(beneficiaryRepository).save(beneficiary);
    }

    @Test
    void deletesBeneficiaryLink() throws Exception {
        Beneficiary beneficiary = beneficiary();
        when(beneficiaryRepository.findById(BENEFICIARY_ID)).thenReturn(Optional.of(beneficiary));

        beneficiaryService.deleteBeneficiary(BENEFICIARY_ID);

        verify(beneficiaryRepository).delete(beneficiary);
    }

    @Test
    void searchesBeneficiariesWithKbFilters() throws Exception {
        Beneficiary withGuardian = beneficiary();
        withGuardian.requireGuardianConsent("Guardian", "guardian@bayer-westphalian.test");
        Beneficiary withoutGuardian =
                Beneficiary.create(
                        customer(
                                UUID.fromString("30000000-0000-0000-0000-000000000201"),
                                CustomerType.CUSTOMER,
                                "Clara",
                                "Policyholder"),
                        customer(
                                UUID.fromString("30000000-0000-0000-0000-000000000202"),
                                CustomerType.BENEFICIARY,
                                "Dana",
                                "Beneficiary"),
                        "Child");
        when(beneficiaryRepository.findByPolicyholderCustomerId(POLICYHOLDER_ID))
                .thenReturn(List.of(withGuardian, withoutGuardian));

        List<BeneficiaryView> views =
                beneficiaryService.searchBeneficiaries(
                        new BeneficiarySearchCriteria(POLICYHOLDER_ID, null, true));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).relationship()).isEqualTo("Grandchild");
        assertThat(views.get(0).guardianConsentRequired()).isTrue();
        verify(beneficiaryRepository).findByPolicyholderCustomerId(POLICYHOLDER_ID);
    }

    @Test
    void findsBeneficiaryAndRejectsMissingOrInvalidInputs() throws Exception {
        Beneficiary beneficiary = beneficiary();
        when(beneficiaryRepository.findById(BENEFICIARY_ID)).thenReturn(Optional.of(beneficiary));

        assertThat(beneficiaryService.findById(BENEFICIARY_ID).relationship())
                .isEqualTo("Grandchild");

        assertThatThrownBy(() -> beneficiaryService.findById(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Beneficiary validation failed");

        UUID missingId = UUID.fromString("30000000-0000-0000-0000-000000000099");
        when(beneficiaryRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beneficiaryService.findById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Beneficiary was not found: " + missingId);
    }

    @Test
    void rejectsSoftDeletedCustomersWhenCreatingBeneficiaryLink() throws Exception {
        Customer deletedPolicyholder =
                customer(POLICYHOLDER_ID, CustomerType.CUSTOMER, "Ada", "Policyholder");
        deletedPolicyholder.markDeleted();
        when(beneficiaryRepository.existsByPolicyholderCustomerIdAndBeneficiaryCustomerId(
                        POLICYHOLDER_ID, BENEFICIARY_CUSTOMER_ID))
                .thenReturn(false);
        when(customerRepository.findById(POLICYHOLDER_ID))
                .thenReturn(Optional.of(deletedPolicyholder));

        assertThatThrownBy(
                        () ->
                                beneficiaryService.createBeneficiary(
                                        new CreateBeneficiaryCommand(
                                                POLICYHOLDER_ID,
                                                BENEFICIARY_CUSTOMER_ID,
                                                "Grandchild",
                                                null,
                                                null,
                                                false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Policyholder customer was not found: " + POLICYHOLDER_ID);
    }

    private static void assertPreAuthorize(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = BeneficiaryService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static Beneficiary beneficiary() throws Exception {
        Beneficiary beneficiary =
                Beneficiary.create(
                        customer(POLICYHOLDER_ID, CustomerType.CUSTOMER, "Ada", "Policyholder"),
                        customer(
                                BENEFICIARY_CUSTOMER_ID,
                                CustomerType.BENEFICIARY,
                                "Ben",
                                "Beneficiary"),
                        "Grandchild");
        setId(beneficiary, BENEFICIARY_ID);
        return beneficiary;
    }

    private static Customer customer(UUID id, CustomerType type, String firstName, String lastName)
            throws Exception {
        Customer customer = Customer.create(type, firstName, lastName);
        setId(customer, id);
        return customer;
    }

    private static void setId(Customer customer, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
    }

    private static void setId(Beneficiary beneficiary, UUID id) throws Exception {
        Field idField = Beneficiary.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(beneficiary, id);
    }
}
