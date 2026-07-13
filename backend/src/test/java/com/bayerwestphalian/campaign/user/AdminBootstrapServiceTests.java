package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.PasswordHashingService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTests {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock PasswordHashingService passwordHashingService;
    @Mock AuditService auditService;
    @InjectMocks AdminBootstrapService service;

    @Test
    void createsHashedAdminRoleAndAuditTrail() throws Exception {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000007320");
        Role role =
                Role.create(
                        SystemRoleName.ADMIN,
                        "Administrator",
                        "Full platform administration",
                        "Manage users and configuration",
                        true);
        User saved = User.create("owner@example.com", "$2a$10$hash", "Production Administrator");
        setId(saved, userId);
        when(userRepository.findByEmailEndingWithIgnoreCase(any())).thenReturn(List.of());
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(SystemRoleName.ADMIN)).thenReturn(Optional.of(role));
        when(passwordHashingService.hash("VeryStrongPassword2026")).thenReturn("$2a$10$hash");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(saved);

        var result = service.bootstrap("owner@example.com", "VeryStrongPassword2026", "Production Administrator");

        assertThat(result.created()).isTrue();
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(user.capture());
        assertThat(user.getValue().getPasswordHash()).isEqualTo("$2a$10$hash");
        verify(userRoleRepository).save(any(UserRole.class));
        verify(auditService).logCreate(eq(null), eq("users"), eq(userId), any());
        verify(auditService).logRoleAssignment(eq(null), eq(userId), any());
    }

    @Test
    void existingAccountIsNeverModifiedOrPromoted() {
        User existing = User.create("owner@example.com", "existing-hash", "Owner");
        when(userRepository.findByEmailEndingWithIgnoreCase(any())).thenReturn(List.of());
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(existing));

        var result = service.bootstrap("owner@example.com", "ReplacementPassword2026", "Replacement");

        assertThat(result.created()).isFalse();
        assertThat(existing.getPasswordHash()).isEqualTo("existing-hash");
        verify(passwordHashingService, never()).hash(any());
        verify(userRepository, never()).saveAndFlush(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void disablesMigrationSeededTestAccountsBeforeBootstrap() throws Exception {
        User seeded = User.create("admin@bayer-westphalian.test", "seed-hash", "MVP Admin");
        setId(seeded, UUID.fromString("10000000-0000-0000-0000-000000000001"));
        when(userRepository.findByEmailEndingWithIgnoreCase(AdminBootstrapService.SEEDED_TEST_EMAIL_SUFFIX))
                .thenReturn(List.of(seeded));
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(User.create("owner@example.com", "hash", "Owner")));

        service.bootstrap("owner@example.com", "UnusedPassword2026", "Owner");

        assertThat(seeded.getStatus()).isEqualTo(UserStatus.DISABLED);
        verify(auditService).logUserDisable(eq(null), eq(seeded.getId()), any(), any());
    }

    private static void setId(User user, UUID id) throws Exception {
        Field field = BaseEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
