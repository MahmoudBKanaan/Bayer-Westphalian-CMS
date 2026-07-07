package com.bayerwestphalian.campaign.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BeneficiaryDtoTests {

    @Test
    void validatesCreateBeneficiaryRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(CreateBeneficiaryRequest.class, "policyholderCustomerId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateBeneficiaryRequest.class, "beneficiaryCustomerId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateBeneficiaryRequest.class, "relationship")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(
                        field(CreateBeneficiaryRequest.class, "relationship")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(100);
        assertThat(
                        field(CreateBeneficiaryRequest.class, "guardianName")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(255);
        assertThat(
                        field(CreateBeneficiaryRequest.class, "guardianEmail")
                                .isAnnotationPresent(Email.class))
                .isTrue();
        assertThat(
                        field(CreateBeneficiaryRequest.class, "guardianEmail")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(255);
    }

    @Test
    void validatesUpdateBeneficiaryRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(UpdateBeneficiaryRequest.class, "relationship")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(
                        field(UpdateBeneficiaryRequest.class, "relationship")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(100);
        assertThat(
                        field(UpdateBeneficiaryRequest.class, "guardianName")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(255);
        assertThat(
                        field(UpdateBeneficiaryRequest.class, "guardianEmail")
                                .isAnnotationPresent(Email.class))
                .isTrue();
        assertThat(
                        field(UpdateBeneficiaryRequest.class, "guardianEmail")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(255);
    }

    @Test
    void mapsCreateAndUpdateRequestsToCommands() {
        UUID policyholderCustomerId = UUID.randomUUID();
        UUID beneficiaryCustomerId = UUID.randomUUID();

        CreateBeneficiaryCommand createCommand =
                new CreateBeneficiaryRequest(
                                policyholderCustomerId,
                                beneficiaryCustomerId,
                                "Grandchild",
                                "Guardian User",
                                "guardian@bayer-westphalian.test",
                                true)
                        .toCommand();
        UpdateBeneficiaryCommand updateCommand =
                new UpdateBeneficiaryRequest(
                                "Grandchild - minor",
                                "Updated Guardian",
                                "updated.guardian@bayer-westphalian.test",
                                false)
                        .toCommand();

        assertThat(createCommand.policyholderCustomerId()).isEqualTo(policyholderCustomerId);
        assertThat(createCommand.beneficiaryCustomerId()).isEqualTo(beneficiaryCustomerId);
        assertThat(createCommand.relationship()).isEqualTo("Grandchild");
        assertThat(createCommand.guardianName()).isEqualTo("Guardian User");
        assertThat(createCommand.guardianEmail()).isEqualTo("guardian@bayer-westphalian.test");
        assertThat(createCommand.guardianConsentRequired()).isTrue();
        assertThat(updateCommand.relationship()).isEqualTo("Grandchild - minor");
        assertThat(updateCommand.guardianName()).isEqualTo("Updated Guardian");
        assertThat(updateCommand.guardianEmail())
                .isEqualTo("updated.guardian@bayer-westphalian.test");
        assertThat(updateCommand.guardianConsentRequired()).isFalse();
    }

    @Test
    void mapsSearchRequestToCriteria() {
        UUID policyholderCustomerId = UUID.randomUUID();
        UUID beneficiaryCustomerId = UUID.randomUUID();

        BeneficiarySearchCriteria criteria =
                new BeneficiarySearchRequest(policyholderCustomerId, beneficiaryCustomerId, true)
                        .toCriteria();

        assertThat(criteria.policyholderCustomerId()).isEqualTo(policyholderCustomerId);
        assertThat(criteria.beneficiaryCustomerId()).isEqualTo(beneficiaryCustomerId);
        assertThat(criteria.guardianConsentRequired()).isTrue();
    }

    @Test
    void mapsBeneficiaryEntityToView() {
        Customer policyholder = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        Customer beneficiaryCustomer =
                Customer.create(CustomerType.BENEFICIARY, "Ben", "Beneficiary");
        Beneficiary beneficiary =
                Beneficiary.create(policyholder, beneficiaryCustomer, "Grandchild");

        beneficiary.requireGuardianConsent("Guardian User", "guardian@bayer-westphalian.test");

        BeneficiaryView view = BeneficiaryView.from(beneficiary);

        assertThat(view.policyholderFullName()).isEqualTo("Ada Policyholder");
        assertThat(view.beneficiaryFullName()).isEqualTo("Ben Beneficiary");
        assertThat(view.relationship()).isEqualTo("Grandchild");
        assertThat(view.guardianName()).isEqualTo("Guardian User");
        assertThat(view.guardianEmail()).isEqualTo("guardian@bayer-westphalian.test");
        assertThat(view.guardianConsentRequired()).isTrue();
        assertThat(view.hasGuardianRequirement()).isTrue();
    }

    private static Field field(Class<?> type, String fieldName) throws Exception {
        return type.getDeclaredField(fieldName);
    }
}
