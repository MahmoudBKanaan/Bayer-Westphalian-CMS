package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.PasswordHashingService;
import com.bayerwestphalian.campaign.auth.method.AdminOnly;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ConflictException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000009901");
    private static final UUID ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000009902");
    private static final String EMAIL = "advisor@bayer-westphalian.test";

    @Mock private UserRepository userRepository;

    @Mock private RoleRepository roleRepository;

    @Mock private UserRoleRepository userRoleRepository;

    @Mock private PasswordHashingService passwordHashingService;

    @Mock private AuditService auditService;

    @InjectMocks private UserService userService;

    @Test
    void serviceMethodsRequireAdminAuthorization() throws Exception {
        assertAdminOnly("createUser", CreateUserCommand.class);
        assertAdminOnly("updateUser", UUID.class, UpdateUserCommand.class);
        assertAdminOnly("disableUser", UUID.class);
        assertAdminOnly("resetPassword", UUID.class, String.class);
        assertAdminOnly("assignRole", UUID.class, SystemRoleName.class, UUID.class);
        assertAdminOnly("findById", UUID.class);
        assertAdminOnly("listUsers", UserStatus.class);
    }

    @Test
    void createsUserWithBCryptHashAndRejectsDuplicateEmail() throws Exception {
        User savedUser = user();
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(passwordHashingService.hash("StrongPassword!2026")).thenReturn("$2a$10$newhash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        UserView view =
                userService.createUser(
                        new CreateUserCommand(EMAIL, "StrongPassword!2026", "Advisor User"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(passwordHashingService).hash("StrongPassword!2026");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$newhash");
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("StrongPassword!2026");
        assertThat(view.email()).isEqualTo(EMAIL);
        verify(auditService)
                .logCreate(
                        eq((UUID) null),
                        eq("users"),
                        eq(USER_ID),
                        eq(Map.of("email", EMAIL, "fullName", "Advisor User", "status", "ACTIVE")));

        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                userService.createUser(
                                        new CreateUserCommand(
                                                EMAIL, "StrongPassword!2026", "Advisor User")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User email already exists");
    }

    @Test
    void doesNotAuditUserCreationWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                userService.createUser(
                                        new CreateUserCommand(
                                                EMAIL, "StrongPassword!2026", "Advisor User")))
                .isInstanceOf(ConflictException.class);

        verify(auditService, never())
                .logCreate(any(), any(String.class), any(UUID.class), any(Map.class));
    }

    @Test
    void validatesCreateUserCommand() {
        assertThatThrownBy(() -> userService.createUser(new CreateUserCommand(" ", " ", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("User validation failed");
    }

    @Test
    void updatesUserProfileAndStatus() throws Exception {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        UserView view =
                userService.updateUser(
                        USER_ID, new UpdateUserCommand("Senior Advisor", UserStatus.LOCKED));

        assertThat(view.fullName()).isEqualTo("Senior Advisor");
        assertThat(view.status()).isEqualTo(UserStatus.LOCKED);
        verify(userRepository).save(user);
    }

    @Test
    void disablesUser() throws Exception {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        UserView view = userService.disableUser(USER_ID);

        assertThat(view.status()).isEqualTo(UserStatus.DISABLED);
        verify(userRepository).save(user);
        verify(auditService)
                .logUserDisable(
                        eq((UUID) null),
                        eq(USER_ID),
                        eq(Map.of("status", "ACTIVE")),
                        eq(Map.of("status", "DISABLED")));
    }

    @Test
    void resetsPasswordWithHashingService() throws Exception {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordHashingService.hash("NewStrongPassword!2026")).thenReturn("$2a$10$newhash");
        when(userRepository.save(user)).thenReturn(user);
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        UserView view = userService.resetPassword(USER_ID, "NewStrongPassword!2026");

        assertThat(view.email()).isEqualTo(EMAIL);
        verify(passwordHashingService).hash("NewStrongPassword!2026");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$10$newhash");
        assertThat(user.getPasswordHash()).isNotEqualTo("NewStrongPassword!2026");
    }

    @Test
    void assignsRoleIdempotentlyWithAssignmentTraceability() throws Exception {
        User user = user();
        User assignedBy = user(ADMIN_ID, "admin@bayer-westphalian.test", "Admin User");
        Role role = role();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(assignedBy));
        when(roleRepository.findByName(SystemRoleName.ADMIN)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsById(new UserRoleId(USER_ID, ROLE_ID))).thenReturn(false);

        UserView view = userService.assignRole(USER_ID, SystemRoleName.ADMIN, ADMIN_ID);

        ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUser()).isSameAs(user);
        assertThat(userRoleCaptor.getValue().getRole()).isSameAs(role);
        assertThat(userRoleCaptor.getValue().getAssignedBy()).isSameAs(assignedBy);
        assertThat(view.email()).isEqualTo(EMAIL);
        verify(auditService)
                .logRoleAssignment(
                        eq(ADMIN_ID),
                        eq(USER_ID),
                        eq(
                                Map.of(
                                        "email",
                                        EMAIL,
                                        "roleName",
                                        "ADMIN",
                                        "roleId",
                                        ROLE_ID.toString())));
    }

    @Test
    void doesNotDuplicateExistingRoleAssignment() throws Exception {
        User user = user();
        Role role = role();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(SystemRoleName.ADMIN)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsById(new UserRoleId(USER_ID, ROLE_ID))).thenReturn(true);

        userService.assignRole(USER_ID, SystemRoleName.ADMIN, null);

        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(auditService, never())
                .logRoleAssignment(any(UUID.class), any(UUID.class), any(Map.class));
    }

    @Test
    void findsAndListsUsersAsViews() throws Exception {
        User user = user();
        Role role = role();
        UserRole userRole = UserRole.assign(user, role, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of(userRole));

        UserView found = userService.findById(USER_ID);

        assertThat(found.roles()).containsExactly(SystemRoleName.ADMIN);

        when(userRepository.findByStatusOrderByFullNameAsc(UserStatus.ACTIVE))
                .thenReturn(List.of(user));

        List<UserView> activeUsers = userService.listUsers(UserStatus.ACTIVE);

        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).email()).isEqualTo(EMAIL);

        when(userRepository.findAll(Sort.by("fullName").ascending())).thenReturn(List.of(user));

        assertThat(userService.listUsers(null)).hasSize(1);
    }

    @Test
    void throwsWhenUserOrRoleDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User was not found: " + USER_ID);
    }

    private static void assertAdminOnly(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = UserService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(AdminOnly.class)).isTrue();
    }

    private static User user() throws Exception {
        return user(USER_ID, EMAIL, "Advisor User");
    }

    private static User user(UUID userId, String email, String fullName) throws Exception {
        User user = User.create(email, "$2a$10$hash", fullName);
        setId(user, userId);
        return user;
    }

    private static Role role() throws Exception {
        Role role =
                Role.create(
                        SystemRoleName.ADMIN,
                        "Admin",
                        "Manages users, roles, settings, and full system configuration",
                        "Manage users, assign roles, manage settings, view audit logs",
                        true);
        setId(role, ROLE_ID);
        return role;
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        java.lang.reflect.Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
