# Production Admin Login Verification

**Sprint 18 item 745** requires an approved production administrator to log in successfully. This is
a deployed acceptance check, distinct from unit/integration authentication tests and from creating
the bootstrap account.

## Current Execution

At `2026-07-13T00:02:04+03:00`, item 745 is **BLOCKED**. No `bwc-production` Compose project is
running, the backend login endpoint on port 8080 is unreachable (`curl` exit 7), and only the
development PostgreSQL service is running. No production administrator credential was requested or
used. Seeded `.test` credentials and the HTTP Vite development server are not production evidence.

## Safe Verification

Run only after item 744 passes for the approved deployed hostname:

```powershell
$adminCredential = Get-Credential -Message "Approved production smoke administrator"
.\scripts\test-production-admin-login.ps1 `
  -BaseUrl https://campaign.example.com `
  -Credential $adminCredential
$adminCredential = $null
```

The verifier:

1. Requires a trusted HTTPS origin.
2. Sends the credential directly from an in-memory `PSCredential` to `POST /api/auth/login`.
3. Requires a successful session and `ACTIVE` user status.
4. Parses JWT claims in memory and requires the `ADMIN` role.
5. Calls authenticated `GET /api/auth/me` and requires the same active user ID.
6. Calls `POST /api/auth/logout` in `finally` and clears credential/token variables.
7. Prints only pass categories; it never prints email, password, access token, refresh token, or JWT
   claims and writes no response or secret to disk.

Use a dedicated approved smoke administrator, not a personal daily-use account. The account must be
created through the item 732 bootstrap/admin process, the bootstrap switch and password must already
be removed, and seeded `.test` accounts must remain disabled. Do not pass a plaintext password on the
command line, in environment variables, or in a script file.

## Evidence And Acceptance

Record UTC time, environment/HTTPS origin, release SHA and image digests, operator, approved test
account reference (not email when unnecessary), sanitized pass output, request ID where available,
logout outcome, and approver. Never capture the credential prompt, browser storage, Authorization
header, tokens, or full authentication response.

Item 745 passes only when item 744 has passed for the same release/environment, the verifier reports
an active ADMIN session and matching `/api/auth/me`, logout succeeds or the session is explicitly
revoked, and evidence is reviewed. An API reachability result, seeded account, mocked test, or menu
visibility alone does not prove an Admin can log in.

Related documentation: [Admin Bootstrap](../operations/admin-bootstrap.md),
[Production HTTPS](https.md), [Smoke Checklist](production-smoke-test-checklist.md),
[Security Checklist](production-security-checklist.md), and
[Incident Response](incident-response-notes.md).
