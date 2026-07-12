# Role-Based Access Documentation

This document records the role-based access model for the Bayer-Westphalian Campaign Management
Platform in accordance with the knowledge base.

## Access Principles

- Backend authorization is authoritative.
- Frontend checks only control navigation and user experience.
- Pages and APIs are restricted by role.
- Protected endpoints fail without authentication.
- Authenticated users with insufficient roles receive `403 Forbidden`.
- Unmatched backend API routes are denied by default until an explicit role rule is added.
- Sensitive user-management and role-change actions are audited.

## Role Inventory

MVP roles:

- `ADMIN`
- `CAMPAIGN_MANAGER`
- `BI_ANALYST`
- `PRODUCT_MANAGER`
- `COMPLIANCE_OFFICER`
- `CUSTOMER_SERVICE_AGENT`

Extended enterprise roles:

- `SALES_AGENT`
- `MARKETING_ANALYST`
- `EXECUTIVE_VIEWER`
- `SYSTEM_AUDITOR`

## User And Role Administration

Only `ADMIN` can manage employee accounts and role assignments.

Admin user management includes:

- Create users
- Edit users
- Disable users
- Assign roles
- Reset passwords
- View user-management screens

Campaign Managers, BI Analysts, Product Managers, Compliance Officers, Customer Service Agents,
and extended enterprise roles cannot manage users unless they also have `ADMIN`.

## Backend Endpoint Authorization

The backend enforces access with Spring Security and method-level authorization.

- `/api/users/**`, `/api/roles/**`: `ADMIN`
- Customer create/update workflows: `ADMIN`, `CUSTOMER_SERVICE_AGENT`, limited
  `COMPLIANCE_OFFICER`
- Customer read workflows: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`,
  `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`
- Product read workflows: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `PRODUCT_MANAGER`,
  `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `EXECUTIVE_VIEWER`
- Product management: `ADMIN`, `PRODUCT_MANAGER`
- Segment creation (`POST /api/segments`, `@authz.canCreateSegments()` / `@SegmentCreateAccess`):
  `ADMIN`, `CAMPAIGN_MANAGER` (KB FR-077 / item 201 — Campaign Manager can create reusable segments
  with name, visibility PRIVATE/TEAM/GLOBAL, and filter criteria for later campaign targeting)
- Segment management edit/delete (`PUT`/`DELETE /api/segments/**`, `@authz.canManageSegments()`):
  `ADMIN`, `CAMPAIGN_MANAGER`
- **BI Analyst cannot edit segments unless allowed** (item 200): `BI_ANALYST` alone has
  read + preview only. Edit/create require `CAMPAIGN_MANAGER` or `ADMIN`. A dual-role user who also
  holds `CAMPAIGN_MANAGER` (or `ADMIN`) is allowed to edit via those roles.
- Segment read (`GET /api/segments/**`): `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`,
  `COMPLIANCE_OFFICER`
- Segment preview (`POST /api/segments/preview`, `@authz.canPreviewSegments()`): `ADMIN`,
  `CAMPAIGN_MANAGER`, `BI_ANALYST`
- Campaign read workflows: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `PRODUCT_MANAGER`,
  `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `EXECUTIVE_VIEWER`,
  `SYSTEM_AUDITOR`
- Campaign write workflows: `CAMPAIGN_MANAGER`
- Campaign approval/rejection: `COMPLIANCE_OFFICER`
- Campaign recipients: `CAMPAIGN_MANAGER`, `COMPLIANCE_OFFICER`
- Reminder read workflows: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`,
  `COMPLIANCE_OFFICER`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `SYSTEM_AUDITOR`
- Analytics and management reports (`/api/analytics/**`, `/api/reports/**`; see
  [Analytics Module Documentation](../modules/analytics-module.md)): `ADMIN`, `BI_ANALYST`,
  `CAMPAIGN_MANAGER`,
  `MARKETING_ANALYST`, `EXECUTIVE_VIEWER`
- AI recommendation reads: `ADMIN`, `CAMPAIGN_MANAGER`, `BI_ANALYST`, `PRODUCT_MANAGER`,
  `COMPLIANCE_OFFICER`, `EXECUTIVE_VIEWER`, `SYSTEM_AUDITOR` — see
  [AI Feature Documentation](../modules/ai-features.md) (item 506)
- Audit logs: `ADMIN`, `COMPLIANCE_OFFICER`, `SYSTEM_AUDITOR`

## Frontend Navigation

The frontend decodes role claims from the access token and filters menus. This improves usability
but does not replace backend enforcement.

| Role | Expected menu access |
| --- | --- |
| `ADMIN` | Dashboard, Customers, Products, Segments, Campaigns, Compliance, Analytics, Reports, Users, Audit |
| `CAMPAIGN_MANAGER` | Dashboard, Customers, Products, Segments, Campaigns, Compliance, Analytics |
| `BI_ANALYST` | Dashboard, Customers, Products, Segments, Analytics, Reports |
| `COMPLIANCE_OFFICER` | Dashboard, Customers, Campaigns, Compliance, Reports, Audit |

## Frontend accessibility (related)

Shell navigation labels and role-filtered menus are part of the accessibility baseline. See
[Accessibility Notes](../development/accessibility-notes.md) (item **611**) and
[UI Role-Based Menu](../testing/ui-role-based-menu.md) (item **607**). Backend authorization remains
authoritative (`SEC-002` / `SEC-003`).

## Verification

Role-based access is verified by:

- Security tests for unauthenticated protected endpoints.
- Security tests for insufficient-role protected endpoints.
- Security tests proving protected resource families require the correct backend role.
- Security tests proving unconfigured backend API routes are denied by default.
- Security tests proving Admin can create users, disable users, and assign roles.
- Security tests proving Campaign Manager cannot manage users.
- Frontend navigation tests proving role-based menus show only allowed menus.
