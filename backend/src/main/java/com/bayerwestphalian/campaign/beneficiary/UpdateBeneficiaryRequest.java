package com.bayerwestphalian.campaign.beneficiary;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBeneficiaryRequest(
        @NotBlank @Size(max = 100) String relationship,
        @Size(max = 255) String guardianName,
        @Email @Size(max = 255) String guardianEmail,
        Boolean guardianConsentRequired) {

    UpdateBeneficiaryCommand toCommand() {
        return new UpdateBeneficiaryCommand(
                relationship, guardianName, guardianEmail, guardianConsentRequired);
    }
}
