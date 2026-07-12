# System Settings Documentation

Admin-managed platform configuration for business limits (KB **System Settings** screen, Sprint 14
**items 534–537**, documentation item **561**). Item **535** wires the monthly contact limit into
eligibility and AI duplicate-risk warnings. Item **536** wires the send retry limit into
`SendRetryService`. Item **537** wires the uninterested exclusion period into
`EligibilityService`.

## Package Boundary

| Layer | Location |
| --- | --- |
| Backend package | `com.bayerwestphalian.campaign.settings` |
| Table | `system_settings` (Flyway **V24**) |
| REST | `GET/PUT /api/system-settings` (Admin only) |
| Frontend page | `frontend/src/pages/SystemSettingsPage.tsx` (`/settings`) |
| Frontend API | `frontend/src/api/systemSettings.ts` |
| Navigation | AppLayout **Settings** (Admin only) |

## Who May Manage

| Role | Access |
| --- | --- |
| Admin | View and update system settings |
| All other roles | No Settings menu; unauthorized message on the page |

## Configurable Fields (Item 534 Screen)

| Field | API / DB | Default | Range | Purpose |
| --- | --- | --- | --- | --- |
| Monthly marketing contact limit | `monthlyContactLimit` / `monthly_contact_limit` | 3 | 1–100 | BR-011 marketing frequency (item **535** wiring; critical test item **652**) |
| Send retry limit | `sendRetryLimit` / `send_retry_limit` | 3 | 1–20 | Delivery retry max attempts (item **536**) |
| Uninterested exclusion period (days) | `uninterestedExclusionDays` / `uninterested_exclusion_days` | 90 | 1–3650 | Exclusion window for uninterested customers (item **537**) |

Defaults align with `app.contact.monthly-limit`, `app.contact.retry-limit`, and
`app.contact.uninterested-exclusion-days` in `application.yml`.

## REST Surface

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/system-settings` | Load singleton settings |
| `PUT` | `/api/system-settings` | Update limits (JSON body) |

Response: `ApiResponse<SystemSettingsView>` with id, three limits, `updatedByUserId`, `updatedAt`.

Security: filter chain + `@AdminOnly` / `@PreAuthorize("@authz.canManageSystemSettings()")`.

## System Settings Screen

1. Intro explaining Admin-only business limits.
2. Form fields with help text for each limit.
3. Client-side range validation before save.
4. **Save settings** → `PUT /api/system-settings`.
5. **Reset form** restores last loaded server values.

## Configurable Monthly Contact Limit (Item 535)

Admin updates to **Monthly marketing contact limit** on `/settings` are applied immediately on the
next eligibility evaluation (no restart):

| Consumer | Behavior |
| --- | --- |
| `EligibilityService` | `evaluateCustomer`, segment/campaign preview, reminder eligibility, `checkMonthlyLimit(customerId)` |
| `AiRecommendationService.detectDuplicateRisk` | AI-006 warning threshold and `monthlyContactLimit` on the response view |

Explicit override `checkMonthlyLimit(customerId, limit)` still accepts a caller-supplied limit for
tests and advanced callers.

## Configurable Send Retry Limit (Item 536)

Admin updates to **Send retry limit** on `/settings` are applied on the next outbound send:

| Consumer | Behavior |
| --- | --- |
| `SendRetryService` | `sendEmailWithRetry` / `sendSmsWithRetry` loop up to `SystemSettingsService.sendRetryLimit()` |

Safety ceiling: `SendRetryService.HARD_MAX_ATTEMPTS` (20) matches the System Settings max range.
Application property `app.contact.retry-limit` only seeds the default settings row.

## Configurable Uninterested Exclusion Period (Item 537)

Admin updates to **Uninterested exclusion period (days)** on `/settings` control how long a
customer with status `UNINTERESTED` is blocked from marketing eligibility:

| Consumer | Behavior |
| --- | --- |
| `EligibilityService` | Excludes `UNINTERESTED` only while `now < status_changed_at + uninterestedExclusionDays` |

- Anchor: `customers.status_changed_at` (set on status change; Flyway **V25**). Fallback:
  `updated_at`. If both missing, still excluded (safe default).
- After the period elapses, the uninterested rule no longer excludes even if status remains
  `UNINTERESTED` (operators may still reclassify status separately).
- Application property `app.contact.uninterested-exclusion-days` only seeds the settings default.

## Domain Wiring Status

| Limit | Stored in settings (534) | Applied in domain services |
| --- | --- | --- |
| Monthly contact | Yes | **Item 535 done** — `EligibilityService` + AI-006 `AiRecommendationService.detectDuplicateRisk` read `SystemSettingsService.monthlyContactLimit()` at evaluation time |
| Send retry | Yes | **Item 536 done** — `SendRetryService` reads `SystemSettingsService.sendRetryLimit()` at send time |
| Uninterested exclusion days | Yes | **Item 537 done** — `EligibilityService` period window via `SystemSettingsService.uninterestedExclusionDays()` |

`SystemSettingsService` exposes `monthlyContactLimit()`, `sendRetryLimit()`, and
`uninterestedExclusionDays()`. Application properties under `app.contact.*` remain
**seed/default** values when the singleton settings row is created, not the sole runtime source
for wired limits.

## Related Documentation

- [`eligibility-rules.md`](../architecture/eligibility-rules.md) — contact limit rules
- [`communication-tracking.md`](communication-tracking.md) — send retry behavior
- [`audit-logging.md`](audit-logging.md) — accountability trail for sensitive actions
