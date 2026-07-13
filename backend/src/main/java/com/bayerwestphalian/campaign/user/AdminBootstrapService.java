package com.bayerwestphalian.campaign.user;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.PasswordHashingService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first production administrator without exposing a public registration path. */
@Service
public class AdminBootstrapService {

    static final String SEEDED_TEST_EMAIL_SUFFIX = "@bayer-westphalian.test";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordHashingService passwordHashingService;
    private final AuditService auditService;

    public AdminBootstrapService(
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

    @Transactional
    public BootstrapResult bootstrap(String email, String rawPassword, String fullName) {
        disableSeededTestAccounts();

        var existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            return new BootstrapResult(false, existing.get().getEmail());
        }

        Role adminRole =
                roleRepository
                        .findByName(SystemRoleName.ADMIN)
                        .orElseThrow(() -> new IllegalStateException("ADMIN system role is missing"));
        User admin =
                userRepository.saveAndFlush(
                        User.create(email, passwordHashingService.hash(rawPassword), fullName));
        userRoleRepository.save(UserRole.assign(admin, adminRole, null));

        Map<String, Object> created = new LinkedHashMap<>();
        created.put("email", admin.getEmail());
        created.put("fullName", admin.getFullName());
        created.put("status", admin.getStatus().name());
        created.put("bootstrap", true);
        auditService.logCreate(null, UserService.AUDIT_ENTITY_TYPE, admin.getId(), created);
        auditService.logRoleAssignment(
                null, admin.getId(), Map.of("email", admin.getEmail(), "roles", java.util.List.of("ADMIN"), "bootstrap", true));
        return new BootstrapResult(true, admin.getEmail());
    }

    private void disableSeededTestAccounts() {
        for (User user : userRepository.findByEmailEndingWithIgnoreCase(SEEDED_TEST_EMAIL_SUFFIX)) {
            if (user.getStatus() != UserStatus.DISABLED) {
                String previousStatus = user.getStatus().name();
                user.disable();
                auditService.logUserDisable(
                        null,
                        user.getId(),
                        Map.of("email", user.getEmail(), "status", previousStatus, "bootstrapSanitization", true),
                        Map.of("email", user.getEmail(), "status", "DISABLED", "bootstrapSanitization", true));
            }
        }
    }

    public record BootstrapResult(boolean created, String email) {}
}
