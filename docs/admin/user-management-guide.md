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

Disabled or locked users cannot log in.

## Disable A User

1. Select an employee from the user selector.
2. Choose `Disable user`.
3. Confirm that the account status changes to `DISABLED`.

Disable is the normal account-removal workflow. Permanent deletion is not part of the MVP.

## Assign A Role

1. Select an employee from the user selector.
2. Review the current roles.
3. Select a role that the user does not already have.
4. Choose `Assign role`.

The UI disables duplicate role choices. The backend enforces role assignment rules and records
role-change audit activity.

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

- User creation
- User disable
- Role assignment
- Password reset
