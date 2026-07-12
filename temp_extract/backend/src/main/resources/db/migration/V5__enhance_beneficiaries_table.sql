create index if not exists idx_beneficiaries_policyholder_customer
    on beneficiaries (policyholder_customer_id);
create index if not exists idx_beneficiaries_beneficiary_customer
    on beneficiaries (beneficiary_customer_id);
create index if not exists idx_beneficiaries_guardian_consent_required
    on beneficiaries (guardian_consent_required);
