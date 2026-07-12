package com.bayerwestphalian.campaign.beneficiary;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BeneficiaryView>>> searchBeneficiaries(
            @RequestParam(required = false) UUID policyholderCustomerId,
            @RequestParam(required = false) UUID beneficiaryCustomerId,
            @RequestParam(required = false) Boolean guardianConsentRequired) {
        BeneficiarySearchCriteria criteria =
                new BeneficiarySearchRequest(
                                policyholderCustomerId,
                                beneficiaryCustomerId,
                                guardianConsentRequired)
                        .toCriteria();
        List<BeneficiaryView> beneficiaries = beneficiaryService.searchBeneficiaries(criteria);

        return ResponseEntity.ok(ApiResponse.success("Beneficiaries loaded", beneficiaries));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BeneficiaryView>> getBeneficiary(@PathVariable UUID id) {
        BeneficiaryView beneficiary = beneficiaryService.findById(id);

        return ResponseEntity.ok(ApiResponse.success("Beneficiary loaded", beneficiary));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BeneficiaryView>> createBeneficiary(
            @Valid @RequestBody CreateBeneficiaryRequest request) {
        BeneficiaryView beneficiary = beneficiaryService.createBeneficiary(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Beneficiary created", beneficiary));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BeneficiaryView>> updateBeneficiary(
            @PathVariable UUID id, @Valid @RequestBody UpdateBeneficiaryRequest request) {
        BeneficiaryView beneficiary = beneficiaryService.updateBeneficiary(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Beneficiary updated", beneficiary));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(@PathVariable UUID id) {
        beneficiaryService.deleteBeneficiary(id);

        return ResponseEntity.ok(ApiResponse.success("Beneficiary deleted", null));
    }
}
