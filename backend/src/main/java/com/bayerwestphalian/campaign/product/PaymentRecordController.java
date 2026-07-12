package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-records")
public class PaymentRecordController {

    private final PaymentRecordService paymentRecordService;

    public PaymentRecordController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentRecordView>>> listPaymentRecords(
            @Valid @ModelAttribute PaymentRecordSearchRequest searchRequest) {
        List<PaymentRecordView> payments =
                paymentRecordService.searchPayments(searchRequest.toCriteria());

        return ResponseEntity.ok(ApiResponse.success("Payment records loaded", payments));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentRecordView>> createPaymentRecord(
            @Valid @RequestBody CreatePaymentRecordRequest request) {
        PaymentRecordView payment = paymentRecordService.createPaymentRecord(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment record created", payment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentRecordView>> updatePaymentRecord(
            @PathVariable UUID id, @Valid @RequestBody UpdatePaymentRecordRequest request) {
        PaymentRecordView payment =
                paymentRecordService.updatePaymentRecord(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Payment record updated", payment));
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<ApiResponse<PaymentRecordView>> markPaid(
            @PathVariable UUID id, @Valid @RequestBody MarkPaymentPaidRequest request) {
        PaymentRecordView payment = paymentRecordService.markPaid(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Payment record marked paid", payment));
    }

    @PatchMapping("/{id}/mark-overdue")
    public ResponseEntity<ApiResponse<PaymentRecordView>> markOverdue(@PathVariable UUID id) {
        PaymentRecordView payment = paymentRecordService.markOverdue(id);

        return ResponseEntity.ok(ApiResponse.success("Payment record marked overdue", payment));
    }

    @PatchMapping("/{id}/increment-reminder")
    public ResponseEntity<ApiResponse<PaymentRecordView>> incrementReminder(@PathVariable UUID id) {
        PaymentRecordView payment = paymentRecordService.incrementReminder(id);

        return ResponseEntity.ok(
                ApiResponse.success("Payment record reminder incremented", payment));
    }
}
