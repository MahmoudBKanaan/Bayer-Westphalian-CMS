package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTests {

    @Test
    void mapsKbUsersTableAsJpaEntity() {
        assertThat(User.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(User.class.getAnnotation(Table.class).name()).isEqualTo("users");
        assertThat(BaseEntity.class).isAssignableFrom(User.class);
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<User> constructor = User.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbUserColumnsAndValidationRules() throws Exception {
        assertColumn("email", "email", false, true, 255);
        assertColumn("passwordHash", "password_hash", false, false, 255);
        assertColumn("fullName", "full_name", false, false, 255);
        assertColumn("lastLoginAt", "last_login_at", true, false, 255);

        assertThat(field("email").isAnnotationPresent(Email.class)).isTrue();
        assertThat(field("email").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("email").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("passwordHash").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("passwordHash").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("fullName").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("fullName").getAnnotation(Size.class).max()).isEqualTo(255);
    }

    @Test
    void mapsStatusToKbPostgreSqlEnum() throws Exception {
        Field status = field("status");
        Column column = status.getAnnotation(Column.class);
        Enumerated enumerated = status.getAnnotation(Enumerated.class);

        assertThat(column.name()).isEqualTo("status");
        assertThat(column.nullable()).isFalse();
        assertThat(column.columnDefinition()).isEqualTo("user_status");
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void createsActiveUserByDefault() {
        User user =
                User.create("advisor@bayer-westphalian.test", "$2a$10$examplehash", "Advisor User");

        assertThat(user.getEmail()).isEqualTo("advisor@bayer-westphalian.test");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$examplehash");
        assertThat(user.getFullName()).isEqualTo("Advisor User");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    void supportsUserStatusTransitionsAndLoginTracking() {
        User user =
                User.create("advisor@bayer-westphalian.test", "$2a$10$examplehash", "Advisor User");
        Instant loginTime = Instant.parse("2026-07-03T12:00:00Z");

        user.disable();
        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.isActive()).isFalse();

        user.lock();
        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);

        user.activate();
        user.recordLogin(loginTime);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getLastLoginAt()).isEqualTo(loginTime);
    }

    @Test
    void supportsProfileAndPasswordHashUpdates() {
        User user = User.create("advisor@bayer-westphalian.test", "$2a$10$oldhash", "Advisor User");

        user.rename("Senior Advisor User");
        user.changePasswordHash("$2a$10$newhash");

        assertThat(user.getFullName()).isEqualTo("Senior Advisor User");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$newhash");
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, boolean unique, int length)
            throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.unique()).isEqualTo(unique);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return User.class.getDeclaredField(fieldName);
    }
}
