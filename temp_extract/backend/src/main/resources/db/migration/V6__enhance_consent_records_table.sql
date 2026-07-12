alter table consent_records
    add constraint consent_records_expiration_after_grant
        check (expires_at is null or granted_at is null or expires_at > granted_at),
    add constraint consent_records_withdrawal_after_grant
        check (withdrawn_at is null or granted_at is null or withdrawn_at >= granted_at);

create index if not exists idx_consent_records_customer
    on consent_records (customer_id);
create index if not exists idx_consent_records_customer_type_status
    on consent_records (customer_id, consent_type, status);
create index if not exists idx_consent_records_status
    on consent_records (status);
create index if not exists idx_consent_records_expires_at
    on consent_records (expires_at);
create index if not exists idx_consent_records_created_by
    on consent_records (created_by);
