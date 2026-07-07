package com.bayerwestphalian.campaign.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class BeneficiaryRepositoryTests {

    @Test
    void extendsJpaRepositoryForBeneficiaryAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(BeneficiaryRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(BeneficiaryRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Beneficiary.class, UUID.class);
    }

    @Test
    void declaresKbPolicyholderAndBeneficiaryCustomerLookupMethods() throws Exception {
        Method findByPolicyholderCustomerId =
                BeneficiaryRepository.class.getMethod("findByPolicyholderCustomerId", UUID.class);
        Method findByBeneficiaryCustomerId =
                BeneficiaryRepository.class.getMethod("findByBeneficiaryCustomerId", UUID.class);

        assertThat(findByPolicyholderCustomerId.getGenericReturnType())
                .isEqualTo(beneficiaryList());
        assertThat(findByBeneficiaryCustomerId.getGenericReturnType()).isEqualTo(beneficiaryList());
    }

    @Test
    void concreteFindersUseCreatedAtOrderingForStableProfileLists() throws Exception {
        assertThat(
                        BeneficiaryRepository.class
                                .getMethod(
                                        "findByPolicyholderCustomerIdOrderByCreatedAtAsc",
                                        UUID.class)
                                .getGenericReturnType())
                .isEqualTo(beneficiaryList());
        assertThat(
                        BeneficiaryRepository.class
                                .getMethod(
                                        "findByBeneficiaryCustomerIdOrderByCreatedAtAsc",
                                        UUID.class)
                                .getGenericReturnType())
                .isEqualTo(beneficiaryList());
    }

    @Test
    void declaresGuardianConsentAndDuplicateLinkSupportMethods() throws Exception {
        Method guardianConsentRequired =
                BeneficiaryRepository.class.getMethod(
                        "findByGuardianConsentRequiredTrueOrderByCreatedAtAsc");
        Method duplicateLink =
                BeneficiaryRepository.class.getMethod(
                        "existsByPolicyholderCustomerIdAndBeneficiaryCustomerId",
                        UUID.class,
                        UUID.class);

        assertThat(guardianConsentRequired.getGenericReturnType()).isEqualTo(beneficiaryList());
        assertThat(duplicateLink.getReturnType()).isEqualTo(boolean.class);
    }

    private static Type beneficiaryList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("beneficiaryList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<Beneficiary> beneficiaryList();
    }
}
