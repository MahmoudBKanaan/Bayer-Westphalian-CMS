package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class RefreshTokenRequestTests {

    @Test
    void validatesKbRefreshTokenRequestField() throws Exception {
        Field refreshToken = RefreshTokenRequest.class.getDeclaredField("refreshToken");

        assertThat(refreshToken.isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(refreshToken.getAnnotation(Size.class).max()).isEqualTo(4096);
    }
}
