package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class LoginRequestTests {

    @Test
    void validatesKbLoginRequestFields() throws Exception {
        Field email = field("email");
        Field password = field("password");

        assertThat(email.isAnnotationPresent(Email.class)).isTrue();
        assertThat(email.isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(email.getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(password.isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(password.getAnnotation(Size.class).max()).isEqualTo(255);
    }

    private static Field field(String name) throws Exception {
        return LoginRequest.class.getDeclaredField(name);
    }
}
