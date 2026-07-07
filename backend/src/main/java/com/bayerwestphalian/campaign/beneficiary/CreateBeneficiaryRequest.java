package com.bayerwestphalian.campaign.beneficiary;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateBeneficiaryRequest(
        @NotNull UUID policyholderCustomerId,
        @NotNull UUID beneficiaryCustomerId,
        @NotBlank @Size(max = 100) String relationship,
        @Size(max = 255) String guardianName,
        @Email @Size(max = 255) String guardianEmail,
        boolean guardianConsentRequired) {

    CreateBeneficiaryCommand toCommand() {
        return new CreateBeneficiaryCommand(
                policyholderCustomerId,
                beneficiaryCustomerId,
                relationship,
                guardianName,
                guardianEmail,
                guardianConsentRequired);
    }
}
