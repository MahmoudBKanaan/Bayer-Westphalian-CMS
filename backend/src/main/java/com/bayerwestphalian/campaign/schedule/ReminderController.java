package com.bayerwestphalian.campaign.schedule;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final ReminderProcessingScheduler reminderProcessingScheduler;

    public ReminderController(
            ReminderService reminderService, ReminderProcessingScheduler reminderProcessingScheduler) {
        this.reminderService = reminderService;
        this.reminderProcessingScheduler = reminderProcessingScheduler;
    }

    @PostMapping("/payment")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<ReminderScheduleView>> createPaymentReminder(
            @Valid @RequestBody CreatePaymentReminderRequest request) {
        ReminderScheduleView reminder = reminderService.createPaymentReminders(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment reminder scheduled", reminder));
    }

    @PostMapping("/payment/generate")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public ResponseEntity<ApiResponse<List<ReminderScheduleView>>> generatePaymentDueReminders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate asOfDate) {
        List<ReminderScheduleView> reminders =
                reminderService.generatePaymentDueReminders(asOfDate);
        return ResponseEntity.ok(ApiResponse.success("Payment due reminders generated", reminders));
    }

    @PostMapping("/expiration")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<ReminderScheduleView>> createExpirationReminder(
            @Valid @RequestBody CreateProductExpirationReminderRequest request) {
        ReminderScheduleView reminder =
                reminderService.createExpirationReminders(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product-expiration reminder scheduled", reminder));
    }

    @PostMapping("/expiration/3-month/generate")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public ResponseEntity<ApiResponse<List<ReminderScheduleView>>>
            generateThreeMonthExpirationReminders(
                    @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate asOfDate) {
        List<ReminderScheduleView> reminders =
                reminderService.generateThreeMonthExpirationReminders(asOfDate);
        return ResponseEntity.ok(
                ApiResponse.success("Three-month product-expiration reminders generated", reminders));
    }

    @PostMapping("/expiration/6-month/generate")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public ResponseEntity<ApiResponse<List<ReminderScheduleView>>>
            generateSixMonthExpirationReminders(
                    @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate asOfDate) {
        List<ReminderScheduleView> reminders =
                reminderService.generateSixMonthExpirationReminders(asOfDate);
        return ResponseEntity.ok(
                ApiResponse.success("Six-month product-expiration reminders generated", reminders));
    }

    @PostMapping("/expiration/12-month/generate")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public ResponseEntity<ApiResponse<List<ReminderScheduleView>>>
            generateTwelveMonthExpirationReminders(
                    @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate asOfDate) {
        List<ReminderScheduleView> reminders =
                reminderService.generateTwelveMonthExpirationReminders(asOfDate);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Twelve-month product-expiration reminders generated", reminders));
    }

    @PostMapping("/due/send")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<List<ReminderScheduleView>>> sendDueReminders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate asOfDate) {
        List<ReminderScheduleView> reminders = reminderService.sendDueReminders(asOfDate);
        return ResponseEntity.ok(ApiResponse.success("Due reminders processed", reminders));
    }

    @PostMapping("/due/manual-trigger")
    @PreAuthorize("@authz.hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReminderScheduleView>>> manuallyTriggerDueReminders() {
        List<ReminderScheduleView> reminders =
                reminderProcessingScheduler.triggerManualProcessing();
        return ResponseEntity.ok(
                ApiResponse.success("Manual reminder processing triggered", reminders));
    }

    @PutMapping("/{id}/sent")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<ReminderScheduleView>> markSent(@PathVariable UUID id) {
        ReminderScheduleView reminder = reminderService.markSent(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder marked sent", reminder));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<ReminderScheduleView>> cancelReminder(@PathVariable UUID id) {
        ReminderScheduleView reminder = reminderService.cancelReminder(id);
        return ResponseEntity.ok(ApiResponse.success("Reminder cancelled", reminder));
    }

    @GetMapping
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<ReminderScheduleView>>> listReminders(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) ReminderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dueOnOrBefore) {
        List<ReminderScheduleView> reminders =
                reminderService.searchReminders(
                        new ReminderScheduleSearchCriteria(customerId, status, dueOnOrBefore));
        return ResponseEntity.ok(ApiResponse.success("Reminders loaded", reminders));
    }
}
