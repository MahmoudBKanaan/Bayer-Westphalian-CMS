create type user_status as enum ('ACTIVE', 'DISABLED', 'LOCKED');
create type system_role_name as enum (
    'ADMIN',
    'CAMPAIGN_MANAGER',
    'BI_ANALYST',
    'PRODUCT_MANAGER',
    'COMPLIANCE_OFFICER',
    'CUSTOMER_SERVICE_AGENT',
    'SALES_AGENT',
    'MARKETING_ANALYST',
    'EXECUTIVE_VIEWER',
    'SYSTEM_AUDITOR'
);
create type customer_type as enum ('CUSTOMER', 'PROSPECT', 'BENEFICIARY');
create type customer_age_group as enum ('MINOR', '18_25', '26_40', '41_60', '60_PLUS');
create type customer_status as enum (
    'ACTIVE',
    'INACTIVE',
    'INTERESTED',
    'UNINTERESTED',
    'CONVERTED'
);
create type consent_type as enum (
    'MARKETING_EMAIL',
    'MARKETING_PHONE',
    'MARKETING_SMS',
    'GUARDIAN',
    'DATA_PROCESSING'
);
create type consent_status as enum ('GIVEN', 'WITHDRAWN', 'REQUIRED', 'EXPIRED', 'REJECTED');
create type product_type as enum (
    'HOMEOWNER_INSURANCE',
    'LIFE_INSURANCE',
    'INVESTMENT_FUND',
    'HEALTH_INSURANCE',
    'AUTO_INSURANCE',
    'OTHER'
);
create type ownership_status as enum ('ACTIVE', 'EXPIRED', 'CANCELLED');
create type product_change_type as enum (
    'PRICE_CHANGE',
    'DURATION_CHANGE',
    'EXPIRATION_RULE_CHANGE',
    'STATUS_CHANGE'
);
create type product_change_status as enum ('OPEN', 'APPROVED', 'REJECTED', 'IMPLEMENTED');
create type payment_status as enum ('DUE', 'PAID', 'OVERDUE', 'DEFAULT_RISK');
create type segment_visibility as enum ('PRIVATE', 'TEAM', 'GLOBAL');
create type segment_operator as enum (
    'EQUALS',
    'NOT_EQUALS',
    'CONTAINS',
    'IN',
    'BETWEEN',
    'BEFORE',
    'AFTER'
);
create type segment_join_operator as enum ('AND', 'OR');
create type campaign_status as enum (
    'DRAFT',
    'SUBMITTED',
    'APPROVED',
    'REJECTED',
    'ACTIVE',
    'PAUSED',
    'COMPLETED',
    'ARCHIVED'
);
create type campaign_channel as enum ('EMAIL', 'PHONE', 'SMS', 'MIXED');
create type campaign_recipient_status as enum (
    'ELIGIBLE',
    'EXCLUDED',
    'SENT',
    'OPENED',
    'CLICKED',
    'REPLIED',
    'CONVERTED',
    'FAILED'
);
create type communication_channel as enum ('EMAIL', 'SMS', 'PHONE', 'IN_APP');
create type contact_event_type as enum (
    'SENT',
    'OPENED',
    'CLICKED',
    'REPLIED',
    'FAILED',
    'UNSUBSCRIBED',
    'CALLED',
    'NOTE'
);
create type contact_outcome as enum (
    'INTERESTED',
    'NOT_INTERESTED',
    'CONVERTED',
    'NO_RESPONSE',
    'FAILED'
);
create type follow_up_status as enum ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
create type work_priority as enum ('LOW', 'MEDIUM', 'HIGH');
create type reminder_type as enum ('PAYMENT_DUE', 'PRODUCT_EXPIRATION');
create type reminder_level as enum ('GREEN', 'YELLOW', 'RED');
create type reminder_status as enum ('PENDING', 'SENT', 'FAILED', 'CANCELLED');
create type report_export_type as enum ('CSV', 'PDF');
create type report_export_status as enum ('REQUESTED', 'COMPLETED', 'FAILED');
create type ai_recommendation_type as enum (
    'PRODUCT',
    'SEGMENT',
    'COPY',
    'RISK',
    'DUPLICATE_WARNING'
);

create table bwc_schema_metadata (
    id bigserial primary key,
    schema_version varchar(32) not null,
    description varchar(255) not null,
    installed_at timestamptz not null default now()
);

create table users (
    id uuid primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    full_name varchar(255) not null,
    status user_status not null default 'ACTIVE',
    last_login_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table roles (
    id uuid primary key,
    name system_role_name not null unique,
    description text
);

create table user_roles (
    user_id uuid not null references users (id) on delete cascade,
    role_id uuid not null references roles (id) on delete restrict,
    assigned_at timestamptz not null default now(),
    primary key (user_id, role_id)
);

create table customers (
    id uuid primary key,
    customer_type customer_type not null,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    email varchar(255),
    phone varchar(50),
    address_line varchar(255),
    city varchar(100),
    country varchar(100),
    date_of_birth date,
    age_group customer_age_group,
    status customer_status not null default 'ACTIVE',
    do_not_contact boolean not null default false,
    source varchar(100),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

create table beneficiaries (
    id uuid primary key,
    policyholder_customer_id uuid not null references customers (id) on delete cascade,
    beneficiary_customer_id uuid not null references customers (id) on delete cascade,
    relationship varchar(100) not null,
    guardian_name varchar(255),
    guardian_email varchar(255),
    guardian_consent_required boolean not null default false,
    created_at timestamptz not null default now(),
    constraint beneficiaries_distinct_customers
        check (policyholder_customer_id <> beneficiary_customer_id),
    constraint beneficiaries_unique_link
        unique (policyholder_customer_id, beneficiary_customer_id)
);

create table consent_records (
    id uuid primary key,
    customer_id uuid not null references customers (id) on delete cascade,
    consent_type consent_type not null,
    status consent_status not null,
    purpose text not null,
    source varchar(100),
    granted_at timestamptz,
    withdrawn_at timestamptz,
    expires_at timestamptz,
    evidence_file_url text,
    created_by uuid references users (id) on delete set null,
    created_at timestamptz not null default now()
);

create table products (
    id uuid primary key,
    name varchar(255) not null,
    product_type product_type not null,
    description text,
    price numeric(12, 2),
    duration_months integer,
    expiration_policy varchar(100),
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

create table product_ownerships (
    id uuid primary key,
    customer_id uuid not null references customers (id) on delete cascade,
    product_id uuid not null references products (id) on delete restrict,
    policy_number varchar(100),
    start_date date not null,
    expiration_date date,
    status ownership_status not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    constraint product_ownerships_policy_number_unique unique (policy_number)
);

create table product_change_requests (
    id uuid primary key,
    product_id uuid not null references products (id) on delete cascade,
    requested_by uuid references users (id) on delete set null,
    request_type product_change_type not null,
    description text not null,
    status product_change_status not null default 'OPEN',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table payment_records (
    id uuid primary key,
    customer_id uuid not null references customers (id) on delete cascade,
    product_ownership_id uuid not null references product_ownerships (id) on delete cascade,
    due_date date not null,
    paid_at timestamptz,
    amount_due numeric(12, 2) not null,
    amount_paid numeric(12, 2),
    status payment_status not null default 'DUE',
    reminder_count integer not null default 0
);

create table segments (
    id uuid primary key,
    name varchar(255) not null,
    description text,
    owner_user_id uuid references users (id) on delete set null,
    visibility segment_visibility not null default 'PRIVATE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table segment_criteria (
    id uuid primary key,
    segment_id uuid not null references segments (id) on delete cascade,
    field_name varchar(100) not null,
    operator segment_operator not null,
    value text not null,
    logical_group varchar(50),
    join_operator segment_join_operator not null default 'AND'
);

create table campaigns (
    id uuid primary key,
    name varchar(255) not null,
    objective text not null,
    status campaign_status not null default 'DRAFT',
    owner_user_id uuid references users (id) on delete set null,
    segment_id uuid references segments (id) on delete set null,
    channel campaign_channel not null,
    message_subject varchar(255),
    message_body text,
    start_date date,
    end_date date,
    approved_by uuid references users (id) on delete set null,
    approved_at timestamptz,
    rejection_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table campaign_products (
    campaign_id uuid not null references campaigns (id) on delete cascade,
    product_id uuid not null references products (id) on delete restrict,
    primary key (campaign_id, product_id)
);

create table campaign_recipients (
    id uuid primary key,
    campaign_id uuid not null references campaigns (id) on delete cascade,
    customer_id uuid not null references customers (id) on delete cascade,
    eligibility_status campaign_recipient_status not null,
    exclusion_reason text,
    eligibility_explanation text,
    sent_at timestamptz,
    opened_at timestamptz,
    clicked_at timestamptz,
    converted_at timestamptz,
    created_at timestamptz not null default now(),
    constraint campaign_recipients_campaign_customer_unique unique (campaign_id, customer_id)
);

create table contact_events (
    id uuid primary key,
    customer_id uuid not null references customers (id) on delete cascade,
    campaign_id uuid references campaigns (id) on delete set null,
    channel communication_channel not null,
    event_type contact_event_type not null,
    outcome contact_outcome,
    notes text,
    occurred_at timestamptz not null,
    created_by uuid references users (id) on delete set null
);

create table follow_up_tasks (
    id uuid primary key,
    customer_id uuid not null references customers (id) on delete cascade,
    campaign_id uuid references campaigns (id) on delete set null,
    assigned_to uuid references users (id) on delete set null,
    title varchar(255) not null,
    description text,
    due_date date,
    status follow_up_status not null default 'OPEN',
    priority work_priority not null default 'MEDIUM',
    created_at timestamptz not null default now(),
    completed_at timestamptz
);

create table reminder_schedules (
    id uuid primary key,
    customer_id uuid not null references customers (id) on delete cascade,
    product_id uuid not null references products (id) on delete restrict,
    reminder_type reminder_type not null,
    reminder_level reminder_level not null,
    scheduled_date date not null,
    status reminder_status not null default 'PENDING',
    created_at timestamptz not null default now(),
    sent_at timestamptz
);

create table campaign_metrics (
    id uuid primary key,
    campaign_id uuid not null unique references campaigns (id) on delete cascade,
    audience_size integer not null default 0,
    eligible_count integer not null default 0,
    excluded_count integer not null default 0,
    sent_count integer not null default 0,
    opened_count integer not null default 0,
    clicked_count integer not null default 0,
    replied_count integer not null default 0,
    converted_count integer not null default 0,
    estimated_cost numeric(12, 2),
    estimated_revenue numeric(12, 2),
    estimated_roi numeric(12, 2),
    updated_at timestamptz not null default now()
);

create table report_exports (
    id uuid primary key,
    requested_by uuid references users (id) on delete set null,
    report_name varchar(255) not null,
    export_type report_export_type not null,
    status report_export_status not null default 'REQUESTED',
    file_url text,
    requested_at timestamptz not null default now(),
    completed_at timestamptz
);

create table audit_logs (
    id uuid primary key,
    actor_user_id uuid references users (id) on delete set null,
    action varchar(255) not null,
    entity_type varchar(100) not null,
    entity_id uuid,
    old_value jsonb,
    new_value jsonb,
    ip_address varchar(100),
    created_at timestamptz not null default now()
);

create table ai_recommendations (
    id uuid primary key,
    recommendation_type ai_recommendation_type not null,
    target_entity_type varchar(100) not null,
    target_entity_id uuid,
    input_summary text not null,
    recommendation text not null,
    explanation text not null,
    confidence_score numeric(5, 2),
    approved_by_user_id uuid references users (id) on delete set null,
    created_at timestamptz not null default now()
);

create index customers_name_idx on customers (last_name, first_name);
create index customers_email_idx on customers (email);
create index consent_records_customer_status_idx on consent_records (customer_id, status);
create index products_type_active_idx on products (product_type, active);
create index product_ownerships_customer_idx on product_ownerships (customer_id);
create index payment_records_due_status_idx on payment_records (due_date, status);
create index campaigns_status_idx on campaigns (status);
create index campaign_recipients_status_idx on campaign_recipients (campaign_id, eligibility_status);
create index contact_events_customer_occurred_idx on contact_events (customer_id, occurred_at);
create index follow_up_tasks_assignee_status_idx on follow_up_tasks (assigned_to, status);
create index reminder_schedules_date_status_idx on reminder_schedules (scheduled_date, status);
create index audit_logs_entity_idx on audit_logs (entity_type, entity_id);
create index audit_logs_actor_created_idx on audit_logs (actor_user_id, created_at);

insert into roles (id, name, description)
values
    ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Admin'),
    ('00000000-0000-0000-0000-000000000002', 'CAMPAIGN_MANAGER', 'Campaign Manager'),
    ('00000000-0000-0000-0000-000000000003', 'BI_ANALYST', 'BI Analyst'),
    ('00000000-0000-0000-0000-000000000004', 'PRODUCT_MANAGER', 'Product Manager'),
    ('00000000-0000-0000-0000-000000000005', 'COMPLIANCE_OFFICER', 'Compliance Officer'),
    (
        '00000000-0000-0000-0000-000000000006',
        'CUSTOMER_SERVICE_AGENT',
        'Customer Service Agent'
    ),
    ('00000000-0000-0000-0000-000000000007', 'SALES_AGENT', 'Sales Agent'),
    ('00000000-0000-0000-0000-000000000008', 'MARKETING_ANALYST', 'Marketing Analyst'),
    ('00000000-0000-0000-0000-000000000009', 'EXECUTIVE_VIEWER', 'Executive Viewer'),
    ('00000000-0000-0000-0000-000000000010', 'SYSTEM_AUDITOR', 'System Auditor');

insert into bwc_schema_metadata (schema_version, description)
values ('v0.2', 'Initial KB schema initialized');
