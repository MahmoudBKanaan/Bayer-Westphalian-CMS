insert into users (id, email, password_hash, full_name, status)
values
    (
        '10000000-0000-0000-0000-000000000001',
        'admin@bayer-westphalian.test',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiJv8QwIEpF7bLlwY1dwwTq9gQz8x6W',
        'MVP Admin',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'campaign.manager@bayer-westphalian.test',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiJv8QwIEpF7bLlwY1dwwTq9gQz8x6W',
        'MVP Campaign Manager',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'bi.analyst@bayer-westphalian.test',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiJv8QwIEpF7bLlwY1dwwTq9gQz8x6W',
        'MVP BI Analyst',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'product.manager@bayer-westphalian.test',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiJv8QwIEpF7bLlwY1dwwTq9gQz8x6W',
        'MVP Product Manager',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        'compliance.officer@bayer-westphalian.test',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiJv8QwIEpF7bLlwY1dwwTq9gQz8x6W',
        'MVP Compliance Officer',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000006',
        'customer.service@bayer-westphalian.test',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiJv8QwIEpF7bLlwY1dwwTq9gQz8x6W',
        'MVP Customer Service Agent',
        'ACTIVE'
    )
on conflict (email) do update
set
    password_hash = excluded.password_hash,
    full_name = excluded.full_name,
    status = excluded.status,
    updated_at = now();

insert into user_roles (user_id, role_id, assigned_by)
select seeded_users.user_id, roles.id, admin_user.id
from (
    values
        ('10000000-0000-0000-0000-000000000001'::uuid, 'ADMIN'::system_role_name),
        ('10000000-0000-0000-0000-000000000002'::uuid, 'CAMPAIGN_MANAGER'::system_role_name),
        ('10000000-0000-0000-0000-000000000003'::uuid, 'BI_ANALYST'::system_role_name),
        ('10000000-0000-0000-0000-000000000004'::uuid, 'PRODUCT_MANAGER'::system_role_name),
        ('10000000-0000-0000-0000-000000000005'::uuid, 'COMPLIANCE_OFFICER'::system_role_name),
        (
            '10000000-0000-0000-0000-000000000006'::uuid,
            'CUSTOMER_SERVICE_AGENT'::system_role_name
        )
) as seeded_users(user_id, role_name)
join roles on roles.name = seeded_users.role_name
join users admin_user on admin_user.email = 'admin@bayer-westphalian.test'
on conflict (user_id, role_id) do update
set assigned_by = excluded.assigned_by;
