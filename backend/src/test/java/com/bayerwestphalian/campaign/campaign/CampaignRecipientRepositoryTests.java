package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

/** KB item 265: CampaignRecipientRepository exposes recipient audience lookup methods. */
class CampaignRecipientRepositoryTests {

    @Test
    void extendsJpaRepositoryForCampaignRecipientAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(CampaignRecipientRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(CampaignRecipientRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(CampaignRecipient.class, UUID.class);
    }

    @Test
    void declaresKbCampaignAndStatusLookupMethods() throws Exception {
        Method findByCampaign =
                CampaignRecipientRepository.class.getMethod("findByCampaignId", UUID.class);
        Method findByCampaignAndStatus =
                CampaignRecipientRepository.class.getMethod(
                        "findByCampaignIdAndEligibilityStatus",
                        UUID.class,
                        CampaignRecipientStatus.class);
        Method countByCampaignAndStatus =
                CampaignRecipientRepository.class.getMethod(
                        "countByCampaignIdAndEligibilityStatus",
                        UUID.class,
                        CampaignRecipientStatus.class);

        assertThat(findByCampaign.getGenericReturnType()).isEqualTo(recipientList());
        assertThat(findByCampaignAndStatus.getGenericReturnType()).isEqualTo(recipientList());
        assertThat(countByCampaignAndStatus.getReturnType()).isEqualTo(long.class);
    }

    @Test
    void declaresKbCampaignCustomerUniquenessLookupMethods() throws Exception {
        Method findByCampaignAndCustomer =
                CampaignRecipientRepository.class.getMethod(
                        "findByCampaignIdAndCustomerId", UUID.class, UUID.class);
        Method existsByCampaignAndCustomer =
                CampaignRecipientRepository.class.getMethod(
                        "existsByCampaignIdAndCustomerId", UUID.class, UUID.class);
        Method findByCustomer =
                CampaignRecipientRepository.class.getMethod("findByCustomerId", UUID.class);
        Method deleteByCampaign =
                CampaignRecipientRepository.class.getMethod("deleteByCampaign_Id", UUID.class);

        assertThat(findByCampaignAndCustomer.getGenericReturnType()).isEqualTo(optionalRecipient());
        assertThat(existsByCampaignAndCustomer.getReturnType()).isEqualTo(boolean.class);
        assertThat(findByCustomer.getGenericReturnType()).isEqualTo(recipientList());
        assertThat(deleteByCampaign.getReturnType()).isEqualTo(void.class);
    }

    private static Type recipientList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("recipientList").getGenericReturnType();
    }

    private static Type optionalRecipient() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("optionalRecipient").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<CampaignRecipient> recipientList();

        Optional<CampaignRecipient> optionalRecipient();
    }
}
