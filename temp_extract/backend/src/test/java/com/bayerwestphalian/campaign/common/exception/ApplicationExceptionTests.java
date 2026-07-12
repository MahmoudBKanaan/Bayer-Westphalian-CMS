package com.bayerwestphalian.campaign.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApplicationExceptionTests {

    @Test
    void storesStatusCodeMessageDetailsAndCause() {
        RuntimeException cause = new RuntimeException("database unavailable");
        ApplicationException exception =
                new ApplicationException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR",
                        "Unexpected failure",
                        List.of("retry later"),
                        cause);

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Unexpected failure");
        assertThat(exception.getDetails()).containsExactly("retry later");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void normalizesNullDetailsToEmptyList() {
        ApplicationException exception =
                new ApplicationException(
                        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", null);

        assertThat(exception.getDetails()).isEmpty();
    }

    @Test
    void copiesDetailsDefensively() {
        List<String> details = new ArrayList<>();
        details.add("initial detail");

        ApplicationException exception =
                new ApplicationException(
                        HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", details);
        details.add("later mutation");

        assertThat(exception.getDetails()).containsExactly("initial detail");
        assertThatThrownBy(() -> exception.getDetails().add("not allowed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
