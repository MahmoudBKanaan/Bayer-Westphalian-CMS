package com.bayerwestphalian.campaign.user;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.PasswordHashingService;
import com.bayerwestphalian.campaign.auth.method.AdminOnly;
import com.bayerwestphalian.campaign.common.exception.ConflictException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private static final Sort FULL_NAME_SORT = Sort.by("fullName").ascending();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordHashingService passwordHashingService;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordHashingService passwordHashingService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordHashingService = passwordHashingService;
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
        auditService.logCreate(null, "users", savedUser.getId(), userAuditPayload(savedUser));

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

    @AdminOnly
    @Transactional
    public UserView disableUser(UUID userId) {
        validateUserId(userId);
        User user = findUser(userId);
        UserStatus previousStatus = user.getStatus();

        user.disable();
        User savedUser = userRepository.save(user);
        auditService.logUserDisable(
                null,
                savedUser.getId(),
                userStatusAuditPayload(previousStatus),
                userStatusAuditPayload(savedUser.getStatus()));
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
        User assignedBy = assignedByUserId == null ? null : findUser(assignedByUserId);
        UserRoleId userRoleId = new UserRoleId(user.getId(), role.getId());

        if (!userRoleRepository.existsById(userRoleId)) {
            userRoleRepository.save(UserRole.assign(user, role, assignedBy));
            auditService.logRoleAssignment(
                    assignedByUserId, user.getId(), roleAssignmentAuditPayload(user, role));
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

    private Map<String, ?> userAuditPayload(User user) {
        return Map.of(
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "status", user.getStatus().name());
    }

    private Map<String, ?> roleAssignmentAuditPayload(User user, Role role) {
        return Map.of(
                "email", user.getEmail(),
                "roleName", role.getName().name(),
                "roleId", role.getId().toString());
    }

    private Map<String, ?> userStatusAuditPayload(UserStatus status) {
        return Map.of("status", status.name());
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
