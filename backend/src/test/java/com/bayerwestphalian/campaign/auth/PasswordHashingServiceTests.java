package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashingServiceTests {

    private final PasswordHashingService passwordHashingService =
            new PasswordHashingService(new BCryptPasswordEncoder());

    @Test
    void hashesPasswordWithBCryptAndDoesNotStoreRawPassword() {
        String rawPassword = "StrongPassword!2026";

        String passwordHash = passwordHashingService.hash(rawPassword);

        assertThat(passwordHash).isNotEqualTo(rawPassword);
        assertThat(passwordHash).startsWith("$2");
        assertThat(passwordHash).hasSizeGreaterThan(50);
        assertThat(passwordHashingService.matches(rawPassword, passwordHash)).isTrue();
    }

    @Test
    void usesUniqueSaltForEachHash() {
        String rawPassword = "StrongPassword!2026";

        String firstHash = passwordHashingService.hash(rawPassword);
        String secondHash = passwordHashingService.hash(rawPassword);

        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(passwordHashingService.matches(rawPassword, firstHash)).isTrue();
        assertThat(passwordHashingService.matches(rawPassword, secondHash)).isTrue();
    }

    @Test
    void rejectsInvalidPasswordMatches() {
        String passwordHash = passwordHashingService.hash("StrongPassword!2026");

        assertThat(passwordHashingService.matches("WrongPassword!2026", passwordHash)).isFalse();
        assertThat(passwordHashingService.matches(null, passwordHash)).isFalse();
        assertThat(passwordHashingService.matches("StrongPassword!2026", null)).isFalse();
        assertThat(passwordHashingService.matches("StrongPassword!2026", " ")).isFalse();
    }

    @Test
    void rejectsBlankRawPasswordBeforeHashing() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> passwordHashingService.hash(" "))
                .withMessage("Password must not be blank");
    }

    @Test
    void treatsMissingHashAsNeedingRehash() {
        String passwordHash = passwordHashingService.hash("StrongPassword!2026");

        assertThat(passwordHashingService.needsRehash(null)).isTrue();
        assertThat(passwordHashingService.needsRehash(" ")).isTrue();
        assertThat(passwordHashingService.needsRehash(passwordHash)).isFalse();
    }
}
