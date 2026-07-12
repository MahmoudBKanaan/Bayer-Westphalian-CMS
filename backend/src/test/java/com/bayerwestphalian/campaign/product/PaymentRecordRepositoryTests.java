package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class PaymentRecordRepositoryTests {

    @Test
    void extendsJpaRepositoryForPaymentRecordAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(PaymentRecordRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(PaymentRecordRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(PaymentRecord.class, UUID.class);
    }

    @Test
    void declaresKbDueOverdueAndCustomerLookupMethods() throws Exception {
        assertThat(
                        PaymentRecordRepository.class
                                .getMethod("findDuePayments")
                                .getGenericReturnType())
                .isEqualTo(paymentRecordList());
        assertThat(
                        PaymentRecordRepository.class
                                .getMethod("findOverduePayments")
                                .getGenericReturnType())
                .isEqualTo(paymentRecordList());
        assertThat(
                        PaymentRecordRepository.class
                                .getMethod("findByCustomerId", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(paymentRecordList());
    }

    @Test
    void concreteFindersUseStableDueDateOrderingForReminderScheduling() throws Exception {
        assertThat(
                        PaymentRecordRepository.class
                                .getMethod("findByStatusOrderByDueDateAsc", PaymentStatus.class)
                                .getGenericReturnType())
                .isEqualTo(paymentRecordList());
        assertThat(
                        PaymentRecordRepository.class
                                .getMethod("findByCustomerIdOrderByDueDateAsc", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(paymentRecordList());
    }

    @Test
    void overdueFinderTargetsKbOverdueAndDefaultRiskStatuses() throws Exception {
        Method overdueStatuses =
                PaymentRecordRepository.class.getMethod(
                        "findByStatusInOrderByDueDateAscReminderCountDesc", Collection.class);

        assertThat(overdueStatuses.getGenericReturnType()).isEqualTo(paymentRecordList());
        assertThat(overdueStatuses.getParameters()[0].getType()).isEqualTo(Collection.class);
    }

    private static Type paymentRecordList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("paymentRecordList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<PaymentRecord> paymentRecordList();
    }
}
