do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'user_roles_user_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table user_roles
            add constraint user_roles_user_id_fkey
            foreign key (user_id) references users (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'user_roles_role_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table user_roles
            add constraint user_roles_role_id_fkey
            foreign key (role_id) references roles (id) on delete restrict;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'user_roles_assigned_by_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table user_roles
            add constraint user_roles_assigned_by_fkey
            foreign key (assigned_by) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'beneficiaries_policyholder_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table beneficiaries
            add constraint beneficiaries_policyholder_customer_id_fkey
            foreign key (policyholder_customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'beneficiaries_beneficiary_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table beneficiaries
            add constraint beneficiaries_beneficiary_customer_id_fkey
            foreign key (beneficiary_customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'consent_records_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table consent_records
            add constraint consent_records_customer_id_fkey
            foreign key (customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'consent_records_created_by_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table consent_records
            add constraint consent_records_created_by_fkey
            foreign key (created_by) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'product_ownerships_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table product_ownerships
            add constraint product_ownerships_customer_id_fkey
            foreign key (customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'product_ownerships_product_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table product_ownerships
            add constraint product_ownerships_product_id_fkey
            foreign key (product_id) references products (id) on delete restrict;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'product_change_requests_product_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table product_change_requests
            add constraint product_change_requests_product_id_fkey
            foreign key (product_id) references products (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'product_change_requests_requested_by_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table product_change_requests
            add constraint product_change_requests_requested_by_fkey
            foreign key (requested_by) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'payment_records_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table payment_records
            add constraint payment_records_customer_id_fkey
            foreign key (customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'payment_records_product_ownership_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table payment_records
            add constraint payment_records_product_ownership_id_fkey
            foreign key (product_ownership_id) references product_ownerships (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'segments_owner_user_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table segments
            add constraint segments_owner_user_id_fkey
            foreign key (owner_user_id) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'segment_criteria_segment_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table segment_criteria
            add constraint segment_criteria_segment_id_fkey
            foreign key (segment_id) references segments (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'campaigns_owner_user_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table campaigns
            add constraint campaigns_owner_user_id_fkey
            foreign key (owner_user_id) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'campaigns_segment_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table campaigns
            add constraint campaigns_segment_id_fkey
            foreign key (segment_id) references segments (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'campaigns_approved_by_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table campaigns
            add constraint campaigns_approved_by_fkey
            foreign key (approved_by) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'campaign_products_campaign_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table campaign_products
            add constraint campaign_products_campaign_id_fkey
            foreign key (campaign_id) references campaigns (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'campaign_products_product_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table campaign_products
            add constraint campaign_products_product_id_fkey
            foreign key (product_id) references products (id) on delete restrict;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'campaign_recipients_campaign_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table campaign_recipients
            add constraint campaign_recipients_campaign_id_fkey
            foreign key (campaign_id) references campaigns (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'campaign_recipients_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table campaign_recipients
            add constraint campaign_recipients_customer_id_fkey
            foreign key (customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'contact_events_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table contact_events
            add constraint contact_events_customer_id_fkey
            foreign key (customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'contact_events_campaign_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table contact_events
            add constraint contact_events_campaign_id_fkey
            foreign key (campaign_id) references campaigns (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'contact_events_created_by_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table contact_events
            add constraint contact_events_created_by_fkey
            foreign key (created_by) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'follow_up_tasks_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table follow_up_tasks
            add constraint follow_up_tasks_customer_id_fkey
            foreign key (customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'follow_up_tasks_campaign_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table follow_up_tasks
            add constraint follow_up_tasks_campaign_id_fkey
            foreign key (campaign_id) references campaigns (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'follow_up_tasks_assigned_to_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table follow_up_tasks
            add constraint follow_up_tasks_assigned_to_fkey
            foreign key (assigned_to) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'reminder_schedules_customer_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table reminder_schedules
            add constraint reminder_schedules_customer_id_fkey
            foreign key (customer_id) references customers (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'reminder_schedules_product_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table reminder_schedules
            add constraint reminder_schedules_product_id_fkey
            foreign key (product_id) references products (id) on delete restrict;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'campaign_metrics_campaign_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table campaign_metrics
            add constraint campaign_metrics_campaign_id_fkey
            foreign key (campaign_id) references campaigns (id) on delete cascade;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'report_exports_requested_by_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table report_exports
            add constraint report_exports_requested_by_fkey
            foreign key (requested_by) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'audit_logs_actor_user_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table audit_logs
            add constraint audit_logs_actor_user_id_fkey
            foreign key (actor_user_id) references users (id) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'ai_recommendations_approved_by_user_id_fkey'
            and connamespace = current_schema()::regnamespace
    ) then
        alter table ai_recommendations
            add constraint ai_recommendations_approved_by_user_id_fkey
            foreign key (approved_by_user_id) references users (id) on delete set null;
    end if;
end $$;
