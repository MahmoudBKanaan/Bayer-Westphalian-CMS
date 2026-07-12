insert into roles (id, name, display_name, description, allowed_functions, mvp_role)
values
    (
        '00000000-0000-0000-0000-000000000001',
        'ADMIN',
        'Admin',
        'Manages users, roles, settings, and full system configuration',
        'Manage users, assign roles, manage settings, view all modules, configure limits, view audit logs',
        true
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'CAMPAIGN_MANAGER',
        'Campaign Manager',
        'Creates campaigns, segments, recipients, messages, schedules, and launches approved campaigns',
        'Create/edit campaigns, define segments, preview recipients, submit campaigns, launch approved campaigns, manage follow-ups, view campaign analytics',
        true
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        'BI_ANALYST',
        'BI Analyst',
        'Views dashboards, reports, customer analytics, segmentation insights, and performance data',
        'View analytics, reports, segmentation insights, audience counts, campaign performance, product performance; may create analytical segment drafts if allowed',
        true
    ),
    (
        '00000000-0000-0000-0000-000000000004',
        'PRODUCT_MANAGER',
        'Product Manager',
        'Manages insurance/investment products and product-change requests',
        'Create/edit/disable products, manage product details, create product-change requests, view product performance',
        true
    ),
    (
        '00000000-0000-0000-0000-000000000005',
        'COMPLIANCE_OFFICER',
        'Compliance Officer',
        'Reviews consent, opt-outs, eligibility, campaign approval, and audit logs',
        'Review consent, opt-outs, guardian consent, eligibility, approve/reject campaigns, view audit logs and compliance reports',
        true
    ),
    (
        '00000000-0000-0000-0000-000000000006',
        'CUSTOMER_SERVICE_AGENT',
        'Customer Service Agent',
        'Manages customer/prospect details, notes, contact outcomes, and consent updates',
        'Create/update customers, update contact details, record consent, mark opt-outs, add notes, update contact outcomes, manage follow-up tasks',
        true
    ),
    (
        '00000000-0000-0000-0000-000000000007',
        'SALES_AGENT',
        'Sales Agent',
        'Follows up with assigned interested prospects and updates conversion status',
        'View assigned leads, update contact outcomes, mark interested/not interested/converted, complete follow-up tasks',
        false
    ),
    (
        '00000000-0000-0000-0000-000000000008',
        'MARKETING_ANALYST',
        'Marketing Analyst',
        'Reviews campaign metrics, audience behavior, and campaign performance',
        'View campaign metrics, audience segment performance, reports, segmentation insights, and recommend targeting improvements',
        false
    ),
    (
        '00000000-0000-0000-0000-000000000009',
        'EXECUTIVE_VIEWER',
        'Executive Viewer',
        'Views high-level dashboards and management reports only',
        'View read-only dashboards, ROI, campaign summaries, and product performance reports',
        false
    ),
    (
        '00000000-0000-0000-0000-000000000010',
        'SYSTEM_AUDITOR',
        'System Auditor',
        'Reviews audit logs, consent history, approval history, and sensitive actions',
        'View audit logs, consent history, campaign approval history, user activity history, and export audit reports',
        false
    )
on conflict (name) do update
set
    display_name = excluded.display_name,
    description = excluded.description,
    allowed_functions = excluded.allowed_functions,
    mvp_role = excluded.mvp_role,
    updated_at = now();
