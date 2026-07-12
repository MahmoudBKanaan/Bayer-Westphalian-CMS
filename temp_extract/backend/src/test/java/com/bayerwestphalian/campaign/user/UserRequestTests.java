package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class UserRequestTests {

    @Test
    void validatesCreateUserRequestFields() throws Exception {
        assertThat(field(CreateUserRequest.class, "email").isAnnotationPresent(Email.class))
                .isTrue();
        assertThat(field(CreateUserRequest.class, "email").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(CreateUserRequest.class, "email").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(field(CreateUserRequest.class, "password").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(CreateUserRequest.class, "fullName").isAnnotationPresent(NotBlank.class))
                .isTrue();
    }

    @Test
    void validatesUpdatePasswordAndRoleRequests() throws Exception {
        assertThat(field(UpdateUserRequest.class, "fullName").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(UpdateUserRequest.class, "fullName").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(
                        field(ResetPasswordRequest.class, "password")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(AssignRoleRequest.class, "roleName").isAnnotationPresent(NotNull.class))
                .isTrue();
    }

    @Test
    void mapsCreateAndUpdateRequestsToServiceCommands() {
        CreateUserCommand createCommand =
                new CreateUserRequest(
                                "admin@bayer-westphalian.test", "StrongPassword!2026", "Admin User")
                        .toCommand();
        UpdateUserCommand updateCommand =
                new UpdateUserRequest("Senior Admin", UserStatus.LOCKED).toCommand();

        assertThat(createCommand.email()).isEqualTo("admin@bayer-westphalian.test");
        assertThat(createCommand.rawPassword()).isEqualTo("StrongPassword!2026");
        assertThat(createCommand.fullName()).isEqualTo("Admin User");
        assertThat(updateCommand.fullName()).isEqualTo("Senior Admin");
        assertThat(updateCommand.status()).isEqualTo(UserStatus.LOCKED);
    }

    private static Field field(Class<?> type, String fieldName) throws Exception {
        return type.getDeclaredField(fieldName);
    }
}
