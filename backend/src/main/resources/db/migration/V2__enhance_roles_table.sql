alter table roles
    add column display_name varchar(100),
    add column allowed_functions text,
    add column mvp_role boolean not null default false,
    add column created_at timestamptz not null default now(),
    add column updated_at timestamptz not null default now();

update roles
set
    display_name = 'Admin',
    description = 'Manages users, roles, settings, and full system configuration',
    allowed_functions = 'Manage users, assign roles, manage settings, view all modules, configure limits, view audit logs',
    mvp_role = true,
    updated_at = now()
where name = 'ADMIN';

update roles
set
    display_name = 'Campaign Manager',
    description = 'Creates campaigns, segments, recipients, messages, schedules, and launches approved campaigns',
    allowed_functions = 'Create/edit campaigns, define segments, preview recipients, submit campaigns, launch approved campaigns, manage follow-ups, view campaign analytics',
    mvp_role = true,
    updated_at = now()
where name = 'CAMPAIGN_MANAGER';

update roles
set
    display_name = 'BI Analyst',
    description = 'Views dashboards, reports, customer analytics, segmentation insights, and performance data',
    allowed_functions = 'View analytics, reports, segmentation insights, audience counts, campaign performance, product performance; may create analytical segment drafts if allowed',
    mvp_role = true,
    updated_at = now()
where name = 'BI_ANALYST';

update roles
set
    display_name = 'Product Manager',
    description = 'Manages insurance/investment products and product-change requests',
    allowed_functions = 'Create/edit/disable products, manage product details, create product-change requests, view product performance',
    mvp_role = true,
    updated_at = now()
where name = 'PRODUCT_MANAGER';

update roles
set
    display_name = 'Compliance Officer',
    description = 'Reviews consent, opt-outs, eligibility, campaign approval, and audit logs',
    allowed_functions = 'Review consent, opt-outs, guardian consent, eligibility, approve/reject campaigns, view audit logs and compliance reports',
    mvp_role = true,
    updated_at = now()
where name = 'COMPLIANCE_OFFICER';

update roles
set
    display_name = 'Customer Service Agent',
    description = 'Manages customer/prospect details, notes, contact outcomes, and consent updates',
    allowed_functions = 'Create/update customers, update contact details, record consent, mark opt-outs, add notes, update contact outcomes, manage follow-up tasks',
    mvp_role = true,
    updated_at = now()
where name = 'CUSTOMER_SERVICE_AGENT';

update roles
set
    display_name = 'Sales Agent',
    description = 'Follows up with assigned interested prospects and updates conversion status',
    allowed_functions = 'View assigned leads, update contact outcomes, mark interested/not interested/converted, complete follow-up tasks',
    mvp_role = false,
    updated_at = now()
where name = 'SALES_AGENT';

update roles
set
    display_name = 'Marketing Analyst',
    description = 'Reviews campaign metrics, audience behavior, and campaign performance',
    allowed_functions = 'View campaign metrics, audience segment performance, reports, segmentation insights, and recommend targeting improvements',
    mvp_role = false,
    updated_at = now()
where name = 'MARKETING_ANALYST';

update roles
set
    display_name = 'Executive Viewer',
    description = 'Views high-level dashboards and management reports only',
    allowed_functions = 'View read-only dashboards, ROI, campaign summaries, and product performance reports',
    mvp_role = false,
    updated_at = now()
where name = 'EXECUTIVE_VIEWER';

update roles
set
    display_name = 'System Auditor',
    description = 'Reviews audit logs, consent history, approval history, and sensitive actions',
    allowed_functions = 'View audit logs, consent history, campaign approval history, user activity history, and export audit reports',
    mvp_role = false,
    updated_at = now()
where name = 'SYSTEM_AUDITOR';

alter table roles
    alter column display_name set not null,
    alter column description set not null,
    alter column allowed_functions set not null;
