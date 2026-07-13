# Production administrator bootstrap

The bootstrap process creates the first production administrator without enabling public user
registration. It runs only with the `prod` profile and only when `ADMIN_BOOTSTRAP_ENABLED=true`.

## First deployment

1. Set `ADMIN_BOOTSTRAP_ENABLED=true` in the deployment environment.
2. Set `ADMIN_BOOTSTRAP_EMAIL` to a real, non-test administrator email.
3. Inject `ADMIN_BOOTSTRAP_PASSWORD` from the deployment secret manager. It must contain at least
   16 characters with upper-case, lower-case, and numeric characters.
4. Optionally set `ADMIN_BOOTSTRAP_FULL_NAME`.
5. Start the backend and confirm the sanitized log reports that the administrator was created.
6. Immediately set `ADMIN_BOOTSTRAP_ENABLED=false`, remove `ADMIN_BOOTSTRAP_PASSWORD`, and restart.

The password is BCrypt-hashed before persistence and is never logged or included in audit data.
The user creation and ADMIN role assignment are written to the immutable audit log with a null
actor and `bootstrap=true`, identifying them as system startup actions.

## Safety behavior

- Accounts seeded by demo migrations under `@bayer-westphalian.test` are disabled when bootstrap
  runs in production.
- If the configured email already exists, bootstrap is an idempotent no-op. It does not reset the
  password, reactivate the account, or assign a role.
- A missing ADMIN system role aborts bootstrap and rolls back the transaction.
- A blank, weak, or `.test` bootstrap identity fails startup with a safe configuration error.
