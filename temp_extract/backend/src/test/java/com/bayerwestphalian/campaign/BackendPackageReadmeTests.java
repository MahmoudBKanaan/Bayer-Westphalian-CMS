package com.bayerwestphalian.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BackendPackageReadmeTests {

    private static final Path README =
            Path.of("src/main/java/com/bayerwestphalian/campaign/README.md");

    @Test
    void documentsKbModularMonolithPackagesAndBoundaries() throws Exception {
        String readme = Files.readString(README, StandardCharsets.UTF_8);

        assertThat(readme)
                .contains("modular monolith")
                .contains("com.bayerwestphalian.campaign")
                .contains("PostgreSQL is the system of record")
                .contains("Flyway owns all schema and seed-data changes")
                .contains("Controlled demonstration data")
                .contains("GlobalExceptionHandler")
                .contains("BaseEntity")
                .contains("SoftDeletableEntity");

        assertThat(requiredPackages())
                .allSatisfy(packageName -> assertThat(readme).contains("`" + packageName + "`"));
    }

    @Test
    void documentsCoreDependencyRules() throws Exception {
        String readme = Files.readString(README, StandardCharsets.UTF_8);

        assertThat(readme)
                .contains("Modules may depend on `common`")
                .contains("Backend validation is authoritative")
                .contains("Security, consent, campaign eligibility, audit, and customer-data rules")
                .contains("Mock providers are allowed only for development and testing");
    }

    private static List<String> requiredPackages() {
        return List.of(
                "auth",
                "user",
                "customer",
                "beneficiary",
                "consent",
                "product",
                "campaign",
                "segment",
                "schedule",
                "communication",
                "analytics",
                "audit",
                "ai",
                "report",
                "common");
    }
}
