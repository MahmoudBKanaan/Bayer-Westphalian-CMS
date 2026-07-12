package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * KB item 213: CampaignRepository declares KB lookup methods for status, owner, and active
 * campaigns.
 */
class CampaignRepositoryTests {

    @Test
    void extendsJpaRepositoryForCampaignAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(CampaignRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(CampaignRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Campaign.class, UUID.class);
    }

    @Test
    void declaresKbFindByStatusMethods() throws Exception {
        Method findByStatus =
                CampaignRepository.class.getMethod("findByStatus", CampaignStatus.class);
        Method findByStatusOrdered =
                CampaignRepository.class.getMethod(
                        "findByStatusOrderByNameAsc", CampaignStatus.class);

        assertThat(findByStatus.getGenericReturnType()).isEqualTo(campaignList());
        assertThat(findByStatusOrdered.getGenericReturnType()).isEqualTo(campaignList());
    }

    @Test
    void declaresKbFindByOwnerUserIdMethods() throws Exception {
        Method findByOwnerUserId =
                CampaignRepository.class.getMethod("findByOwnerUserId", UUID.class);
        Method findByOwnerIdOrdered =
                CampaignRepository.class.getMethod("findByOwner_IdOrderByNameAsc", UUID.class);

        assertThat(findByOwnerUserId.getGenericReturnType()).isEqualTo(campaignList());
        assertThat(findByOwnerIdOrdered.getGenericReturnType()).isEqualTo(campaignList());
    }

    @Test
    void declaresKbFindActiveCampaignsMethod() throws Exception {
        Method findActiveCampaigns = CampaignRepository.class.getMethod("findActiveCampaigns");

        assertThat(findActiveCampaigns.getGenericReturnType()).isEqualTo(campaignList());
        assertThat(findActiveCampaigns.getParameterCount()).isZero();
    }

    @Test
    void declaresSubmittedAndSegmentLookupHelpers() throws Exception {
        Method findSubmitted = CampaignRepository.class.getMethod("findSubmittedCampaigns");
        Method findBySegment =
                CampaignRepository.class.getMethod("findBySegment_IdOrderByNameAsc", UUID.class);
        Method findByStatusIn =
                CampaignRepository.class.getMethod("findByStatusInOrderByNameAsc", List.class);

        assertThat(findSubmitted.getGenericReturnType()).isEqualTo(campaignList());
        assertThat(findBySegment.getGenericReturnType()).isEqualTo(campaignList());
        assertThat(findByStatusIn.getGenericReturnType()).isEqualTo(campaignList());
    }

    private static Type campaignList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("campaignList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<Campaign> campaignList();
    }
}
