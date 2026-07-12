package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

/** KB item 316: ContactEventRepository exposes contact history lookup methods. */
class ContactEventRepositoryTests {

    @Test
    void extendsJpaRepositoryForContactEventAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(ContactEventRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(ContactEventRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(ContactEvent.class, UUID.class);
    }

    @Test
    void declaresKbCampaignAndCustomerHistoryLookupMethods() throws Exception {
        Method findByCampaign =
                ContactEventRepository.class.getMethod("findByCampaignId", UUID.class);
        Method findByCustomer =
                ContactEventRepository.class.getMethod("findByCustomerId", UUID.class);
        Method findByCampaignOrdered =
                ContactEventRepository.class.getMethod(
                        "findByCampaign_IdOrderByOccurredAtDesc", UUID.class);
        Method findByCustomerOrdered =
                ContactEventRepository.class.getMethod(
                        "findByCustomer_IdOrderByOccurredAtDesc", UUID.class);

        assertThat(findByCampaign.getGenericReturnType()).isEqualTo(contactEventList());
        assertThat(findByCustomer.getGenericReturnType()).isEqualTo(contactEventList());
        assertThat(findByCampaignOrdered.getGenericReturnType()).isEqualTo(contactEventList());
        assertThat(findByCustomerOrdered.getGenericReturnType()).isEqualTo(contactEventList());
    }

    @Test
    void declaresKbEventTypeHistoryLookupMethods() throws Exception {
        Method findCampaignEventType =
                ContactEventRepository.class.getMethod(
                        "findByCampaignIdAndEventType", UUID.class, ContactEventType.class);
        Method findCustomerEventType =
                ContactEventRepository.class.getMethod(
                        "findByCustomerIdAndEventType", UUID.class, ContactEventType.class);
        Method findCampaignEventTypeOrdered =
                ContactEventRepository.class.getMethod(
                        "findByCampaign_IdAndEventTypeOrderByOccurredAtDesc",
                        UUID.class,
                        ContactEventType.class);
        Method findCustomerEventTypeOrdered =
                ContactEventRepository.class.getMethod(
                        "findByCustomer_IdAndEventTypeOrderByOccurredAtDesc",
                        UUID.class,
                        ContactEventType.class);

        assertThat(findCampaignEventType.getGenericReturnType()).isEqualTo(contactEventList());
        assertThat(findCustomerEventType.getGenericReturnType()).isEqualTo(contactEventList());
        assertThat(findCampaignEventTypeOrdered.getGenericReturnType()).isEqualTo(contactEventList());
        assertThat(findCustomerEventTypeOrdered.getGenericReturnType()).isEqualTo(contactEventList());
    }

    @Test
    void declaresRecentCustomerContactCountForKbFrequencyRules() throws Exception {
        Method countRecent =
                ContactEventRepository.class.getMethod(
                        "countRecentCustomerMarketingContacts", UUID.class, Instant.class);
        Method countByTypesSince =
                ContactEventRepository.class.getMethod(
                        "countByCustomer_IdAndEventTypeInAndOccurredAtGreaterThanEqual",
                        UUID.class,
                        Collection.class,
                        Instant.class);

        assertThat(countRecent.getReturnType()).isEqualTo(long.class);
        assertThat(countByTypesSince.getReturnType()).isEqualTo(long.class);
        assertThat(countByTypesSince.getParameters()[1].getType()).isEqualTo(Collection.class);
    }

    private static Type contactEventList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("contactEventList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<ContactEvent> contactEventList();
    }
}
