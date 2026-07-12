-- System Settings (KB item 534 / Sprint 14): admin-configurable business limits.
-- Defaults align with app.contact.* application properties (monthly contact, send retry,
-- uninterested exclusion period). Domain wiring of these values is items 535–537.

create table system_settings (
    id uuid primary key,
    monthly_contact_limit integer not null,
    send_retry_limit integer not null,
    uninterested_exclusion_days integer not null,
    updated_by_user_id uuid null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint system_settings_monthly_contact_limit_chk check (monthly_contact_limit >= 1 and monthly_contact_limit <= 100),
    constraint system_settings_send_retry_limit_chk check (send_retry_limit >= 1 and send_retry_limit <= 20),
    constraint system_settings_uninterested_exclusion_days_chk check (uninterested_exclusion_days >= 1 and uninterested_exclusion_days <= 3650)
);

alter table system_settings
    add constraint system_settings_updated_by_user_fk
        foreign key (updated_by_user_id) references users (id);

-- Singleton configuration row (fixed id for stable admin upsert).
insert into system_settings (
    id,
    monthly_contact_limit,
    send_retry_limit,
    uninterested_exclusion_days,
    updated_by_user_id,
    created_at,
    updated_at
) values (
    'a1000000-0000-0000-0000-000000000001',
    3,
    3,
    90,
    null,
    now(),
    now()
);
