insert into customers (
    id,
    customer_type,
    first_name,
    last_name,
    email,
    phone,
    city,
    country,
    date_of_birth,
    age_group,
    status,
    do_not_contact,
    source
)
values
    (
        '20000000-0000-0000-0000-000000000101',
        'CUSTOMER',
        'Anna',
        'Keller',
        'anna.keller.demo@example.test',
        '+49-30-555-0101',
        'Berlin',
        'Germany',
        '1968-04-12',
        '41_60',
        'ACTIVE',
        false,
        'DEMO_DATA'
    ),
    (
        '20000000-0000-0000-0000-000000000102',
        'BENEFICIARY',
        'Lena',
        'Keller',
        'lena.keller.demo@example.test',
        '+49-30-555-0102',
        'Berlin',
        'Germany',
        '2002-09-18',
        '18_25',
        'INTERESTED',
        false,
        'LIFE_INSURANCE_BENEFICIARY'
    ),
    (
        '20000000-0000-0000-0000-000000000103',
        'PROSPECT',
        'Jonas',
        'Weber',
        'jonas.weber.demo@example.test',
        '+49-89-555-0103',
        'Munich',
        'Germany',
        '1984-02-26',
        '26_40',
        'ACTIVE',
        true,
        'DEMO_DATA'
    )
on conflict (id) do update
set
    customer_type = excluded.customer_type,
    first_name = excluded.first_name,
    last_name = excluded.last_name,
    email = excluded.email,
    phone = excluded.phone,
    city = excluded.city,
    country = excluded.country,
    date_of_birth = excluded.date_of_birth,
    age_group = excluded.age_group,
    status = excluded.status,
    do_not_contact = excluded.do_not_contact,
    source = excluded.source,
    updated_at = now();

insert into beneficiaries (
    id,
    policyholder_customer_id,
    beneficiary_customer_id,
    relationship,
    guardian_consent_required
)
values (
    '21000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000102',
    'Grandchild',
    false
)
on conflict (policyholder_customer_id, beneficiary_customer_id) do update
set
    relationship = excluded.relationship,
    guardian_consent_required = excluded.guardian_consent_required;

insert into products (id, name, product_type, description, price, duration_months, expiration_policy)
values
    (
        '30000000-0000-0000-0000-000000000101',
        'Demo Life Protect',
        'LIFE_INSURANCE',
        'Controlled demo life insurance product for dev/test workflows',
        120.00,
        24,
        'RENEWAL_30_DAYS'
    ),
    (
        '30000000-0000-0000-0000-000000000102',
        'Demo Future Fund',
        'INVESTMENT_FUND',
        'Controlled demo investment product for beneficiary campaigns',
        250.00,
        36,
        'ANNUAL_REVIEW'
    )
on conflict (id) do update
set
    name = excluded.name,
    product_type = excluded.product_type,
    description = excluded.description,
    price = excluded.price,
    duration_months = excluded.duration_months,
    expiration_policy = excluded.expiration_policy,
    active = true,
    updated_at = now();

insert into product_ownerships (
    id,
    customer_id,
    product_id,
    policy_number,
    start_date,
    expiration_date,
    status
)
values (
    '31000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000101',
    '30000000-0000-0000-0000-000000000101',
    'DEMO-LIFE-0001',
    current_date - interval '18 months',
    current_date + interval '45 days',
    'ACTIVE'
)
on conflict (policy_number) do update
set
    customer_id = excluded.customer_id,
    product_id = excluded.product_id,
    start_date = excluded.start_date,
    expiration_date = excluded.expiration_date,
    status = excluded.status;

insert into payment_records (
    id,
    customer_id,
    product_ownership_id,
    due_date,
    amount_due,
    amount_paid,
    status,
    reminder_count
)
values (
    '32000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000101',
    '31000000-0000-0000-0000-000000000101',
    current_date + interval '10 days',
    120.00,
    null,
    'DUE',
    1
)
on conflict (id) do update
set
    due_date = excluded.due_date,
    amount_due = excluded.amount_due,
    amount_paid = excluded.amount_paid,
    status = excluded.status,
    reminder_count = excluded.reminder_count;

insert into consent_records (
    id,
    customer_id,
    consent_type,
    status,
    purpose,
    source,
    granted_at,
    created_by
)
values
    (
        '33000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000101',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '30 days',
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000102',
        '20000000-0000-0000-0000-000000000102',
        'MARKETING_EMAIL',
        'GIVEN',
        'Beneficiary campaign outreach',
        'DEMO_DATA',
        now() - interval '15 days',
        '10000000-0000-0000-0000-000000000005'
    )
on conflict (id) do update
set
    status = excluded.status,
    purpose = excluded.purpose,
    source = excluded.source,
    granted_at = excluded.granted_at,
    created_by = excluded.created_by;

insert into segments (id, name, description, owner_user_id, visibility)
values (
    '40000000-0000-0000-0000-000000000101',
    'Demo Beneficiaries With Consent',
    'Controlled demo segment for eligible beneficiaries with email consent',
    '10000000-0000-0000-0000-000000000002',
    'TEAM'
)
on conflict (id) do update
set
    name = excluded.name,
    description = excluded.description,
    owner_user_id = excluded.owner_user_id,
    visibility = excluded.visibility,
    updated_at = now();

insert into segment_criteria (id, segment_id, field_name, operator, value, logical_group, join_operator)
values
    (
        '41000000-0000-0000-0000-000000000101',
        '40000000-0000-0000-0000-000000000101',
        'customer_type',
        'EQUALS',
        'BENEFICIARY',
        'eligibility',
        'AND'
    ),
    (
        '41000000-0000-0000-0000-000000000102',
        '40000000-0000-0000-0000-000000000101',
        'consent_status',
        'EQUALS',
        'GIVEN',
        'eligibility',
        'AND'
    )
on conflict (id) do update
set
    field_name = excluded.field_name,
    operator = excluded.operator,
    value = excluded.value,
    logical_group = excluded.logical_group,
    join_operator = excluded.join_operator;

insert into campaigns (
    id,
    name,
    objective,
    status,
    owner_user_id,
    segment_id,
    channel,
    message_subject,
    message_body,
    start_date,
    end_date,
    approved_by,
    approved_at
)
values (
    '50000000-0000-0000-0000-000000000101',
    'Demo Beneficiary Investment Outreach',
    'Offer investment products to eligible beneficiaries with consent',
    'APPROVED',
    '10000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000101',
    'EMAIL',
    'Plan your next financial step',
    'Controlled demo email body for dev/test campaign previews.',
    current_date,
    current_date + interval '30 days',
    '10000000-0000-0000-0000-000000000005',
    now() - interval '1 day'
)
on conflict (id) do update
set
    name = excluded.name,
    objective = excluded.objective,
    status = excluded.status,
    owner_user_id = excluded.owner_user_id,
    segment_id = excluded.segment_id,
    channel = excluded.channel,
    message_subject = excluded.message_subject,
    message_body = excluded.message_body,
    start_date = excluded.start_date,
    end_date = excluded.end_date,
    approved_by = excluded.approved_by,
    approved_at = excluded.approved_at,
    updated_at = now();

insert into campaign_products (campaign_id, product_id)
values
    ('50000000-0000-0000-0000-000000000101', '30000000-0000-0000-0000-000000000102')
on conflict (campaign_id, product_id) do nothing;

insert into campaign_recipients (
    id,
    campaign_id,
    customer_id,
    eligibility_status,
    eligibility_explanation
)
values (
    '51000000-0000-0000-0000-000000000101',
    '50000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000102',
    'ELIGIBLE',
    'Demo beneficiary has valid email consent and is not marked do-not-contact'
)
on conflict (campaign_id, customer_id) do update
set
    eligibility_status = excluded.eligibility_status,
    eligibility_explanation = excluded.eligibility_explanation;

insert into contact_events (
    id,
    customer_id,
    campaign_id,
    channel,
    event_type,
    outcome,
    notes,
    occurred_at,
    created_by
)
values (
    '52000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000102',
    '50000000-0000-0000-0000-000000000101',
    'EMAIL',
    'SENT',
    'NO_RESPONSE',
    'Controlled demo contact event',
    now() - interval '2 hours',
    '10000000-0000-0000-0000-000000000006'
)
on conflict (id) do update
set
    event_type = excluded.event_type,
    outcome = excluded.outcome,
    notes = excluded.notes,
    occurred_at = excluded.occurred_at,
    created_by = excluded.created_by;

insert into follow_up_tasks (
    id,
    customer_id,
    campaign_id,
    assigned_to,
    title,
    description,
    due_date,
    status,
    priority
)
values (
    '53000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000102',
    '50000000-0000-0000-0000-000000000101',
    '10000000-0000-0000-0000-000000000006',
    'Demo beneficiary follow-up',
    'Controlled demo follow-up task for customer service workflow',
    current_date + interval '3 days',
    'OPEN',
    'MEDIUM'
)
on conflict (id) do update
set
    assigned_to = excluded.assigned_to,
    title = excluded.title,
    description = excluded.description,
    due_date = excluded.due_date,
    status = excluded.status,
    priority = excluded.priority;

insert into reminder_schedules (
    id,
    customer_id,
    product_id,
    reminder_type,
    reminder_level,
    scheduled_date,
    status
)
values (
    '54000000-0000-0000-0000-000000000101',
    '20000000-0000-0000-0000-000000000101',
    '30000000-0000-0000-0000-000000000101',
    'PRODUCT_EXPIRATION',
    'YELLOW',
    current_date + interval '15 days',
    'PENDING'
)
on conflict (id) do update
set
    reminder_level = excluded.reminder_level,
    scheduled_date = excluded.scheduled_date,
    status = excluded.status;

insert into campaign_metrics (
    id,
    campaign_id,
    audience_size,
    eligible_count,
    excluded_count,
    sent_count,
    opened_count,
    clicked_count,
    replied_count,
    converted_count,
    estimated_cost,
    estimated_revenue,
    estimated_roi
)
values (
    '55000000-0000-0000-0000-000000000101',
    '50000000-0000-0000-0000-000000000101',
    3,
    1,
    1,
    1,
    0,
    0,
    0,
    0,
    12.50,
    0.00,
    0.00
)
on conflict (campaign_id) do update
set
    audience_size = excluded.audience_size,
    eligible_count = excluded.eligible_count,
    excluded_count = excluded.excluded_count,
    sent_count = excluded.sent_count,
    opened_count = excluded.opened_count,
    clicked_count = excluded.clicked_count,
    replied_count = excluded.replied_count,
    converted_count = excluded.converted_count,
    estimated_cost = excluded.estimated_cost,
    estimated_revenue = excluded.estimated_revenue,
    estimated_roi = excluded.estimated_roi,
    updated_at = now();

insert into report_exports (id, requested_by, report_name, export_type, status, file_url, completed_at)
values (
    '56000000-0000-0000-0000-000000000101',
    '10000000-0000-0000-0000-000000000003',
    'Demo Campaign Performance',
    'CSV',
    'COMPLETED',
    'local://demo/reports/campaign-performance.csv',
    now() - interval '1 hour'
)
on conflict (id) do update
set
    requested_by = excluded.requested_by,
    report_name = excluded.report_name,
    export_type = excluded.export_type,
    status = excluded.status,
    file_url = excluded.file_url,
    completed_at = excluded.completed_at;

insert into ai_recommendations (
    id,
    recommendation_type,
    target_entity_type,
    target_entity_id,
    input_summary,
    recommendation,
    explanation,
    confidence_score,
    approved_by_user_id
)
values (
    '57000000-0000-0000-0000-000000000101',
    'PRODUCT',
    'customer',
    '20000000-0000-0000-0000-000000000102',
    'Demo beneficiary is eligible and has marketing consent',
    'Recommend Demo Future Fund campaign follow-up',
    'Controlled demo recommendation for dev/test AI-assisted workflows',
    82.50,
    '10000000-0000-0000-0000-000000000003'
)
on conflict (id) do update
set
    recommendation_type = excluded.recommendation_type,
    target_entity_type = excluded.target_entity_type,
    target_entity_id = excluded.target_entity_id,
    input_summary = excluded.input_summary,
    recommendation = excluded.recommendation,
    explanation = excluded.explanation,
    confidence_score = excluded.confidence_score,
    approved_by_user_id = excluded.approved_by_user_id;

insert into audit_logs (
    id,
    actor_user_id,
    action,
    entity_type,
    entity_id,
    new_value,
    ip_address
)
values (
    '58000000-0000-0000-0000-000000000101',
    '10000000-0000-0000-0000-000000000001',
    'LOAD_DEMO_DATA',
    'database',
    null,
    '{"scope":"dev-test-only","source":"R__controlled_demo_data.sql"}'::jsonb,
    '127.0.0.1'
)
on conflict (id) do update
set
    actor_user_id = excluded.actor_user_id,
    action = excluded.action,
    new_value = excluded.new_value,
    ip_address = excluded.ip_address;
