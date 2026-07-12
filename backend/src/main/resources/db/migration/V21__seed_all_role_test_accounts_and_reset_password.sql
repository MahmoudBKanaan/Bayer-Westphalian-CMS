-- Seed one ACTIVE test account for every KB system role and set a shared login password.
-- Password (plaintext): Neoarel@7368
-- BCrypt (Spring Security BCryptPasswordEncoder): see password_hash below.

insert into users (id, email, password_hash, full_name, status)
values
    (
        '10000000-0000-0000-0000-000000000001',
        'admin@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test Admin',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'campaign.manager@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test Campaign Manager',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'bi.analyst@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test BI Analyst',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'product.manager@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test Product Manager',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        'compliance.officer@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test Compliance Officer',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000006',
        'customer.service@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test Customer Service Agent',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000007',
        'sales.agent@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test Sales Agent',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000008',
        'marketing.analyst@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test Marketing Analyst',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000009',
        'executive.viewer@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test Executive Viewer',
        'ACTIVE'
    ),
    (
        '10000000-0000-0000-0000-000000000010',
        'system.auditor@bayer-westphalian.test',
        '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
        'Test System Auditor',
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
        ('10000000-0000-0000-0000-000000000006'::uuid, 'CUSTOMER_SERVICE_AGENT'::system_role_name),
        ('10000000-0000-0000-0000-000000000007'::uuid, 'SALES_AGENT'::system_role_name),
        ('10000000-0000-0000-0000-000000000008'::uuid, 'MARKETING_ANALYST'::system_role_name),
        ('10000000-0000-0000-0000-000000000009'::uuid, 'EXECUTIVE_VIEWER'::system_role_name),
        ('10000000-0000-0000-0000-000000000010'::uuid, 'SYSTEM_AUDITOR'::system_role_name)
) as seeded_users(user_id, role_name)
join roles on roles.name = seeded_users.role_name
join users admin_user on admin_user.email = 'admin@bayer-westphalian.test'
on conflict (user_id, role_id) do update
set assigned_by = excluded.assigned_by;

-- Ensure every listed test account uses the shared password (covers any pre-existing rows).
update users
set
    password_hash = '$2a$10$HHvuCrxGvdTIKKbGZ9sIH.MwhGTVtHWeBV2p6Gfn0bjjhvxdxaVLm',
    status = 'ACTIVE',
    updated_at = now()
where email in (
    'admin@bayer-westphalian.test',
    'campaign.manager@bayer-westphalian.test',
    'bi.analyst@bayer-westphalian.test',
    'product.manager@bayer-westphalian.test',
    'compliance.officer@bayer-westphalian.test',
    'customer.service@bayer-westphalian.test',
    'sales.agent@bayer-westphalian.test',
    'marketing.analyst@bayer-westphalian.test',
    'executive.viewer@bayer-westphalian.test',
    'system.auditor@bayer-westphalian.test'
);
