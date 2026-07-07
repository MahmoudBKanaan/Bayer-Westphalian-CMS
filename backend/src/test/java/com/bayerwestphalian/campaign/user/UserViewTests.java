package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserViewTests {

    @Test
    void mapsUserAndAssignedRolesWithoutPasswordHash() {
        User user = User.create("advisor@bayer-westphalian.test", "$2a$10$hash", "Advisor User");
        Role role = role(SystemRoleName.ADMIN);

        UserView view = UserView.from(user, List.of(UserRole.assign(user, role, null)));

        assertThat(view.email()).isEqualTo("advisor@bayer-westphalian.test");
        assertThat(view.fullName()).isEqualTo("Advisor User");
        assertThat(view.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(view.roles()).containsExactly(SystemRoleName.ADMIN);
    }

    @Test
    void defensivelyCopiesRoles() {
        List<SystemRoleName> roles = new ArrayList<>(List.of(SystemRoleName.ADMIN));

        UserView view =
                new UserView(
                        null, "admin@example.test", "Admin User", UserStatus.ACTIVE, null, roles);
        roles.clear();

        assertThat(view.roles()).containsExactly(SystemRoleName.ADMIN);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> view.roles().add(SystemRoleName.BI_ANALYST));
    }

    private static Role role(SystemRoleName roleName) {
        return Role.create(roleName, "Admin", "Admin role", "Manage users", true);
    }
}
