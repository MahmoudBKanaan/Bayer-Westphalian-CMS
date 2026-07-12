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
import com.bayerwestphalian.campaign.common.exception.ConflictException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB item 520: Admin user creation writes an immutable {@code CREATE} audit log for entity type
 * {@code users} (SEC-012 / admin user-management audit expectations).
 *
 * <p>Uses a real {@link AuditService} so the persisted {@link AuditLog} row shape is asserted
 * end-to-end through the service boundary.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("520 Log user creation")
class UserCreationCreatesAuditLogTests {

    private static final UUID NEW_USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000520");
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String EMAIL = "new.agent@bayer-westphalian.test";
    private static final String FULL_NAME = "New Service Agent";
    private static final String RAW_PASSWORD = "TempPassword!2026";
    private static final String PASSWORD_HASH = "$2a$10$hashed-for-item-520";

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
    @DisplayName("547 User creation creates audit log")
    void createUserPersistsCreateAuditLogWithActorAndNonSecretPayload() throws Exception {
        when(authorizationExpressions.currentUserId()).thenReturn(ADMIN_ID);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(passwordHashingService.hash(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            setId(user, NEW_USER_ID);
                            return user;
                        });
        when(userRoleRepository.findByIdUserId(NEW_USER_ID)).thenReturn(java.util.List.of());

        UserView view =
                userService.createUser(new CreateUserCommand(EMAIL, RAW_PASSWORD, FULL_NAME));

        assertThat(view.id()).isEqualTo(NEW_USER_ID);
        assertThat(view.email()).isEqualTo(EMAIL);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo(UserService.AUDIT_ENTITY_TYPE);
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(NEW_USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ADMIN_ID);
        assertThat(auditLog.getOldValue()).isNull();
        assertThat(auditLog.getNewValue())
                .containsEntry("id", NEW_USER_ID.toString())
                .containsEntry("email", EMAIL)
                .containsEntry("fullName", FULL_NAME)
                .containsEntry("status", "ACTIVE")
                .doesNotContainKey("password")
                .doesNotContainKey("rawPassword")
                .doesNotContainKey("passwordHash");
        assertThat(auditLog.getNewValue().values())
                .noneMatch(value -> RAW_PASSWORD.equals(String.valueOf(value)))
                .noneMatch(value -> PASSWORD_HASH.equals(String.valueOf(value)));
    }

    @Test
    void createUserDoesNotWriteAuditLogWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                userService.createUser(
                                        new CreateUserCommand(EMAIL, RAW_PASSWORD, FULL_NAME)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("User email already exists");

        verify(userRepository, never()).save(any(User.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
        verify(passwordHashingService, never()).hash(any());
    }

    @Test
    void createUserDoesNotWriteAuditLogWhenCommandIsInvalid() {
        assertThatThrownBy(
                        () -> userService.createUser(new CreateUserCommand(" ", " ", " ")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("User validation failed");

        verify(userRepository, never()).save(any(User.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void createUserStillAuditsWhenActorPrincipalIsUnavailable() throws Exception {
        // Admin-only method security normally guarantees an actor; if principal resolution fails,
        // still persist CREATE for accountability with a null actor rather than skipping the log.
        when(authorizationExpressions.currentUserId())
                .thenThrow(new RuntimeException("no principal in unit test"));
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(passwordHashingService.hash(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            setId(user, NEW_USER_ID);
                            return user;
                        });
        when(userRoleRepository.findByIdUserId(NEW_USER_ID)).thenReturn(java.util.List.of());

        userService.createUser(new CreateUserCommand(EMAIL, RAW_PASSWORD, FULL_NAME));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(NEW_USER_ID);
        assertThat(auditLog.getActorUserId()).isNull();
        assertThat(auditLog.getNewValue()).containsEntry("email", EMAIL);
    }

    private AuditLog captureSavedAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
