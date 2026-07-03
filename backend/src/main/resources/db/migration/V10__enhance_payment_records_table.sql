alter table payment_records
    add constraint payment_records_amount_due_non_negative
        check (amount_due >= 0),
    add constraint payment_records_amount_paid_non_negative
        check (amount_paid is null or amount_paid >= 0),
    add constraint payment_records_reminder_count_non_negative
        check (reminder_count >= 0);

create index if not exists idx_payment_records_customer
    on payment_records (customer_id);
create index if not exists idx_payment_records_ownership
    on payment_records (product_ownership_id);
create index if not exists idx_payment_records_status
    on payment_records (status);
create index if not exists idx_payment_records_customer_status
    on payment_records (customer_id, status);
