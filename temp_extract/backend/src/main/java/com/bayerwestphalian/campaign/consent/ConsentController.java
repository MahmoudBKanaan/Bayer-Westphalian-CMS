package com.bayerwestphalian.campaign.consent;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consents")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsentRecordView>>> listConsents(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) ConsentType consentType,
            @RequestParam(required = false) ConsentStatus status,
            @RequestParam(required = false) Boolean validOnly) {
        ConsentSearchCriteria criteria =
                new ConsentSearchRequest(customerId, consentType, status, validOnly).toCriteria();
        List<ConsentRecordView> consents = consentService.listConsents(criteria);

        return ResponseEntity.ok(ApiResponse.success("Consents loaded", consents));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ConsentRecordView>> getConsentStatus(
            @RequestParam UUID customerId, @RequestParam ConsentType consentType) {
        Optional<ConsentRecordView> consentStatus =
                consentService.getConsentStatus(customerId, consentType);

        return ResponseEntity.ok(
                ApiResponse.success("Consent status loaded", consentStatus.orElse(null)));
    }

    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkEligibility(
            @RequestParam UUID customerId, @RequestParam ConsentType consentType) {
        boolean eligible = consentService.isCommunicationEligible(customerId, consentType);

        return ResponseEntity.ok(ApiResponse.success("Consent eligibility checked", eligible));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConsentRecordView>> recordConsent(
            @Valid @RequestBody RecordConsentRequest request) {
        ConsentRecordView consent = consentService.recordConsent(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Consent recorded", consent));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<ConsentRecordView>> withdrawConsent(
            @Valid @RequestBody WithdrawConsentRequest request) {
        ConsentRecordView consent = consentService.withdrawConsent(request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Consent withdrawn", consent));
    }
}
