package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class UserRoleTests {

    @Test
    void mapsKbUserRolesTableAsJpaEntity() throws Exception {
        assertThat(UserRole.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(UserRole.class.getAnnotation(Table.class).name()).isEqualTo("user_roles");
        assertThat(field("id").isAnnotationPresent(EmbeddedId.class)).isTrue();
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<UserRole> constructor = UserRole.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsUserAndRoleWithCompositeKeyParts() throws Exception {
        assertManyToOne("user", "userId", "user_id", false);
        assertManyToOne("role", "roleId", "role_id", false);
    }

    @Test
    void mapsAssignerAsOptionalUserReference() throws Exception {
        Field assignedBy = field("assignedBy");
        ManyToOne manyToOne = assignedBy.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = assignedBy.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(joinColumn.name()).isEqualTo("assigned_by");
    }

    @Test
    void mapsAssignedAtAndInitializesBeforePersist() throws Exception {
        Field assignedAt = field("assignedAt");
        Column column = assignedAt.getAnnotation(Column.class);
        Method callback = UserRole.class.getDeclaredMethod("onCreate");
        UserRole userRole = UserRole.assign(user(), role(), user());

        callback.setAccessible(true);
        callback.invoke(userRole);

        assertThat(column.name()).isEqualTo("assigned_at");
        assertThat(column.nullable()).isFalse();
        assertThat(column.updatable()).isFalse();
        assertThat(callback.isAnnotationPresent(PrePersist.class)).isTrue();
        assertThat(userRole.getAssignedAt()).isNotNull();
    }

    @Test
    void createsAssignmentWithUserRoleAndAssigner() {
        User assignee = user();
        Role role = role();
        User assigner = user();

        UserRole userRole = UserRole.assign(assignee, role, assigner);

        assertThat(userRole.getUser()).isSameAs(assignee);
        assertThat(userRole.getRole()).isSameAs(role);
        assertThat(userRole.getAssignedBy()).isSameAs(assigner);
        assertThat(userRole.wasAssignedByUser()).isTrue();

        userRole.clearAssigner();

        assertThat(userRole.getAssignedBy()).isNull();
        assertThat(userRole.wasAssignedByUser()).isFalse();
    }

    private static void assertManyToOne(
            String fieldName, String mapsIdValue, String joinColumnName, boolean optional)
            throws Exception {
        Field relationship = field(fieldName);
        ManyToOne manyToOne = relationship.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = relationship.getAnnotation(JoinColumn.class);
        MapsId mapsId = relationship.getAnnotation(MapsId.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isEqualTo(optional);
        assertThat(joinColumn.name()).isEqualTo(joinColumnName);
        assertThat(joinColumn.nullable()).isEqualTo(optional);
        assertThat(mapsId.value()).isEqualTo(mapsIdValue);
    }

    private static Field field(String fieldName) throws Exception {
        return UserRole.class.getDeclaredField(fieldName);
    }

    private static User user() {
        return User.create("advisor@bayer-westphalian.test", "$2a$10$hash", "Advisor User");
    }

    private static Role role() {
        return Role.create(
                SystemRoleName.BI_ANALYST,
                "BI Analyst",
                "Views dashboards, reports, customer analytics, segmentation insights,"
                        + " and performance data",
                "View analytics, reports, segmentation insights, audience counts,"
                        + " campaign performance, product performance",
                true);
    }
}
