package com.bayerwestphalian.campaign.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("bwc_flyway_test_schema")
class FlywayMigrationIntegrationTests {

    private static final String DB_URL =
            System.getProperty("bwc.test.db.url", "jdbc:postgresql://localhost:5432/bwc_campaign");
    private static final String DB_USERNAME = System.getProperty("bwc.test.db.username", "bwc_app");
    private static final String DB_PASSWORD = System.getProperty("bwc.test.db.password", "bwc_app");
    private static final String TEST_SCHEMA = "bwc_flyway_test";
    private static final Pattern VERSIONED_MIGRATION_NAME =
            Pattern.compile("V([1-9][0-9]*)__[a-z][a-z0-9]*(?:_[a-z0-9]+)*\\.sql");
    private static final List<String> KB_INITIAL_SCHEMA_TABLES =
            List.of(
                    "users",
                    "roles",
                    "user_roles",
                    "customers",
                    "beneficiaries",
                    "consent_records",
                    "products",
                    "product_ownerships",
                    "product_change_requests",
                    "payment_records",
                    "segments",
                    "segment_criteria",
                    "campaigns",
                    "campaign_products",
                    "campaign_recipients",
                    "contact_events",
                    "follow_up_tasks",
                    "reminder_schedules",
                    "campaign_metrics",
                    "report_exports",
                    "audit_logs",
                    "ai_recommendations");

    @BeforeEach
    void prepareSchema() throws Exception {
        assumeTrue(isPortOpen("localhost", 5432), "Local PostgreSQL is not running on port 5432");
        dropTestSchema();
    }

    @AfterEach
    void removeSchema() throws Exception {
        if (isPortOpen("localhost", 5432)) {
            dropTestSchema();
        }
    }

    @Test
    void appliesInitialSchemaMigrationToPostgreSql() throws Exception {
        Flyway flyway =
                Flyway.configure()
                        .dataSource(DB_URL, DB_USERNAME, DB_PASSWORD)
                        .locations("classpath:db/migration")
                        .schemas(TEST_SCHEMA)
                        .defaultSchema(TEST_SCHEMA)
                        .createSchemas(true)
                        .initSql("set search_path to " + TEST_SCHEMA)
                        .load();

        flyway.migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "select schema_version, description from "
                                        + TEST_SCHEMA
                                        + ".bwc_schema_metadata")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("schema_version")).isEqualTo("v0.2");
            assertThat(resultSet.getString("description"))
                    .isEqualTo("Initial KB schema initialized");
        }
    }

    @Test
    void migratesEmptyDatabaseSchemaThroughLatestProductionVersion() throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "select to_regnamespace('" + TEST_SCHEMA + "') is null as empty")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getBoolean("empty")).isTrue();
        }

        MigrateResult migrationResult = migrate();

        assertThat(migrationResult.success).isTrue();
        assertThat(migrationResult.migrationsExecuted).isEqualTo(25);
        assertThat(migrationResult.schemaName).isEqualTo(TEST_SCHEMA);
        assertThat(migrationResult.targetSchemaVersion).isEqualTo("25");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as migration_count,"
                                    + " max(version::int) as latest_version,"
                                    + " count(*) filter (where not success) as failed_count,"
                                    + " count(*) filter (where script like 'R__%') as repeatable_count"
                                    + " from "
                                    + TEST_SCHEMA
                                    + ".flyway_schema_history"
                                    + " where type = 'SQL'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("migration_count")).isEqualTo(25);
                assertThat(resultSet.getInt("latest_version")).isEqualTo(25);
                assertThat(resultSet.getInt("failed_count")).isZero();
                assertThat(resultSet.getInt("repeatable_count")).isZero();
            }

            for (String tableName : KB_INITIAL_SCHEMA_TABLES) {
                try (ResultSet resultSet =
                        statement.executeQuery(
                                "select to_regclass('"
                                        + TEST_SCHEMA
                                        + "."
                                        + tableName
                                        + "') is not null as exists")) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getBoolean("exists")).as(tableName).isTrue();
                }
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as seeded_users from "
                                    + TEST_SCHEMA
                                    + ".users"
                                    + " where email like '%@bayer-westphalian.test'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("seeded_users")).isEqualTo(10);
            }
        }
    }

    @Test
    void createsKbInitialSchemaTablesAndSeedRoles() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (String tableName : KB_INITIAL_SCHEMA_TABLES) {
                try (ResultSet resultSet =
                        statement.executeQuery(
                                "select to_regclass('"
                                        + TEST_SCHEMA
                                        + "."
                                        + tableName
                                        + "') is not null as exists")) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getBoolean("exists")).as(tableName).isTrue();
                }
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as role_count from " + TEST_SCHEMA + ".roles")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("role_count")).isEqualTo(10);
            }
        }
    }

    @Test
    void migrationCreatesAllKbRequiredTables() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as required_table_count from information_schema.tables"
                                    + " where table_schema = '"
                                    + TEST_SCHEMA
                                    + "' and table_type = 'BASE TABLE'"
                                    + " and table_name in ("
                                    + kbRequiredTableNamesSql()
                                    + ")")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("required_table_count"))
                        .isEqualTo(KB_INITIAL_SCHEMA_TABLES.size());
            }

            for (String tableName : KB_INITIAL_SCHEMA_TABLES) {
                try (ResultSet resultSet =
                        statement.executeQuery(
                                "select exists (select 1 from information_schema.tables"
                                        + " where table_schema = '"
                                        + TEST_SCHEMA
                                        + "' and table_type = 'BASE TABLE'"
                                        + " and table_name = '"
                                        + tableName
                                        + "') as exists")) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getBoolean("exists")).as(tableName).isTrue();
                }
            }
        }
    }

    @Test
    void createsKeyForeignKeysAndUniqueConstraints() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertConstraintExists(
                    statement, "beneficiaries", "beneficiaries_policyholder_customer_id_fkey");
            assertConstraintExists(
                    statement, "beneficiaries", "beneficiaries_beneficiary_customer_id_fkey");
            assertConstraintExists(
                    statement, "consent_records", "consent_records_customer_id_fkey");
            assertConstraintExists(
                    statement, "campaign_products", "campaign_products_campaign_id_fkey");
            assertConstraintExists(
                    statement,
                    "campaign_recipients",
                    "campaign_recipients_campaign_customer_unique");
        }
    }

    @Test
    void createsKbForeignKeyConstraintsWithExpectedDeleteRules() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertForeignKeyExists(
                    statement, "user_roles", "user_roles_user_id_fkey", "users", "CASCADE");
            assertForeignKeyExists(
                    statement, "user_roles", "user_roles_role_id_fkey", "roles", "RESTRICT");
            assertForeignKeyExists(
                    statement, "user_roles", "user_roles_assigned_by_fkey", "users", "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "beneficiaries",
                    "beneficiaries_policyholder_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "beneficiaries",
                    "beneficiaries_beneficiary_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "consent_records",
                    "consent_records_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "consent_records",
                    "consent_records_created_by_fkey",
                    "users",
                    "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "product_ownerships",
                    "product_ownerships_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "product_ownerships",
                    "product_ownerships_product_id_fkey",
                    "products",
                    "RESTRICT");
            assertForeignKeyExists(
                    statement,
                    "product_change_requests",
                    "product_change_requests_product_id_fkey",
                    "products",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "product_change_requests",
                    "product_change_requests_requested_by_fkey",
                    "users",
                    "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "payment_records",
                    "payment_records_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "payment_records",
                    "payment_records_product_ownership_id_fkey",
                    "product_ownerships",
                    "CASCADE");
            assertForeignKeyExists(
                    statement, "segments", "segments_owner_user_id_fkey", "users", "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "segment_criteria",
                    "segment_criteria_segment_id_fkey",
                    "segments",
                    "CASCADE");
            assertForeignKeyExists(
                    statement, "campaigns", "campaigns_owner_user_id_fkey", "users", "SET NULL");
            assertForeignKeyExists(
                    statement, "campaigns", "campaigns_segment_id_fkey", "segments", "SET NULL");
            assertForeignKeyExists(
                    statement, "campaigns", "campaigns_approved_by_fkey", "users", "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "campaign_products",
                    "campaign_products_campaign_id_fkey",
                    "campaigns",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "campaign_products",
                    "campaign_products_product_id_fkey",
                    "products",
                    "RESTRICT");
            assertForeignKeyExists(
                    statement,
                    "campaign_recipients",
                    "campaign_recipients_campaign_id_fkey",
                    "campaigns",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "campaign_recipients",
                    "campaign_recipients_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "contact_events",
                    "contact_events_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "contact_events",
                    "contact_events_campaign_id_fkey",
                    "campaigns",
                    "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "contact_events",
                    "contact_events_created_by_fkey",
                    "users",
                    "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "follow_up_tasks",
                    "follow_up_tasks_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "follow_up_tasks",
                    "follow_up_tasks_campaign_id_fkey",
                    "campaigns",
                    "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "follow_up_tasks",
                    "follow_up_tasks_assigned_to_fkey",
                    "users",
                    "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "reminder_schedules",
                    "reminder_schedules_customer_id_fkey",
                    "customers",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "reminder_schedules",
                    "reminder_schedules_product_id_fkey",
                    "products",
                    "RESTRICT");
            assertForeignKeyExists(
                    statement,
                    "campaign_metrics",
                    "campaign_metrics_campaign_id_fkey",
                    "campaigns",
                    "CASCADE");
            assertForeignKeyExists(
                    statement,
                    "report_exports",
                    "report_exports_requested_by_fkey",
                    "users",
                    "SET NULL");
            assertForeignKeyExists(
                    statement, "audit_logs", "audit_logs_actor_user_id_fkey", "users", "SET NULL");
            assertForeignKeyExists(
                    statement,
                    "ai_recommendations",
                    "ai_recommendations_approved_by_user_id_fkey",
                    "users",
                    "SET NULL");
        }
    }

    @Test
    void enforcesKbForeignKeyConstraintsOnInvalidReferences() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name, status)"
                            + " values ('20000000-0000-0000-0000-000000009901', 'CUSTOMER',"
                            + " 'Foreign', 'Key', 'ACTIVE')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".products (id, name, product_type, active)"
                            + " values ('30000000-0000-0000-0000-000000009901',"
                            + " 'FK Enforcement Product', 'OTHER', true)");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaigns (id, name, objective, channel)"
                            + " values ('50000000-0000-0000-0000-000000009901',"
                            + " 'FK Enforcement Campaign', 'Verify FK enforcement', 'EMAIL')");

            assertForeignKeyViolation(
                    () ->
                            statement.executeUpdate(
                                    "insert into "
                                            + TEST_SCHEMA
                                            + ".user_roles (user_id, role_id)"
                                            + " values ("
                                            + "'10000000-0000-0000-0000-000000009999',"
                                            + " '00000000-0000-0000-0000-000000000001')"),
                    "user_roles_user_id_fkey");
            assertForeignKeyViolation(
                    () ->
                            statement.executeUpdate(
                                    "insert into "
                                            + TEST_SCHEMA
                                            + ".consent_records"
                                            + " (id, customer_id, consent_type, status, purpose)"
                                            + " values ("
                                            + "'21000000-0000-0000-0000-000000009901',"
                                            + " '20000000-0000-0000-0000-000000009999',"
                                            + " 'MARKETING_EMAIL', 'GIVEN', 'FK test')"),
                    "consent_records_customer_id_fkey");
            assertForeignKeyViolation(
                    () ->
                            statement.executeUpdate(
                                    "insert into "
                                            + TEST_SCHEMA
                                            + ".product_ownerships"
                                            + " (id, customer_id, product_id, start_date)"
                                            + " values ("
                                            + "'31000000-0000-0000-0000-000000009901',"
                                            + " '20000000-0000-0000-0000-000000009901',"
                                            + " '30000000-0000-0000-0000-000000009999',"
                                            + " current_date)"),
                    "product_ownerships_product_id_fkey");
            assertForeignKeyViolation(
                    () ->
                            statement.executeUpdate(
                                    "insert into "
                                            + TEST_SCHEMA
                                            + ".campaign_recipients"
                                            + " (id, campaign_id, customer_id, eligibility_status)"
                                            + " values ("
                                            + "'51000000-0000-0000-0000-000000009901',"
                                            + " '50000000-0000-0000-0000-000000009999',"
                                            + " '20000000-0000-0000-0000-000000009901',"
                                            + " 'ELIGIBLE')"),
                    "campaign_recipients_campaign_id_fkey");
        }
    }

    @Test
    void createsKbSearchFilterAndForeignKeyIndexes() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertIndexExists(statement, "idx_users_full_name");
            assertIndexExists(statement, "idx_users_status");
            assertIndexExists(statement, "idx_user_roles_role");
            assertIndexExists(statement, "idx_customers_search_name");
            assertIndexExists(statement, "idx_customers_phone");
            assertIndexExists(statement, "idx_customers_type_status");
            assertIndexExists(statement, "idx_customers_status_deleted");
            assertIndexExists(statement, "idx_products_name_type_active");
            assertIndexExists(statement, "idx_product_ownerships_status_expiration");
            assertIndexExists(statement, "idx_product_ownerships_product_expiration");
            assertIndexExists(statement, "idx_campaigns_owner_status");
            assertIndexExists(statement, "idx_campaigns_segment");
            assertIndexExists(statement, "idx_campaigns_approved_by");
            assertIndexExists(statement, "idx_campaigns_status_dates");
            assertIndexExists(statement, "idx_campaign_products_product");
            assertIndexExists(statement, "idx_campaign_recipients_customer");
            assertIndexExists(statement, "idx_campaign_recipients_customer_status");
            assertIndexExists(statement, "idx_contact_events_campaign");
            assertIndexExists(statement, "idx_contact_events_created_by");
            assertIndexExists(statement, "idx_contact_events_campaign_occurred");
            assertIndexExists(statement, "idx_follow_up_tasks_customer");
            assertIndexExists(statement, "idx_follow_up_tasks_campaign");
            assertIndexExists(statement, "idx_follow_up_tasks_status_due");
            assertIndexExists(statement, "idx_reminder_schedules_customer");
            assertIndexExists(statement, "idx_reminder_schedules_product");
            assertIndexExists(statement, "idx_reminder_schedules_customer_date");
            assertIndexExists(statement, "idx_reminder_schedules_product_date");
            assertIndexExists(statement, "idx_reminder_schedules_status_date");
            assertIndexExists(statement, "idx_report_exports_requested_by");
            assertIndexExists(statement, "idx_report_exports_status_requested");
        }
    }

    @Test
    void createsKbUsersTableColumnsAndConstraints() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.of(
                        "id", "uuid",
                        "email", "varchar",
                        "password_hash", "varchar",
                        "full_name", "varchar",
                        "status", "user_status",
                        "last_login_at", "timestamptz",
                        "created_at", "timestamptz",
                        "updated_at", "timestamptz");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement, "users", expectedColumn.getKey(), expectedColumn.getValue());
            }

            assertConstraintExists(statement, "users", "users_pkey");
            assertConstraintExists(statement, "users", "users_email_key");
            assertConstraintExists(statement, "user_roles", "user_roles_user_id_fkey");
        }
    }

    @Test
    void createsKbCustomersTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("customer_type", "customer_type"),
                        Map.entry("first_name", "varchar"),
                        Map.entry("last_name", "varchar"),
                        Map.entry("email", "varchar"),
                        Map.entry("phone", "varchar"),
                        Map.entry("address_line", "varchar"),
                        Map.entry("city", "varchar"),
                        Map.entry("country", "varchar"),
                        Map.entry("date_of_birth", "date"),
                        Map.entry("age_group", "customer_age_group"),
                        Map.entry("status", "customer_status"),
                        Map.entry("do_not_contact", "bool"),
                        Map.entry("source", "varchar"),
                        Map.entry("created_at", "timestamptz"),
                        Map.entry("updated_at", "timestamptz"),
                        Map.entry("deleted_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement, "customers", expectedColumn.getKey(), expectedColumn.getValue());
            }

            assertConstraintExists(statement, "customers", "customers_pkey");
            assertIndexExists(statement, "customers_name_idx");
            assertIndexExists(statement, "customers_email_idx");
            assertIndexExists(statement, "idx_customers_email");
            assertIndexExists(statement, "idx_customers_status");
            assertIndexExists(statement, "idx_customers_city");
            assertIndexExists(statement, "idx_customers_do_not_contact");
        }
    }

    @Test
    void createsKbBeneficiariesTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.of(
                        "id", "uuid",
                        "policyholder_customer_id", "uuid",
                        "beneficiary_customer_id", "uuid",
                        "relationship", "varchar",
                        "guardian_name", "varchar",
                        "guardian_email", "varchar",
                        "guardian_consent_required", "bool",
                        "created_at", "timestamptz");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "beneficiaries",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "beneficiaries", "beneficiaries_pkey");
            assertConstraintExists(
                    statement, "beneficiaries", "beneficiaries_policyholder_customer_id_fkey");
            assertConstraintExists(
                    statement, "beneficiaries", "beneficiaries_beneficiary_customer_id_fkey");
            assertConstraintExists(statement, "beneficiaries", "beneficiaries_distinct_customers");
            assertConstraintExists(statement, "beneficiaries", "beneficiaries_unique_link");
            assertIndexExists(statement, "idx_beneficiaries_policyholder_customer");
            assertIndexExists(statement, "idx_beneficiaries_beneficiary_customer");
            assertIndexExists(statement, "idx_beneficiaries_guardian_consent_required");
        }
    }

    @Test
    void createsKbConsentRecordsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("customer_id", "uuid"),
                        Map.entry("consent_type", "consent_type"),
                        Map.entry("status", "consent_status"),
                        Map.entry("purpose", "text"),
                        Map.entry("source", "varchar"),
                        Map.entry("granted_at", "timestamptz"),
                        Map.entry("withdrawn_at", "timestamptz"),
                        Map.entry("expires_at", "timestamptz"),
                        Map.entry("evidence_file_url", "text"),
                        Map.entry("created_by", "uuid"),
                        Map.entry("created_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "consent_records",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "consent_records", "consent_records_pkey");
            assertConstraintExists(
                    statement, "consent_records", "consent_records_customer_id_fkey");
            assertConstraintExists(statement, "consent_records", "consent_records_created_by_fkey");
            assertConstraintExists(
                    statement, "consent_records", "consent_records_expiration_after_grant");
            assertConstraintExists(
                    statement, "consent_records", "consent_records_withdrawal_after_grant");
            assertIndexExists(statement, "consent_records_customer_status_idx");
            assertIndexExists(statement, "idx_consent_records_customer");
            assertIndexExists(statement, "idx_consent_records_customer_type_status");
            assertIndexExists(statement, "idx_consent_records_status");
            assertIndexExists(statement, "idx_consent_records_expires_at");
            assertIndexExists(statement, "idx_consent_records_created_by");
        }
    }

    @Test
    void createsKbProductsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("name", "varchar"),
                        Map.entry("product_type", "product_type"),
                        Map.entry("description", "text"),
                        Map.entry("price", "numeric"),
                        Map.entry("duration_months", "int4"),
                        Map.entry("expiration_policy", "varchar"),
                        Map.entry("active", "bool"),
                        Map.entry("created_at", "timestamptz"),
                        Map.entry("updated_at", "timestamptz"),
                        Map.entry("deleted_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement, "products", expectedColumn.getKey(), expectedColumn.getValue());
            }

            assertConstraintExists(statement, "products", "products_pkey");
            assertConstraintExists(statement, "products", "products_price_non_negative");
            assertConstraintExists(statement, "products", "products_duration_positive");
            assertIndexExists(statement, "products_type_active_idx");
            assertIndexExists(statement, "idx_products_type");
            assertIndexExists(statement, "idx_products_active");
            assertIndexExists(statement, "idx_products_name");
            assertIndexExists(statement, "idx_products_deleted_at");
        }
    }

    @Test
    void createsKbProductOwnershipsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("customer_id", "uuid"),
                        Map.entry("product_id", "uuid"),
                        Map.entry("policy_number", "varchar"),
                        Map.entry("start_date", "date"),
                        Map.entry("expiration_date", "date"),
                        Map.entry("status", "ownership_status"),
                        Map.entry("created_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "product_ownerships",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "product_ownerships", "product_ownerships_pkey");
            assertConstraintExists(
                    statement, "product_ownerships", "product_ownerships_customer_id_fkey");
            assertConstraintExists(
                    statement, "product_ownerships", "product_ownerships_product_id_fkey");
            assertConstraintExists(
                    statement, "product_ownerships", "product_ownerships_policy_number_unique");
            assertConstraintExists(
                    statement, "product_ownerships", "product_ownerships_expiration_after_start");
            assertIndexExists(statement, "product_ownerships_customer_idx");
            assertIndexExists(statement, "idx_product_ownership_expiration");
            assertIndexExists(statement, "idx_product_ownerships_product");
            assertIndexExists(statement, "idx_product_ownerships_status");
            assertIndexExists(statement, "idx_product_ownerships_customer_status");
        }
    }

    @Test
    void createsKbProductChangeRequestsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("product_id", "uuid"),
                        Map.entry("requested_by", "uuid"),
                        Map.entry("request_type", "product_change_type"),
                        Map.entry("description", "text"),
                        Map.entry("status", "product_change_status"),
                        Map.entry("created_at", "timestamptz"),
                        Map.entry("updated_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "product_change_requests",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(
                    statement, "product_change_requests", "product_change_requests_pkey");
            assertConstraintExists(
                    statement,
                    "product_change_requests",
                    "product_change_requests_product_id_fkey");
            assertConstraintExists(
                    statement,
                    "product_change_requests",
                    "product_change_requests_requested_by_fkey");
            assertConstraintExists(
                    statement,
                    "product_change_requests",
                    "product_change_requests_description_not_blank");
            assertIndexExists(statement, "idx_product_change_requests_status");
            assertIndexExists(statement, "idx_product_change_requests_product");
            assertIndexExists(statement, "idx_product_change_requests_requested_by");
            assertIndexExists(statement, "idx_product_change_requests_product_status");
        }
    }

    @Test
    void createsKbPaymentRecordsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("customer_id", "uuid"),
                        Map.entry("product_ownership_id", "uuid"),
                        Map.entry("due_date", "date"),
                        Map.entry("paid_at", "timestamptz"),
                        Map.entry("amount_due", "numeric"),
                        Map.entry("amount_paid", "numeric"),
                        Map.entry("status", "payment_status"),
                        Map.entry("reminder_count", "int4"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "payment_records",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "payment_records", "payment_records_pkey");
            assertConstraintExists(
                    statement, "payment_records", "payment_records_customer_id_fkey");
            assertConstraintExists(
                    statement, "payment_records", "payment_records_product_ownership_id_fkey");
            assertConstraintExists(
                    statement, "payment_records", "payment_records_amount_due_non_negative");
            assertConstraintExists(
                    statement, "payment_records", "payment_records_amount_paid_non_negative");
            assertConstraintExists(
                    statement, "payment_records", "payment_records_reminder_count_non_negative");
            assertIndexExists(statement, "payment_records_due_status_idx");
            assertIndexExists(statement, "idx_payment_records_customer");
            assertIndexExists(statement, "idx_payment_records_ownership");
            assertIndexExists(statement, "idx_payment_records_status");
            assertIndexExists(statement, "idx_payment_records_customer_status");
        }
    }

    @Test
    void createsKbSegmentsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.of(
                        "id", "uuid",
                        "name", "varchar",
                        "description", "text",
                        "owner_user_id", "uuid",
                        "visibility", "segment_visibility",
                        "created_at", "timestamptz",
                        "updated_at", "timestamptz");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement, "segments", expectedColumn.getKey(), expectedColumn.getValue());
            }

            assertConstraintExists(statement, "segments", "segments_pkey");
            assertConstraintExists(statement, "segments", "segments_owner_user_id_fkey");
            assertConstraintExists(statement, "segments", "segments_name_not_blank");
            assertConstraintExists(statement, "segments", "segments_updated_at_after_created_at");
            assertIndexExists(statement, "idx_segments_owner_user");
            assertIndexExists(statement, "idx_segments_visibility");
            assertIndexExists(statement, "idx_segments_owner_visibility");
            assertIndexExists(statement, "idx_segments_name");
        }
    }

    @Test
    void createsKbSegmentCriteriaTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.of(
                        "id", "uuid",
                        "segment_id", "uuid",
                        "field_name", "varchar",
                        "operator", "segment_operator",
                        "value", "text",
                        "logical_group", "varchar",
                        "join_operator", "segment_join_operator");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "segment_criteria",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "segment_criteria", "segment_criteria_pkey");
            assertConstraintExists(
                    statement, "segment_criteria", "segment_criteria_segment_id_fkey");
            assertConstraintExists(
                    statement, "segment_criteria", "segment_criteria_field_name_not_blank");
            assertConstraintExists(
                    statement, "segment_criteria", "segment_criteria_value_not_blank");
            assertConstraintExists(
                    statement, "segment_criteria", "segment_criteria_logical_group_not_blank");
            assertIndexExists(statement, "idx_segment_criteria_segment");
            assertIndexExists(statement, "idx_segment_criteria_field_name");
            assertIndexExists(statement, "idx_segment_criteria_operator");
            assertIndexExists(statement, "idx_segment_criteria_segment_field");
        }
    }

    @Test
    void createsKbCampaignRecipientsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("campaign_id", "uuid"),
                        Map.entry("customer_id", "uuid"),
                        Map.entry("eligibility_status", "campaign_recipient_status"),
                        Map.entry("exclusion_reason", "text"),
                        Map.entry("eligibility_explanation", "text"),
                        Map.entry("sent_at", "timestamptz"),
                        Map.entry("opened_at", "timestamptz"),
                        Map.entry("clicked_at", "timestamptz"),
                        Map.entry("converted_at", "timestamptz"),
                        Map.entry("created_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "campaign_recipients",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "campaign_recipients", "campaign_recipients_pkey");
            assertConstraintExists(
                    statement, "campaign_recipients", "campaign_recipients_campaign_id_fkey");
            assertConstraintExists(
                    statement, "campaign_recipients", "campaign_recipients_customer_id_fkey");
            assertConstraintExists(
                    statement,
                    "campaign_recipients",
                    "campaign_recipients_campaign_customer_unique");
            assertIndexExists(statement, "campaign_recipients_status_idx");
        }
    }

    @Test
    void createsKbCampaignProductsTableColumnsAndConstraints() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.of(
                        "campaign_id", "uuid",
                        "product_id", "uuid");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "campaign_products",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "campaign_products", "campaign_products_pkey");
            assertConstraintExists(
                    statement, "campaign_products", "campaign_products_campaign_id_fkey");
            assertConstraintExists(
                    statement, "campaign_products", "campaign_products_product_id_fkey");
        }
    }

    @Test
    void createsKbCampaignsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("name", "varchar"),
                        Map.entry("objective", "text"),
                        Map.entry("status", "campaign_status"),
                        Map.entry("owner_user_id", "uuid"),
                        Map.entry("segment_id", "uuid"),
                        Map.entry("channel", "campaign_channel"),
                        Map.entry("message_subject", "varchar"),
                        Map.entry("message_body", "text"),
                        Map.entry("start_date", "date"),
                        Map.entry("end_date", "date"),
                        Map.entry("approved_by", "uuid"),
                        Map.entry("approved_at", "timestamptz"),
                        Map.entry("rejection_reason", "text"),
                        Map.entry("created_at", "timestamptz"),
                        Map.entry("updated_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement, "campaigns", expectedColumn.getKey(), expectedColumn.getValue());
            }

            assertConstraintExists(statement, "campaigns", "campaigns_pkey");
            assertConstraintExists(statement, "campaigns", "campaigns_owner_user_id_fkey");
            assertConstraintExists(statement, "campaigns", "campaigns_segment_id_fkey");
            assertConstraintExists(statement, "campaigns", "campaigns_approved_by_fkey");
            assertIndexExists(statement, "campaigns_status_idx");
        }
    }

    @Test
    void createsKbFollowUpTasksTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("customer_id", "uuid"),
                        Map.entry("campaign_id", "uuid"),
                        Map.entry("assigned_to", "uuid"),
                        Map.entry("title", "varchar"),
                        Map.entry("description", "text"),
                        Map.entry("due_date", "date"),
                        Map.entry("status", "follow_up_status"),
                        Map.entry("priority", "work_priority"),
                        Map.entry("created_at", "timestamptz"),
                        Map.entry("completed_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "follow_up_tasks",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "follow_up_tasks", "follow_up_tasks_pkey");
            assertConstraintExists(
                    statement, "follow_up_tasks", "follow_up_tasks_customer_id_fkey");
            assertConstraintExists(
                    statement, "follow_up_tasks", "follow_up_tasks_campaign_id_fkey");
            assertConstraintExists(
                    statement, "follow_up_tasks", "follow_up_tasks_assigned_to_fkey");
            assertIndexExists(statement, "follow_up_tasks_assignee_status_idx");
        }
    }

    @Test
    void createsKbReminderSchedulesTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("customer_id", "uuid"),
                        Map.entry("product_id", "uuid"),
                        Map.entry("reminder_type", "reminder_type"),
                        Map.entry("reminder_level", "reminder_level"),
                        Map.entry("scheduled_date", "date"),
                        Map.entry("status", "reminder_status"),
                        Map.entry("created_at", "timestamptz"),
                        Map.entry("sent_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "reminder_schedules",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "reminder_schedules", "reminder_schedules_pkey");
            assertConstraintExists(
                    statement, "reminder_schedules", "reminder_schedules_customer_id_fkey");
            assertConstraintExists(
                    statement, "reminder_schedules", "reminder_schedules_product_id_fkey");
            assertIndexExists(statement, "reminder_schedules_date_status_idx");
        }
    }

    @Test
    void createsKbCampaignMetricsTableColumnsAndConstraints() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("campaign_id", "uuid"),
                        Map.entry("audience_size", "int4"),
                        Map.entry("eligible_count", "int4"),
                        Map.entry("excluded_count", "int4"),
                        Map.entry("sent_count", "int4"),
                        Map.entry("opened_count", "int4"),
                        Map.entry("clicked_count", "int4"),
                        Map.entry("replied_count", "int4"),
                        Map.entry("converted_count", "int4"),
                        Map.entry("estimated_cost", "numeric"),
                        Map.entry("estimated_revenue", "numeric"),
                        Map.entry("estimated_roi", "numeric"),
                        Map.entry("updated_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "campaign_metrics",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "campaign_metrics", "campaign_metrics_pkey");
            assertConstraintExists(
                    statement, "campaign_metrics", "campaign_metrics_campaign_id_fkey");
            assertConstraintExists(
                    statement, "campaign_metrics", "campaign_metrics_campaign_id_key");
        }
    }

    @Test
    void createsKbAuditLogsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("actor_user_id", "uuid"),
                        Map.entry("action", "varchar"),
                        Map.entry("entity_type", "varchar"),
                        Map.entry("entity_id", "uuid"),
                        Map.entry("old_value", "jsonb"),
                        Map.entry("new_value", "jsonb"),
                        Map.entry("ip_address", "varchar"),
                        Map.entry("created_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "audit_logs",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "audit_logs", "audit_logs_pkey");
            assertConstraintExists(statement, "audit_logs", "audit_logs_actor_user_id_fkey");
            assertIndexExists(statement, "audit_logs_entity_idx");
            assertIndexExists(statement, "audit_logs_actor_created_idx");
        }
    }

    @Test
    void createsKbReportExportsTableColumnsAndConstraints() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("requested_by", "uuid"),
                        Map.entry("report_name", "varchar"),
                        Map.entry("export_type", "report_export_type"),
                        Map.entry("status", "report_export_status"),
                        Map.entry("file_url", "text"),
                        Map.entry("requested_at", "timestamptz"),
                        Map.entry("completed_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "report_exports",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "report_exports", "report_exports_pkey");
            assertConstraintExists(statement, "report_exports", "report_exports_requested_by_fkey");
        }
    }

    @Test
    void createsKbAiRecommendationsTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.ofEntries(
                        Map.entry("id", "uuid"),
                        Map.entry("recommendation_type", "ai_recommendation_type"),
                        Map.entry("target_entity_type", "varchar"),
                        Map.entry("target_entity_id", "uuid"),
                        Map.entry("input_summary", "text"),
                        Map.entry("recommendation", "text"),
                        Map.entry("explanation", "text"),
                        Map.entry("confidence_score", "numeric"),
                        Map.entry("approved_by_user_id", "uuid"),
                        Map.entry("created_at", "timestamptz"));

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "ai_recommendations",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "ai_recommendations", "ai_recommendations_pkey");
            assertConstraintExists(
                    statement, "ai_recommendations", "ai_recommendations_approved_by_user_id_fkey");
            assertConstraintExists(
                    statement,
                    "ai_recommendations",
                    "ai_recommendations_target_entity_type_not_blank");
            assertConstraintExists(
                    statement, "ai_recommendations", "ai_recommendations_input_summary_not_blank");
            assertConstraintExists(
                    statement, "ai_recommendations", "ai_recommendations_recommendation_not_blank");
            assertConstraintExists(
                    statement, "ai_recommendations", "ai_recommendations_explanation_not_blank");
            assertConstraintExists(
                    statement, "ai_recommendations", "ai_recommendations_confidence_score_range");
            assertIndexExists(statement, "idx_ai_recommendations_type");
            assertIndexExists(statement, "idx_ai_recommendations_target");
            assertIndexExists(statement, "idx_ai_recommendations_approved_by");
            assertIndexExists(statement, "idx_ai_recommendations_created_at");
        }
    }

    @Test
    void createsKbUserRolesTableColumnsConstraintsAndIndexes() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.of(
                        "user_id", "uuid",
                        "role_id", "uuid",
                        "assigned_at", "timestamptz",
                        "assigned_by", "uuid");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement,
                        "user_roles",
                        expectedColumn.getKey(),
                        expectedColumn.getValue());
            }

            assertConstraintExists(statement, "user_roles", "user_roles_pkey");
            assertConstraintExists(statement, "user_roles", "user_roles_user_id_fkey");
            assertConstraintExists(statement, "user_roles", "user_roles_role_id_fkey");
            assertConstraintExists(statement, "user_roles", "user_roles_assigned_by_fkey");
            assertIndexExists(statement, "user_roles_assigned_by_idx");
        }
    }

    @Test
    void createsKbRolesTableColumnsAndSeedData() throws Exception {
        migrate();

        Map<String, String> expectedColumns =
                Map.of(
                        "id", "uuid",
                        "name", "system_role_name",
                        "display_name", "varchar",
                        "description", "text",
                        "allowed_functions", "text",
                        "mvp_role", "bool",
                        "created_at", "timestamptz",
                        "updated_at", "timestamptz");

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> expectedColumn : expectedColumns.entrySet()) {
                assertColumnExists(
                        statement, "roles", expectedColumn.getKey(), expectedColumn.getValue());
            }

            assertConstraintExists(statement, "roles", "roles_pkey");
            assertConstraintExists(statement, "roles", "roles_name_key");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as role_count,"
                                    + " count(*) filter (where mvp_role) as mvp_role_count,"
                                    + " count(*) filter (where not mvp_role) as extended_role_count"
                                    + " from "
                                    + TEST_SCHEMA
                                    + ".roles")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("role_count")).isEqualTo(10);
                assertThat(resultSet.getInt("mvp_role_count")).isEqualTo(6);
                assertThat(resultSet.getInt("extended_role_count")).isEqualTo(4);
            }

            assertRoleSeedId(statement, "ADMIN", "00000000-0000-0000-0000-000000000001");
            assertRoleSeedId(statement, "CAMPAIGN_MANAGER", "00000000-0000-0000-0000-000000000002");
            assertRoleSeedId(statement, "BI_ANALYST", "00000000-0000-0000-0000-000000000003");
            assertRoleSeedId(statement, "PRODUCT_MANAGER", "00000000-0000-0000-0000-000000000004");
            assertRoleSeedId(
                    statement, "COMPLIANCE_OFFICER", "00000000-0000-0000-0000-000000000005");
            assertRoleSeedId(
                    statement, "CUSTOMER_SERVICE_AGENT", "00000000-0000-0000-0000-000000000006");
            assertRoleSeedId(statement, "SALES_AGENT", "00000000-0000-0000-0000-000000000007");
            assertRoleSeedId(
                    statement, "MARKETING_ANALYST", "00000000-0000-0000-0000-000000000008");
            assertRoleSeedId(statement, "EXECUTIVE_VIEWER", "00000000-0000-0000-0000-000000000009");
            assertRoleSeedId(statement, "SYSTEM_AUDITOR", "00000000-0000-0000-0000-000000000010");

            assertRoleSeed(
                    statement,
                    "ADMIN",
                    "Admin",
                    "Manages users, roles, settings, and full system configuration",
                    "Manage users, assign roles, manage settings, view all modules,"
                            + " configure limits, view audit logs",
                    true);
            assertRoleSeed(
                    statement,
                    "EXECUTIVE_VIEWER",
                    "Executive Viewer",
                    "Views high-level dashboards and management reports only",
                    "View read-only dashboards, ROI, campaign summaries,"
                            + " and product performance reports",
                    false);
            assertRoleSeed(
                    statement,
                    "SYSTEM_AUDITOR",
                    "System Auditor",
                    "Reviews audit logs, consent history, approval history, and sensitive actions",
                    "View audit logs, consent history, campaign approval history,"
                            + " user activity history, and export audit reports",
                    false);
        }
    }

    @Test
    void seedRolesMigrationCreatesAllKbRolesIdempotently() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as role_count,"
                                    + " count(distinct name) as distinct_role_count,"
                                    + " count(*) filter (where display_name is not null) as named_roles,"
                                    + " count(*) filter (where allowed_functions is not null) as scoped_roles,"
                                    + " count(*) filter (where mvp_role) as mvp_role_count"
                                    + " from "
                                    + TEST_SCHEMA
                                    + ".roles")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("role_count")).isEqualTo(10);
                assertThat(resultSet.getInt("distinct_role_count")).isEqualTo(10);
                assertThat(resultSet.getInt("named_roles")).isEqualTo(10);
                assertThat(resultSet.getInt("scoped_roles")).isEqualTo(10);
                assertThat(resultSet.getInt("mvp_role_count")).isEqualTo(6);
            }

            assertRoleSeed(
                    statement,
                    "ADMIN",
                    "Admin",
                    "Manages users, roles, settings, and full system configuration",
                    "Manage users, assign roles, manage settings, view all modules,"
                            + " configure limits, view audit logs",
                    true);
            assertRoleSeed(
                    statement,
                    "CAMPAIGN_MANAGER",
                    "Campaign Manager",
                    "Creates campaigns, segments, recipients, messages, schedules,"
                            + " and launches approved campaigns",
                    "Create/edit campaigns, define segments, preview recipients,"
                            + " submit campaigns, launch approved campaigns, manage follow-ups,"
                            + " view campaign analytics",
                    true);
            assertRoleSeed(
                    statement,
                    "BI_ANALYST",
                    "BI Analyst",
                    "Views dashboards, reports, customer analytics, segmentation insights,"
                            + " and performance data",
                    "View analytics, reports, segmentation insights, audience counts,"
                            + " campaign performance, product performance; may create analytical"
                            + " segment drafts if allowed",
                    true);
            assertRoleSeed(
                    statement,
                    "PRODUCT_MANAGER",
                    "Product Manager",
                    "Manages insurance/investment products and product-change requests",
                    "Create/edit/disable products, manage product details,"
                            + " create product-change requests, view product performance",
                    true);
            assertRoleSeed(
                    statement,
                    "COMPLIANCE_OFFICER",
                    "Compliance Officer",
                    "Reviews consent, opt-outs, eligibility, campaign approval, and audit logs",
                    "Review consent, opt-outs, guardian consent, eligibility,"
                            + " approve/reject campaigns, view audit logs and compliance reports",
                    true);
            assertRoleSeed(
                    statement,
                    "CUSTOMER_SERVICE_AGENT",
                    "Customer Service Agent",
                    "Manages customer/prospect details, notes, contact outcomes,"
                            + " and consent updates",
                    "Create/update customers, update contact details, record consent,"
                            + " mark opt-outs, add notes, update contact outcomes,"
                            + " manage follow-up tasks",
                    true);
            assertRoleSeed(
                    statement,
                    "SALES_AGENT",
                    "Sales Agent",
                    "Follows up with assigned interested prospects and updates conversion status",
                    "View assigned leads, update contact outcomes,"
                            + " mark interested/not interested/converted, complete follow-up tasks",
                    false);
            assertRoleSeed(
                    statement,
                    "MARKETING_ANALYST",
                    "Marketing Analyst",
                    "Reviews campaign metrics, audience behavior, and campaign performance",
                    "View campaign metrics, audience segment performance, reports,"
                            + " segmentation insights, and recommend targeting improvements",
                    false);
            assertRoleSeed(
                    statement,
                    "EXECUTIVE_VIEWER",
                    "Executive Viewer",
                    "Views high-level dashboards and management reports only",
                    "View read-only dashboards, ROI, campaign summaries,"
                            + " and product performance reports",
                    false);
            assertRoleSeed(
                    statement,
                    "SYSTEM_AUDITOR",
                    "System Auditor",
                    "Reviews audit logs, consent history, approval history, and sensitive actions",
                    "View audit logs, consent history, campaign approval history,"
                            + " user activity history, and export audit reports",
                    false);
        }
    }

    @Test
    void seedUsersMigrationCreatesMvpRoleUsersAndAssignments() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as seeded_user_count,"
                                    + " count(*) filter (where status = 'ACTIVE') as active_count,"
                                    + " count(*) filter (where password_hash like '$2a$10$%')"
                                    + " as bcrypt_hash_count"
                                    + " from "
                                    + TEST_SCHEMA
                                    + ".users where email like '%@bayer-westphalian.test'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("seeded_user_count")).isEqualTo(10);
                assertThat(resultSet.getInt("active_count")).isEqualTo(10);
                assertThat(resultSet.getInt("bcrypt_hash_count")).isEqualTo(10);
            }

            assertSeedUserRole(
                    statement,
                    "admin@bayer-westphalian.test",
                    "Test Admin",
                    "ADMIN",
                    "admin@bayer-westphalian.test");
            assertSeedUserRole(
                    statement,
                    "campaign.manager@bayer-westphalian.test",
                    "Test Campaign Manager",
                    "CAMPAIGN_MANAGER",
                    "admin@bayer-westphalian.test");
            assertSeedUserRole(
                    statement,
                    "bi.analyst@bayer-westphalian.test",
                    "Test BI Analyst",
                    "BI_ANALYST",
                    "admin@bayer-westphalian.test");
            assertSeedUserRole(
                    statement,
                    "product.manager@bayer-westphalian.test",
                    "Test Product Manager",
                    "PRODUCT_MANAGER",
                    "admin@bayer-westphalian.test");
            assertSeedUserRole(
                    statement,
                    "compliance.officer@bayer-westphalian.test",
                    "Test Compliance Officer",
                    "COMPLIANCE_OFFICER",
                    "admin@bayer-westphalian.test");
            assertSeedUserRole(
                    statement,
                    "customer.service@bayer-westphalian.test",
                    "Test Customer Service Agent",
                    "CUSTOMER_SERVICE_AGENT",
                    "admin@bayer-westphalian.test");
        }
    }

    @Test
    void controlledDemoDataLoadsOnlyWhenDemoFlywayLocationIsIncluded() throws Exception {
        migrateWithDemoData();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as demo_customer_count from "
                                    + TEST_SCHEMA
                                    + ".customers where source = 'DEMO_DATA'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("demo_customer_count")).isEqualTo(2);
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select c.status, c.channel, r.eligibility_status,"
                                    + " m.audience_size, m.eligible_count"
                                    + " from "
                                    + TEST_SCHEMA
                                    + ".campaigns c"
                                    + " join "
                                    + TEST_SCHEMA
                                    + ".campaign_recipients r on r.campaign_id = c.id"
                                    + " join "
                                    + TEST_SCHEMA
                                    + ".campaign_metrics m on m.campaign_id = c.id"
                                    + " where c.id = '50000000-0000-0000-0000-000000000101'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("APPROVED");
                assertThat(resultSet.getString("channel")).isEqualTo("EMAIL");
                assertThat(resultSet.getString("eligibility_status")).isEqualTo("ELIGIBLE");
                assertThat(resultSet.getInt("audience_size")).isEqualTo(20);
                assertThat(resultSet.getInt("eligible_count")).isEqualTo(9);
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select exists(select 1 from "
                                    + TEST_SCHEMA
                                    + ".follow_up_tasks where id ="
                                    + " '53000000-0000-0000-0000-000000000101') as has_follow_up,"
                                    + " exists(select 1 from "
                                    + TEST_SCHEMA
                                    + ".reminder_schedules where customer_id ="
                                    + " '20000000-0000-0000-0000-000000000101') as has_reminder,"
                                    + " exists(select 1 from "
                                    + TEST_SCHEMA
                                    + ".ai_recommendations where target_entity_id ="
                                    + " '20000000-0000-0000-0000-000000000102') as has_ai_recommendation")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("has_follow_up")).isTrue();
                assertThat(resultSet.getBoolean("has_reminder")).isTrue();
                assertThat(resultSet.getBoolean("has_ai_recommendation")).isTrue();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select new_value ->> 'scope' as scope from "
                                    + TEST_SCHEMA
                                    + ".audit_logs where action = 'LOAD_DEMO_DATA'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("scope")).isEqualTo("dev-test-only");
            }
        }
    }

    @Test
    void rebuildsLocalDatabaseSchemaRepeatablyFromScratch() throws Exception {
        migrateWithDemoData();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertLocalRebuildState(statement);
        }

        dropTestSchema();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "select to_regnamespace('" + TEST_SCHEMA + "') is null as empty")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getBoolean("empty")).isTrue();
        }

        migrateWithDemoData();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertLocalRebuildState(statement);
        }
    }

    @Test
    void supportsCustomerInsertDefaultsSoftDeleteAndDoNotContactFiltering() throws Exception {
        migrate();

        String activeCustomerId = "20000000-0000-0000-0000-000000000001";
        String optedOutCustomerId = "20000000-0000-0000-0000-000000000002";
        String deletedCustomerId = "20000000-0000-0000-0000-000000000003";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name, email, city,"
                            + " country, source) values ('"
                            + activeCustomerId
                            + "', 'CUSTOMER', 'Amina', 'Keller', 'amina@example.test',"
                            + " 'Munich', 'Germany', 'CSV_IMPORT'), ('"
                            + optedOutCustomerId
                            + "', 'PROSPECT', 'Ben', 'Schmidt', 'ben@example.test',"
                            + " 'Cologne', 'Germany', 'MANUAL'), ('"
                            + deletedCustomerId
                            + "', 'BENEFICIARY', 'Clara', 'Weber', 'clara@example.test',"
                            + " 'Berlin', 'Germany', 'POLICY_IMPORT')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".customers set do_not_contact = true where id = '"
                            + optedOutCustomerId
                            + "'");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".customers set deleted_at = now() where id = '"
                            + deletedCustomerId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select status, do_not_contact,"
                                    + " created_at is not null as has_created_at,"
                                    + " updated_at is not null as has_updated_at,"
                                    + " deleted_at is null as not_deleted from "
                                    + TEST_SCHEMA
                                    + ".customers where id = '"
                                    + activeCustomerId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("ACTIVE");
                assertThat(resultSet.getBoolean("do_not_contact")).isFalse();
                assertThat(resultSet.getBoolean("has_created_at")).isTrue();
                assertThat(resultSet.getBoolean("has_updated_at")).isTrue();
                assertThat(resultSet.getBoolean("not_deleted")).isTrue();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as eligible_active_customers from "
                                    + TEST_SCHEMA
                                    + ".customers where deleted_at is null"
                                    + " and do_not_contact = false and status = 'ACTIVE'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("eligible_active_customers")).isEqualTo(1);
            }
        }
    }

    @Test
    void supportsBeneficiaryLinksGuardianConsentDefaultsAndCascadeDeletion() throws Exception {
        migrate();

        String policyholderCustomerId = "30000000-0000-0000-0000-000000000001";
        String beneficiaryCustomerId = "30000000-0000-0000-0000-000000000002";
        String secondBeneficiaryCustomerId = "30000000-0000-0000-0000-000000000003";
        String beneficiaryLinkId = "30000000-0000-0000-0000-000000000101";
        String minorBeneficiaryLinkId = "30000000-0000-0000-0000-000000000102";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name, email)"
                            + " values ('"
                            + policyholderCustomerId
                            + "', 'CUSTOMER', 'Policy', 'Holder', 'policyholder@example.test'),"
                            + " ('"
                            + beneficiaryCustomerId
                            + "', 'BENEFICIARY', 'Adult', 'Beneficiary', 'adult@example.test'),"
                            + " ('"
                            + secondBeneficiaryCustomerId
                            + "', 'BENEFICIARY', 'Minor', 'Beneficiary', 'minor@example.test')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".beneficiaries (id, policyholder_customer_id,"
                            + " beneficiary_customer_id, relationship) values ('"
                            + beneficiaryLinkId
                            + "', '"
                            + policyholderCustomerId
                            + "', '"
                            + beneficiaryCustomerId
                            + "', 'Grandchild')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".beneficiaries (id, policyholder_customer_id,"
                            + " beneficiary_customer_id, relationship, guardian_name,"
                            + " guardian_email, guardian_consent_required) values ('"
                            + minorBeneficiaryLinkId
                            + "', '"
                            + policyholderCustomerId
                            + "', '"
                            + secondBeneficiaryCustomerId
                            + "', 'Grandchild', 'Guardian User',"
                            + " 'guardian@example.test', true)");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select guardian_consent_required,"
                                    + " created_at is not null as has_created_at from "
                                    + TEST_SCHEMA
                                    + ".beneficiaries where id = '"
                                    + beneficiaryLinkId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("guardian_consent_required")).isFalse();
                assertThat(resultSet.getBoolean("has_created_at")).isTrue();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as guardian_required_count from "
                                    + TEST_SCHEMA
                                    + ".beneficiaries where guardian_consent_required = true")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("guardian_required_count")).isEqualTo(1);
            }

            statement.executeUpdate(
                    "delete from "
                            + TEST_SCHEMA
                            + ".customers where id = '"
                            + policyholderCustomerId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_links from "
                                    + TEST_SCHEMA
                                    + ".beneficiaries where policyholder_customer_id = '"
                                    + policyholderCustomerId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_links")).isZero();
            }
        }
    }

    @Test
    void beneficiariesRejectSelfLinksAndDuplicateLinks() throws Exception {
        migrate();

        String policyholderCustomerId = "30000000-0000-0000-0000-000000000011";
        String beneficiaryCustomerId = "30000000-0000-0000-0000-000000000012";
        String beneficiaryLinkId = "30000000-0000-0000-0000-000000000111";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + policyholderCustomerId
                            + "', 'CUSTOMER', 'Primary', 'Customer'), ('"
                            + beneficiaryCustomerId
                            + "', 'BENEFICIARY', 'Linked', 'Beneficiary')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".beneficiaries (id, policyholder_customer_id,"
                            + " beneficiary_customer_id, relationship) values ('"
                            + beneficiaryLinkId
                            + "', '"
                            + policyholderCustomerId
                            + "', '"
                            + beneficiaryCustomerId
                            + "', 'Grandchild')");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".beneficiaries (id, policyholder_customer_id,"
                                                    + " beneficiary_customer_id, relationship)"
                                                    + " values ('30000000-0000-0000-0000-000000000112', '"
                                                    + policyholderCustomerId
                                                    + "', '"
                                                    + policyholderCustomerId
                                                    + "', 'Self')"))
                    .hasMessageContaining("beneficiaries_distinct_customers");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".beneficiaries (id, policyholder_customer_id,"
                                                    + " beneficiary_customer_id, relationship)"
                                                    + " values ('30000000-0000-0000-0000-000000000113', '"
                                                    + policyholderCustomerId
                                                    + "', '"
                                                    + beneficiaryCustomerId
                                                    + "', 'Grandchild')"))
                    .hasMessageContaining("beneficiaries_unique_link");
        }
    }

    @Test
    void supportsConsentEvidenceValidityAndWithdrawalFiltering() throws Exception {
        migrate();

        String customerWithConsentId = "40000000-0000-0000-0000-000000000001";
        String withdrawnCustomerId = "40000000-0000-0000-0000-000000000002";
        String createdByUserId = "40000000-0000-0000-0000-000000000101";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + createdByUserId
                            + "', 'consent-agent@example.test', '$2a$10$examplehash',"
                            + " 'Consent Agent')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name, email)"
                            + " values ('"
                            + customerWithConsentId
                            + "', 'CUSTOMER', 'Consent', 'Granted', 'granted@example.test'),"
                            + " ('"
                            + withdrawnCustomerId
                            + "', 'CUSTOMER', 'Consent', 'Withdrawn', 'withdrawn@example.test')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".consent_records (id, customer_id, consent_type, status,"
                            + " purpose, source, granted_at, expires_at, evidence_file_url,"
                            + " created_by) values"
                            + " ('40000000-0000-0000-0000-000000000201', '"
                            + customerWithConsentId
                            + "', 'MARKETING_EMAIL', 'GIVEN', 'Marketing campaigns',"
                            + " 'CALL_CENTER', now(), now() + interval '30 days',"
                            + " 'evidence://grant-1', '"
                            + createdByUserId
                            + "'), ('40000000-0000-0000-0000-000000000202', '"
                            + withdrawnCustomerId
                            + "', 'MARKETING_EMAIL', 'WITHDRAWN', 'Marketing campaigns',"
                            + " 'CUSTOMER_REQUEST', now() - interval '10 days',"
                            + " now() + interval '20 days', 'evidence://withdraw-1', '"
                            + createdByUserId
                            + "')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".consent_records set withdrawn_at = now()"
                            + " where customer_id = '"
                            + withdrawnCustomerId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as valid_marketing_consents from "
                                    + TEST_SCHEMA
                                    + ".consent_records where consent_type = 'MARKETING_EMAIL'"
                                    + " and status = 'GIVEN'"
                                    + " and granted_at is not null"
                                    + " and (expires_at is null or expires_at > now())"
                                    + " and withdrawn_at is null")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("valid_marketing_consents")).isEqualTo(1);
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select evidence_file_url, created_at is not null as has_created_at"
                                    + " from "
                                    + TEST_SCHEMA
                                    + ".consent_records where customer_id = '"
                                    + customerWithConsentId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("evidence_file_url"))
                        .isEqualTo("evidence://grant-1");
                assertThat(resultSet.getBoolean("has_created_at")).isTrue();
            }
        }
    }

    @Test
    void consentRecordsRetainHistoryWhenCreatorIsDeletedAndCascadeWithCustomer() throws Exception {
        migrate();

        String customerId = "40000000-0000-0000-0000-000000000011";
        String createdByUserId = "40000000-0000-0000-0000-000000000111";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + createdByUserId
                            + "', 'creator@example.test', '$2a$10$examplehash',"
                            + " 'Consent Creator')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'Cascade', 'Consent')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".consent_records (id, customer_id, consent_type, status,"
                            + " purpose, source, granted_at, created_by) values"
                            + " ('40000000-0000-0000-0000-000000000211', '"
                            + customerId
                            + "', 'DATA_PROCESSING', 'GIVEN', 'Data processing',"
                            + " 'ADMIN_ENTRY', now(), '"
                            + createdByUserId
                            + "')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".users where id = '" + createdByUserId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select created_by is null as creator_removed from "
                                    + TEST_SCHEMA
                                    + ".consent_records where customer_id = '"
                                    + customerId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("creator_removed")).isTrue();
            }

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".customers where id = '" + customerId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_records from "
                                    + TEST_SCHEMA
                                    + ".consent_records where customer_id = '"
                                    + customerId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_records")).isZero();
            }
        }
    }

    @Test
    void consentRecordsRejectTemporalInconsistencies() throws Exception {
        migrate();

        String customerId = "40000000-0000-0000-0000-000000000021";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'Temporal', 'Consent')");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".consent_records (id, customer_id, consent_type,"
                                                    + " status, purpose, granted_at, expires_at)"
                                                    + " values ('40000000-0000-0000-0000-000000000221', '"
                                                    + customerId
                                                    + "', 'MARKETING_SMS', 'GIVEN', 'SMS marketing',"
                                                    + " now(), now() - interval '1 day')"))
                    .hasMessageContaining("consent_records_expiration_after_grant");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".consent_records (id, customer_id, consent_type,"
                                                    + " status, purpose, granted_at, withdrawn_at)"
                                                    + " values ('40000000-0000-0000-0000-000000000222', '"
                                                    + customerId
                                                    + "', 'MARKETING_PHONE', 'WITHDRAWN',"
                                                    + " 'Phone marketing', now(), now() - interval '1 day')"))
                    .hasMessageContaining("consent_records_withdrawal_after_grant");
        }
    }

    @Test
    void supportsPaymentDefaultsReminderCountsAndRiskFiltering() throws Exception {
        migrate();

        String customerId = "80000000-0000-0000-0000-000000000001";
        String productId = "80000000-0000-0000-0000-000000000101";
        String ownershipId = "80000000-0000-0000-0000-000000000201";
        String duePaymentId = "80000000-0000-0000-0000-000000000301";
        String riskPaymentId = "80000000-0000-0000-0000-000000000302";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertCustomerProductOwnership(statement, customerId, productId, ownershipId);
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".payment_records (id, customer_id, product_ownership_id,"
                            + " due_date, amount_due) values ('"
                            + duePaymentId
                            + "', '"
                            + customerId
                            + "', '"
                            + ownershipId
                            + "', current_date + interval '10 days', 100.00), ('"
                            + riskPaymentId
                            + "', '"
                            + customerId
                            + "', '"
                            + ownershipId
                            + "', current_date - interval '45 days', 250.00)");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".payment_records set status = 'DEFAULT_RISK', reminder_count = 3"
                            + " where id = '"
                            + riskPaymentId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select status, reminder_count from "
                                    + TEST_SCHEMA
                                    + ".payment_records where id = '"
                                    + duePaymentId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("DUE");
                assertThat(resultSet.getInt("reminder_count")).isZero();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as risky_payments from "
                                    + TEST_SCHEMA
                                    + ".payment_records where customer_id = '"
                                    + customerId
                                    + "' and status = 'DEFAULT_RISK'"
                                    + " and reminder_count >= 3")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("risky_payments")).isEqualTo(1);
            }
        }
    }

    @Test
    void supportsPaidPaymentUpdatesAndOverdueQueries() throws Exception {
        migrate();

        String customerId = "80000000-0000-0000-0000-000000000011";
        String productId = "80000000-0000-0000-0000-000000000111";
        String ownershipId = "80000000-0000-0000-0000-000000000211";
        String paymentId = "80000000-0000-0000-0000-000000000311";
        String overduePaymentId = "80000000-0000-0000-0000-000000000312";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertCustomerProductOwnership(statement, customerId, productId, ownershipId);
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".payment_records (id, customer_id, product_ownership_id,"
                            + " due_date, amount_due) values ('"
                            + paymentId
                            + "', '"
                            + customerId
                            + "', '"
                            + ownershipId
                            + "', current_date, 80.00), ('"
                            + overduePaymentId
                            + "', '"
                            + customerId
                            + "', '"
                            + ownershipId
                            + "', current_date - interval '10 days', 90.00)");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".payment_records set status = 'PAID', paid_at = now(),"
                            + " amount_paid = 80.00 where id = '"
                            + paymentId
                            + "'");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".payment_records set status = 'OVERDUE', reminder_count = 1"
                            + " where id = '"
                            + overduePaymentId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select paid_at is not null as has_paid_at, amount_paid from "
                                    + TEST_SCHEMA
                                    + ".payment_records where id = '"
                                    + paymentId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("has_paid_at")).isTrue();
                assertThat(resultSet.getBigDecimal("amount_paid")).isEqualByComparingTo("80.00");
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as overdue_payments from "
                                    + TEST_SCHEMA
                                    + ".payment_records where due_date < current_date"
                                    + " and status = 'OVERDUE'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("overdue_payments")).isEqualTo(1);
            }
        }
    }

    @Test
    void paymentRecordsCascadeWithCustomerAndProductOwnership() throws Exception {
        migrate();

        String customerId = "80000000-0000-0000-0000-000000000021";
        String productId = "80000000-0000-0000-0000-000000000121";
        String ownershipId = "80000000-0000-0000-0000-000000000221";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertCustomerProductOwnership(statement, customerId, productId, ownershipId);
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".payment_records (id, customer_id, product_ownership_id,"
                            + " due_date, amount_due) values ('80000000-0000-0000-0000-000000000321', '"
                            + customerId
                            + "', '"
                            + ownershipId
                            + "', current_date, 40.00)");

            statement.executeUpdate(
                    "delete from "
                            + TEST_SCHEMA
                            + ".product_ownerships where id = '"
                            + ownershipId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_payments from "
                                    + TEST_SCHEMA
                                    + ".payment_records where product_ownership_id = '"
                                    + ownershipId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_payments")).isZero();
            }
        }
    }

    @Test
    void paymentRecordsRejectNegativeAmountsAndReminderCounts() throws Exception {
        migrate();

        String customerId = "80000000-0000-0000-0000-000000000031";
        String productId = "80000000-0000-0000-0000-000000000131";
        String ownershipId = "80000000-0000-0000-0000-000000000231";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertCustomerProductOwnership(statement, customerId, productId, ownershipId);

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".payment_records (id, customer_id,"
                                                    + " product_ownership_id, due_date, amount_due)"
                                                    + " values ('80000000-0000-0000-0000-000000000331', '"
                                                    + customerId
                                                    + "', '"
                                                    + ownershipId
                                                    + "', current_date, -1.00)"))
                    .hasMessageContaining("payment_records_amount_due_non_negative");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".payment_records (id, customer_id,"
                                                    + " product_ownership_id, due_date, amount_due,"
                                                    + " amount_paid) values"
                                                    + " ('80000000-0000-0000-0000-000000000332', '"
                                                    + customerId
                                                    + "', '"
                                                    + ownershipId
                                                    + "', current_date, 10.00, -1.00)"))
                    .hasMessageContaining("payment_records_amount_paid_non_negative");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".payment_records (id, customer_id,"
                                                    + " product_ownership_id, due_date, amount_due,"
                                                    + " reminder_count) values"
                                                    + " ('80000000-0000-0000-0000-000000000333', '"
                                                    + customerId
                                                    + "', '"
                                                    + ownershipId
                                                    + "', current_date, 10.00, -1)"))
                    .hasMessageContaining("payment_records_reminder_count_non_negative");
        }
    }

    @Test
    void supportsSegmentDefaultsVisibilityFilteringAndOwnerLookup() throws Exception {
        migrate();

        String ownerUserId = "90000000-0000-0000-0000-000000000001";
        String privateSegmentId = "90000000-0000-0000-0000-000000000101";
        String teamSegmentId = "90000000-0000-0000-0000-000000000102";
        String globalSegmentId = "90000000-0000-0000-0000-000000000103";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name)"
                            + " values ('"
                            + ownerUserId
                            + "', 'segment-owner@example.test', '$2a$10$examplehash',"
                            + " 'Segment Owner')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segments (id, name, description, owner_user_id) values ('"
                            + privateSegmentId
                            + "', 'Private renewal audience', 'Owned renewal segment', '"
                            + ownerUserId
                            + "'), ('"
                            + teamSegmentId
                            + "', 'Team retirement prospects', 'Shared with the team', '"
                            + ownerUserId
                            + "')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".segments set visibility = 'TEAM' where id = '"
                            + teamSegmentId
                            + "'");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segments (id, name, visibility) values ('"
                            + globalSegmentId
                            + "', 'Global default risk', 'GLOBAL')");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select visibility from "
                                    + TEST_SCHEMA
                                    + ".segments where id = '"
                                    + privateSegmentId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("visibility")).isEqualTo("PRIVATE");
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as owned_visible_segments from "
                                    + TEST_SCHEMA
                                    + ".segments where owner_user_id = '"
                                    + ownerUserId
                                    + "' and visibility in ('PRIVATE', 'TEAM')")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("owned_visible_segments")).isEqualTo(2);
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as global_segments from "
                                    + TEST_SCHEMA
                                    + ".segments where visibility = 'GLOBAL'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("global_segments")).isEqualTo(1);
            }
        }
    }

    @Test
    void segmentOwnerDeletionKeepsSegmentAsUnowned() throws Exception {
        migrate();

        String ownerUserId = "90000000-0000-0000-0000-000000000011";
        String segmentId = "90000000-0000-0000-0000-000000000111";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name)"
                            + " values ('"
                            + ownerUserId
                            + "', 'segment-delete@example.test', '$2a$10$examplehash',"
                            + " 'Segment Delete Owner')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segments (id, name, owner_user_id) values ('"
                            + segmentId
                            + "', 'Persisted unowned segment', '"
                            + ownerUserId
                            + "')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".users where id = '" + ownerUserId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select owner_user_id is null as owner_removed from "
                                    + TEST_SCHEMA
                                    + ".segments where id = '"
                                    + segmentId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("owner_removed")).isTrue();
            }
        }
    }

    @Test
    void segmentsRejectBlankNamesAndInvalidTimestampOrder() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".segments (id, name) values"
                                                    + " ('90000000-0000-0000-0000-000000000121',"
                                                    + " '   ')"))
                    .hasMessageContaining("segments_name_not_blank");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".segments (id, name, created_at, updated_at)"
                                                    + " values"
                                                    + " ('90000000-0000-0000-0000-000000000122',"
                                                    + " 'Invalid timestamp segment', now(),"
                                                    + " now() - interval '1 day')"))
                    .hasMessageContaining("segments_updated_at_after_created_at");
        }
    }

    @Test
    void supportsSegmentCriteriaDefaultsFilteringAndGrouping() throws Exception {
        migrate();

        String segmentId = "91000000-0000-0000-0000-000000000101";
        String defaultCriterionId = "91000000-0000-0000-0000-000000000201";
        String groupedCriterionId = "91000000-0000-0000-0000-000000000202";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segments (id, name, visibility) values ('"
                            + segmentId
                            + "', 'Segment criteria audience', 'TEAM')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segment_criteria (id, segment_id, field_name, operator, value)"
                            + " values ('"
                            + defaultCriterionId
                            + "', '"
                            + segmentId
                            + "', 'customer_type', 'EQUALS', 'CUSTOMER')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segment_criteria (id, segment_id, field_name, operator, value,"
                            + " logical_group, join_operator) values ('"
                            + groupedCriterionId
                            + "', '"
                            + segmentId
                            + "', 'age', 'BETWEEN', '30..65', 'retirement-readiness', 'OR')");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select join_operator from "
                                    + TEST_SCHEMA
                                    + ".segment_criteria where id = '"
                                    + defaultCriterionId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("join_operator")).isEqualTo("AND");
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as matching_criteria from "
                                    + TEST_SCHEMA
                                    + ".segment_criteria where segment_id = '"
                                    + segmentId
                                    + "' and field_name = 'age' and operator = 'BETWEEN'"
                                    + " and logical_group = 'retirement-readiness'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("matching_criteria")).isEqualTo(1);
            }
        }
    }

    @Test
    void segmentCriteriaCascadeWhenSegmentIsDeleted() throws Exception {
        migrate();

        String segmentId = "91000000-0000-0000-0000-000000000111";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segments (id, name) values ('"
                            + segmentId
                            + "', 'Cascade criteria segment')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segment_criteria (id, segment_id, field_name, operator, value)"
                            + " values ('91000000-0000-0000-0000-000000000211', '"
                            + segmentId
                            + "', 'policy_status', 'IN', 'ACTIVE,LAPSED')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".segments where id = '" + segmentId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_criteria from "
                                    + TEST_SCHEMA
                                    + ".segment_criteria where segment_id = '"
                                    + segmentId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_criteria")).isZero();
            }
        }
    }

    @Test
    void segmentCriteriaRejectBlankFieldValueAndLogicalGroup() throws Exception {
        migrate();

        String segmentId = "91000000-0000-0000-0000-000000000121";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segments (id, name) values ('"
                            + segmentId
                            + "', 'Invalid criteria segment')");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".segment_criteria"
                                                    + " (id, segment_id, field_name, operator, value)"
                                                    + " values"
                                                    + " ('91000000-0000-0000-0000-000000000221', '"
                                                    + segmentId
                                                    + "', '   ', 'EQUALS', 'CUSTOMER')"))
                    .hasMessageContaining("segment_criteria_field_name_not_blank");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".segment_criteria"
                                                    + " (id, segment_id, field_name, operator, value)"
                                                    + " values"
                                                    + " ('91000000-0000-0000-0000-000000000222', '"
                                                    + segmentId
                                                    + "', 'customer_type', 'EQUALS', '   ')"))
                    .hasMessageContaining("segment_criteria_value_not_blank");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".segment_criteria"
                                                    + " (id, segment_id, field_name, operator, value,"
                                                    + " logical_group) values"
                                                    + " ('91000000-0000-0000-0000-000000000223', '"
                                                    + segmentId
                                                    + "', 'customer_type', 'EQUALS', 'CUSTOMER',"
                                                    + " '   ')"))
                    .hasMessageContaining("segment_criteria_logical_group_not_blank");
        }
    }

    @Test
    void campaignRecipientsRejectDuplicateCustomerWithinCampaign() throws Exception {
        migrate();

        String campaignId = "92000000-0000-0000-0000-000000000101";
        String customerId = "92000000-0000-0000-0000-000000000201";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaigns (id, name, objective, channel) values ('"
                            + campaignId
                            + "', 'Unique recipient campaign',"
                            + " 'Prevent duplicate customers in one campaign', 'EMAIL')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'Campaign', 'Recipient')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaign_recipients"
                            + " (id, campaign_id, customer_id, eligibility_status) values"
                            + " ('92000000-0000-0000-0000-000000000301', '"
                            + campaignId
                            + "', '"
                            + customerId
                            + "', 'ELIGIBLE')");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".campaign_recipients"
                                                    + " (id, campaign_id, customer_id,"
                                                    + " eligibility_status) values"
                                                    + " ('92000000-0000-0000-0000-000000000302', '"
                                                    + campaignId
                                                    + "', '"
                                                    + customerId
                                                    + "', 'EXCLUDED')"))
                    .isInstanceOf(SQLException.class)
                    .satisfies(
                            error ->
                                    assertThat(((SQLException) error).getSQLState())
                                            .as("campaign recipient unique constraint")
                                            .isEqualTo("23505"))
                    .hasMessageContaining("campaign_recipients_campaign_customer_unique");
        }
    }

    @Test
    void supportsCampaignRecipientEligibilityLifecycleAndFiltering() throws Exception {
        migrate();

        String campaignId = "92000000-0000-0000-0000-000000000111";
        String eligibleCustomerId = "92000000-0000-0000-0000-000000000211";
        String excludedCustomerId = "92000000-0000-0000-0000-000000000212";
        String recipientId = "92000000-0000-0000-0000-000000000311";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaigns (id, name, objective, channel) values ('"
                            + campaignId
                            + "', 'Recipient lifecycle campaign',"
                            + " 'Track eligible and excluded campaign recipients', 'EMAIL')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + eligibleCustomerId
                            + "', 'CUSTOMER', 'Eligible', 'Recipient'), ('"
                            + excludedCustomerId
                            + "', 'CUSTOMER', 'Excluded', 'Recipient')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaign_recipients"
                            + " (id, campaign_id, customer_id, eligibility_status,"
                            + " eligibility_explanation) values ('"
                            + recipientId
                            + "', '"
                            + campaignId
                            + "', '"
                            + eligibleCustomerId
                            + "', 'ELIGIBLE', 'Has active consent'),"
                            + " ('92000000-0000-0000-0000-000000000312', '"
                            + campaignId
                            + "', '"
                            + excludedCustomerId
                            + "', 'EXCLUDED', 'Missing consent')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".campaign_recipients set eligibility_status = 'CLICKED',"
                            + " sent_at = now(), opened_at = now(), clicked_at = now()"
                            + " where id = '"
                            + recipientId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select sent_at is not null as sent,"
                                    + " opened_at is not null as opened,"
                                    + " clicked_at is not null as clicked from "
                                    + TEST_SCHEMA
                                    + ".campaign_recipients where id = '"
                                    + recipientId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("sent")).isTrue();
                assertThat(resultSet.getBoolean("opened")).isTrue();
                assertThat(resultSet.getBoolean("clicked")).isTrue();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as clicked_recipients from "
                                    + TEST_SCHEMA
                                    + ".campaign_recipients where campaign_id = '"
                                    + campaignId
                                    + "' and eligibility_status = 'CLICKED'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("clicked_recipients")).isEqualTo(1);
            }
        }
    }

    @Test
    void campaignProductsRejectDuplicateProductWithinCampaign() throws Exception {
        migrate();

        String campaignId = "93000000-0000-0000-0000-000000000101";
        String productId = "93000000-0000-0000-0000-000000000201";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertCampaignProduct(statement, campaignId, productId);

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".campaign_products"
                                                    + " (campaign_id, product_id) values ('"
                                                    + campaignId
                                                    + "', '"
                                                    + productId
                                                    + "')"))
                    .hasMessageContaining("campaign_products_pkey");
        }
    }

    @Test
    void campaignProductsCascadeWithCampaignButRestrictProductDeletion() throws Exception {
        migrate();

        String campaignId = "93000000-0000-0000-0000-000000000111";
        String productId = "93000000-0000-0000-0000-000000000211";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertCampaignProduct(statement, campaignId, productId);

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "delete from "
                                                    + TEST_SCHEMA
                                                    + ".products where id = '"
                                                    + productId
                                                    + "'"))
                    .hasMessageContaining("campaign_products_product_id_fkey");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".campaigns where id = '" + campaignId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_links from "
                                    + TEST_SCHEMA
                                    + ".campaign_products where campaign_id = '"
                                    + campaignId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_links")).isZero();
            }
        }
    }

    @Test
    void supportsCampaignDefaultsStatusUpdatesAndFiltering() throws Exception {
        migrate();

        String draftCampaignId = "94000000-0000-0000-0000-000000000101";
        String activeCampaignId = "94000000-0000-0000-0000-000000000102";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaigns (id, name, objective, channel, message_subject,"
                            + " message_body, start_date, end_date) values ('"
                            + draftCampaignId
                            + "', 'Draft campaign', 'Validate default campaign status',"
                            + " 'EMAIL', 'Draft subject', 'Draft body', current_date,"
                            + " current_date + interval '7 days'), ('"
                            + activeCampaignId
                            + "', 'Active campaign', 'Validate campaign status filtering',"
                            + " 'SMS', 'Active subject', 'Active body', current_date,"
                            + " current_date + interval '14 days')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".campaigns set status = 'ACTIVE' where id = '"
                            + activeCampaignId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select status from "
                                    + TEST_SCHEMA
                                    + ".campaigns where id = '"
                                    + draftCampaignId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("DRAFT");
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as active_campaigns from "
                                    + TEST_SCHEMA
                                    + ".campaigns where status = 'ACTIVE'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("active_campaigns")).isEqualTo(1);
            }
        }
    }

    @Test
    void campaignUserAndSegmentDeletionKeepsCampaignWithNullableReferences() throws Exception {
        migrate();

        String ownerUserId = "94000000-0000-0000-0000-000000000201";
        String approverUserId = "94000000-0000-0000-0000-000000000202";
        String segmentId = "94000000-0000-0000-0000-000000000301";
        String campaignId = "94000000-0000-0000-0000-000000000401";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + ownerUserId
                            + "', 'campaign-owner@example.test', '$2a$10$examplehash',"
                            + " 'Campaign Owner'), ('"
                            + approverUserId
                            + "', 'campaign-approver@example.test', '$2a$10$examplehash',"
                            + " 'Campaign Approver')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".segments (id, name, owner_user_id) values ('"
                            + segmentId
                            + "', 'Campaign target segment', '"
                            + ownerUserId
                            + "')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaigns (id, name, objective, owner_user_id, segment_id,"
                            + " channel, approved_by, approved_at, status) values ('"
                            + campaignId
                            + "', 'Approved campaign', 'Validate nullable campaign references', '"
                            + ownerUserId
                            + "', '"
                            + segmentId
                            + "', 'MIXED', '"
                            + approverUserId
                            + "', now(), 'APPROVED')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".segments where id = '" + segmentId + "'");
            statement.executeUpdate(
                    "delete from "
                            + TEST_SCHEMA
                            + ".users where id in ('"
                            + ownerUserId
                            + "', '"
                            + approverUserId
                            + "')");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select owner_user_id is null as owner_removed,"
                                    + " segment_id is null as segment_removed,"
                                    + " approved_by is null as approver_removed from "
                                    + TEST_SCHEMA
                                    + ".campaigns where id = '"
                                    + campaignId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("owner_removed")).isTrue();
                assertThat(resultSet.getBoolean("segment_removed")).isTrue();
                assertThat(resultSet.getBoolean("approver_removed")).isTrue();
            }
        }
    }

    @Test
    void supportsFollowUpTaskDefaultsCompletionAndAssigneeStatusFiltering() throws Exception {
        migrate();

        String customerId = "95000000-0000-0000-0000-000000000101";
        String assigneeId = "95000000-0000-0000-0000-000000000201";
        String openTaskId = "95000000-0000-0000-0000-000000000301";
        String completedTaskId = "95000000-0000-0000-0000-000000000302";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'Follow', 'Up')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + assigneeId
                            + "', 'follow-up-assignee@example.test', '$2a$10$examplehash',"
                            + " 'Follow Up Assignee')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".follow_up_tasks (id, customer_id, assigned_to, title,"
                            + " description, due_date) values ('"
                            + openTaskId
                            + "', '"
                            + customerId
                            + "', '"
                            + assigneeId
                            + "', 'Call customer', 'Discuss campaign response',"
                            + " current_date + interval '2 days'), ('"
                            + completedTaskId
                            + "', '"
                            + customerId
                            + "', '"
                            + assigneeId
                            + "', 'Send summary', 'Send follow-up details', current_date)");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".follow_up_tasks set status = 'COMPLETED', priority = 'HIGH',"
                            + " completed_at = now() where id = '"
                            + completedTaskId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select status, priority from "
                                    + TEST_SCHEMA
                                    + ".follow_up_tasks where id = '"
                                    + openTaskId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("OPEN");
                assertThat(resultSet.getString("priority")).isEqualTo("MEDIUM");
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as completed_tasks from "
                                    + TEST_SCHEMA
                                    + ".follow_up_tasks where assigned_to = '"
                                    + assigneeId
                                    + "' and status = 'COMPLETED' and completed_at is not null")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("completed_tasks")).isEqualTo(1);
            }
        }
    }

    @Test
    void followUpTasksCascadeWithCustomerAndRetainWhenCampaignOrAssigneeDeleted() throws Exception {
        migrate();

        String customerId = "95000000-0000-0000-0000-000000000111";
        String campaignId = "95000000-0000-0000-0000-000000000211";
        String assigneeId = "95000000-0000-0000-0000-000000000311";
        String taskId = "95000000-0000-0000-0000-000000000411";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'Cascade', 'Task')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaigns (id, name, objective, channel) values ('"
                            + campaignId
                            + "', 'Follow-up campaign', 'Create follow-up work', 'PHONE')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + assigneeId
                            + "', 'follow-up-delete@example.test', '$2a$10$examplehash',"
                            + " 'Deleted Follow Up Assignee')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".follow_up_tasks (id, customer_id, campaign_id, assigned_to, title)"
                            + " values ('"
                            + taskId
                            + "', '"
                            + customerId
                            + "', '"
                            + campaignId
                            + "', '"
                            + assigneeId
                            + "', 'Retain task after nullable references are deleted')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".campaigns where id = '" + campaignId + "'");
            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".users where id = '" + assigneeId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select campaign_id is null as campaign_removed,"
                                    + " assigned_to is null as assignee_removed from "
                                    + TEST_SCHEMA
                                    + ".follow_up_tasks where id = '"
                                    + taskId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("campaign_removed")).isTrue();
                assertThat(resultSet.getBoolean("assignee_removed")).isTrue();
            }

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".customers where id = '" + customerId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_tasks from "
                                    + TEST_SCHEMA
                                    + ".follow_up_tasks where id = '"
                                    + taskId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_tasks")).isZero();
            }
        }
    }

    @Test
    void supportsReminderScheduleDefaultsSentUpdatesAndDateStatusFiltering() throws Exception {
        migrate();

        String customerId = "96000000-0000-0000-0000-000000000101";
        String productId = "96000000-0000-0000-0000-000000000201";
        String pendingReminderId = "96000000-0000-0000-0000-000000000301";
        String sentReminderId = "96000000-0000-0000-0000-000000000302";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertReminderCustomerAndProduct(statement, customerId, productId);
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".reminder_schedules (id, customer_id, product_id, reminder_type,"
                            + " reminder_level, scheduled_date) values ('"
                            + pendingReminderId
                            + "', '"
                            + customerId
                            + "', '"
                            + productId
                            + "', 'PAYMENT_DUE', 'YELLOW', current_date + interval '3 days'),"
                            + " ('"
                            + sentReminderId
                            + "', '"
                            + customerId
                            + "', '"
                            + productId
                            + "', 'PRODUCT_EXPIRATION', 'RED', current_date)");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".reminder_schedules set status = 'SENT', sent_at = now()"
                            + " where id = '"
                            + sentReminderId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select status from "
                                    + TEST_SCHEMA
                                    + ".reminder_schedules where id = '"
                                    + pendingReminderId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("PENDING");
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as sent_today from "
                                    + TEST_SCHEMA
                                    + ".reminder_schedules where scheduled_date = current_date"
                                    + " and status = 'SENT' and sent_at is not null")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("sent_today")).isEqualTo(1);
            }
        }
    }

    @Test
    void reminderSchedulesCascadeWithCustomerButRestrictProductDeletion() throws Exception {
        migrate();

        String customerId = "96000000-0000-0000-0000-000000000111";
        String productId = "96000000-0000-0000-0000-000000000211";
        String reminderId = "96000000-0000-0000-0000-000000000311";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertReminderCustomerAndProduct(statement, customerId, productId);
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".reminder_schedules (id, customer_id, product_id, reminder_type,"
                            + " reminder_level, scheduled_date) values ('"
                            + reminderId
                            + "', '"
                            + customerId
                            + "', '"
                            + productId
                            + "', 'PAYMENT_DUE', 'GREEN', current_date)");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "delete from "
                                                    + TEST_SCHEMA
                                                    + ".products where id = '"
                                                    + productId
                                                    + "'"))
                    .hasMessageContaining("reminder_schedules_product_id_fkey");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".customers where id = '" + customerId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_reminders from "
                                    + TEST_SCHEMA
                                    + ".reminder_schedules where id = '"
                                    + reminderId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_reminders")).isZero();
            }
        }
    }

    @Test
    void supportsCampaignMetricDefaultsUpdatesAndUniqueCampaignMetric() throws Exception {
        migrate();

        String campaignId = "97000000-0000-0000-0000-000000000101";
        String metricId = "97000000-0000-0000-0000-000000000201";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertCampaign(statement, campaignId, "Metrics campaign");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaign_metrics (id, campaign_id) values ('"
                            + metricId
                            + "', '"
                            + campaignId
                            + "')");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select audience_size, eligible_count, sent_count, converted_count from "
                                    + TEST_SCHEMA
                                    + ".campaign_metrics where id = '"
                                    + metricId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("audience_size")).isZero();
                assertThat(resultSet.getInt("eligible_count")).isZero();
                assertThat(resultSet.getInt("sent_count")).isZero();
                assertThat(resultSet.getInt("converted_count")).isZero();
            }

            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".campaign_metrics set audience_size = 100, eligible_count = 80,"
                            + " excluded_count = 20, sent_count = 75, opened_count = 40,"
                            + " clicked_count = 12, replied_count = 5, converted_count = 3,"
                            + " estimated_cost = 250.00, estimated_revenue = 1500.00,"
                            + " estimated_roi = 5.00, updated_at = now() where id = '"
                            + metricId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select converted_count, estimated_roi from "
                                    + TEST_SCHEMA
                                    + ".campaign_metrics where id = '"
                                    + metricId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("converted_count")).isEqualTo(3);
                assertThat(resultSet.getBigDecimal("estimated_roi")).isEqualByComparingTo("5.00");
            }

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".campaign_metrics (id, campaign_id)"
                                                    + " values"
                                                    + " ('97000000-0000-0000-0000-000000000202', '"
                                                    + campaignId
                                                    + "')"))
                    .hasMessageContaining("campaign_metrics_campaign_id_key");
        }
    }

    @Test
    void campaignMetricsCascadeWhenCampaignIsDeleted() throws Exception {
        migrate();

        String campaignId = "97000000-0000-0000-0000-000000000111";
        String metricId = "97000000-0000-0000-0000-000000000211";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            insertCampaign(statement, campaignId, "Metrics cascade campaign");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".campaign_metrics (id, campaign_id, audience_size)"
                            + " values ('"
                            + metricId
                            + "', '"
                            + campaignId
                            + "', 10)");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".campaigns where id = '" + campaignId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_metrics from "
                                    + TEST_SCHEMA
                                    + ".campaign_metrics where id = '"
                                    + metricId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_metrics")).isZero();
            }
        }
    }

    @Test
    void supportsAuditLogJsonPayloadsAndEntityFiltering() throws Exception {
        migrate();

        String actorUserId = "98000000-0000-0000-0000-000000000101";
        String entityId = "98000000-0000-0000-0000-000000000201";
        String auditLogId = "98000000-0000-0000-0000-000000000301";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + actorUserId
                            + "', 'audit-actor@example.test', '$2a$10$examplehash',"
                            + " 'Audit Actor')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".audit_logs (id, actor_user_id, action, entity_type, entity_id,"
                            + " old_value, new_value, ip_address) values ('"
                            + auditLogId
                            + "', '"
                            + actorUserId
                            + "', 'UPDATE_STATUS', 'campaign', '"
                            + entityId
                            + "', '{\"status\":\"DRAFT\"}'::jsonb,"
                            + " '{\"status\":\"ACTIVE\"}'::jsonb, '127.0.0.1')");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select old_value ->> 'status' as old_status,"
                                    + " new_value ->> 'status' as new_status from "
                                    + TEST_SCHEMA
                                    + ".audit_logs where id = '"
                                    + auditLogId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("old_status")).isEqualTo("DRAFT");
                assertThat(resultSet.getString("new_status")).isEqualTo("ACTIVE");
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as matching_logs from "
                                    + TEST_SCHEMA
                                    + ".audit_logs where entity_type = 'campaign'"
                                    + " and entity_id = '"
                                    + entityId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("matching_logs")).isEqualTo(1);
            }
        }
    }

    @Test
    void auditLogsRetainHistoryWhenActorIsDeleted() throws Exception {
        migrate();

        String actorUserId = "98000000-0000-0000-0000-000000000111";
        String auditLogId = "98000000-0000-0000-0000-000000000311";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + actorUserId
                            + "', 'audit-delete@example.test', '$2a$10$examplehash',"
                            + " 'Deleted Audit Actor')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".audit_logs (id, actor_user_id, action, entity_type, ip_address)"
                            + " values ('"
                            + auditLogId
                            + "', '"
                            + actorUserId
                            + "', 'DELETE_USER', 'user', '127.0.0.1')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".users where id = '" + actorUserId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select actor_user_id is null as actor_removed, action from "
                                    + TEST_SCHEMA
                                    + ".audit_logs where id = '"
                                    + auditLogId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("actor_removed")).isTrue();
                assertThat(resultSet.getString("action")).isEqualTo("DELETE_USER");
            }
        }
    }

    @Test
    void supportsReportExportDefaultsCompletionAndFailureStates() throws Exception {
        migrate();

        String requesterId = "99000000-0000-0000-0000-000000000101";
        String requestedExportId = "99000000-0000-0000-0000-000000000201";
        String completedExportId = "99000000-0000-0000-0000-000000000202";
        String failedExportId = "99000000-0000-0000-0000-000000000203";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + requesterId
                            + "', 'report-exporter@example.test', '$2a$10$examplehash',"
                            + " 'Report Exporter')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".report_exports (id, requested_by, report_name, export_type)"
                            + " values ('"
                            + requestedExportId
                            + "', '"
                            + requesterId
                            + "', 'Campaign performance CSV', 'CSV'), ('"
                            + completedExportId
                            + "', '"
                            + requesterId
                            + "', 'Audit history PDF', 'PDF'), ('"
                            + failedExportId
                            + "', '"
                            + requesterId
                            + "', 'Failed export CSV', 'CSV')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".report_exports set status = 'COMPLETED',"
                            + " file_url = 's3://reports/audit-history.pdf', completed_at = now()"
                            + " where id = '"
                            + completedExportId
                            + "'");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".report_exports set status = 'FAILED'"
                            + " where id = '"
                            + failedExportId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select status from "
                                    + TEST_SCHEMA
                                    + ".report_exports where id = '"
                                    + requestedExportId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("REQUESTED");
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as completed_exports from "
                                    + TEST_SCHEMA
                                    + ".report_exports where requested_by = '"
                                    + requesterId
                                    + "' and status = 'COMPLETED'"
                                    + " and file_url is not null and completed_at is not null")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("completed_exports")).isEqualTo(1);
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as failed_exports from "
                                    + TEST_SCHEMA
                                    + ".report_exports where status = 'FAILED'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("failed_exports")).isEqualTo(1);
            }
        }
    }

    @Test
    void reportExportsRetainRequestWhenRequesterIsDeleted() throws Exception {
        migrate();

        String requesterId = "99000000-0000-0000-0000-000000000111";
        String exportId = "99000000-0000-0000-0000-000000000211";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + requesterId
                            + "', 'report-delete@example.test', '$2a$10$examplehash',"
                            + " 'Deleted Report Exporter')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".report_exports (id, requested_by, report_name, export_type)"
                            + " values ('"
                            + exportId
                            + "', '"
                            + requesterId
                            + "', 'Retained export request', 'PDF')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".users where id = '" + requesterId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select requested_by is null as requester_removed, report_name from "
                                    + TEST_SCHEMA
                                    + ".report_exports where id = '"
                                    + exportId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("requester_removed")).isTrue();
                assertThat(resultSet.getString("report_name")).isEqualTo("Retained export request");
            }
        }
    }

    @Test
    void supportsAiRecommendationDefaultsApprovalAndFiltering() throws Exception {
        migrate();

        String approverId = "9a000000-0000-0000-0000-000000000101";
        String customerId = "9a000000-0000-0000-0000-000000000201";
        String productRecommendationId = "9a000000-0000-0000-0000-000000000301";
        String riskRecommendationId = "9a000000-0000-0000-0000-000000000302";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + approverId
                            + "', 'ai-approver@example.test', '$2a$10$examplehash',"
                            + " 'AI Approver')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'AI', 'Target')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".ai_recommendations (id, recommendation_type, target_entity_type,"
                            + " target_entity_id, input_summary, recommendation, explanation,"
                            + " confidence_score, approved_by_user_id) values ('"
                            + productRecommendationId
                            + "', 'PRODUCT', 'customer', '"
                            + customerId
                            + "', 'Customer has an active life policy',"
                            + " 'Recommend investment fund follow-up',"
                            + " 'The customer profile matches the investment segment', 87.50, '"
                            + approverId
                            + "'), ('"
                            + riskRecommendationId
                            + "', 'RISK', 'customer', '"
                            + customerId
                            + "', 'Customer has overdue payment reminders',"
                            + " 'Review outreach timing before campaign launch',"
                            + " 'Risk recommendation protects compliant communication', 62.25, null)");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select created_at is not null as has_created_at from "
                                    + TEST_SCHEMA
                                    + ".ai_recommendations where id = '"
                                    + productRecommendationId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("has_created_at")).isTrue();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as customer_recommendations from "
                                    + TEST_SCHEMA
                                    + ".ai_recommendations where target_entity_type = 'customer'"
                                    + " and target_entity_id = '"
                                    + customerId
                                    + "' and confidence_score >= 60")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("customer_recommendations")).isEqualTo(2);
            }
        }
    }

    @Test
    void aiRecommendationsValidateRequiredNarrativeFieldsAndConfidenceRange() throws Exception {
        migrate();

        String recommendationId = "9a000000-0000-0000-0000-000000000311";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".ai_recommendations (id,"
                                                    + " recommendation_type, target_entity_type,"
                                                    + " input_summary, recommendation, explanation)"
                                                    + " values ('"
                                                    + recommendationId
                                                    + "', 'COPY', ' ', 'Input',"
                                                    + " 'Recommendation', 'Explanation')"))
                    .hasMessageContaining("ai_recommendations_target_entity_type_not_blank");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".ai_recommendations (id,"
                                                    + " recommendation_type, target_entity_type,"
                                                    + " input_summary, recommendation, explanation,"
                                                    + " confidence_score) values ('"
                                                    + recommendationId
                                                    + "', 'SEGMENT', 'campaign', 'Input',"
                                                    + " 'Recommendation', 'Explanation', 100.01)"))
                    .hasMessageContaining("ai_recommendations_confidence_score_range");
        }
    }

    @Test
    void aiRecommendationsRetainSuggestionWhenApproverIsDeleted() throws Exception {
        migrate();

        String approverId = "9a000000-0000-0000-0000-000000000111";
        String recommendationId = "9a000000-0000-0000-0000-000000000321";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + approverId
                            + "', 'ai-delete@example.test', '$2a$10$examplehash',"
                            + " 'Deleted AI Approver')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".ai_recommendations (id, recommendation_type, target_entity_type,"
                            + " input_summary, recommendation, explanation, approved_by_user_id)"
                            + " values ('"
                            + recommendationId
                            + "', 'DUPLICATE_WARNING', 'customer',"
                            + " 'Potential duplicate target found',"
                            + " 'Review duplicate customer before outreach',"
                            + " 'The recommendation should remain auditable', '"
                            + approverId
                            + "')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".users where id = '" + approverId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select approved_by_user_id is null as approver_removed,"
                                    + " recommendation_type from "
                                    + TEST_SCHEMA
                                    + ".ai_recommendations where id = '"
                                    + recommendationId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("approver_removed")).isTrue();
                assertThat(resultSet.getString("recommendation_type"))
                        .isEqualTo("DUPLICATE_WARNING");
            }
        }
    }

    @Test
    void supportsProductChangeRequestDefaultsStatusUpdatesAndFiltering() throws Exception {
        migrate();

        String productId = "70000000-0000-0000-0000-000000000101";
        String requesterId = "70000000-0000-0000-0000-000000000201";
        String openRequestId = "70000000-0000-0000-0000-000000000301";
        String approvedRequestId = "70000000-0000-0000-0000-000000000302";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + requesterId
                            + "', 'product-manager@example.test', '$2a$10$examplehash',"
                            + " 'Product Manager')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".products (id, name, product_type, price, duration_months)"
                            + " values ('"
                            + productId
                            + "', 'Change Request Product', 'OTHER', 45.00, 12)");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".product_change_requests (id, product_id, requested_by,"
                            + " request_type, description) values ('"
                            + openRequestId
                            + "', '"
                            + productId
                            + "', '"
                            + requesterId
                            + "', 'PRICE_CHANGE', 'Increase price for new actuarial model'), ('"
                            + approvedRequestId
                            + "', '"
                            + productId
                            + "', '"
                            + requesterId
                            + "', 'DURATION_CHANGE', 'Extend duration for retention campaign')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".product_change_requests set status = 'APPROVED'"
                            + " where id = '"
                            + approvedRequestId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select status, created_at is not null as has_created_at,"
                                    + " updated_at is not null as has_updated_at from "
                                    + TEST_SCHEMA
                                    + ".product_change_requests where id = '"
                                    + openRequestId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("OPEN");
                assertThat(resultSet.getBoolean("has_created_at")).isTrue();
                assertThat(resultSet.getBoolean("has_updated_at")).isTrue();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as open_requests from "
                                    + TEST_SCHEMA
                                    + ".product_change_requests where product_id = '"
                                    + productId
                                    + "' and status = 'OPEN'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("open_requests")).isEqualTo(1);
            }
        }
    }

    @Test
    void productChangeRequestsRetainHistoryWhenRequesterIsDeletedAndCascadeWithProduct()
            throws Exception {
        migrate();

        String productId = "70000000-0000-0000-0000-000000000111";
        String requesterId = "70000000-0000-0000-0000-000000000211";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name) values ('"
                            + requesterId
                            + "', 'requester@example.test', '$2a$10$examplehash',"
                            + " 'Requester User')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".products (id, name, product_type, price, duration_months)"
                            + " values ('"
                            + productId
                            + "', 'Cascade Change Product', 'OTHER', 25.00, 6)");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".product_change_requests (id, product_id, requested_by,"
                            + " request_type, description) values"
                            + " ('70000000-0000-0000-0000-000000000311', '"
                            + productId
                            + "', '"
                            + requesterId
                            + "', 'STATUS_CHANGE', 'Deactivate old product')");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".users where id = '" + requesterId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select requested_by is null as requester_removed from "
                                    + TEST_SCHEMA
                                    + ".product_change_requests where product_id = '"
                                    + productId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("requester_removed")).isTrue();
            }

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".products where id = '" + productId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_requests from "
                                    + TEST_SCHEMA
                                    + ".product_change_requests where product_id = '"
                                    + productId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_requests")).isZero();
            }
        }
    }

    @Test
    void productChangeRequestsRejectBlankDescriptions() throws Exception {
        migrate();

        String productId = "70000000-0000-0000-0000-000000000121";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".products (id, name, product_type, price, duration_months)"
                            + " values ('"
                            + productId
                            + "', 'Blank Description Product', 'OTHER', 15.00, 3)");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".product_change_requests (id, product_id,"
                                                    + " request_type, description)"
                                                    + " values ('70000000-0000-0000-0000-000000000321', '"
                                                    + productId
                                                    + "', 'PRICE_CHANGE', '   ')"))
                    .hasMessageContaining("product_change_requests_description_not_blank");
        }
    }

    @Test
    void supportsProductOwnershipDefaultsStatusFilteringAndExpirationQueries() throws Exception {
        migrate();

        String customerId = "60000000-0000-0000-0000-000000000001";
        String productId = "60000000-0000-0000-0000-000000000101";
        String activeOwnershipId = "60000000-0000-0000-0000-000000000201";
        String expiredOwnershipId = "60000000-0000-0000-0000-000000000202";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'Owner', 'Customer')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".products (id, name, product_type, price, duration_months)"
                            + " values ('"
                            + productId
                            + "', 'Ownership Life', 'LIFE_INSURANCE', 120.00, 12)");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".product_ownerships (id, customer_id, product_id, policy_number,"
                            + " start_date, expiration_date) values ('"
                            + activeOwnershipId
                            + "', '"
                            + customerId
                            + "', '"
                            + productId
                            + "', 'POL-ACTIVE-001', current_date,"
                            + " current_date + interval '90 days'), ('"
                            + expiredOwnershipId
                            + "', '"
                            + customerId
                            + "', '"
                            + productId
                            + "', 'POL-EXPIRED-001', current_date - interval '400 days',"
                            + " current_date - interval '30 days')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".product_ownerships set status = 'EXPIRED'"
                            + " where id = '"
                            + expiredOwnershipId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select status, created_at is not null as has_created_at from "
                                    + TEST_SCHEMA
                                    + ".product_ownerships where id = '"
                                    + activeOwnershipId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("ACTIVE");
                assertThat(resultSet.getBoolean("has_created_at")).isTrue();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as expiring_active_ownerships from "
                                    + TEST_SCHEMA
                                    + ".product_ownerships where status = 'ACTIVE'"
                                    + " and expiration_date between current_date"
                                    + " and current_date + interval '3 months'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("expiring_active_ownerships")).isEqualTo(1);
            }
        }
    }

    @Test
    void productOwnershipsRejectDuplicatePoliciesAndInvalidExpirationDates() throws Exception {
        migrate();

        String customerId = "60000000-0000-0000-0000-000000000011";
        String productId = "60000000-0000-0000-0000-000000000111";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'Policy', 'Owner')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".products (id, name, product_type, price, duration_months)"
                            + " values ('"
                            + productId
                            + "', 'Policy Product', 'OTHER', 10.00, 1)");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".product_ownerships (id, customer_id, product_id, policy_number,"
                            + " start_date) values ('60000000-0000-0000-0000-000000000211', '"
                            + customerId
                            + "', '"
                            + productId
                            + "', 'POL-DUP-001', current_date)");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".product_ownerships (id, customer_id, product_id,"
                                                    + " policy_number, start_date)"
                                                    + " values ('60000000-0000-0000-0000-000000000212', '"
                                                    + customerId
                                                    + "', '"
                                                    + productId
                                                    + "', 'POL-DUP-001', current_date)"))
                    .hasMessageContaining("product_ownerships_policy_number_unique");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".product_ownerships (id, customer_id, product_id,"
                                                    + " policy_number, start_date, expiration_date)"
                                                    + " values ('60000000-0000-0000-0000-000000000213', '"
                                                    + customerId
                                                    + "', '"
                                                    + productId
                                                    + "', 'POL-BAD-DATE-001', current_date,"
                                                    + " current_date - interval '1 day')"))
                    .hasMessageContaining("product_ownerships_expiration_after_start");
        }
    }

    @Test
    void productOwnershipsCascadeWithCustomersAndRestrictProductDeletion() throws Exception {
        migrate();

        String customerId = "60000000-0000-0000-0000-000000000021";
        String productId = "60000000-0000-0000-0000-000000000121";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".customers (id, customer_type, first_name, last_name)"
                            + " values ('"
                            + customerId
                            + "', 'CUSTOMER', 'Cascade', 'Owner')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".products (id, name, product_type, price, duration_months)"
                            + " values ('"
                            + productId
                            + "', 'Restricted Product', 'OTHER', 15.00, 1)");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".product_ownerships (id, customer_id, product_id, policy_number,"
                            + " start_date) values ('60000000-0000-0000-0000-000000000221', '"
                            + customerId
                            + "', '"
                            + productId
                            + "', 'POL-RESTRICT-001', current_date)");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "delete from "
                                                    + TEST_SCHEMA
                                                    + ".products where id = '"
                                                    + productId
                                                    + "'"))
                    .hasMessageContaining("product_ownerships_product_id_fkey");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".customers where id = '" + customerId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as remaining_ownerships from "
                                    + TEST_SCHEMA
                                    + ".product_ownerships where customer_id = '"
                                    + customerId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("remaining_ownerships")).isZero();
            }
        }
    }

    @Test
    void supportsProductInsertDefaultsActiveFilteringAndSoftDelete() throws Exception {
        migrate();

        String activeProductId = "50000000-0000-0000-0000-000000000001";
        String disabledProductId = "50000000-0000-0000-0000-000000000002";
        String deletedProductId = "50000000-0000-0000-0000-000000000003";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".products (id, name, product_type, description, price,"
                            + " duration_months, expiration_policy) values ('"
                            + activeProductId
                            + "', 'Future Secure Life', 'LIFE_INSURANCE',"
                            + " 'Life insurance product', 125.50, 12, 'ANNUAL_RENEWAL'), ('"
                            + disabledProductId
                            + "', 'Legacy Auto Cover', 'AUTO_INSURANCE',"
                            + " 'Disabled auto insurance product', 75.00, 6, 'FIXED_TERM'), ('"
                            + deletedProductId
                            + "', 'Old Investment Fund', 'INVESTMENT_FUND',"
                            + " 'Soft deleted investment product', 250.00, 24, 'FUND_REVIEW')");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".products set active = false where id = '"
                            + disabledProductId
                            + "'");
            statement.executeUpdate(
                    "update "
                            + TEST_SCHEMA
                            + ".products set deleted_at = now() where id = '"
                            + deletedProductId
                            + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select active, created_at is not null as has_created_at,"
                                    + " updated_at is not null as has_updated_at,"
                                    + " deleted_at is null as not_deleted from "
                                    + TEST_SCHEMA
                                    + ".products where id = '"
                                    + activeProductId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("active")).isTrue();
                assertThat(resultSet.getBoolean("has_created_at")).isTrue();
                assertThat(resultSet.getBoolean("has_updated_at")).isTrue();
                assertThat(resultSet.getBoolean("not_deleted")).isTrue();
            }

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select count(*) as active_products from "
                                    + TEST_SCHEMA
                                    + ".products where active = true and deleted_at is null")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("active_products")).isEqualTo(1);
            }
        }
    }

    @Test
    void productsRejectNegativePriceAndNonPositiveDuration() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".products (id, name, product_type, price)"
                                                    + " values ('50000000-0000-0000-0000-000000000011',"
                                                    + " 'Invalid Price', 'OTHER', -1.00)"))
                    .hasMessageContaining("products_price_non_negative");
            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".products (id, name, product_type, duration_months)"
                                                    + " values ('50000000-0000-0000-0000-000000000012',"
                                                    + " 'Invalid Duration', 'OTHER', 0)"))
                    .hasMessageContaining("products_duration_positive");
        }
    }

    @Test
    void supportsUserInsertDefaultsAndRoleAssignment() throws Exception {
        migrate();

        String assigneeUserId = "10000000-0000-0000-0000-000000009901";
        String assigningAdminId = "10000000-0000-0000-0000-000000009902";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name)"
                            + " values ('"
                            + assigneeUserId
                            + "', 'analyst@example.test', '$2a$10$examplehash', 'Analyst User'),"
                            + " ('"
                            + assigningAdminId
                            + "', 'admin@example.test', '$2a$10$examplehash', 'Admin User')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".user_roles (user_id, role_id, assigned_by)"
                            + " select '"
                            + assigneeUserId
                            + "', id, '"
                            + assigningAdminId
                            + "' from "
                            + TEST_SCHEMA
                            + ".roles where name = 'BI_ANALYST'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select u.status, u.created_at is not null as has_created_at,"
                                    + " u.updated_at is not null as has_updated_at,"
                                    + " ur.assigned_at is not null as has_assigned_at,"
                                    + " ur.assigned_by::text as assigned_by,"
                                    + " count(ur.role_id) as role_count from "
                                    + TEST_SCHEMA
                                    + ".users u join "
                                    + TEST_SCHEMA
                                    + ".user_roles ur on ur.user_id = u.id"
                                    + " where u.id = '"
                                    + assigneeUserId
                                    + "' group by u.status, u.created_at, u.updated_at,"
                                    + " ur.assigned_at, ur.assigned_by")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("status")).isEqualTo("ACTIVE");
                assertThat(resultSet.getBoolean("has_created_at")).isTrue();
                assertThat(resultSet.getBoolean("has_updated_at")).isTrue();
                assertThat(resultSet.getBoolean("has_assigned_at")).isTrue();
                assertThat(resultSet.getString("assigned_by")).isEqualTo(assigningAdminId);
                assertThat(resultSet.getInt("role_count")).isEqualTo(1);
            }
        }
    }

    @Test
    void userRolesPreventDuplicateAssignmentsAndRetainAssignmentWhenAssignerIsDeleted()
            throws Exception {
        migrate();

        String assigneeUserId = "10000000-0000-0000-0000-000000009903";
        String assigningAdminId = "10000000-0000-0000-0000-000000009904";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".users (id, email, password_hash, full_name)"
                            + " values ('"
                            + assigneeUserId
                            + "', 'service@example.test', '$2a$10$examplehash', 'Service User'),"
                            + " ('"
                            + assigningAdminId
                            + "', 'role-admin@example.test', '$2a$10$examplehash', 'Role Admin')");
            statement.executeUpdate(
                    "insert into "
                            + TEST_SCHEMA
                            + ".user_roles (user_id, role_id, assigned_by)"
                            + " select '"
                            + assigneeUserId
                            + "', id, '"
                            + assigningAdminId
                            + "' from "
                            + TEST_SCHEMA
                            + ".roles where name = 'CUSTOMER_SERVICE_AGENT'");

            assertThatThrownBy(
                            () ->
                                    statement.executeUpdate(
                                            "insert into "
                                                    + TEST_SCHEMA
                                                    + ".user_roles (user_id, role_id, assigned_by)"
                                                    + " select '"
                                                    + assigneeUserId
                                                    + "', id, '"
                                                    + assigningAdminId
                                                    + "' from "
                                                    + TEST_SCHEMA
                                                    + ".roles where name = 'CUSTOMER_SERVICE_AGENT'"))
                    .hasMessageContaining("user_roles_pkey");

            statement.executeUpdate(
                    "delete from " + TEST_SCHEMA + ".users where id = '" + assigningAdminId + "'");

            try (ResultSet resultSet =
                    statement.executeQuery(
                            "select assigned_by is null as assigner_removed from "
                                    + TEST_SCHEMA
                                    + ".user_roles where user_id = '"
                                    + assigneeUserId
                                    + "'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("assigner_removed")).isTrue();
            }
        }
    }

    @Test
    void recordsOnlyConventionCompliantMigrationScripts() throws Exception {
        migrate();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "select script from "
                                        + TEST_SCHEMA
                                        + ".flyway_schema_history where success = true"
                                        + " and type = 'SQL'")) {
            assertThat(resultSet.next()).isTrue();
            do {
                assertThat(resultSet.getString("script")).matches(VERSIONED_MIGRATION_NAME);
            } while (resultSet.next());
        }
    }

    private static MigrateResult migrate() {
        Flyway flyway =
                Flyway.configure()
                        .dataSource(DB_URL, DB_USERNAME, DB_PASSWORD)
                        .locations("classpath:db/migration")
                        .schemas(TEST_SCHEMA)
                        .defaultSchema(TEST_SCHEMA)
                        .createSchemas(true)
                        .initSql("set search_path to " + TEST_SCHEMA)
                        .load();

        return flyway.migrate();
    }

    private static MigrateResult migrateWithDemoData() {
        Flyway flyway =
                Flyway.configure()
                        .dataSource(DB_URL, DB_USERNAME, DB_PASSWORD)
                        .locations("classpath:db/migration", "classpath:db/demo")
                        .schemas(TEST_SCHEMA)
                        .defaultSchema(TEST_SCHEMA)
                        .createSchemas(true)
                        .initSql("set search_path to " + TEST_SCHEMA)
                        .load();

        return flyway.migrate();
    }

    private static void assertLocalRebuildState(Statement statement) throws Exception {
        try (ResultSet resultSet =
                statement.executeQuery(
                        "select count(*) filter (where version is not null) as versioned_count,"
                                + " max(version::int) filter (where version is not null) as latest_version,"
                                + " count(*) filter (where script = 'R__controlled_demo_data.sql')"
                                + " as demo_repeatable_count,"
                                + " count(*) filter (where not success) as failed_count"
                                + " from "
                                + TEST_SCHEMA
                                + ".flyway_schema_history")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("versioned_count")).isEqualTo(25);
            assertThat(resultSet.getInt("latest_version")).isEqualTo(25);
            assertThat(resultSet.getInt("demo_repeatable_count")).isEqualTo(1);
            assertThat(resultSet.getInt("failed_count")).isZero();
        }

        try (ResultSet resultSet =
                statement.executeQuery(
                        "select"
                                + " (select count(*) from "
                                + TEST_SCHEMA
                                + ".roles) as role_count,"
                                + " (select count(*) from "
                                + TEST_SCHEMA
                                + ".users where email like '%@bayer-westphalian.test')"
                                + " as seed_user_count,"
                                + " (select count(*) from "
                                + TEST_SCHEMA
                                + ".customers where source = 'DEMO_DATA') as demo_customer_count,"
                                + " (select count(*) from "
                                + TEST_SCHEMA
                                + ".campaigns"
                                + " where id = '50000000-0000-0000-0000-000000000101')"
                                + " as demo_campaign_count,"
                                + " (select count(*) from "
                                + TEST_SCHEMA
                                + ".audit_logs where action = 'LOAD_DEMO_DATA')"
                                + " as demo_audit_count")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("role_count")).isEqualTo(10);
            assertThat(resultSet.getInt("seed_user_count")).isEqualTo(10);
            assertThat(resultSet.getInt("demo_customer_count")).isEqualTo(2);
            assertThat(resultSet.getInt("demo_campaign_count")).isEqualTo(1);
            assertThat(resultSet.getInt("demo_audit_count")).isEqualTo(1);
        }
    }

    private static String kbRequiredTableNamesSql() {
        return KB_INITIAL_SCHEMA_TABLES.stream()
                .map(tableName -> "'" + tableName + "'")
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
    }

    private static void assertForeignKeyViolation(
            ThrowingSqlOperation operation, String constraintName) {
        assertThatThrownBy(operation::execute)
                .isInstanceOf(SQLException.class)
                .satisfies(
                        error ->
                                assertThat(((SQLException) error).getSQLState())
                                        .as(constraintName)
                                        .isEqualTo("23503"))
                .hasMessageContaining(constraintName);
    }

    private static void assertConstraintExists(
            Statement statement, String tableName, String constraintName) throws Exception {
        try (ResultSet resultSet =
                statement.executeQuery(
                        "select count(*) as constraint_count from information_schema.table_constraints"
                                + " where table_schema = '"
                                + TEST_SCHEMA
                                + "' and table_name = '"
                                + tableName
                                + "' and constraint_name = '"
                                + constraintName
                                + "'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("constraint_count")).as(constraintName).isEqualTo(1);
        }
    }

    private static void assertColumnExists(
            Statement statement, String tableName, String columnName, String expectedType)
            throws Exception {
        try (ResultSet resultSet =
                statement.executeQuery(
                        "select data_type, udt_name from information_schema.columns"
                                + " where table_schema = '"
                                + TEST_SCHEMA
                                + "' and table_name = '"
                                + tableName
                                + "' and column_name = '"
                                + columnName
                                + "'")) {
            assertThat(resultSet.next()).as(columnName).isTrue();
            assertThat(List.of(resultSet.getString("data_type"), resultSet.getString("udt_name")))
                    .as(columnName)
                    .contains(expectedType);
        }
    }

    private static void assertIndexExists(Statement statement, String indexName) throws Exception {
        try (ResultSet resultSet =
                statement.executeQuery(
                        "select count(*) as index_count from pg_indexes"
                                + " where schemaname = '"
                                + TEST_SCHEMA
                                + "' and indexname = '"
                                + indexName
                                + "'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("index_count")).as(indexName).isEqualTo(1);
        }
    }

    private static void assertForeignKeyExists(
            Statement statement,
            String tableName,
            String constraintName,
            String referencedTableName,
            String expectedDeleteRule)
            throws Exception {
        try (ResultSet resultSet =
                statement.executeQuery(
                        "select rc.delete_rule, ccu.table_name as referenced_table_name"
                                + " from information_schema.referential_constraints rc"
                                + " join information_schema.table_constraints tc"
                                + " on rc.constraint_schema = tc.constraint_schema"
                                + " and rc.constraint_name = tc.constraint_name"
                                + " join information_schema.constraint_column_usage ccu"
                                + " on rc.unique_constraint_schema = ccu.constraint_schema"
                                + " and rc.unique_constraint_name = ccu.constraint_name"
                                + " where rc.constraint_schema = '"
                                + TEST_SCHEMA
                                + "' and tc.table_schema = '"
                                + TEST_SCHEMA
                                + "' and tc.table_name = '"
                                + tableName
                                + "' and rc.constraint_name = '"
                                + constraintName
                                + "'")) {
            assertThat(resultSet.next()).as(constraintName).isTrue();
            assertThat(resultSet.getString("referenced_table_name"))
                    .as(constraintName)
                    .isEqualTo(referencedTableName);
            assertThat(resultSet.getString("delete_rule"))
                    .as(constraintName)
                    .isEqualTo(expectedDeleteRule);
        }
    }

    private static void insertCustomerProductOwnership(
            Statement statement, String customerId, String productId, String ownershipId)
            throws Exception {
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".customers (id, customer_type, first_name, last_name)"
                        + " values ('"
                        + customerId
                        + "', 'CUSTOMER', 'Payment', 'Customer')");
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".products (id, name, product_type, price, duration_months)"
                        + " values ('"
                        + productId
                        + "', 'Payment Product', 'OTHER', 50.00, 12)");
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".product_ownerships (id, customer_id, product_id, policy_number,"
                        + " start_date) values ('"
                        + ownershipId
                        + "', '"
                        + customerId
                        + "', '"
                        + productId
                        + "', 'POL-"
                        + ownershipId.substring(ownershipId.length() - 12)
                        + "', current_date)");
    }

    private static void insertCampaignProduct(
            Statement statement, String campaignId, String productId) throws Exception {
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".campaigns (id, name, objective, channel) values ('"
                        + campaignId
                        + "', 'Campaign Product Link', 'Associate campaign with product', 'EMAIL')");
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".products (id, name, product_type, price, duration_months)"
                        + " values ('"
                        + productId
                        + "', 'Campaign Product', 'OTHER', 75.00, 12)");
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".campaign_products (campaign_id, product_id) values ('"
                        + campaignId
                        + "', '"
                        + productId
                        + "')");
    }

    private static void insertCampaign(Statement statement, String campaignId, String name)
            throws Exception {
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".campaigns (id, name, objective, channel) values ('"
                        + campaignId
                        + "', '"
                        + name
                        + "', 'Track campaign performance', 'EMAIL')");
    }

    private static void insertReminderCustomerAndProduct(
            Statement statement, String customerId, String productId) throws Exception {
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".customers (id, customer_type, first_name, last_name)"
                        + " values ('"
                        + customerId
                        + "', 'CUSTOMER', 'Reminder', 'Customer')");
        statement.executeUpdate(
                "insert into "
                        + TEST_SCHEMA
                        + ".products (id, name, product_type, price, duration_months)"
                        + " values ('"
                        + productId
                        + "', 'Reminder Product', 'OTHER', 60.00, 12)");
    }

    private static void assertRoleSeed(
            Statement statement,
            String roleName,
            String displayName,
            String description,
            String allowedFunctions,
            boolean mvpRole)
            throws Exception {
        try (ResultSet resultSet =
                statement.executeQuery(
                        "select display_name, description, allowed_functions, mvp_role from "
                                + TEST_SCHEMA
                                + ".roles where name = '"
                                + roleName
                                + "'")) {
            assertThat(resultSet.next()).as(roleName).isTrue();
            assertThat(resultSet.getString("display_name")).isEqualTo(displayName);
            assertThat(resultSet.getString("description")).isEqualTo(description);
            assertThat(resultSet.getString("allowed_functions")).isEqualTo(allowedFunctions);
            assertThat(resultSet.getBoolean("mvp_role")).isEqualTo(mvpRole);
        }
    }

    private static void assertRoleSeedId(Statement statement, String roleName, String expectedId)
            throws Exception {
        try (ResultSet resultSet =
                statement.executeQuery(
                        "select id::text as role_id from "
                                + TEST_SCHEMA
                                + ".roles where name = '"
                                + roleName
                                + "'")) {
            assertThat(resultSet.next()).as(roleName).isTrue();
            assertThat(resultSet.getString("role_id")).isEqualTo(expectedId);
        }
    }

    private static void assertSeedUserRole(
            Statement statement,
            String email,
            String fullName,
            String roleName,
            String assignedByEmail)
            throws Exception {
        try (ResultSet resultSet =
                statement.executeQuery(
                        "select u.full_name, r.name::text as role_name,"
                                + " assigned_by.email as assigned_by_email"
                                + " from "
                                + TEST_SCHEMA
                                + ".users u"
                                + " join "
                                + TEST_SCHEMA
                                + ".user_roles ur on ur.user_id = u.id"
                                + " join "
                                + TEST_SCHEMA
                                + ".roles r on r.id = ur.role_id"
                                + " left join "
                                + TEST_SCHEMA
                                + ".users assigned_by on assigned_by.id = ur.assigned_by"
                                + " where u.email = '"
                                + email
                                + "'")) {
            assertThat(resultSet.next()).as(email).isTrue();
            assertThat(resultSet.getString("full_name")).isEqualTo(fullName);
            assertThat(resultSet.getString("role_name")).isEqualTo(roleName);
            assertThat(resultSet.getString("assigned_by_email")).isEqualTo(assignedByEmail);
        }
    }

    private static void dropTestSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("drop schema if exists " + TEST_SCHEMA + " cascade");
        }
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1_000);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @FunctionalInterface
    private interface ThrowingSqlOperation {
        void execute() throws SQLException;
    }
}
