package com.bayerwestphalian.campaign.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class FlywayMigrationResourceTests {

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

    @Test
    void includesInitialSchemaMigration() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        assertThat(migration.exists()).isTrue();

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create table bwc_schema_metadata")
                .contains("create table users")
                .contains("create table campaigns")
                .contains("create table audit_logs")
                .contains("Initial KB schema initialized");
    }

    @Test
    void initialSchemaMigrationCoversKbCoreTables() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(KB_INITIAL_SCHEMA_TABLES)
                .allSatisfy(table -> assertThat(sql).contains("create table " + table));
    }

    @Test
    void initialSchemaMigrationDefinesKbUsersTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type user_status as enum ('ACTIVE', 'DISABLED', 'LOCKED')")
                .contains("create table users")
                .contains("id uuid primary key")
                .contains("email varchar(255) not null unique")
                .contains("password_hash varchar(255) not null")
                .contains("full_name varchar(255) not null")
                .contains("status user_status not null default 'ACTIVE'")
                .contains("last_login_at timestamptz")
                .contains("created_at timestamptz not null default now()")
                .contains("updated_at timestamptz not null default now()")
                .contains("create table user_roles")
                .contains("user_id uuid not null references users (id) on delete cascade");
    }

    @Test
    void initialSchemaMigrationDefinesKbCustomersTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type customer_type as enum ('CUSTOMER', 'PROSPECT', 'BENEFICIARY')")
                .contains("create type customer_age_group as enum")
                .contains("create type customer_status as enum")
                .contains("create table customers")
                .contains("id uuid primary key")
                .contains("customer_type customer_type not null")
                .contains("first_name varchar(100) not null")
                .contains("last_name varchar(100) not null")
                .contains("email varchar(255)")
                .contains("phone varchar(50)")
                .contains("address_line varchar(255)")
                .contains("city varchar(100)")
                .contains("country varchar(100)")
                .contains("date_of_birth date")
                .contains("age_group customer_age_group")
                .contains("status customer_status not null default 'ACTIVE'")
                .contains("do_not_contact boolean not null default false")
                .contains("source varchar(100)")
                .contains("created_at timestamptz not null default now()")
                .contains("updated_at timestamptz not null default now()")
                .contains("deleted_at timestamptz");
    }

    @Test
    void customersMigrationAddsKbSearchAndEligibilityIndexes() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V4__enhance_customers_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create index if not exists idx_customers_email on customers (email)")
                .contains("create index if not exists idx_customers_status on customers (status)")
                .contains("create index if not exists idx_customers_city on customers (city)")
                .contains(
                        "create index if not exists idx_customers_do_not_contact on customers (do_not_contact)");
    }

    @Test
    void initialSchemaMigrationDefinesKbBeneficiariesTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create table beneficiaries")
                .contains("id uuid primary key")
                .contains("policyholder_customer_id uuid not null references customers (id) on delete cascade")
                .contains("beneficiary_customer_id uuid not null references customers (id) on delete cascade")
                .contains("relationship varchar(100) not null")
                .contains("guardian_name varchar(255)")
                .contains("guardian_email varchar(255)")
                .contains("guardian_consent_required boolean not null default false")
                .contains("created_at timestamptz not null default now()")
                .contains("constraint beneficiaries_distinct_customers")
                .contains("check (policyholder_customer_id <> beneficiary_customer_id)")
                .contains("constraint beneficiaries_unique_link")
                .contains("unique (policyholder_customer_id, beneficiary_customer_id)");
    }

    @Test
    void beneficiariesMigrationAddsKbLookupAndEligibilityIndexes() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V5__enhance_beneficiaries_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create index if not exists idx_beneficiaries_policyholder_customer")
                .contains("on beneficiaries (policyholder_customer_id)")
                .contains("create index if not exists idx_beneficiaries_beneficiary_customer")
                .contains("on beneficiaries (beneficiary_customer_id)")
                .contains("create index if not exists idx_beneficiaries_guardian_consent_required")
                .contains("on beneficiaries (guardian_consent_required)");
    }

    @Test
    void initialSchemaMigrationDefinesKbConsentRecordsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type consent_type as enum")
                .contains("'MARKETING_EMAIL'")
                .contains("'MARKETING_PHONE'")
                .contains("'MARKETING_SMS'")
                .contains("'GUARDIAN'")
                .contains("'DATA_PROCESSING'")
                .contains("create type consent_status as enum")
                .contains("'GIVEN'")
                .contains("'WITHDRAWN'")
                .contains("'REQUIRED'")
                .contains("'EXPIRED'")
                .contains("'REJECTED'")
                .contains("create table consent_records")
                .contains("id uuid primary key")
                .contains("customer_id uuid not null references customers (id) on delete cascade")
                .contains("consent_type consent_type not null")
                .contains("status consent_status not null")
                .contains("purpose text not null")
                .contains("source varchar(100)")
                .contains("granted_at timestamptz")
                .contains("withdrawn_at timestamptz")
                .contains("expires_at timestamptz")
                .contains("evidence_file_url text")
                .contains("created_by uuid references users (id) on delete set null")
                .contains("created_at timestamptz not null default now()");
    }

    @Test
    void consentRecordsMigrationAddsKbLookupIndexesAndTemporalChecks() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V6__enhance_consent_records_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table consent_records")
                .contains("add constraint consent_records_expiration_after_grant")
                .contains("check (expires_at is null or granted_at is null or expires_at > granted_at)")
                .contains("add constraint consent_records_withdrawal_after_grant")
                .contains("check (withdrawn_at is null or granted_at is null or withdrawn_at >= granted_at)")
                .contains("create index if not exists idx_consent_records_customer")
                .contains("on consent_records (customer_id)")
                .contains("create index if not exists idx_consent_records_customer_type_status")
                .contains("on consent_records (customer_id, consent_type, status)")
                .contains("create index if not exists idx_consent_records_status")
                .contains("on consent_records (status)")
                .contains("create index if not exists idx_consent_records_expires_at")
                .contains("on consent_records (expires_at)")
                .contains("create index if not exists idx_consent_records_created_by")
                .contains("on consent_records (created_by)");
    }

    @Test
    void initialSchemaMigrationDefinesKbProductsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type product_type as enum")
                .contains("'HOMEOWNER_INSURANCE'")
                .contains("'LIFE_INSURANCE'")
                .contains("'INVESTMENT_FUND'")
                .contains("'HEALTH_INSURANCE'")
                .contains("'AUTO_INSURANCE'")
                .contains("'OTHER'")
                .contains("create table products")
                .contains("id uuid primary key")
                .contains("name varchar(255) not null")
                .contains("product_type product_type not null")
                .contains("description text")
                .contains("price numeric(12, 2)")
                .contains("duration_months integer")
                .contains("expiration_policy varchar(100)")
                .contains("active boolean not null default true")
                .contains("created_at timestamptz not null default now()")
                .contains("updated_at timestamptz not null default now()")
                .contains("deleted_at timestamptz");
    }

    @Test
    void productsMigrationAddsKbIndexesAndValueChecks() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V7__enhance_products_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table products")
                .contains("add constraint products_price_non_negative")
                .contains("check (price is null or price >= 0)")
                .contains("add constraint products_duration_positive")
                .contains("check (duration_months is null or duration_months > 0)")
                .contains("create index if not exists idx_products_type on products (product_type)")
                .contains("create index if not exists idx_products_active on products (active)")
                .contains("create index if not exists idx_products_name on products (name)")
                .contains("create index if not exists idx_products_deleted_at on products (deleted_at)");
    }

    @Test
    void initialSchemaMigrationDefinesKbProductOwnershipsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type ownership_status as enum ('ACTIVE', 'EXPIRED', 'CANCELLED')")
                .contains("create table product_ownerships")
                .contains("id uuid primary key")
                .contains("customer_id uuid not null references customers (id) on delete cascade")
                .contains("product_id uuid not null references products (id) on delete restrict")
                .contains("policy_number varchar(100)")
                .contains("start_date date not null")
                .contains("expiration_date date")
                .contains("status ownership_status not null default 'ACTIVE'")
                .contains("created_at timestamptz not null default now()")
                .contains("constraint product_ownerships_policy_number_unique unique (policy_number)")
                .contains("create index product_ownerships_customer_idx on product_ownerships (customer_id)");
    }

    @Test
    void productOwnershipsMigrationAddsKbIndexesAndDateChecks() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V8__enhance_product_ownerships_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table product_ownerships")
                .contains("add constraint product_ownerships_expiration_after_start")
                .contains("check (expiration_date is null or expiration_date >= start_date)")
                .contains("create index if not exists idx_product_ownership_expiration")
                .contains("on product_ownerships (expiration_date)")
                .contains("create index if not exists idx_product_ownerships_product")
                .contains("on product_ownerships (product_id)")
                .contains("create index if not exists idx_product_ownerships_status")
                .contains("on product_ownerships (status)")
                .contains("create index if not exists idx_product_ownerships_customer_status")
                .contains("on product_ownerships (customer_id, status)");
    }

    @Test
    void initialSchemaMigrationDefinesKbProductChangeRequestsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type product_change_type as enum")
                .contains("'PRICE_CHANGE'")
                .contains("'DURATION_CHANGE'")
                .contains("'EXPIRATION_RULE_CHANGE'")
                .contains("'STATUS_CHANGE'")
                .contains("create type product_change_status as enum")
                .contains("'OPEN'")
                .contains("'APPROVED'")
                .contains("'REJECTED'")
                .contains("'IMPLEMENTED'")
                .contains("create table product_change_requests")
                .contains("id uuid primary key")
                .contains("product_id uuid not null references products (id) on delete cascade")
                .contains("requested_by uuid references users (id) on delete set null")
                .contains("request_type product_change_type not null")
                .contains("description text not null")
                .contains("status product_change_status not null default 'OPEN'")
                .contains("created_at timestamptz not null default now()")
                .contains("updated_at timestamptz not null default now()");
    }

    @Test
    void productChangeRequestsMigrationAddsKbWorkflowIndexesAndValidation() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V9__enhance_product_change_requests_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table product_change_requests")
                .contains("add constraint product_change_requests_description_not_blank")
                .contains("check (length(trim(description)) > 0)")
                .contains("create index if not exists idx_product_change_requests_status")
                .contains("on product_change_requests (status)")
                .contains("create index if not exists idx_product_change_requests_product")
                .contains("on product_change_requests (product_id)")
                .contains("create index if not exists idx_product_change_requests_requested_by")
                .contains("on product_change_requests (requested_by)")
                .contains("create index if not exists idx_product_change_requests_product_status")
                .contains("on product_change_requests (product_id, status)");
    }

    @Test
    void initialSchemaMigrationDefinesKbPaymentRecordsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type payment_status as enum ('DUE', 'PAID', 'OVERDUE', 'DEFAULT_RISK')")
                .contains("create table payment_records")
                .contains("id uuid primary key")
                .contains("customer_id uuid not null references customers (id) on delete cascade")
                .contains(
                        "product_ownership_id uuid not null references product_ownerships (id) on delete cascade")
                .contains("due_date date not null")
                .contains("paid_at timestamptz")
                .contains("amount_due numeric(12, 2) not null")
                .contains("amount_paid numeric(12, 2)")
                .contains("status payment_status not null default 'DUE'")
                .contains("reminder_count integer not null default 0")
                .contains("create index payment_records_due_status_idx on payment_records (due_date, status)");
    }

    @Test
    void paymentRecordsMigrationAddsKbIndexesAndValueChecks() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V10__enhance_payment_records_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table payment_records")
                .contains("add constraint payment_records_amount_due_non_negative")
                .contains("check (amount_due >= 0)")
                .contains("add constraint payment_records_amount_paid_non_negative")
                .contains("check (amount_paid is null or amount_paid >= 0)")
                .contains("add constraint payment_records_reminder_count_non_negative")
                .contains("check (reminder_count >= 0)")
                .contains("create index if not exists idx_payment_records_customer")
                .contains("on payment_records (customer_id)")
                .contains("create index if not exists idx_payment_records_ownership")
                .contains("on payment_records (product_ownership_id)")
                .contains("create index if not exists idx_payment_records_status")
                .contains("on payment_records (status)")
                .contains("create index if not exists idx_payment_records_customer_status")
                .contains("on payment_records (customer_id, status)");
    }

    @Test
    void initialSchemaMigrationDefinesKbSegmentsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type segment_visibility as enum ('PRIVATE', 'TEAM', 'GLOBAL')")
                .contains("create table segments")
                .contains("id uuid primary key")
                .contains("name varchar(255) not null")
                .contains("description text")
                .contains("owner_user_id uuid references users (id) on delete set null")
                .contains("visibility segment_visibility not null default 'PRIVATE'")
                .contains("created_at timestamptz not null default now()")
                .contains("updated_at timestamptz not null default now()");
    }

    @Test
    void segmentsMigrationAddsKbLookupIndexesAndValidation() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V11__enhance_segments_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table segments")
                .contains("add constraint segments_name_not_blank")
                .contains("check (length(trim(name)) > 0)")
                .contains("add constraint segments_updated_at_after_created_at")
                .contains("check (updated_at >= created_at)")
                .contains("create index if not exists idx_segments_owner_user")
                .contains("on segments (owner_user_id)")
                .contains("create index if not exists idx_segments_visibility")
                .contains("on segments (visibility)")
                .contains("create index if not exists idx_segments_owner_visibility")
                .contains("on segments (owner_user_id, visibility)")
                .contains("create index if not exists idx_segments_name")
                .contains("on segments (name)");
    }

    @Test
    void initialSchemaMigrationDefinesKbSegmentCriteriaTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type segment_operator as enum")
                .contains("'EQUALS'")
                .contains("'NOT_EQUALS'")
                .contains("'CONTAINS'")
                .contains("'IN'")
                .contains("'BETWEEN'")
                .contains("'BEFORE'")
                .contains("'AFTER'")
                .contains("create type segment_join_operator as enum ('AND', 'OR')")
                .contains("create table segment_criteria")
                .contains("id uuid primary key")
                .contains("segment_id uuid not null references segments (id) on delete cascade")
                .contains("field_name varchar(100) not null")
                .contains("operator segment_operator not null")
                .contains("value text not null")
                .contains("logical_group varchar(50)")
                .contains("join_operator segment_join_operator not null default 'AND'");
    }

    @Test
    void initialSchemaMigrationDefinesKbCampaignRecipientsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type campaign_recipient_status as enum")
                .contains("'ELIGIBLE'")
                .contains("'EXCLUDED'")
                .contains("'SENT'")
                .contains("'OPENED'")
                .contains("'CLICKED'")
                .contains("'REPLIED'")
                .contains("'CONVERTED'")
                .contains("'FAILED'")
                .contains("create table campaign_recipients")
                .contains("id uuid primary key")
                .contains("campaign_id uuid not null references campaigns (id) on delete cascade")
                .contains("customer_id uuid not null references customers (id) on delete cascade")
                .contains("eligibility_status campaign_recipient_status not null")
                .contains("exclusion_reason text")
                .contains("eligibility_explanation text")
                .contains("sent_at timestamptz")
                .contains("opened_at timestamptz")
                .contains("clicked_at timestamptz")
                .contains("converted_at timestamptz")
                .contains("created_at timestamptz not null default now()")
                .contains("constraint campaign_recipients_campaign_customer_unique")
                .contains("unique (campaign_id, customer_id)")
                .contains(
                        "create index campaign_recipients_status_idx"
                                + " on campaign_recipients (campaign_id, eligibility_status)");
    }

    @Test
    void initialSchemaMigrationDefinesKbCampaignProductsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create table campaign_products")
                .contains("campaign_id uuid not null references campaigns (id) on delete cascade")
                .contains("product_id uuid not null references products (id) on delete restrict")
                .contains("primary key (campaign_id, product_id)");
    }

    @Test
    void initialSchemaMigrationDefinesKbCampaignsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type campaign_status as enum")
                .contains("'DRAFT'")
                .contains("'SUBMITTED'")
                .contains("'APPROVED'")
                .contains("'REJECTED'")
                .contains("'ACTIVE'")
                .contains("'PAUSED'")
                .contains("'COMPLETED'")
                .contains("'ARCHIVED'")
                .contains("create type campaign_channel as enum ('EMAIL', 'PHONE', 'SMS', 'MIXED')")
                .contains("create table campaigns")
                .contains("id uuid primary key")
                .contains("name varchar(255) not null")
                .contains("objective text not null")
                .contains("status campaign_status not null default 'DRAFT'")
                .contains("owner_user_id uuid references users (id) on delete set null")
                .contains("segment_id uuid references segments (id) on delete set null")
                .contains("channel campaign_channel not null")
                .contains("message_subject varchar(255)")
                .contains("message_body text")
                .contains("start_date date")
                .contains("end_date date")
                .contains("approved_by uuid references users (id) on delete set null")
                .contains("approved_at timestamptz")
                .contains("rejection_reason text")
                .contains("created_at timestamptz not null default now()")
                .contains("updated_at timestamptz not null default now()")
                .contains("create index campaigns_status_idx on campaigns (status)");
    }

    @Test
    void initialSchemaMigrationDefinesKbFollowUpTasksTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type follow_up_status as enum ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')")
                .contains("create type work_priority as enum ('LOW', 'MEDIUM', 'HIGH')")
                .contains("create table follow_up_tasks")
                .contains("id uuid primary key")
                .contains("customer_id uuid not null references customers (id) on delete cascade")
                .contains("campaign_id uuid references campaigns (id) on delete set null")
                .contains("assigned_to uuid references users (id) on delete set null")
                .contains("title varchar(255) not null")
                .contains("description text")
                .contains("due_date date")
                .contains("status follow_up_status not null default 'OPEN'")
                .contains("priority work_priority not null default 'MEDIUM'")
                .contains("created_at timestamptz not null default now()")
                .contains("completed_at timestamptz")
                .contains(
                        "create index follow_up_tasks_assignee_status_idx"
                                + " on follow_up_tasks (assigned_to, status)");
    }

    @Test
    void initialSchemaMigrationDefinesKbReminderSchedulesTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type reminder_type as enum ('PAYMENT_DUE', 'PRODUCT_EXPIRATION')")
                .contains("create type reminder_level as enum ('GREEN', 'YELLOW', 'RED')")
                .contains("create type reminder_status as enum ('PENDING', 'SENT', 'FAILED', 'CANCELLED')")
                .contains("create table reminder_schedules")
                .contains("id uuid primary key")
                .contains("customer_id uuid not null references customers (id) on delete cascade")
                .contains("product_id uuid not null references products (id) on delete restrict")
                .contains("reminder_type reminder_type not null")
                .contains("reminder_level reminder_level not null")
                .contains("scheduled_date date not null")
                .contains("status reminder_status not null default 'PENDING'")
                .contains("created_at timestamptz not null default now()")
                .contains("sent_at timestamptz")
                .contains(
                        "create index reminder_schedules_date_status_idx"
                                + " on reminder_schedules (scheduled_date, status)");
    }

    @Test
    void initialSchemaMigrationDefinesKbCampaignMetricsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create table campaign_metrics")
                .contains("id uuid primary key")
                .contains("campaign_id uuid not null unique references campaigns (id) on delete cascade")
                .contains("audience_size integer not null default 0")
                .contains("eligible_count integer not null default 0")
                .contains("excluded_count integer not null default 0")
                .contains("sent_count integer not null default 0")
                .contains("opened_count integer not null default 0")
                .contains("clicked_count integer not null default 0")
                .contains("replied_count integer not null default 0")
                .contains("converted_count integer not null default 0")
                .contains("estimated_cost numeric(12, 2)")
                .contains("estimated_revenue numeric(12, 2)")
                .contains("estimated_roi numeric(12, 2)")
                .contains("updated_at timestamptz not null default now()");
    }

    @Test
    void initialSchemaMigrationDefinesKbAuditLogsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create table audit_logs")
                .contains("id uuid primary key")
                .contains("actor_user_id uuid references users (id) on delete set null")
                .contains("action varchar(255) not null")
                .contains("entity_type varchar(100) not null")
                .contains("entity_id uuid")
                .contains("old_value jsonb")
                .contains("new_value jsonb")
                .contains("ip_address varchar(100)")
                .contains("created_at timestamptz not null default now()")
                .contains("create index audit_logs_entity_idx on audit_logs (entity_type, entity_id)")
                .contains(
                        "create index audit_logs_actor_created_idx"
                                + " on audit_logs (actor_user_id, created_at)");
    }

    @Test
    void initialSchemaMigrationDefinesKbReportExportsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type report_export_type as enum ('CSV', 'PDF')")
                .contains("create type report_export_status as enum ('REQUESTED', 'COMPLETED', 'FAILED')")
                .contains("create table report_exports")
                .contains("id uuid primary key")
                .contains("requested_by uuid references users (id) on delete set null")
                .contains("report_name varchar(255) not null")
                .contains("export_type report_export_type not null")
                .contains("status report_export_status not null default 'REQUESTED'")
                .contains("file_url text")
                .contains("requested_at timestamptz not null default now()")
                .contains("completed_at timestamptz");
    }

    @Test
    void initialSchemaMigrationDefinesKbAiRecommendationsTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create type ai_recommendation_type as enum")
                .contains("'PRODUCT'")
                .contains("'SEGMENT'")
                .contains("'COPY'")
                .contains("'RISK'")
                .contains("'DUPLICATE_WARNING'")
                .contains("create table ai_recommendations")
                .contains("id uuid primary key")
                .contains("recommendation_type ai_recommendation_type not null")
                .contains("target_entity_type varchar(100) not null")
                .contains("target_entity_id uuid")
                .contains("input_summary text not null")
                .contains("recommendation text not null")
                .contains("explanation text not null")
                .contains("confidence_score numeric(5, 2)")
                .contains("approved_by_user_id uuid references users (id) on delete set null")
                .contains("created_at timestamptz not null default now()");
    }

    @Test
    void aiRecommendationsMigrationAddsKbLookupIndexesAndValidation() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V13__enhance_ai_recommendations_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table ai_recommendations")
                .contains("add constraint ai_recommendations_target_entity_type_not_blank")
                .contains("add constraint ai_recommendations_input_summary_not_blank")
                .contains("add constraint ai_recommendations_recommendation_not_blank")
                .contains("add constraint ai_recommendations_explanation_not_blank")
                .contains("add constraint ai_recommendations_confidence_score_range")
                .contains("create index if not exists idx_ai_recommendations_type")
                .contains("on ai_recommendations (recommendation_type)")
                .contains("create index if not exists idx_ai_recommendations_target")
                .contains("on ai_recommendations (target_entity_type, target_entity_id)")
                .contains("create index if not exists idx_ai_recommendations_approved_by")
                .contains("on ai_recommendations (approved_by_user_id)")
                .contains("create index if not exists idx_ai_recommendations_created_at")
                .contains("on ai_recommendations (created_at)");
    }

    @Test
    void segmentCriteriaMigrationAddsKbLookupIndexesAndValidation() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V12__enhance_segment_criteria_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table segment_criteria")
                .contains("add constraint segment_criteria_field_name_not_blank")
                .contains("check (length(trim(field_name)) > 0)")
                .contains("add constraint segment_criteria_value_not_blank")
                .contains("check (length(trim(value)) > 0)")
                .contains("add constraint segment_criteria_logical_group_not_blank")
                .contains("check (logical_group is null or length(trim(logical_group)) > 0)")
                .contains("create index if not exists idx_segment_criteria_segment")
                .contains("on segment_criteria (segment_id)")
                .contains("create index if not exists idx_segment_criteria_field_name")
                .contains("on segment_criteria (field_name)")
                .contains("create index if not exists idx_segment_criteria_operator")
                .contains("on segment_criteria (operator)")
                .contains("create index if not exists idx_segment_criteria_segment_field")
                .contains("on segment_criteria (segment_id, field_name)");
    }

    @Test
    void initialSchemaMigrationDefinesKbUserRolesTable() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create table user_roles")
                .contains("user_id uuid not null references users (id) on delete cascade")
                .contains("role_id uuid not null references roles (id) on delete restrict")
                .contains("assigned_at timestamptz not null default now()")
                .contains("primary key (user_id, role_id)");
    }

    @Test
    void userRolesMigrationAddsKbAssignmentTraceability() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V3__enhance_user_roles_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table user_roles")
                .contains("add column assigned_by uuid references users (id) on delete set null")
                .contains("create index user_roles_assigned_by_idx on user_roles (assigned_by)");
    }

    @Test
    void foreignKeyMigrationAddsKbRelationshipConstraints() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V14__add_kb_foreign_key_constraints.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table user_roles")
                .contains("foreign key (user_id) references users (id) on delete cascade")
                .contains("foreign key (role_id) references roles (id) on delete restrict")
                .contains("foreign key (assigned_by) references users (id) on delete set null")
                .contains("alter table beneficiaries")
                .contains(
                        "foreign key (policyholder_customer_id) references customers (id)"
                                + " on delete cascade")
                .contains(
                        "foreign key (beneficiary_customer_id) references customers (id)"
                                + " on delete cascade")
                .contains("alter table consent_records")
                .contains("foreign key (customer_id) references customers (id) on delete cascade")
                .contains("foreign key (created_by) references users (id) on delete set null")
                .contains("alter table product_ownerships")
                .contains("foreign key (product_id) references products (id) on delete restrict")
                .contains("alter table product_change_requests")
                .contains("foreign key (requested_by) references users (id) on delete set null")
                .contains("alter table payment_records")
                .contains(
                        "foreign key (product_ownership_id) references product_ownerships (id)"
                                + " on delete cascade")
                .contains("alter table campaigns")
                .contains("foreign key (segment_id) references segments (id) on delete set null")
                .contains("foreign key (approved_by) references users (id) on delete set null")
                .contains("alter table campaign_products")
                .contains("foreign key (campaign_id) references campaigns (id) on delete cascade")
                .contains("alter table contact_events")
                .contains("alter table follow_up_tasks")
                .contains("alter table reminder_schedules")
                .contains("alter table campaign_metrics")
                .contains("alter table report_exports")
                .contains("alter table audit_logs")
                .contains("alter table ai_recommendations");
    }

    @Test
    void searchFilterIndexMigrationAddsKbLookupIndexes() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V15__add_kb_search_filter_indexes.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("create index if not exists idx_users_full_name")
                .contains("on users (full_name)")
                .contains("create index if not exists idx_users_status")
                .contains("on users (status)")
                .contains("create index if not exists idx_user_roles_role")
                .contains("on user_roles (role_id)")
                .contains("create index if not exists idx_customers_search_name")
                .contains("on customers (last_name, first_name, email)")
                .contains("create index if not exists idx_customers_phone")
                .contains("create index if not exists idx_customers_type_status")
                .contains("on customers (customer_type, status)")
                .contains("create index if not exists idx_customers_status_deleted")
                .contains("on customers (status, deleted_at)")
                .contains("create index if not exists idx_products_name_type_active")
                .contains("on products (name, product_type, active)")
                .contains("create index if not exists idx_product_ownerships_status_expiration")
                .contains("on product_ownerships (status, expiration_date)")
                .contains("create index if not exists idx_campaigns_owner_status")
                .contains("on campaigns (owner_user_id, status)")
                .contains("create index if not exists idx_campaigns_status_dates")
                .contains("on campaigns (status, start_date, end_date)")
                .contains("create index if not exists idx_campaign_products_product")
                .contains("on campaign_products (product_id)")
                .contains("create index if not exists idx_campaign_recipients_customer")
                .contains("on campaign_recipients (customer_id)")
                .contains("create index if not exists idx_contact_events_campaign")
                .contains("on contact_events (campaign_id)")
                .contains("create index if not exists idx_follow_up_tasks_status_due")
                .contains("on follow_up_tasks (status, due_date)")
                .contains("create index if not exists idx_reminder_schedules_customer_date")
                .contains("on reminder_schedules (customer_id, scheduled_date)")
                .contains("create index if not exists idx_reminder_schedules_product_date")
                .contains("on reminder_schedules (product_id, scheduled_date)")
                .contains("create index if not exists idx_reminder_schedules_status_date")
                .contains("on reminder_schedules (status, scheduled_date)")
                .contains("create index if not exists idx_report_exports_status_requested")
                .contains("on report_exports (status, requested_at)");
    }

    @Test
    void rolesMigrationAddsKbRoleMetadataAndSeedData() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V2__enhance_roles_table.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("alter table roles")
                .contains("add column display_name varchar(100)")
                .contains("add column allowed_functions text")
                .contains("add column mvp_role boolean not null default false")
                .contains("alter column display_name set not null")
                .contains("alter column description set not null")
                .contains("alter column allowed_functions set not null")
                .contains("where name = 'ADMIN'")
                .contains("where name = 'CAMPAIGN_MANAGER'")
                .contains("where name = 'BI_ANALYST'")
                .contains("where name = 'PRODUCT_MANAGER'")
                .contains("where name = 'COMPLIANCE_OFFICER'")
                .contains("where name = 'CUSTOMER_SERVICE_AGENT'")
                .contains("where name = 'SALES_AGENT'")
                .contains("where name = 'MARKETING_ANALYST'")
                .contains("where name = 'EXECUTIVE_VIEWER'")
                .contains("where name = 'SYSTEM_AUDITOR'")
                .contains("Manages users, roles, settings, and full system configuration")
                .contains("View read-only dashboards, ROI, campaign summaries, and product performance reports");
    }

    @Test
    void roleSeedMigrationUpsertsKbRoles() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V16__seed_kb_roles.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("insert into roles")
                .contains("on conflict (name) do update")
                .contains("updated_at = now()")
                .contains("'00000000-0000-0000-0000-000000000001'")
                .contains("'ADMIN'")
                .contains("'CAMPAIGN_MANAGER'")
                .contains("'BI_ANALYST'")
                .contains("'PRODUCT_MANAGER'")
                .contains("'COMPLIANCE_OFFICER'")
                .contains("'CUSTOMER_SERVICE_AGENT'")
                .contains("'SALES_AGENT'")
                .contains("'MARKETING_ANALYST'")
                .contains("'EXECUTIVE_VIEWER'")
                .contains("'SYSTEM_AUDITOR'")
                .contains("Manage users, assign roles, manage settings, view all modules")
                .contains("Create/edit campaigns, define segments, preview recipients")
                .contains("View audit logs, consent history, campaign approval history");
    }

    @Test
    void mvpRoleUserSeedMigrationUpsertsUsersAndAssignments() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V17__seed_mvp_role_users.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("insert into users")
                .contains("on conflict (email) do update")
                .contains("insert into user_roles")
                .contains("on conflict (user_id, role_id) do update")
                .contains("admin@bayer-westphalian.test")
                .contains("campaign.manager@bayer-westphalian.test")
                .contains("bi.analyst@bayer-westphalian.test")
                .contains("product.manager@bayer-westphalian.test")
                .contains("compliance.officer@bayer-westphalian.test")
                .contains("customer.service@bayer-westphalian.test")
                .contains("'ADMIN'::system_role_name")
                .contains("'CAMPAIGN_MANAGER'::system_role_name")
                .contains("'BI_ANALYST'::system_role_name")
                .contains("'PRODUCT_MANAGER'::system_role_name")
                .contains("'COMPLIANCE_OFFICER'::system_role_name")
                .contains("'CUSTOMER_SERVICE_AGENT'::system_role_name")
                .contains("status = excluded.status")
                .contains("assigned_by = excluded.assigned_by");
    }

    @Test
    void devAndTestProfilesIncludeDemoDataButProductionDoesNot() throws Exception {
        String baseConfig =
                new ClassPathResource("application.yml").getContentAsString(StandardCharsets.UTF_8);
        String devConfig =
                new ClassPathResource("application-dev.yml")
                        .getContentAsString(StandardCharsets.UTF_8);
        String testConfig =
                new ClassPathResource("application-test.yml")
                        .getContentAsString(StandardCharsets.UTF_8);
        String prodConfig =
                new ClassPathResource("application-prod.yml")
                        .getContentAsString(StandardCharsets.UTF_8);

        assertThat(baseConfig).contains("locations: classpath:db/migration");
        assertThat(devConfig).contains("locations: classpath:db/migration,classpath:db/demo");
        assertThat(testConfig).contains("locations: classpath:db/migration,classpath:db/demo");
        assertThat(prodConfig).doesNotContain("classpath:db/demo");
    }

    @Test
    void controlledDemoDataCoversKbDevTestWorkflows() throws Exception {
        ClassPathResource migration =
                new ClassPathResource("db/demo/R__controlled_demo_data.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("DEMO_DATA")
                .contains("on conflict (id) do update")
                .contains("insert into customers")
                .contains("insert into beneficiaries")
                .contains("insert into products")
                .contains("insert into product_ownerships")
                .contains("insert into payment_records")
                .contains("insert into consent_records")
                .contains("insert into segments")
                .contains("insert into segment_criteria")
                .contains("insert into campaigns")
                .contains("insert into campaign_products")
                .contains("insert into campaign_recipients")
                .contains("insert into contact_events")
                .contains("insert into follow_up_tasks")
                .contains("insert into reminder_schedules")
                .contains("insert into campaign_metrics")
                .contains("insert into report_exports")
                .contains("insert into ai_recommendations")
                .contains("insert into audit_logs")
                .contains("'LOAD_DEMO_DATA'")
                .contains("\"scope\":\"dev-test-only\"");
    }

    @Test
    void localRebuildUsesVersionedMigrationsPlusRepeatableDemoData() throws Exception {
        Resource[] productionMigrations =
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:db/migration/*.sql");
        Resource[] demoMigrations =
                new PathMatchingResourcePatternResolver().getResources("classpath*:db/demo/*.sql");

        assertThat(Arrays.stream(productionMigrations).map(Resource::getFilename))
                .allSatisfy(filename -> assertThat(filename).matches(VERSIONED_MIGRATION_NAME))
                .contains(
                        "V1__create_initial_schema.sql",
                        "V16__seed_kb_roles.sql",
                        "V17__seed_mvp_role_users.sql")
                .doesNotContain("R__controlled_demo_data.sql");

        assertThat(Arrays.stream(demoMigrations).map(Resource::getFilename))
                .containsExactly("R__controlled_demo_data.sql");

        String demoSql =
                new ClassPathResource("db/demo/R__controlled_demo_data.sql")
                        .getContentAsString(StandardCharsets.UTF_8);

        assertThat(demoSql)
                .contains("on conflict (id) do update")
                .contains("DEMO_DATA")
                .contains("'LOAD_DEMO_DATA'");
    }

    @Test
    void migrationFilesFollowNamingConvention() throws Exception {
        Resource[] migrations =
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:db/migration/*.sql");

        assertThat(migrations).isNotEmpty();
        assertThat(Arrays.stream(migrations).map(Resource::getFilename))
                .allSatisfy(
                        filename ->
                                assertThat(filename)
                                        .as("Flyway migration name")
                                        .matches(VERSIONED_MIGRATION_NAME));
    }

    @Test
    void migrationVersionsAreUnique() throws Exception {
        Resource[] migrations =
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:db/migration/*.sql");

        assertThat(Arrays.stream(migrations).map(Resource::getFilename).map(this::versionFrom))
                .doesNotHaveDuplicates();
    }

    @Test
    void productionMigrationsCoverEmptyDatabaseBootstrapThroughLatestVersion() throws Exception {
        Resource[] migrations =
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:db/migration/*.sql");

        assertThat(Arrays.stream(migrations).map(Resource::getFilename))
                .contains(
                        "V1__create_initial_schema.sql",
                        "V14__add_kb_foreign_key_constraints.sql",
                        "V15__add_kb_search_filter_indexes.sql",
                        "V16__seed_kb_roles.sql",
                        "V17__seed_mvp_role_users.sql")
                .doesNotContain("R__controlled_demo_data.sql");

        assertThat(
                        Arrays.stream(migrations)
                                .map(Resource::getFilename)
                                .map(this::versionFrom)
                                .mapToInt(Integer::parseInt)
                                .max())
                .hasValue(17);
    }

    @Test
    void rejectsInvalidMigrationNames() {
        assertThat("V1_create_missing_separator.sql").doesNotMatch(VERSIONED_MIGRATION_NAME);
        assertThat("V01__uses_leading_zero.sql").doesNotMatch(VERSIONED_MIGRATION_NAME);
        assertThat("V2__Uses_Uppercase.sql").doesNotMatch(VERSIONED_MIGRATION_NAME);
        assertThat("V3__uses-hyphen.sql").doesNotMatch(VERSIONED_MIGRATION_NAME);
        assertThat("V4__uses space.sql").doesNotMatch(VERSIONED_MIGRATION_NAME);
        assertThat("R__repeatable_seed_data.sql").doesNotMatch(VERSIONED_MIGRATION_NAME);
    }

    private String versionFrom(String filename) {
        return VERSIONED_MIGRATION_NAME.matcher(filename).replaceAll("$1");
    }
}
