package com.bayerwestphalian.campaign.user;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.PasswordHashingService;
import com.bayerwestphalian.campaign.auth.method.AdminOnly;
import com.bayerwestphalian.campaign.common.exception.ConflictException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Employee user administration (KB FR-005 / E06).
 *
 * <p>Sensitive mutations write immutable {@code audit_logs} rows via {@link AuditService}:
 *
 * <ul>
 *   <li>Item 520 — user creation ({@code CREATE} on {@code users})
 *   <li>Item 521 — role changes ({@code ASSIGN_ROLE} / KB {@code logRoleChange})
 *   <li>Item 522 — user disable ({@code DISABLE_USER})
 * </ul>
 *
 * <p>Password hashes and raw passwords are never included in audit payloads.
 */
@Service
public class UserService {

    /** KB audit entity type for employee accounts ({@code audit_logs.entity_type}). */
    public static final String AUDIT_ENTITY_TYPE = "users";

    private static final Sort FULL_NAME_SORT = Sort.by("fullName").ascending();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordHashingService passwordHashingService;
    private final AuthorizationExpressions authorizationExpressions;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordHashingService passwordHashingService,
            AuthorizationExpressions authorizationExpressions,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordHashingService = passwordHashingService;
        this.authorizationExpressions = authorizationExpressions;
        this.auditService = auditService;
    }

    @AdminOnly
    @Transactional
    public UserView createUser(CreateUserCommand command) {
        validateCreateCommand(command);
        String email = command.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("USER_EMAIL_EXISTS", "User email already exists");
        }

        User user =
                User.create(
                        email,
                        passwordHashingService.hash(command.rawPassword()),
                        command.fullName().trim());
        User savedUser = userRepository.save(user);

        // Item 520 / SEC-012 / admin user-management guide: log user creation with actor + payload.
        auditService.logCreate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                savedUser.getId(),
                userCreationAuditPayload(savedUser));

        return toView(savedUser);
    }

    @AdminOnly
    @Transactional
    public UserView updateUser(UUID userId, UpdateUserCommand command) {
        validateUserId(userId);
        validateUpdateCommand(command);
        User user = findUser(userId);

        user.rename(command.fullName().trim());
        if (command.status() != null) {
            applyStatus(user, command.status());
        }

        return toView(userRepository.save(user));
    }

    /**
     * Disables an employee account (FR-005 soft offboarding).
     *
     * <p>Item 522 / SEC-012: when status transitions to {@code DISABLED}, writes a {@code
     * DISABLE_USER} audit row on entity type {@code users} with the Admin actor and before/after
     * status (plus email/fullName for traceability). Already-disabled accounts are idempotent and
     * do not emit a second audit entry.
     */
    @AdminOnly
    @Transactional
    public UserView disableUser(UUID userId) {
        validateUserId(userId);
        User user = findUser(userId);
        UserStatus previousStatus = user.getStatus();

        if (previousStatus == UserStatus.DISABLED) {
            return toView(user);
        }

        Map<String, Object> oldValue = userDisableAuditPayload(user, previousStatus);
        user.disable();
        User savedUser = userRepository.save(user);
        auditService.logUserDisable(
                currentActorUserId(),
                savedUser.getId(),
                oldValue,
                userDisableAuditPayload(savedUser, savedUser.getStatus()));
        return toView(savedUser);
    }

    @AdminOnly
    @Transactional
    public UserView resetPassword(UUID userId, String rawPassword) {
        validateUserId(userId);
        if (!StringUtils.hasText(rawPassword)) {
            throw new ValidationException(
                    "User validation failed", List.of("rawPassword: must not be blank"));
        }
        User user = findUser(userId);

        user.changePasswordHash(passwordHashingService.hash(rawPassword));
        return toView(userRepository.save(user));
    }

    /**
     * Assigns a system role to a user (FR-005 / SEC-012).
     *
     * <p>Item 521: each successful new assignment writes an {@code ASSIGN_ROLE} audit row via
     * {@link AuditService#logRoleChange} with before/after role sets. Duplicate assignments are
     * idempotent and do not write another audit entry.
     */
    @AdminOnly
    @Transactional
    public UserView assignRole(UUID userId, SystemRoleName roleName, UUID assignedByUserId) {
        validateUserId(userId);
        if (roleName == null) {
            throw new ValidationException(
                    "User validation failed", List.of("roleName: must not be null"));
        }

        User user = findUser(userId);
        Role role =
                roleRepository
                        .findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));
        UserRoleId userRoleId = new UserRoleId(user.getId(), role.getId());

        if (!userRoleRepository.existsById(userRoleId)) {
            UUID actorUserId = resolveRoleChangeActor(assignedByUserId);
            User assignedBy = assignedByUserId == null ? null : findUser(assignedByUserId);
            if (assignedBy == null && actorUserId != null) {
                assignedBy = userRepository.findById(actorUserId).orElse(null);
            }
            List<String> previousRoles = currentRoleNames(user.getId());
            Map<String, Object> oldValue = roleChangeOldPayload(user, previousRoles);

            userRoleRepository.save(UserRole.assign(user, role, assignedBy));

            // Item 521 / KB logRoleChange: actor + old/new role sets (no secrets).
            auditService.logRoleChange(
                    actorUserId,
                    user.getId(),
                    oldValue,
                    roleChangeNewPayload(user, role, previousRoles, actorUserId));
        }

        return toView(user);
    }

    @AdminOnly
    @Transactional(readOnly = true)
    public UserView findById(UUID userId) {
        validateUserId(userId);
        return toView(findUser(userId));
    }

    @AdminOnly
    @Transactional(readOnly = true)
    public List<UserView> listUsers(UserStatus status) {
        List<User> users =
                status == null
                        ? userRepository.findAll(FULL_NAME_SORT)
                        : userRepository.findByStatusOrderByFullNameAsc(status);

        return users.stream().map(this::toView).toList();
    }

    private UserView toView(User user) {
        List<UserRole> userRoles =
                user.getId() == null ? List.of() : userRoleRepository.findByIdUserId(user.getId());
        if (userRoles == null) {
            userRoles = List.of();
        }
        return UserView.from(user, userRoles);
    }

    /**
     * Structured CREATE payload for item 520. Intentionally omits password hashes and raw
     * credentials.
     */
    private Map<String, Object> userCreationAuditPayload(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (user.getId() != null) {
            payload.put("id", user.getId().toString());
        }
        payload.put("email", user.getEmail());
        payload.put("fullName", user.getFullName());
        payload.put("status", user.getStatus().name());
        return payload;
    }

    private Map<String, Object> roleChangeOldPayload(User user, List<String> previousRoles) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", user.getEmail());
        payload.put("roles", List.copyOf(previousRoles));
        return payload;
    }

    private Map<String, Object> roleChangeNewPayload(
            User user, Role role, List<String> previousRoles, UUID actorUserId) {
        List<String> nextRoles = new ArrayList<>(previousRoles);
        String assignedRole = role.getName().name();
        if (!nextRoles.contains(assignedRole)) {
            nextRoles.add(assignedRole);
        }
        Collections.sort(nextRoles);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", user.getEmail());
        payload.put("roles", List.copyOf(nextRoles));
        payload.put("assignedRole", assignedRole);
        payload.put("roleName", assignedRole);
        payload.put("roleId", role.getId().toString());
        if (actorUserId != null) {
            payload.put("assignedByUserId", actorUserId.toString());
        }
        return payload;
    }

    private List<String> currentRoleNames(UUID userId) {
        List<UserRole> userRoles = userRoleRepository.findByIdUserId(userId);
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }
        return userRoles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(Enum::name)
                .sorted()
                .toList();
    }

    /**
     * Prefer explicit assigner from the API request; otherwise the authenticated Admin principal.
     */
    private UUID resolveRoleChangeActor(UUID assignedByUserId) {
        if (assignedByUserId != null) {
            return assignedByUserId;
        }
        return currentActorUserId();
    }

    /**
     * Structured DISABLE_USER payload for item 522. Includes identity fields for audit search;
     * never includes password material.
     */
    private Map<String, Object> userDisableAuditPayload(User user, UserStatus status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (user.getId() != null) {
            payload.put("id", user.getId().toString());
        }
        payload.put("email", user.getEmail());
        payload.put("fullName", user.getFullName());
        payload.put("status", status.name());
        return payload;
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void applyStatus(User user, UserStatus status) {
        if (status == UserStatus.ACTIVE) {
            user.activate();
        } else if (status == UserStatus.DISABLED) {
            user.disable();
        } else {
            user.lock();
        }
    }

    private UUID currentActorUserId() {
        try {
            return authorizationExpressions.currentUserId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void validateCreateCommand(CreateUserCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "User validation failed", List.of("command: is required"));
        }

        List<String> errors =
                List.of(
                                required("email", command.email()),
                                required("rawPassword", command.rawPassword()),
                                required("fullName", command.fullName()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("User validation failed", errors);
        }
    }

    private void validateUpdateCommand(UpdateUserCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "User validation failed", List.of("command: is required"));
        }
        if (!StringUtils.hasText(command.fullName())) {
            throw new ValidationException(
                    "User validation failed", List.of("fullName: must not be blank"));
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User validation failed", List.of("userId: is required"));
        }
    }

    private String required(String fieldName, String value) {
        return StringUtils.hasText(value) ? "" : fieldName + ": must not be blank";
    }
}
