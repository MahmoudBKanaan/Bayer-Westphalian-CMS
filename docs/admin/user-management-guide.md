# Admin User-Management Guide

This guide explains how Admin users manage internal employee accounts for the
Bayer-Westphalian Campaign Management Platform in accordance with the knowledge base.

## Scope

The platform is not a public customer portal. There is no public signup. Employee accounts are
created, updated, disabled, and assigned roles by Admin users.

## Admin Permissions

Only users with the `ADMIN` role can manage users and roles.

Admin user-management permissions include:

- Create employee users
- Edit employee names and account status
- Disable employee users
- Assign roles
- Reset passwords
- View the user-management screen
- Open **Settings** (`/settings`, item 534) to configure monthly contact limit, send retry limit,
  and uninterested exclusion period

Campaign Managers, BI Analysts, Product Managers, Compliance Officers, Customer Service Agents,
and extended enterprise roles cannot manage users unless they also have `ADMIN`.

## Create A User

1. Open the `Users` menu.
2. Enter the employee full name.
3. Enter the employee email address.
4. Enter a temporary password.
5. Submit the create-user form.

The backend stores the password as a BCrypt hash. Password hashes are never returned in API user
views.

## Edit A User

1. Select an employee from the user selector.
2. Update the full name if needed.
3. Update the account status if needed.
4. Save changes.

Supported account statuses are:

- `ACTIVE`
- `DISABLED`
- `LOCKED`

Disabled or locked users cannot log in (Sprint 16 critical item **659** —
`DisabledUserCannotLogInTests`; status must be `ACTIVE` for `POST /api/auth/login`).

## Disable A User

1. Select an employee from the user selector.
2. Choose `Disable user`.
3. Confirm that the account status changes to `DISABLED`.

Disable is the normal account-removal workflow. Permanent deletion is not part of the MVP.

Item 522: successful disable (`PATCH /api/users/{id}/disable`) writes a `DISABLE_USER` audit row on
entity type `users` with the Admin actor and before/after `status` (plus email and full name).
Disabling an already-disabled account is a no-op and does not create another audit entry.

## Assign A Role

1. Select an employee from the user selector.
2. Review the current roles.
3. Select a role that the user does not already have.
4. Choose `Assign role`.

The UI disables duplicate role choices. The backend enforces role assignment rules and records
role-change audit activity (item 521): each successful new role assignment writes an
`ASSIGN_ROLE` audit row on entity type `users` with the Admin actor, previous role set, and new
role set (including `assignedRole` / `roleId`). Re-assigning an existing role is a no-op and does
not create another audit entry.

## Reset A Password

1. Select an employee from the user selector.
2. Enter a new temporary password.
3. Choose `Reset password`.
4. Communicate the temporary password through an approved internal process.

The password reset stores a new BCrypt hash and never exposes the raw password after submission.

## Error Handling

- Duplicate email addresses are rejected.
- Invalid email, blank name, and blank password inputs are rejected.
- Missing authentication returns an unauthorized response.
- Non-Admin users receive a forbidden response.
- User-management failures show a user-facing error message.

## Audit Expectations

The following actions are sensitive and must be auditable:

- User creation (item 520): successful `POST /api/users` writes a `CREATE` row on entity type
  `users` with the Admin actor, email, full name, and status — never the password or hash
- User disable (item 522): successful `PATCH /api/users/{id}/disable` writes `DISABLE_USER` with
  before/after status via `AuditService.logUserDisable`
- Role assignment (item 521): successful `POST /api/users/{id}/roles` writes `ASSIGN_ROLE` with
  old/new `roles` lists via `AuditService.logRoleChange`
- Password reset
