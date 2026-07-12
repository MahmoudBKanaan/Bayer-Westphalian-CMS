create index if not exists idx_users_full_name
    on users (full_name);

create index if not exists idx_users_status
    on users (status);

create index if not exists idx_user_roles_role
    on user_roles (role_id);

create index if not exists idx_customers_search_name
    on customers (last_name, first_name, email);

create index if not exists idx_customers_phone
    on customers (phone);

create index if not exists idx_customers_type_status
    on customers (customer_type, status);

create index if not exists idx_customers_status_deleted
    on customers (status, deleted_at);

create index if not exists idx_products_name_type_active
    on products (name, product_type, active);

create index if not exists idx_product_ownerships_status_expiration
    on product_ownerships (status, expiration_date);

create index if not exists idx_product_ownerships_product_expiration
    on product_ownerships (product_id, expiration_date);

create index if not exists idx_campaigns_owner_status
    on campaigns (owner_user_id, status);

create index if not exists idx_campaigns_segment
    on campaigns (segment_id);

create index if not exists idx_campaigns_approved_by
    on campaigns (approved_by);

create index if not exists idx_campaigns_status_dates
    on campaigns (status, start_date, end_date);

create index if not exists idx_campaign_products_product
    on campaign_products (product_id);

create index if not exists idx_campaign_recipients_customer
    on campaign_recipients (customer_id);

create index if not exists idx_campaign_recipients_customer_status
    on campaign_recipients (customer_id, eligibility_status);

create index if not exists idx_contact_events_campaign
    on contact_events (campaign_id);

create index if not exists idx_contact_events_created_by
    on contact_events (created_by);

create index if not exists idx_contact_events_campaign_occurred
    on contact_events (campaign_id, occurred_at);

create index if not exists idx_follow_up_tasks_customer
    on follow_up_tasks (customer_id);

create index if not exists idx_follow_up_tasks_campaign
    on follow_up_tasks (campaign_id);

create index if not exists idx_follow_up_tasks_status_due
    on follow_up_tasks (status, due_date);

create index if not exists idx_reminder_schedules_customer
    on reminder_schedules (customer_id);

create index if not exists idx_reminder_schedules_product
    on reminder_schedules (product_id);

create index if not exists idx_reminder_schedules_customer_date
    on reminder_schedules (customer_id, scheduled_date);

create index if not exists idx_reminder_schedules_product_date
    on reminder_schedules (product_id, scheduled_date);

create index if not exists idx_reminder_schedules_status_date
    on reminder_schedules (status, scheduled_date);

create index if not exists idx_report_exports_requested_by
    on report_exports (requested_by);

create index if not exists idx_report_exports_status_requested
    on report_exports (status, requested_at);
