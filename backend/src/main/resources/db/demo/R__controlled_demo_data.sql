-- Final controlled demo dataset (item 781).
-- Synthetic dev/test data only. application-prod.yml must never load classpath:db/demo.
-- Deterministic UUIDs and upserts make repeated Flyway execution safe for demonstrations.

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
        '20000000-0000-0000-0000-000000000104',
        'PROSPECT',
        'Mia',
        'Schneider',
        'mia.schneider.demo@example.test',
        '+49-40-555-0104',
        'Hamburg',
        'Germany',
        '1992-06-04',
        '26_40',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_WITHDRAWN_CONSENT'
    ),
    (
        '20000000-0000-0000-0000-000000000105',
        'CUSTOMER',
        'Noah',
        'Fischer',
        'noah.fischer.demo@example.test',
        '+49-69-555-0105',
        'Frankfurt',
        'Germany',
        '1976-11-21',
        '41_60',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_REJECTED_CONSENT'
    ),
    (
        '20000000-0000-0000-0000-000000000106',
        'PROSPECT',
        'Sofia',
        'Becker',
        'sofia.becker.demo@example.test',
        '+49-211-555-0106',
        'Dusseldorf',
        'Germany',
        '1989-01-14',
        '26_40',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_MISSING_CONSENT'
    ),
    (
        '20000000-0000-0000-0000-000000000107',
        'BENEFICIARY',
        'Emil',
        'Keller',
        'emil.keller.demo@example.test',
        '+49-30-555-0107',
        'Berlin',
        'Germany',
        '2011-08-09',
        'MINOR',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_GUARDIAN_REQUIRED'
    ),
    (
        '20000000-0000-0000-0000-000000000108',
        'BENEFICIARY',
        'Clara',
        'Keller',
        'clara.keller.demo@example.test',
        '+49-30-555-0108',
        'Berlin',
        'Germany',
        '2010-03-27',
        'MINOR',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_GUARDIAN_VALID'
    ),
    (
        '20000000-0000-0000-0000-000000000109',
        'CUSTOMER',
        'Lukas',
        'Hoffmann',
        'lukas.hoffmann.demo@example.test',
        '+49-711-555-0109',
        'Stuttgart',
        'Germany',
        '1964-12-18',
        '60_PLUS',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_EXPIRED_CONSENT'
    ),
    (
        '20000000-0000-0000-0000-000000000110',
        'PROSPECT',
        'Ella',
        'Meyer',
        'ella.meyer.demo@example.test',
        '+49-221-555-0110',
        'Cologne',
        'Germany',
        '1996-05-30',
        '26_40',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_DUPLICATE_RECIPIENT'
    ),
    (
        '20000000-0000-0000-0000-000000000111',
        'CUSTOMER',
        'Paul',
        'Wagner',
        'paul.wagner.demo@example.test',
        '+49-351-555-0111',
        'Dresden',
        'Germany',
        '1981-10-06',
        '41_60',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_MONTHLY_LIMIT'
    ),
    (
        '20000000-0000-0000-0000-000000000112',
        'PROSPECT',
        'Greta',
        'Schulz',
        'greta.schulz.demo@example.test',
        '+49-421-555-0112',
        'Bremen',
        'Germany',
        '1999-07-19',
        '26_40',
        'INTERESTED',
        false,
        'DEMO_ELIGIBLE'
    ),
    (
        '20000000-0000-0000-0000-000000000113',
        'CUSTOMER',
        'Felix',
        'Koch',
        'felix.koch.demo@example.test',
        '+49-341-555-0113',
        'Leipzig',
        'Germany',
        '1972-02-11',
        '41_60',
        'ACTIVE',
        false,
        'DEMO_ELIGIBLE'
    ),
    (
        '20000000-0000-0000-0000-000000000114',
        'BENEFICIARY',
        'Hanna',
        'Koch',
        'hanna.koch.demo@example.test',
        '+49-341-555-0114',
        'Leipzig',
        'Germany',
        '2003-09-03',
        '18_25',
        'ACTIVE',
        false,
        'LIFE_INSURANCE_BENEFICIARY'
    ),
    (
        '20000000-0000-0000-0000-000000000115',
        'PROSPECT',
        'Oscar',
        'Richter',
        'oscar.richter.demo@example.test',
        '+49-511-555-0115',
        'Hanover',
        'Germany',
        '1987-04-24',
        '26_40',
        'ACTIVE',
        false,
        'DEMO_ELIGIBLE'
    ),
    (
        '20000000-0000-0000-0000-000000000116',
        'CUSTOMER',
        'Marie',
        'Wolf',
        'marie.wolf.demo@example.test',
        '+49-231-555-0116',
        'Dortmund',
        'Germany',
        '1958-01-05',
        '60_PLUS',
        'ACTIVE',
        false,
        'DEMO_ELIGIBLE'
    ),
    (
        '20000000-0000-0000-0000-000000000117',
        'PROSPECT',
        'Anton',
        'Neumann',
        'anton.neumann.demo@example.test',
        '+49-351-555-0117',
        'Dresden',
        'Germany',
        '1991-12-13',
        '26_40',
        'ACTIVE',
        true,
        'DEMO_ELIGIBILITY_DO_NOT_CONTACT'
    ),
    (
        '20000000-0000-0000-0000-000000000118',
        'CUSTOMER',
        'Lea',
        'Schwarz',
        'lea.schwarz.demo@example.test',
        '+49-911-555-0118',
        'Nuremberg',
        'Germany',
        '1983-06-16',
        '41_60',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_REQUIRED_CONSENT'
    ),
    (
        '20000000-0000-0000-0000-000000000119',
        'BENEFICIARY',
        'Max',
        'Schwarz',
        'max.schwarz.demo@example.test',
        '+49-911-555-0119',
        'Nuremberg',
        'Germany',
        '2004-07-22',
        '18_25',
        'ACTIVE',
        false,
        'DEMO_ELIGIBILITY_MARKETING_OPT_OUT'
    ),
    (
        '20000000-0000-0000-0000-000000000120',
        'PROSPECT',
        'Nora',
        'Braun',
        'nora.braun.demo@example.test',
        '+49-621-555-0120',
        'Mannheim',
        'Germany',
        '1995-03-08',
        '26_40',
        'ACTIVE',
        false,
        'DEMO_ELIGIBLE'
    ),
    (
        '20000000-0000-0000-0000-000000000130',
        'PROSPECT',
        'Ahmed',
        'Saleh',
        'ahmed.saleh.demo@example.test',
        '+49-30-555-0130',
        'Berlin',
        'Germany',
        '1990-05-12',
        '26_40',
        'INTERESTED',
        false,
        'DEMO_AI_SPRINT13'
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

insert into beneficiaries (
    id,
    policyholder_customer_id,
    beneficiary_customer_id,
    relationship,
    guardian_name,
    guardian_email,
    guardian_consent_required
)
values
    (
        '21000000-0000-0000-0000-000000000107',
        '20000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000107',
        'Grandchild - minor',
        'Anna Keller',
        'anna.keller.demo@example.test',
        true
    ),
    (
        '21000000-0000-0000-0000-000000000108',
        '20000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000108',
        'Grandchild - minor',
        'Anna Keller',
        'anna.keller.demo@example.test',
        true
    ),
    (
        '21000000-0000-0000-0000-000000000114',
        '20000000-0000-0000-0000-000000000113',
        '20000000-0000-0000-0000-000000000114',
        'Grandchild',
        null,
        null,
        false
    ),
    (
        '21000000-0000-0000-0000-000000000119',
        '20000000-0000-0000-0000-000000000118',
        '20000000-0000-0000-0000-000000000119',
        'Grandchild',
        null,
        null,
        false
    )
on conflict (policyholder_customer_id, beneficiary_customer_id) do update
set
    relationship = excluded.relationship,
    guardian_name = excluded.guardian_name,
    guardian_email = excluded.guardian_email,
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
    ),
    (
        '30000000-0000-0000-0000-000000000103',
        'Home Secure Insurance',
        'HOMEOWNER_INSURANCE',
        'Sprint 13 AI demo homeowner product (not owned by Ahmed Saleh)',
        89.00,
        12,
        'RENEWAL_30_DAYS'
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
values
    (
        '31000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000101',
        '30000000-0000-0000-0000-000000000101',
        'DEMO-LIFE-0001',
        current_date - interval '18 months',
        current_date + interval '45 days',
        'ACTIVE'
    ),
    (
        '31000000-0000-0000-0000-000000000130',
        '20000000-0000-0000-0000-000000000130',
        '30000000-0000-0000-0000-000000000101',
        'DEMO-LIFE-AHMED-0001',
        current_date - interval '12 months',
        current_date + interval '80 days',
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
values
    (
        '32000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000101',
        '31000000-0000-0000-0000-000000000101',
        current_date + interval '10 days',
        120.00,
        null,
        'DUE',
        1
    ),
    (
        '32000000-0000-0000-0000-000000000130',
        '20000000-0000-0000-0000-000000000130',
        '31000000-0000-0000-0000-000000000130',
        current_date - interval '35 days',
        120.00,
        null,
        'OVERDUE',
        3
    ),
    (
        '32000000-0000-0000-0000-000000000131',
        '20000000-0000-0000-0000-000000000130',
        '31000000-0000-0000-0000-000000000130',
        current_date - interval '20 days',
        120.00,
        null,
        'OVERDUE',
        2
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

insert into consent_records (
    id,
    customer_id,
    consent_type,
    status,
    purpose,
    source,
    granted_at,
    withdrawn_at,
    expires_at,
    created_by
)
values
    (
        '33000000-0000-0000-0000-000000000103',
        '20000000-0000-0000-0000-000000000103',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '20 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000104',
        '20000000-0000-0000-0000-000000000104',
        'MARKETING_EMAIL',
        'WITHDRAWN',
        'Customer withdrew marketing permission',
        'DEMO_DATA',
        now() - interval '90 days',
        now() - interval '5 days',
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000105',
        '20000000-0000-0000-0000-000000000105',
        'MARKETING_EMAIL',
        'REJECTED',
        'Customer rejected marketing permission',
        'DEMO_DATA',
        null,
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000107',
        '20000000-0000-0000-0000-000000000107',
        'MARKETING_EMAIL',
        'GIVEN',
        'Minor beneficiary campaign outreach',
        'DEMO_DATA',
        now() - interval '15 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000108',
        '20000000-0000-0000-0000-000000000108',
        'MARKETING_EMAIL',
        'GIVEN',
        'Minor beneficiary campaign outreach',
        'DEMO_DATA',
        now() - interval '15 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000208',
        '20000000-0000-0000-0000-000000000108',
        'GUARDIAN',
        'GIVEN',
        'Guardian approved marketing eligibility review',
        'DEMO_DATA',
        now() - interval '14 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000109',
        '20000000-0000-0000-0000-000000000109',
        'MARKETING_EMAIL',
        'GIVEN',
        'Expired campaign outreach permission',
        'DEMO_DATA',
        now() - interval '120 days',
        null,
        now() - interval '1 day',
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000110',
        '20000000-0000-0000-0000-000000000110',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '30 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000111',
        '20000000-0000-0000-0000-000000000111',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '30 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000112',
        '20000000-0000-0000-0000-000000000112',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '25 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000113',
        '20000000-0000-0000-0000-000000000113',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '25 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000114',
        '20000000-0000-0000-0000-000000000114',
        'MARKETING_EMAIL',
        'GIVEN',
        'Beneficiary campaign outreach',
        'DEMO_DATA',
        now() - interval '25 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000115',
        '20000000-0000-0000-0000-000000000115',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '25 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000116',
        '20000000-0000-0000-0000-000000000116',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '25 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000117',
        '20000000-0000-0000-0000-000000000117',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '25 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000118',
        '20000000-0000-0000-0000-000000000118',
        'MARKETING_EMAIL',
        'REQUIRED',
        'Marketing permission still required',
        'DEMO_DATA',
        null,
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000119',
        '20000000-0000-0000-0000-000000000119',
        'MARKETING_SMS',
        'WITHDRAWN',
        'Beneficiary opted out of marketing messages',
        'DEMO_DATA',
        now() - interval '60 days',
        now() - interval '3 days',
        null,
        '10000000-0000-0000-0000-000000000005'
    ),
    (
        '33000000-0000-0000-0000-000000000120',
        '20000000-0000-0000-0000-000000000120',
        'MARKETING_EMAIL',
        'GIVEN',
        'Demo campaign outreach',
        'DEMO_DATA',
        now() - interval '25 days',
        null,
        null,
        '10000000-0000-0000-0000-000000000005'
    )
on conflict (id) do update
set
    consent_type = excluded.consent_type,
    status = excluded.status,
    purpose = excluded.purpose,
    source = excluded.source,
    granted_at = excluded.granted_at,
    withdrawn_at = excluded.withdrawn_at,
    expires_at = excluded.expires_at,
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

insert into campaign_recipients (
    id,
    campaign_id,
    customer_id,
    eligibility_status,
    exclusion_reason,
    eligibility_explanation
)
values
    (
        '51000000-0000-0000-0000-000000000201',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000101',
        'ELIGIBLE',
        null,
        'Customer has valid email consent and is not marked do-not-contact'
    ),
    (
        '51000000-0000-0000-0000-000000000203',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000103',
        'EXCLUDED',
        'DO_NOT_CONTACT',
        'Customer has do-not-contact enabled'
    ),
    (
        '51000000-0000-0000-0000-000000000204',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000104',
        'EXCLUDED',
        'MARKETING_OPT_OUT',
        'Customer has withdrawn or rejected marketing consent'
    ),
    (
        '51000000-0000-0000-0000-000000000205',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000105',
        'EXCLUDED',
        'MARKETING_OPT_OUT',
        'Customer has withdrawn or rejected marketing consent'
    ),
    (
        '51000000-0000-0000-0000-000000000206',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000106',
        'EXCLUDED',
        'INVALID_CONSENT',
        'Customer does not have valid required consent'
    ),
    (
        '51000000-0000-0000-0000-000000000207',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000107',
        'EXCLUDED',
        'INVALID_CONSENT',
        'Minor beneficiary requires guardian consent before outreach'
    ),
    (
        '51000000-0000-0000-0000-000000000208',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000108',
        'ELIGIBLE',
        null,
        'Minor beneficiary has valid marketing and guardian consent'
    ),
    (
        '51000000-0000-0000-0000-000000000209',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000109',
        'EXCLUDED',
        'INVALID_CONSENT',
        'Customer consent is expired and no longer valid'
    ),
    (
        '51000000-0000-0000-0000-000000000210',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000110',
        'EXCLUDED',
        'DUPLICATE_CAMPAIGN_RECIPIENT',
        'Customer is already assigned to this campaign'
    ),
    (
        '51000000-0000-0000-0000-000000000211',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000111',
        'EXCLUDED',
        'MONTHLY_CONTACT_LIMIT',
        'Customer has reached the monthly marketing contact limit'
    ),
    (
        '51000000-0000-0000-0000-000000000212',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000112',
        'ELIGIBLE',
        null,
        'Customer has valid email consent and is not marked do-not-contact'
    ),
    (
        '51000000-0000-0000-0000-000000000213',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000113',
        'ELIGIBLE',
        null,
        'Customer has valid email consent and is not marked do-not-contact'
    ),
    (
        '51000000-0000-0000-0000-000000000214',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000114',
        'ELIGIBLE',
        null,
        'Beneficiary has valid email consent and no guardian consent requirement'
    ),
    (
        '51000000-0000-0000-0000-000000000215',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000115',
        'ELIGIBLE',
        null,
        'Customer has valid email consent and is not marked do-not-contact'
    ),
    (
        '51000000-0000-0000-0000-000000000216',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000116',
        'ELIGIBLE',
        null,
        'Customer has valid email consent and is not marked do-not-contact'
    ),
    (
        '51000000-0000-0000-0000-000000000217',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000117',
        'EXCLUDED',
        'DO_NOT_CONTACT',
        'Customer has do-not-contact enabled'
    ),
    (
        '51000000-0000-0000-0000-000000000218',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000118',
        'EXCLUDED',
        'INVALID_CONSENT',
        'Customer does not have valid required consent'
    ),
    (
        '51000000-0000-0000-0000-000000000219',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000119',
        'EXCLUDED',
        'MARKETING_OPT_OUT',
        'Customer has withdrawn or rejected marketing consent'
    ),
    (
        '51000000-0000-0000-0000-000000000220',
        '50000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000120',
        'ELIGIBLE',
        null,
        'Customer has valid email consent and is not marked do-not-contact'
    )
on conflict (campaign_id, customer_id) do update
set
    eligibility_status = excluded.eligibility_status,
    exclusion_reason = excluded.exclusion_reason,
    eligibility_explanation = excluded.eligibility_explanation;

-- Sprint 13 AI-006: Campaign Test 4 + Ahmed Saleh for duplicate-contact warning screenshots.
-- Same-campaign SENT contact + three recent marketing SENT events (monthly limit = 3).
-- Campaign Test 4 is declared before the larger mock-data section, so seed its referenced
-- segment here as an idempotent prerequisite. The later mock-segment upsert keeps it aligned.
insert into segments (id, name, description, owner_user_id, visibility)
values (
    '40000000-0000-0000-0000-000000000201',
    'Mock Berlin Adults',
    'Mock segment 01 - adults in Berlin',
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
    '50000000-0000-0000-0000-000000000130',
    'Campaign Test 4',
    'Promote Demo Life Protect to eligible customers.',
    'DRAFT',
    '10000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000201',
    'EMAIL',
    'Protect what matters with Demo Life Protect',
    'Discover flexible protection designed to support your family''s long-term financial security.',
    current_date,
    current_date + interval '30 days',
    null,
    null
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
    ('50000000-0000-0000-0000-000000000130', '30000000-0000-0000-0000-000000000101')
on conflict (campaign_id, product_id) do nothing;

insert into campaign_recipients (
    id,
    campaign_id,
    customer_id,
    eligibility_status,
    exclusion_reason,
    eligibility_explanation
)
values (
    '51000000-0000-0000-0000-000000000130',
    '50000000-0000-0000-0000-000000000130',
    '20000000-0000-0000-0000-000000000130',
    'EXCLUDED',
    'MONTHLY_CONTACT_LIMIT',
    'Customer has reached the monthly marketing contact limit and previously received this campaign'
)
on conflict (campaign_id, customer_id) do update
set
    eligibility_status = excluded.eligibility_status,
    exclusion_reason = excluded.exclusion_reason,
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
values
    (
        '52000000-0000-0000-0000-000000000130',
        '20000000-0000-0000-0000-000000000130',
        '50000000-0000-0000-0000-000000000130',
        'EMAIL',
        'SENT',
        'NO_RESPONSE',
        'Sprint 13 AI-006 same-campaign contact for Campaign Test 4 / Ahmed Saleh',
        date_trunc('month', now()) + interval '1 day',
        '10000000-0000-0000-0000-000000000006'
    ),
    (
        '52000000-0000-0000-0000-000000000131',
        '20000000-0000-0000-0000-000000000130',
        null,
        'EMAIL',
        'SENT',
        'NO_RESPONSE',
        'Sprint 13 AI-006 monthly contact 2 of 3 for Ahmed Saleh',
        date_trunc('month', now()) + interval '2 days',
        '10000000-0000-0000-0000-000000000006'
    ),
    (
        '52000000-0000-0000-0000-000000000132',
        '20000000-0000-0000-0000-000000000130',
        null,
        'EMAIL',
        'SENT',
        'NO_RESPONSE',
        'Sprint 13 AI-006 monthly contact 3 of 3 for Ahmed Saleh',
        date_trunc('month', now()) + interval '3 days',
        '10000000-0000-0000-0000-000000000006'
    )
on conflict (id) do update
set
    customer_id = excluded.customer_id,
    campaign_id = excluded.campaign_id,
    channel = excluded.channel,
    event_type = excluded.event_type,
    outcome = excluded.outcome,
    notes = excluded.notes,
    occurred_at = excluded.occurred_at,
    created_by = excluded.created_by;

-- Lena Keller (customer 20000000-...-0102) email engagement timeline for campaign demo.
-- Required fields: customer_id, channel, event_type, occurred_at; campaign_id links metrics;
-- notes carry provider/tracking context the communication service records for OPENED/REPLIED.
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
values
    (
        '52000000-0000-0000-0000-000000000101',
        '20000000-0000-0000-0000-000000000102',
        '50000000-0000-0000-0000-000000000101',
        'EMAIL',
        'SENT',
        'NO_RESPONSE',
        'providerMessageId=demo-email-lena-keller-001; Controlled demo contact event',
        now() - interval '2 hours',
        '10000000-0000-0000-0000-000000000006'
    ),
    (
        '52000000-0000-0000-0000-000000000102',
        '20000000-0000-0000-0000-000000000102',
        '50000000-0000-0000-0000-000000000101',
        'EMAIL',
        'OPENED',
        null,
        'providerMessageId=demo-email-lena-keller-001; trackingReference=demo-open-lena-keller-001',
        now() - interval '90 minutes',
        '10000000-0000-0000-0000-000000000006'
    ),
    (
        '52000000-0000-0000-0000-000000000103',
        '20000000-0000-0000-0000-000000000102',
        '50000000-0000-0000-0000-000000000101',
        'EMAIL',
        'REPLIED',
        'INTERESTED',
        'providerMessageId=demo-email-lena-keller-001; inboundMessageId=demo-reply-lena-keller-001; replyText=Thanks for the product information. I would like to schedule a call.',
        now() - interval '45 minutes',
        '10000000-0000-0000-0000-000000000006'
    )
on conflict (id) do update
set
    customer_id = excluded.customer_id,
    campaign_id = excluded.campaign_id,
    channel = excluded.channel,
    event_type = excluded.event_type,
    outcome = excluded.outcome,
    notes = excluded.notes,
    occurred_at = excluded.occurred_at,
    created_by = excluded.created_by;

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
values
    (
        '52000000-0000-0000-0000-000000000211',
        '20000000-0000-0000-0000-000000000111',
        null,
        'EMAIL',
        'SENT',
        'NO_RESPONSE',
        'Demo monthly contact limit event 1 of 3',
        now() - interval '12 days',
        '10000000-0000-0000-0000-000000000006'
    ),
    (
        '52000000-0000-0000-0000-000000000212',
        '20000000-0000-0000-0000-000000000111',
        null,
        'EMAIL',
        'SENT',
        'NO_RESPONSE',
        'Demo monthly contact limit event 2 of 3',
        now() - interval '7 days',
        '10000000-0000-0000-0000-000000000006'
    ),
    (
        '52000000-0000-0000-0000-000000000213',
        '20000000-0000-0000-0000-000000000111',
        null,
        'PHONE',
        'CALLED',
        'NO_RESPONSE',
        'Demo monthly contact limit event 3 of 3',
        now() - interval '2 days',
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
    20,
    9,
    11,
    1,
    1,
    0,
    1,
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

-- =============================================================================
-- Mock catalog: 10 variants each for products, segments, campaigns, follow-ups,
-- contact history, and reminders (UI demo richness). Deterministic UUIDs.
-- products 300...201-210, segments 400...201-210, criteria 410...201-210,
-- campaigns 500...201-210, contact 520...301-310, follow-ups 530...201-210,
-- reminders 540...201-210.
-- =============================================================================

insert into products (id, name, product_type, description, price, duration_months, expiration_policy)
values
    ('30000000-0000-0000-0000-000000000201', 'Mock Family Life Shield', 'LIFE_INSURANCE', 'Mock product 01 - family term life cover', 89.00, 12, 'RENEWAL_30_DAYS'),
    ('30000000-0000-0000-0000-000000000202', 'Mock Growth Portfolio A', 'INVESTMENT_FUND', 'Mock product 02 - balanced growth fund', 150.00, 24, 'ANNUAL_REVIEW'),
    ('30000000-0000-0000-0000-000000000203', 'Mock Home Secure Plus', 'HOMEOWNER_INSURANCE', 'Mock product 03 - homeowner property cover', 45.50, 12, 'RENEWAL_30_DAYS'),
    ('30000000-0000-0000-0000-000000000204', 'Mock Health Care Basic', 'HEALTH_INSURANCE', 'Mock product 04 - supplemental health plan', 72.00, 12, 'RENEWAL_60_DAYS'),
    ('30000000-0000-0000-0000-000000000205', 'Mock Auto Compact', 'AUTO_INSURANCE', 'Mock product 05 - third-party auto cover', 38.00, 12, 'RENEWAL_30_DAYS'),
    ('30000000-0000-0000-0000-000000000206', 'Mock Senior Life Flex', 'LIFE_INSURANCE', 'Mock product 06 - senior flexible life', 110.00, 36, 'RENEWAL_90_DAYS'),
    ('30000000-0000-0000-0000-000000000207', 'Mock Equity Opportunity', 'INVESTMENT_FUND', 'Mock product 07 - equity opportunity fund', 500.00, 60, 'ANNUAL_REVIEW'),
    ('30000000-0000-0000-0000-000000000208', 'Mock Health Premium', 'HEALTH_INSURANCE', 'Mock product 08 - premium health package', 145.00, 24, 'RENEWAL_30_DAYS'),
    ('30000000-0000-0000-0000-000000000209', 'Mock Auto Full Cover', 'AUTO_INSURANCE', 'Mock product 09 - comprehensive auto', 95.00, 12, 'RENEWAL_30_DAYS'),
    ('30000000-0000-0000-0000-000000000210', 'Mock Wealth Builder Other', 'OTHER', 'Mock product 10 - advisory / other product type', 25.00, 6, 'ONE_TIME')
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

insert into segments (id, name, description, owner_user_id, visibility)
values
    ('40000000-0000-0000-0000-000000000201', 'Mock Berlin Adults', 'Mock segment 01 - adults in Berlin', '10000000-0000-0000-0000-000000000002', 'TEAM'),
    ('40000000-0000-0000-0000-000000000202', 'Mock Munich Prospects', 'Mock segment 02 - Munich prospects', '10000000-0000-0000-0000-000000000002', 'PRIVATE'),
    ('40000000-0000-0000-0000-000000000203', 'Mock Beneficiaries 18-25', 'Mock segment 03 - young beneficiaries', '10000000-0000-0000-0000-000000000002', 'TEAM'),
    ('40000000-0000-0000-0000-000000000204', 'Mock High Interest Customers', 'Mock segment 04 - INTERESTED status', '10000000-0000-0000-0000-000000000002', 'GLOBAL'),
    ('40000000-0000-0000-0000-000000000205', 'Mock Hamburg Region', 'Mock segment 05 - Hamburg city filter', '10000000-0000-0000-0000-000000000002', 'TEAM'),
    ('40000000-0000-0000-0000-000000000206', 'Mock Life Product Owners', 'Mock segment 06 - product ownership style', '10000000-0000-0000-0000-000000000002', 'PRIVATE'),
    ('40000000-0000-0000-0000-000000000207', 'Mock Email Consent Given', 'Mock segment 07 - consent given', '10000000-0000-0000-0000-000000000002', 'TEAM'),
    ('40000000-0000-0000-0000-000000000208', 'Mock Age 41-60 Book', 'Mock segment 08 - mid-career age group', '10000000-0000-0000-0000-000000000002', 'TEAM'),
    ('40000000-0000-0000-0000-000000000209', 'Mock Frankfurt Customers', 'Mock segment 09 - Frankfurt customers', '10000000-0000-0000-0000-000000000002', 'PRIVATE'),
    ('40000000-0000-0000-0000-000000000210', 'Mock Global Prospects Pool', 'Mock segment 10 - all prospects (global visibility)', '10000000-0000-0000-0000-000000000002', 'GLOBAL')
on conflict (id) do update
set
    name = excluded.name,
    description = excluded.description,
    owner_user_id = excluded.owner_user_id,
    visibility = excluded.visibility,
    updated_at = now();

insert into segment_criteria (id, segment_id, field_name, operator, value, logical_group, join_operator)
values
    ('41000000-0000-0000-0000-000000000201', '40000000-0000-0000-0000-000000000201', 'city', 'EQUALS', 'Berlin', 'location', 'AND'),
    ('41000000-0000-0000-0000-000000000202', '40000000-0000-0000-0000-000000000202', 'city', 'EQUALS', 'Munich', 'location', 'AND'),
    ('41000000-0000-0000-0000-000000000203', '40000000-0000-0000-0000-000000000203', 'customer_type', 'EQUALS', 'BENEFICIARY', 'type', 'AND'),
    ('41000000-0000-0000-0000-000000000204', '40000000-0000-0000-0000-000000000204', 'status', 'EQUALS', 'INTERESTED', 'status', 'AND'),
    ('41000000-0000-0000-0000-000000000205', '40000000-0000-0000-0000-000000000205', 'city', 'EQUALS', 'Hamburg', 'location', 'AND'),
    ('41000000-0000-0000-0000-000000000206', '40000000-0000-0000-0000-000000000206', 'customer_type', 'EQUALS', 'CUSTOMER', 'type', 'AND'),
    ('41000000-0000-0000-0000-000000000207', '40000000-0000-0000-0000-000000000207', 'consent_status', 'EQUALS', 'GIVEN', 'consent', 'AND'),
    ('41000000-0000-0000-0000-000000000208', '40000000-0000-0000-0000-000000000208', 'age_group', 'EQUALS', '41_60', 'demographics', 'AND'),
    ('41000000-0000-0000-0000-000000000209', '40000000-0000-0000-0000-000000000209', 'city', 'EQUALS', 'Frankfurt', 'location', 'AND'),
    ('41000000-0000-0000-0000-000000000210', '40000000-0000-0000-0000-000000000210', 'customer_type', 'EQUALS', 'PROSPECT', 'type', 'AND')
on conflict (id) do update
set
    field_name = excluded.field_name,
    operator = excluded.operator,
    value = excluded.value,
    logical_group = excluded.logical_group,
    join_operator = excluded.join_operator;

insert into campaigns (
    id, name, objective, status, owner_user_id, segment_id, channel,
    message_subject, message_body, start_date, end_date, approved_by, approved_at, rejection_reason
)
values
    ('50000000-0000-0000-0000-000000000201', 'Mock Draft Welcome Series', 'Mock campaign 01 - draft welcome nurture', 'DRAFT', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000201', 'EMAIL', 'Welcome to Bayer-Westphalian', 'Mock draft email body for welcome series.', current_date, current_date + interval '14 days', null, null, null),
    ('50000000-0000-0000-0000-000000000202', 'Mock Submitted Product Push', 'Mock campaign 02 - awaiting compliance', 'SUBMITTED', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000202', 'EMAIL', 'New investment opportunities', 'Mock submitted campaign body for product push.', current_date + interval '1 day', current_date + interval '21 days', null, null, null),
    ('50000000-0000-0000-0000-000000000203', 'Mock Approved SMS Nudge', 'Mock campaign 03 - approved SMS outreach', 'APPROVED', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000203', 'SMS', null, 'Mock SMS: Your product review is ready. Reply STOP to opt out.', current_date, current_date + interval '7 days', '10000000-0000-0000-0000-000000000005', now() - interval '2 hours', null),
    ('50000000-0000-0000-0000-000000000204', 'Mock Rejected Phone Drive', 'Mock campaign 04 - rejected for compliance notes', 'REJECTED', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000204', 'PHONE', 'Call script: product renewal', 'Mock phone script body for rejection sample.', current_date - interval '5 days', current_date + interval '10 days', '10000000-0000-0000-0000-000000000005', now() - interval '1 day', 'Missing consent evidence for phone channel'),
    ('50000000-0000-0000-0000-000000000205', 'Mock Active Mixed Channel', 'Mock campaign 05 - currently active mixed channel', 'ACTIVE', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000205', 'MIXED', 'Your personalized offer', 'Mock active multi-channel campaign body.', current_date - interval '3 days', current_date + interval '27 days', '10000000-0000-0000-0000-000000000005', now() - interval '4 days', null),
    ('50000000-0000-0000-0000-000000000206', 'Mock Paused Retention', 'Mock campaign 06 - paused retention wave', 'PAUSED', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000206', 'EMAIL', 'We paused outreach temporarily', 'Mock paused retention campaign body.', current_date - interval '10 days', current_date + interval '20 days', '10000000-0000-0000-0000-000000000005', now() - interval '9 days', null),
    ('50000000-0000-0000-0000-000000000207', 'Mock Completed Q1 Drive', 'Mock campaign 07 - completed quarterly drive', 'COMPLETED', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000207', 'EMAIL', 'Thank you for engaging with us', 'Mock completed campaign body.', current_date - interval '60 days', current_date - interval '30 days', '10000000-0000-0000-0000-000000000005', now() - interval '61 days', null),
    ('50000000-0000-0000-0000-000000000208', 'Mock Archived Legacy Blast', 'Mock campaign 08 - archived historical blast', 'ARCHIVED', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000208', 'EMAIL', 'Archived seasonal offer', 'Mock archived campaign body.', current_date - interval '180 days', current_date - interval '150 days', '10000000-0000-0000-0000-000000000005', now() - interval '181 days', null),
    ('50000000-0000-0000-0000-000000000209', 'Mock Approved Email Renewal', 'Mock campaign 09 - approved policy renewal email', 'APPROVED', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000209', 'EMAIL', 'Policy renewal window open', 'Mock approved renewal email body.', current_date + interval '2 days', current_date + interval '32 days', '10000000-0000-0000-0000-000000000005', now() - interval '6 hours', null),
    ('50000000-0000-0000-0000-000000000210', 'Mock Draft SMS Reminder Wave', 'Mock campaign 10 - draft SMS reminder wave', 'DRAFT', '10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000210', 'SMS', null, 'Mock SMS draft: Payment reminder for your product.', current_date + interval '5 days', current_date + interval '12 days', null, null, null)
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
    rejection_reason = excluded.rejection_reason,
    updated_at = now();

insert into campaign_products (campaign_id, product_id)
values
    ('50000000-0000-0000-0000-000000000201', '30000000-0000-0000-0000-000000000201'),
    ('50000000-0000-0000-0000-000000000202', '30000000-0000-0000-0000-000000000202'),
    ('50000000-0000-0000-0000-000000000203', '30000000-0000-0000-0000-000000000203'),
    ('50000000-0000-0000-0000-000000000204', '30000000-0000-0000-0000-000000000204'),
    ('50000000-0000-0000-0000-000000000205', '30000000-0000-0000-0000-000000000205'),
    ('50000000-0000-0000-0000-000000000206', '30000000-0000-0000-0000-000000000206'),
    ('50000000-0000-0000-0000-000000000207', '30000000-0000-0000-0000-000000000207'),
    ('50000000-0000-0000-0000-000000000208', '30000000-0000-0000-0000-000000000208'),
    ('50000000-0000-0000-0000-000000000209', '30000000-0000-0000-0000-000000000209'),
    ('50000000-0000-0000-0000-000000000210', '30000000-0000-0000-0000-000000000210')
on conflict (campaign_id, product_id) do nothing;

insert into follow_up_tasks (
    id, customer_id, campaign_id, assigned_to, title, description, due_date, status, priority, completed_at
)
values
    ('53000000-0000-0000-0000-000000000201', '20000000-0000-0000-0000-000000000101', '50000000-0000-0000-0000-000000000201', '10000000-0000-0000-0000-000000000006', 'Mock FU 01 - Call Anna about life product', 'Mock open follow-up after email open.', current_date + interval '1 day', 'OPEN', 'HIGH', null),
    ('53000000-0000-0000-0000-000000000202', '20000000-0000-0000-0000-000000000102', '50000000-0000-0000-0000-000000000203', '10000000-0000-0000-0000-000000000006', 'Mock FU 02 - SMS reply follow-up for Lena', 'Mock in-progress follow-up after SMS reply.', current_date + interval '2 days', 'IN_PROGRESS', 'MEDIUM', null),
    ('53000000-0000-0000-0000-000000000203', '20000000-0000-0000-0000-000000000103', '50000000-0000-0000-0000-000000000205', '10000000-0000-0000-0000-000000000002', 'Mock FU 03 - Campaign manager review Jonas', 'Mock completed manager review task.', current_date - interval '1 day', 'COMPLETED', 'LOW', now() - interval '6 hours'),
    ('53000000-0000-0000-0000-000000000204', '20000000-0000-0000-0000-000000000104', '50000000-0000-0000-0000-000000000204', '10000000-0000-0000-0000-000000000006', 'Mock FU 04 - Cancelled consent check Mia', 'Mock cancelled because consent withdrawn.', current_date + interval '5 days', 'CANCELLED', 'MEDIUM', null),
    ('53000000-0000-0000-0000-000000000205', '20000000-0000-0000-0000-000000000105', '50000000-0000-0000-0000-000000000209', '10000000-0000-0000-0000-000000000006', 'Mock FU 05 - Renewal discussion Noah', 'Mock open renewal discussion.', current_date + interval '4 days', 'OPEN', 'HIGH', null),
    ('53000000-0000-0000-0000-000000000206', '20000000-0000-0000-0000-000000000106', null, '10000000-0000-0000-0000-000000000006', 'Mock FU 06 - Missing consent outreach Sofia', 'Mock follow-up without campaign link.', current_date + interval '6 days', 'OPEN', 'LOW', null),
    ('53000000-0000-0000-0000-000000000207', '20000000-0000-0000-0000-000000000111', '50000000-0000-0000-0000-000000000206', '10000000-0000-0000-0000-000000000002', 'Mock FU 07 - Monthly limit coaching Paul', 'Mock in-progress coaching for contact limit.', current_date + interval '3 days', 'IN_PROGRESS', 'HIGH', null),
    ('53000000-0000-0000-0000-000000000208', '20000000-0000-0000-0000-000000000112', '50000000-0000-0000-0000-000000000207', '10000000-0000-0000-0000-000000000006', 'Mock FU 08 - Post-campaign thank you Greta', 'Mock completed thank-you task.', current_date - interval '2 days', 'COMPLETED', 'MEDIUM', now() - interval '1 day'),
    ('53000000-0000-0000-0000-000000000209', '20000000-0000-0000-0000-000000000113', '50000000-0000-0000-0000-000000000205', '10000000-0000-0000-0000-000000000006', 'Mock FU 09 - Product fit call Felix', 'Mock open product fit call.', current_date + interval '8 days', 'OPEN', 'MEDIUM', null),
    ('53000000-0000-0000-0000-000000000210', '20000000-0000-0000-0000-000000000115', '50000000-0000-0000-0000-000000000202', '10000000-0000-0000-0000-000000000002', 'Mock FU 10 - Submitted campaign prep Oscar', 'Mock open prep before launch.', current_date + interval '1 day', 'OPEN', 'HIGH', null)
on conflict (id) do update
set
    customer_id = excluded.customer_id,
    campaign_id = excluded.campaign_id,
    assigned_to = excluded.assigned_to,
    title = excluded.title,
    description = excluded.description,
    due_date = excluded.due_date,
    status = excluded.status,
    priority = excluded.priority,
    completed_at = excluded.completed_at;

insert into contact_events (
    id, customer_id, campaign_id, channel, event_type, outcome, notes, occurred_at, created_by
)
values
    ('52000000-0000-0000-0000-000000000301', '20000000-0000-0000-0000-000000000101', '50000000-0000-0000-0000-000000000205', 'EMAIL', 'SENT', 'NO_RESPONSE', 'Mock contact 01 - email sent to Anna Keller', now() - interval '10 days', '10000000-0000-0000-0000-000000000002'),
    ('52000000-0000-0000-0000-000000000302', '20000000-0000-0000-0000-000000000101', '50000000-0000-0000-0000-000000000205', 'EMAIL', 'OPENED', null, 'Mock contact 02 - email opened by Anna Keller', now() - interval '9 days', '10000000-0000-0000-0000-000000000002'),
    ('52000000-0000-0000-0000-000000000303', '20000000-0000-0000-0000-000000000101', '50000000-0000-0000-0000-000000000205', 'EMAIL', 'CLICKED', 'INTERESTED', 'Mock contact 03 - link clicked by Anna Keller', now() - interval '8 days', '10000000-0000-0000-0000-000000000002'),
    ('52000000-0000-0000-0000-000000000304', '20000000-0000-0000-0000-000000000102', '50000000-0000-0000-0000-000000000203', 'SMS', 'SENT', 'NO_RESPONSE', 'Mock contact 04 - SMS sent to Lena Keller', now() - interval '7 days', '10000000-0000-0000-0000-000000000006'),
    ('52000000-0000-0000-0000-000000000305', '20000000-0000-0000-0000-000000000102', '50000000-0000-0000-0000-000000000203', 'SMS', 'REPLIED', 'INTERESTED', 'Mock contact 05 - SMS reply from Lena Keller', now() - interval '6 days', '10000000-0000-0000-0000-000000000006'),
    ('52000000-0000-0000-0000-000000000306', '20000000-0000-0000-0000-000000000103', '50000000-0000-0000-0000-000000000205', 'PHONE', 'CALLED', 'NOT_INTERESTED', 'Mock contact 06 - phone call Jonas Weber', now() - interval '5 days', '10000000-0000-0000-0000-000000000006'),
    ('52000000-0000-0000-0000-000000000307', '20000000-0000-0000-0000-000000000104', null, 'EMAIL', 'FAILED', 'FAILED', 'Mock contact 07 - email failed for Mia Schneider', now() - interval '4 days', '10000000-0000-0000-0000-000000000002'),
    ('52000000-0000-0000-0000-000000000308', '20000000-0000-0000-0000-000000000105', '50000000-0000-0000-0000-000000000209', 'EMAIL', 'SENT', 'CONVERTED', 'Mock contact 08 - conversion path Noah Fischer', now() - interval '3 days', '10000000-0000-0000-0000-000000000002'),
    ('52000000-0000-0000-0000-000000000309', '20000000-0000-0000-0000-000000000112', '50000000-0000-0000-0000-000000000207', 'IN_APP', 'NOTE', null, 'Mock contact 09 - in-app agent note for Greta Schulz', now() - interval '2 days', '10000000-0000-0000-0000-000000000006'),
    ('52000000-0000-0000-0000-000000000310', '20000000-0000-0000-0000-000000000113', '50000000-0000-0000-0000-000000000205', 'EMAIL', 'UNSUBSCRIBED', null, 'Mock contact 10 - unsubscribe Felix Koch', now() - interval '1 day', '10000000-0000-0000-0000-000000000002')
on conflict (id) do update
set
    customer_id = excluded.customer_id,
    campaign_id = excluded.campaign_id,
    channel = excluded.channel,
    event_type = excluded.event_type,
    outcome = excluded.outcome,
    notes = excluded.notes,
    occurred_at = excluded.occurred_at,
    created_by = excluded.created_by;

insert into reminder_schedules (
    id, customer_id, product_id, reminder_type, reminder_level, scheduled_date, status, sent_at
)
values
    ('54000000-0000-0000-0000-000000000201', '20000000-0000-0000-0000-000000000101', '30000000-0000-0000-0000-000000000201', 'PRODUCT_EXPIRATION', 'GREEN', current_date + interval '30 days', 'PENDING', null),
    ('54000000-0000-0000-0000-000000000202', '20000000-0000-0000-0000-000000000101', '30000000-0000-0000-0000-000000000101', 'PAYMENT_DUE', 'YELLOW', current_date + interval '10 days', 'PENDING', null),
    ('54000000-0000-0000-0000-000000000203', '20000000-0000-0000-0000-000000000102', '30000000-0000-0000-0000-000000000202', 'PRODUCT_EXPIRATION', 'RED', current_date + interval '3 days', 'PENDING', null),
    ('54000000-0000-0000-0000-000000000204', '20000000-0000-0000-0000-000000000105', '30000000-0000-0000-0000-000000000204', 'PAYMENT_DUE', 'RED', current_date - interval '1 day', 'SENT', now() - interval '12 hours'),
    ('54000000-0000-0000-0000-000000000205', '20000000-0000-0000-0000-000000000106', '30000000-0000-0000-0000-000000000205', 'PRODUCT_EXPIRATION', 'YELLOW', current_date + interval '14 days', 'PENDING', null),
    ('54000000-0000-0000-0000-000000000206', '20000000-0000-0000-0000-000000000111', '30000000-0000-0000-0000-000000000206', 'PAYMENT_DUE', 'GREEN', current_date + interval '20 days', 'PENDING', null),
    ('54000000-0000-0000-0000-000000000207', '20000000-0000-0000-0000-000000000112', '30000000-0000-0000-0000-000000000207', 'PRODUCT_EXPIRATION', 'GREEN', current_date + interval '45 days', 'CANCELLED', null),
    ('54000000-0000-0000-0000-000000000208', '20000000-0000-0000-0000-000000000113', '30000000-0000-0000-0000-000000000208', 'PAYMENT_DUE', 'YELLOW', current_date + interval '7 days', 'FAILED', null),
    ('54000000-0000-0000-0000-000000000209', '20000000-0000-0000-0000-000000000114', '30000000-0000-0000-0000-000000000209', 'PRODUCT_EXPIRATION', 'RED', current_date + interval '2 days', 'PENDING', null),
    ('54000000-0000-0000-0000-000000000210', '20000000-0000-0000-0000-000000000115', '30000000-0000-0000-0000-000000000210', 'PAYMENT_DUE', 'GREEN', current_date + interval '12 days', 'PENDING', null)
on conflict (id) do update
set
    customer_id = excluded.customer_id,
    product_id = excluded.product_id,
    reminder_type = excluded.reminder_type,
    reminder_level = excluded.reminder_level,
    scheduled_date = excluded.scheduled_date,
    status = excluded.status,
    sent_at = excluded.sent_at;
