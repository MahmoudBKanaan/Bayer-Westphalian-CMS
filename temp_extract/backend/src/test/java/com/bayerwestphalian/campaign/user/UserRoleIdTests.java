package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserRoleIdTests {

    @Test
    void mapsCompositePrimaryKeyForUserRolesTable() throws Exception {
        assertThat(UserRoleId.class.isAnnotationPresent(Embeddable.class)).isTrue();
        assertThat(Serializable.class).isAssignableFrom(UserRoleId.class);

        assertColumn("userId", "user_id");
        assertColumn("roleId", "role_id");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<UserRoleId> constructor = UserRoleId.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void comparesByUserIdAndRoleId() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009901");
        UUID roleId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        UserRoleId id = new UserRoleId(userId, roleId);
        UserRoleId sameId = new UserRoleId(userId, roleId);
        UserRoleId differentRole = new UserRoleId(userId, UUID.randomUUID());

        assertThat(id).isEqualTo(sameId);
        assertThat(id).hasSameHashCodeAs(sameId);
        assertThat(id).isNotEqualTo(differentRole);
        assertThat(id.getUserId()).isEqualTo(userId);
        assertThat(id.getRoleId()).isEqualTo(roleId);
    }

    private static void assertColumn(String fieldName, String columnName) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isFalse();
    }

    private static Field field(String fieldName) throws Exception {
        return UserRoleId.class.getDeclaredField(fieldName);
    }
}
