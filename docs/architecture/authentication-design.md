# Authentication Design

This document records the authentication design for the Bayer-Westphalian Campaign Management
Platform in accordance with the knowledge base.

## Scope

The application is an internal employee system. There is no public signup. Admin users create and
manage employee accounts, assign roles, disable users, and reset passwords.

## Login Flow

1. The React frontend posts employee credentials to `POST /api/auth/login`.
2. The Spring Boot backend validates the email and password through `AuthService`.
3. Password verification uses BCrypt through `PasswordHashingService`.
4. Disabled or locked users are rejected before a session is issued.
5. Successful login returns an authenticated user view plus a JWT access token and refresh token.
6. The frontend stores the session in `sessionStorage`, not `localStorage`.

## Token Model

- Access tokens are JWTs used as `Authorization: Bearer <token>` on protected API calls.
- Refresh tokens are used with `POST /api/auth/refresh` to issue a fresh token pair.
- JWT claims include the user id, email, token type, expiry, and assigned roles.
- JWT secrets and token settings are environment-driven backend configuration.

## Frontend Session Strategy

- `AuthProvider` owns frontend authentication state.
- `sessionStorageStrategy` persists the access token, refresh token, current user, and role claims.
- The API client attaches the stored access token to authenticated requests by default.
- Public auth requests, such as login, explicitly skip stored token attachment.
- Protected frontend routes redirect unauthenticated users to `/login` and preserve the requested path.

## Backend Enforcement

- Spring Security is the authoritative access-control boundary.
- Frontend role checks only control navigation and user experience.
- Protected endpoints fail without authentication.
- Endpoints fail with `403 Forbidden` when the authenticated user lacks the required role.
- Admin-only user management endpoints require the `ADMIN` role.

## Role-Based Access

Roles are assigned through the `roles` and `user_roles` model. The MVP roles are:

- `ADMIN`
- `CAMPAIGN_MANAGER`
- `BI_ANALYST`
- `PRODUCT_MANAGER`
- `COMPLIANCE_OFFICER`
- `CUSTOMER_SERVICE_AGENT`

Extended roles are supported by the same model:

- `SALES_AGENT`
- `MARKETING_ANALYST`
- `EXECUTIVE_VIEWER`
- `SYSTEM_AUDITOR`

## Error Handling

- Invalid credentials, disabled accounts, and locked accounts return an authentication failure.
- Missing or invalid bearer tokens return an unauthorized response.
- Authenticated users with insufficient roles receive a forbidden response.
- The frontend shows clear login and authorization failure messages without exposing stack traces.

## Audit And Security Notes

- User creation, role assignment, and user disable actions are sensitive and must be audited.
- Password hashes are never returned in API user views.
- Production must use HTTPS and approved frontend origins.
