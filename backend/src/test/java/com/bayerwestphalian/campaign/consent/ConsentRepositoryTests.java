package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

class ConsentRepositoryTests {

    @Test
    void extendsJpaRepositoryForConsentRecordAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(ConsentRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(ConsentRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(ConsentRecord.class, UUID.class);
    }

    @Test
    void declaresKbCustomerHistoryValidLatestAndOptOutFinders() throws Exception {
        assertThat(
                        ConsentRepository.class
                                .getMethod("findByCustomerId", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(consentRecordList());
        assertThat(
                        ConsentRepository.class
                                .getMethod(
                                        "findValidConsent",
                                        UUID.class,
                                        ConsentType.class,
                                        Instant.class)
                                .getGenericReturnType())
                .isEqualTo(optionalConsentRecord());
        assertThat(
                        ConsentRepository.class
                                .getMethod("findLatestByType", UUID.class, ConsentType.class)
                                .getGenericReturnType())
                .isEqualTo(optionalConsentRecord());
        assertThat(
                        ConsentRepository.class
                                .getMethod("findOptOuts", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(consentRecordList());
    }

    @Test
    void concreteFindersUseStableConsentHistoryOrdering() throws Exception {
        assertThat(
                        ConsentRepository.class
                                .getMethod("findByCustomerIdOrderByCreatedAtDesc", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(consentRecordList());
        assertThat(
                        ConsentRepository.class
                                .getMethod(
                                        "findFirstByCustomerIdAndConsentTypeOrderByCreatedAtDesc",
                                        UUID.class,
                                        ConsentType.class)
                                .getGenericReturnType())
                .isEqualTo(optionalConsentRecord());
        assertThat(
                        ConsentRepository.class
                                .getMethod(
                                        "findByCustomerIdAndStatusInOrderByCreatedAtDesc",
                                        UUID.class,
                                        List.class)
                                .getGenericReturnType())
                .isEqualTo(consentRecordList());
    }

    @Test
    void validConsentQueryRequiresGivenStatusAndUnexpiredConsent() throws Exception {
        Method method =
                ConsentRepository.class.getMethod(
                        "findValidConsents",
                        UUID.class,
                        ConsentType.class,
                        Instant.class,
                        Pageable.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(method.getGenericReturnType()).isEqualTo(consentRecordList());
        assertThat(method.getParameters()[0].getAnnotation(Param.class).value())
                .isEqualTo("customerId");
        assertThat(method.getParameters()[1].getAnnotation(Param.class).value())
                .isEqualTo("consentType");
        assertThat(method.getParameters()[2].getAnnotation(Param.class).value()).isEqualTo("now");
        assertThat(query.value())
                .contains("consentRecord.customer.id = :customerId")
                .contains("consentRecord.consentType = :consentType")
                .contains("consentRecord.status = 'GIVEN'")
                .contains("consentRecord.withdrawnAt is null")
                .contains("consentRecord.expiresAt is null or consentRecord.expiresAt > :now")
                .contains("order by consentRecord.grantedAt desc nulls last");
    }

    private static Type consentRecordList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("consentRecordList").getGenericReturnType();
    }

    private static Type optionalConsentRecord() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("optionalConsentRecord").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<ConsentRecord> consentRecordList();

        Optional<ConsentRecord> optionalConsentRecord();
    }
}
