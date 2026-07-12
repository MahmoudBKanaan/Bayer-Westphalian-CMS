package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class RoleTests {

    @Test
    void mapsKbRolesTableAsJpaEntity() {
        assertThat(Role.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Role.class.getAnnotation(Table.class).name()).isEqualTo("roles");
        assertThat(BaseEntity.class).isAssignableFrom(Role.class);
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<Role> constructor = Role.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbRoleColumnsAndValidationRules() throws Exception {
        assertColumn("name", "name", false, true, 255);
        assertColumn("displayName", "display_name", false, false, 100);
        assertColumn("description", "description", false, false, 255);
        assertColumn("allowedFunctions", "allowed_functions", false, false, 255);
        assertColumn("mvpRole", "mvp_role", false, false, 255);

        assertThat(field("name").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("displayName").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("displayName").getAnnotation(Size.class).max()).isEqualTo(100);
        assertThat(field("description").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("allowedFunctions").isAnnotationPresent(NotBlank.class)).isTrue();
    }

    @Test
    void mapsNameToKbPostgreSqlEnum() throws Exception {
        Field name = field("name");
        Column column = name.getAnnotation(Column.class);
        Enumerated enumerated = name.getAnnotation(Enumerated.class);

        assertThat(column.name()).isEqualTo("name");
        assertThat(column.nullable()).isFalse();
        assertThat(column.unique()).isTrue();
        assertThat(column.columnDefinition()).isEqualTo("system_role_name");
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void createsMvpRoleWithKbMetadata() {
        Role role =
                Role.create(
                        SystemRoleName.CAMPAIGN_MANAGER,
                        "Campaign Manager",
                        "Creates campaigns, segments, recipients, messages, schedules,"
                                + " and launches approved campaigns",
                        "Create/edit campaigns, define segments, preview recipients,"
                                + " submit campaigns, launch approved campaigns, manage follow-ups,"
                                + " view campaign analytics",
                        true);

        assertThat(role.getName()).isEqualTo(SystemRoleName.CAMPAIGN_MANAGER);
        assertThat(role.getDisplayName()).isEqualTo("Campaign Manager");
        assertThat(role.getDescription())
                .isEqualTo(
                        "Creates campaigns, segments, recipients, messages, schedules,"
                                + " and launches approved campaigns");
        assertThat(role.getAllowedFunctions())
                .contains("Create/edit campaigns", "view campaign analytics");
        assertThat(role.isMvpRole()).isTrue();
        assertThat(role.isExtendedRole()).isFalse();
    }

    @Test
    void supportsMetadataUpdatesAndExtendedRoleFlag() {
        Role role =
                Role.create(
                        SystemRoleName.EXECUTIVE_VIEWER,
                        "Executive Viewer",
                        "Views high-level dashboards and management reports only",
                        "View read-only dashboards",
                        false);

        role.updateMetadata(
                "Executive Viewer",
                "Views high-level dashboards and management reports only",
                "View read-only dashboards, ROI, campaign summaries,"
                        + " and product performance reports",
                false);

        assertThat(role.isMvpRole()).isFalse();
        assertThat(role.isExtendedRole()).isTrue();
        assertThat(role.getAllowedFunctions())
                .isEqualTo(
                        "View read-only dashboards, ROI, campaign summaries,"
                                + " and product performance reports");
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
        return Role.class.getDeclaredField(fieldName);
    }
}
