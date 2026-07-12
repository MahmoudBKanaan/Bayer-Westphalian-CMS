package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.support.ControllerTestSupport;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class PaymentRecordControllerTests {

    private static final UUID PAYMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000201");
    private static final LocalDate DUE_DATE = LocalDate.parse("2026-07-15");
    private static final Instant PAID_AT = Instant.parse("2026-07-10T09:30:00Z");

    @Mock private PaymentRecordService paymentRecordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                ControllerTestSupport.standaloneController(
                        new PaymentRecordController(paymentRecordService),
                        new GlobalExceptionHandler());
    }

    @Test
    void exposesPaymentRecordApiRoute() {
        assertThat(PaymentRecordController.class.isAnnotationPresent(RestController.class))
                .isTrue();
        assertThat(PaymentRecordController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/payment-records");
    }

    @Test
    void exposesKbPaymentRecordCreateAndUpdateEndpoints() throws Exception {
        Method listMethod =
                PaymentRecordController.class.getMethod(
                        "listPaymentRecords", PaymentRecordSearchRequest.class);
        Method createMethod =
                PaymentRecordController.class.getMethod(
                        "createPaymentRecord", CreatePaymentRecordRequest.class);
        Method updateMethod =
                PaymentRecordController.class.getMethod(
                        "updatePaymentRecord", UUID.class, UpdatePaymentRecordRequest.class);
        Method markPaidMethod =
                PaymentRecordController.class.getMethod(
                        "markPaid", UUID.class, MarkPaymentPaidRequest.class);
        Method markOverdueMethod =
                PaymentRecordController.class.getMethod("markOverdue", UUID.class);
        Method incrementReminderMethod =
                PaymentRecordController.class.getMethod("incrementReminder", UUID.class);

        assertThat(listMethod.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(createMethod.isAnnotationPresent(PostMapping.class)).isTrue();
        assertThat(updateMethod.isAnnotationPresent(PutMapping.class)).isTrue();
        assertThat(updateMethod.getAnnotation(PutMapping.class).value()).containsExactly("/{id}");
        assertThat(markPaidMethod.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(markPaidMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{id}/mark-paid");
        assertThat(markOverdueMethod.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(markOverdueMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{id}/mark-overdue");
        assertThat(incrementReminderMethod.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(incrementReminderMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{id}/increment-reminder");
    }

    @Test
    void listsPaymentRecordsByCustomer() throws Exception {
        when(paymentRecordService.searchPayments(any(PaymentRecordSearchCriteria.class)))
                .thenReturn(java.util.List.of(duePaymentView()));

        mockMvc.perform(get("/api/payment-records").param("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment records loaded"))
                .andExpect(jsonPath("$.data[0].id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data[0].dueDate").value("2026-07-15"))
                .andExpect(jsonPath("$.data[0].amountDue").value(129.99))
                .andExpect(jsonPath("$.data[0].status").value("DUE"));

        ArgumentCaptor<PaymentRecordSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(PaymentRecordSearchCriteria.class);
        verify(paymentRecordService).searchPayments(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void createsPaymentRecordFromKbRequest() throws Exception {
        when(paymentRecordService.createPaymentRecord(any(CreatePaymentRecordCommand.class)))
                .thenReturn(duePaymentView());

        mockMvc.perform(
                        post("/api/payment-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "productOwnershipId": "%s",
                                          "dueDate": "2026-07-15",
                                          "amountDue": 129.99
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, OWNERSHIP_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment record created"))
                .andExpect(jsonPath("$.data.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.customerFullName").value("Ada Payer"))
                .andExpect(jsonPath("$.data.productOwnershipId").value(OWNERSHIP_ID.toString()))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.productName").value("Life Protection"))
                .andExpect(jsonPath("$.data.dueDate").value("2026-07-15"))
                .andExpect(jsonPath("$.data.amountDue").value(129.99))
                .andExpect(jsonPath("$.data.status").value("DUE"))
                .andExpect(jsonPath("$.data.reminderCount").value(0))
                .andExpect(jsonPath("$.data.defaultRisk").value(false));

        ArgumentCaptor<CreatePaymentRecordCommand> commandCaptor =
                ArgumentCaptor.forClass(CreatePaymentRecordCommand.class);
        verify(paymentRecordService).createPaymentRecord(commandCaptor.capture());
        assertThat(commandCaptor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(commandCaptor.getValue().productOwnershipId()).isEqualTo(OWNERSHIP_ID);
        assertThat(commandCaptor.getValue().dueDate()).isEqualTo(DUE_DATE);
        assertThat(commandCaptor.getValue().amountDue()).isEqualByComparingTo("129.99");
    }

    @Test
    void rejectsInvalidCreatePaymentRecordRequest() throws Exception {
        mockMvc.perform(
                        post("/api/payment-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/payment-records"));
    }

    @Test
    void updatesPaymentRecordFromKbRequest() throws Exception {
        when(paymentRecordService.updatePaymentRecord(
                        eq(PAYMENT_ID), any(UpdatePaymentRecordCommand.class)))
                .thenReturn(updatedPaymentView());

        mockMvc.perform(
                        put("/api/payment-records/{id}", PAYMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "dueDate": "2026-08-01",
                                          "amountDue": 150.25
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment record updated"))
                .andExpect(jsonPath("$.data.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data.dueDate").value("2026-08-01"))
                .andExpect(jsonPath("$.data.amountDue").value(150.25))
                .andExpect(jsonPath("$.data.status").value("DUE"));

        ArgumentCaptor<UpdatePaymentRecordCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdatePaymentRecordCommand.class);
        verify(paymentRecordService).updatePaymentRecord(eq(PAYMENT_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().dueDate()).isEqualTo(LocalDate.parse("2026-08-01"));
        assertThat(commandCaptor.getValue().amountDue()).isEqualByComparingTo("150.25");
    }

    @Test
    void rejectsInvalidUpdatePaymentRecordRequest() throws Exception {
        mockMvc.perform(
                        put("/api/payment-records/{id}", PAYMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/payment-records/" + PAYMENT_ID));
    }

    @Test
    void mapsMissingCustomerToNotFoundResponseOnCreate() throws Exception {
        when(paymentRecordService.createPaymentRecord(any(CreatePaymentRecordCommand.class)))
                .thenThrow(new ResourceNotFoundException("Customer", CUSTOMER_ID));

        mockMvc.perform(
                        post("/api/payment-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "productOwnershipId": "%s",
                                          "dueDate": "2026-07-15",
                                          "amountDue": 50.00
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, OWNERSHIP_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/payment-records"));
    }

    @Test
    void mapsOwnershipValidationFailureToValidationResponseOnCreate() throws Exception {
        when(paymentRecordService.createPaymentRecord(any(CreatePaymentRecordCommand.class)))
                .thenThrow(
                        new ValidationException(
                                "Payment record validation failed",
                                java.util.List.of(
                                        "productOwnershipId: must belong to the specified"
                                                + " customer")));

        mockMvc.perform(
                        post("/api/payment-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "productOwnershipId": "%s",
                                          "dueDate": "2026-07-15",
                                          "amountDue": 50.00
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, OWNERSHIP_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/payment-records"));
    }

    @Test
    void marksPaymentRecordPaidFromKbRequest() throws Exception {
        when(paymentRecordService.markPaid(eq(PAYMENT_ID), any(MarkPaymentPaidCommand.class)))
                .thenReturn(paidPaymentView());

        mockMvc.perform(
                        patch("/api/payment-records/{id}/mark-paid", PAYMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "amountPaid": 129.99,
                                          "paidAt": "2026-07-10T09:30:00Z"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment record marked paid"))
                .andExpect(jsonPath("$.data.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.amountPaid").value(129.99))
                .andExpect(jsonPath("$.data.paidAt").value("2026-07-10T09:30:00Z"));

        ArgumentCaptor<MarkPaymentPaidCommand> commandCaptor =
                ArgumentCaptor.forClass(MarkPaymentPaidCommand.class);
        verify(paymentRecordService).markPaid(eq(PAYMENT_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().amountPaid()).isEqualByComparingTo("129.99");
        assertThat(commandCaptor.getValue().paidAt()).isEqualTo(PAID_AT);
    }

    @Test
    void rejectsInvalidMarkPaidRequest() throws Exception {
        mockMvc.perform(
                        patch("/api/payment-records/{id}/mark-paid", PAYMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/payment-records/" + PAYMENT_ID + "/mark-paid"));
    }

    @Test
    void mapsMissingPaymentRecordToNotFoundResponseOnMarkPaid() throws Exception {
        when(paymentRecordService.markPaid(eq(PAYMENT_ID), any(MarkPaymentPaidCommand.class)))
                .thenThrow(new ResourceNotFoundException("Payment record", PAYMENT_ID));

        mockMvc.perform(
                        patch("/api/payment-records/{id}/mark-paid", PAYMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "amountPaid": 129.99
                                        }
                                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/payment-records/" + PAYMENT_ID + "/mark-paid"));
    }

    @Test
    void mapsAlreadyPaidValidationFailureOnMarkPaid() throws Exception {
        when(paymentRecordService.markPaid(eq(PAYMENT_ID), any(MarkPaymentPaidCommand.class)))
                .thenThrow(
                        new ValidationException(
                                "Payment record validation failed",
                                java.util.List.of(
                                        "status: payment is already PAID and cannot mark paid")));

        mockMvc.perform(
                        patch("/api/payment-records/{id}/mark-paid", PAYMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "amountPaid": 129.99
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/payment-records/" + PAYMENT_ID + "/mark-paid"));
    }

    @Test
    void marksPaymentRecordOverdue() throws Exception {
        when(paymentRecordService.markOverdue(PAYMENT_ID)).thenReturn(overduePaymentView());

        mockMvc.perform(patch("/api/payment-records/{id}/mark-overdue", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment record marked overdue"))
                .andExpect(jsonPath("$.data.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("OVERDUE"));

        verify(paymentRecordService).markOverdue(PAYMENT_ID);
    }

    @Test
    void incrementsPaymentRecordReminder() throws Exception {
        when(paymentRecordService.incrementReminder(PAYMENT_ID))
                .thenReturn(defaultRiskPaymentView());

        mockMvc.perform(patch("/api/payment-records/{id}/increment-reminder", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment record reminder incremented"))
                .andExpect(jsonPath("$.data.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.data.reminderCount").value(3))
                .andExpect(jsonPath("$.data.status").value("DEFAULT_RISK"))
                .andExpect(jsonPath("$.data.defaultRisk").value(true));

        verify(paymentRecordService).incrementReminder(PAYMENT_ID);
    }

    @Test
    void mapsMissingPaymentRecordToNotFoundResponseOnMarkOverdue() throws Exception {
        when(paymentRecordService.markOverdue(PAYMENT_ID))
                .thenThrow(new ResourceNotFoundException("Payment record", PAYMENT_ID));

        mockMvc.perform(patch("/api/payment-records/{id}/mark-overdue", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/payment-records/" + PAYMENT_ID + "/mark-overdue"));
    }

    private static PaymentRecordView duePaymentView() {
        return new PaymentRecordView(
                PAYMENT_ID,
                CUSTOMER_ID,
                "Ada Payer",
                OWNERSHIP_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                DUE_DATE,
                null,
                new BigDecimal("129.99"),
                null,
                PaymentStatus.DUE,
                0,
                0,
                false);
    }

    private static PaymentRecordView paidPaymentView() {
        return new PaymentRecordView(
                PAYMENT_ID,
                CUSTOMER_ID,
                "Ada Payer",
                OWNERSHIP_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                DUE_DATE,
                PAID_AT,
                new BigDecimal("129.99"),
                new BigDecimal("129.99"),
                PaymentStatus.PAID,
                0,
                0,
                false);
    }

    private static PaymentRecordView updatedPaymentView() {
        return new PaymentRecordView(
                PAYMENT_ID,
                CUSTOMER_ID,
                "Ada Payer",
                OWNERSHIP_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                LocalDate.parse("2026-08-01"),
                null,
                new BigDecimal("150.25"),
                null,
                PaymentStatus.DUE,
                0,
                0,
                false);
    }

    private static PaymentRecordView overduePaymentView() {
        return new PaymentRecordView(
                PAYMENT_ID,
                CUSTOMER_ID,
                "Ada Payer",
                OWNERSHIP_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                DUE_DATE,
                null,
                new BigDecimal("129.99"),
                null,
                PaymentStatus.OVERDUE,
                1,
                5,
                false);
    }

    private static PaymentRecordView defaultRiskPaymentView() {
        return new PaymentRecordView(
                PAYMENT_ID,
                CUSTOMER_ID,
                "Ada Payer",
                OWNERSHIP_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                DUE_DATE,
                null,
                new BigDecimal("129.99"),
                null,
                PaymentStatus.DEFAULT_RISK,
                3,
                10,
                true);
    }
}
