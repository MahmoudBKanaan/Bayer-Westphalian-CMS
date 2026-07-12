package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidationErrorTests {

    @Test
    void createsValidationErrorWithFieldMessageRejectedValueAndObjectName() {
        ValidationError error =
                ValidationError.of(
                        "email", "must be a well-formed email address", "bad", "customer");

        assertThat(error.field()).isEqualTo("email");
        assertThat(error.message()).isEqualTo("must be a well-formed email address");
        assertThat(error.rejectedValue()).isEqualTo("bad");
        assertThat(error.objectName()).isEqualTo("customer");
    }
}
