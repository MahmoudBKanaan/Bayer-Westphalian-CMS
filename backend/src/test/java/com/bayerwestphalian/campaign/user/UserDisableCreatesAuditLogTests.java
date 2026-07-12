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
 * KB item 522: Admin user disable writes an immutable {@code DISABLE_USER} audit log on entity type
 * {@code users} (SEC-012 / admin user-management audit expectations).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("522 Log user disable")
class UserDisableCreatesAuditLogTests {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000522");
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String EMAIL = "advisor@bayer-westphalian.test";
    private static final String FULL_NAME = "Advisor User";

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
    void disableUserPersistsDisableUserAuditWithActorAndStatusTransition() throws Exception {
        User target = user(USER_ID, EMAIL, FULL_NAME);
        when(authorizationExpressions.currentUserId()).thenReturn(ADMIN_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        UserView view = userService.disableUser(USER_ID);

        assertThat(view.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(target.getStatus()).isEqualTo(UserStatus.DISABLED);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("DISABLE_USER");
        assertThat(auditLog.getEntityType()).isEqualTo(UserService.AUDIT_ENTITY_TYPE);
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ADMIN_ID);
        assertThat(auditLog.getOldValue())
                .containsEntry("id", USER_ID.toString())
                .containsEntry("email", EMAIL)
                .containsEntry("fullName", FULL_NAME)
                .containsEntry("status", "ACTIVE")
                .doesNotContainKey("password")
                .doesNotContainKey("passwordHash");
        assertThat(auditLog.getNewValue())
                .containsEntry("id", USER_ID.toString())
                .containsEntry("email", EMAIL)
                .containsEntry("fullName", FULL_NAME)
                .containsEntry("status", "DISABLED")
                .doesNotContainKey("password")
                .doesNotContainKey("passwordHash");
    }

    @Test
    void disableUserFromLockedStatusStillAuditsTransitionToDisabled() throws Exception {
        User target = user(USER_ID, EMAIL, FULL_NAME);
        target.lock();
        when(authorizationExpressions.currentUserId()).thenReturn(ADMIN_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        userService.disableUser(USER_ID);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("DISABLE_USER");
        assertThat(auditLog.getOldValue()).containsEntry("status", "LOCKED");
        assertThat(auditLog.getNewValue()).containsEntry("status", "DISABLED");
    }

    @Test
    void alreadyDisabledUserDoesNotWriteSecondAuditLog() throws Exception {
        User target = user(USER_ID, EMAIL, FULL_NAME);
        target.disable();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        UserView view = userService.disableUser(USER_ID);

        assertThat(view.status()).isEqualTo(UserStatus.DISABLED);
        verify(userRepository, never()).save(any(User.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void disableUserDoesNotWriteAuditWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.disableUser(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void disableUserDoesNotWriteAuditWhenUserIdNull() {
        assertThatThrownBy(() -> userService.disableUser(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("User validation failed");

        verify(userRepository, never()).save(any(User.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void disableUserStillAuditsWhenActorPrincipalIsUnavailable() throws Exception {
        User target = user(USER_ID, EMAIL, FULL_NAME);
        when(authorizationExpressions.currentUserId())
                .thenThrow(new RuntimeException("no principal in unit test"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of());

        userService.disableUser(USER_ID);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("DISABLE_USER");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isNull();
        assertThat(auditLog.getNewValue()).containsEntry("status", "DISABLED");
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

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
