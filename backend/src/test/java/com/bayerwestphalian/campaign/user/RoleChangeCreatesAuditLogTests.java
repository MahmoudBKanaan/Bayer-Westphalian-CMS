package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditLogRepository;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.PasswordHashingService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB items 521 / 548: Admin role assignment writes an immutable {@code ASSIGN_ROLE} audit log (KB
 * {@code logRoleChange} / SEC-012) with before/after role sets on entity type {@code users}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("521 Log role changes")
class RoleChangeCreatesAuditLogTests {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000521");
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID BI_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CM_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String EMAIL = "advisor@bayer-westphalian.test";

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordHashingService passwordHashingService;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private UserService userService;

    @BeforeEach
    void setUp() {
        AuditService auditService = new AuditService(auditLogRepository);
        lenient()
                .when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService =
                new UserService(
                        userRepository,
                        roleRepository,
                        userRoleRepository,
                        passwordHashingService,
                        authorizationExpressions,
                        auditService);
    }

    @Test
    @DisplayName("548 Role change creates audit log")
    void assignRolePersistsAssignRoleAuditWithOldAndNewRoleSets() throws Exception {
        User target = user(USER_ID, EMAIL, "Advisor User");
        User admin = user(ADMIN_ID, "admin@bayer-westphalian.test", "Admin User");
        Role existingBi = role(BI_ROLE_ID, SystemRoleName.BI_ANALYST, "BI Analyst");
        Role newCampaignManager =
                role(CM_ROLE_ID, SystemRoleName.CAMPAIGN_MANAGER, "Campaign Manager");
        UserRole existingAssignment = UserRole.assign(target, existingBi, admin);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(roleRepository.findByName(SystemRoleName.CAMPAIGN_MANAGER))
                .thenReturn(Optional.of(newCampaignManager));
        when(userRoleRepository.existsById(new UserRoleId(USER_ID, CM_ROLE_ID))).thenReturn(false);
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of(existingAssignment));

        userService.assignRole(USER_ID, SystemRoleName.CAMPAIGN_MANAGER, ADMIN_ID);

        ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getRole().getName())
                .isEqualTo(SystemRoleName.CAMPAIGN_MANAGER);
        assertThat(userRoleCaptor.getValue().getAssignedBy()).isSameAs(admin);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("ASSIGN_ROLE");
        assertThat(auditLog.getEntityType()).isEqualTo(UserService.AUDIT_ENTITY_TYPE);
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ADMIN_ID);
        assertThat(auditLog.getOldValue())
                .containsEntry("email", EMAIL)
                .containsEntry("roles", List.of("BI_ANALYST"));
        assertThat(auditLog.getNewValue())
                .containsEntry("email", EMAIL)
                .containsEntry("roles", List.of("BI_ANALYST", "CAMPAIGN_MANAGER"))
                .containsEntry("assignedRole", "CAMPAIGN_MANAGER")
                .containsEntry("roleName", "CAMPAIGN_MANAGER")
                .containsEntry("roleId", CM_ROLE_ID.toString())
                .containsEntry("assignedByUserId", ADMIN_ID.toString())
                .doesNotContainKey("password")
                .doesNotContainKey("passwordHash");
    }

    @Test
    void assignRoleUsesCurrentPrincipalWhenAssignedByOmitted() throws Exception {
        User target = user(USER_ID, EMAIL, "Advisor User");
        User admin = user(ADMIN_ID, "admin@bayer-westphalian.test", "Admin User");
        Role role = role(CM_ROLE_ID, SystemRoleName.CAMPAIGN_MANAGER, "Campaign Manager");

        when(authorizationExpressions.currentUserId()).thenReturn(ADMIN_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(roleRepository.findByName(SystemRoleName.CAMPAIGN_MANAGER))
                .thenReturn(Optional.of(role));
        when(userRoleRepository.existsById(new UserRoleId(USER_ID, CM_ROLE_ID))).thenReturn(false);
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        userService.assignRole(USER_ID, SystemRoleName.CAMPAIGN_MANAGER, null);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getActorUserId()).isEqualTo(ADMIN_ID);
        assertThat(auditLog.getOldValue()).containsEntry("roles", List.of());
        assertThat(auditLog.getNewValue())
                .containsEntry("roles", List.of("CAMPAIGN_MANAGER"))
                .containsEntry("assignedByUserId", ADMIN_ID.toString());
    }

    @Test
    void duplicateRoleAssignmentDoesNotWriteAuditLog() throws Exception {
        User target = user(USER_ID, EMAIL, "Advisor User");
        Role role = role(CM_ROLE_ID, SystemRoleName.CAMPAIGN_MANAGER, "Campaign Manager");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(roleRepository.findByName(SystemRoleName.CAMPAIGN_MANAGER))
                .thenReturn(Optional.of(role));
        when(userRoleRepository.existsById(new UserRoleId(USER_ID, CM_ROLE_ID))).thenReturn(true);

        userService.assignRole(USER_ID, SystemRoleName.CAMPAIGN_MANAGER, ADMIN_ID);

        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void assignRoleDoesNotWriteAuditWhenRoleMissing() throws Exception {
        User target = user(USER_ID, EMAIL, "Advisor User");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(roleRepository.findByName(SystemRoleName.SYSTEM_AUDITOR)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                userService.assignRole(
                                        USER_ID, SystemRoleName.SYSTEM_AUDITOR, ADMIN_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void assignRoleDoesNotWriteAuditWhenRoleNameNull() throws Exception {
        assertThatThrownBy(() -> userService.assignRole(USER_ID, null, ADMIN_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("User validation failed");

        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    private AuditLog captureSavedAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static User user(UUID userId, String email, String fullName) throws Exception {
        User user = User.create(email, "$2a$10$hash", fullName);
        setId(user, userId);
        return user;
    }

    private static Role role(UUID roleId, SystemRoleName name, String displayName)
            throws Exception {
        Role role =
                Role.create(
                        name,
                        displayName,
                        displayName + " description",
                        "Permissions for " + displayName,
                        true);
        setId(role, roleId);
        return role;
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
