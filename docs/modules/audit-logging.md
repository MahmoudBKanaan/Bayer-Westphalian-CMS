# Audit Logging Documentation

Central immutable audit trail for sensitive system actions (KB epic **E22**, **COMP-008**,
Sprint 14 items **515–533**, documentation item **559**). Domain services write audit rows inside
the same database transaction as the business change; the Audit Log screen is **read-only**.

Acceptance item **566** ties this audit evidence to production security: sensitive actions must be
auditable while production errors and configuration checks avoid leaking secrets or stack traces.

## Package Boundary

| Layer | Location |
| --- | --- |
| Backend entity / service / API | `com.bayerwestphalian.campaign.audit` |
| REST | `GET /api/audit-logs`, `GET /api/audit-logs/entity-history` |
| Frontend page | `frontend/src/pages/AuditPage.tsx` (`/audit`) |
| Frontend API | `frontend/src/api/auditLogs.ts` |
| Navigation | AppLayout **Audit** for Admin, Compliance Officer, System Auditor |

## Who May View

| Role | Access |
| --- | --- |
| Admin | Full sensitive-action history |
| Compliance Officer | Consent changes, approvals, opt-outs, related compliance events |
| System Auditor | System actions, user activity, approval history, export evidence |
| Other roles | No Audit menu; screen shows unauthorized message |

Writes (create/update/delete of `audit_logs`) are **not** exposed over the API or UI (COMP-008).
They are not exposed over the API or UI for normal application roles.

## Audit Log Screen (Item 532)

The **Audit log** screen provides:

1. **Intro** — explains immutable sensitive-action history (consent, opt-out, users/roles,
   products, campaign submit/approve/reject/launch, report exports).
2. **Sensitive actions table** — action badge, entity type + id, actor, recorded time, IP,
   value summary; row selection.
3. **Selected entry panel** — full field list plus pretty-printed previous/new JSON values.
4. **Entity history** — when the selected row has an entity type and id, loads
   `getEntityHistory` for that entity.
5. **Refresh** — reloads the main list from the backend.
6. **Filters (item 533)** — actor user id, action, entity type, entity id, and recorded-from /
   recorded-to date range; **Apply filters** / **Reset** send matching query params to
   `GET /api/audit-logs`.

## Audit Filters (Item 533)

| UI control | API query param | Notes |
| --- | --- | --- |
| Actor user id | `actorUserId` | UUID of the user who performed the action |
| Action | `action` | Preset list (CREATE, APPROVE, EXPORT_REPORT, …) |
| Entity type | `entityType` | Preset list (`campaigns`, `consent_records`, …) |
| Entity id | `entityId` | UUID of the affected record |
| From (recorded) | `createdFrom` | ISO-8601 instant (from `datetime-local`) |
| To (recorded) | `createdTo` | ISO-8601 instant (from `datetime-local`) |

Empty filters request the recent unfiltered list. A filtered request that returns no rows shows
“No audit log entries match the current filters.” Backend matching is implemented in
`AuditService.listAuditLogs(AuditLogSearchCriteria)` / `AuditController`.

## Sensitive Actions Logged (Items 520–531)

| Domain | Typical actions | Entity type (examples) |
| --- | --- | --- |
| Users | CREATE, role assign, DISABLE_USER | `users` |
| Customers | soft DELETE, UPDATE_DO_NOT_CONTACT | `customers` |
| Consent | CREATE, WITHDRAW_CONSENT, OPT_OUT | `consent_records` |

Sprint 16 critical item **658** (*Audit log is created after consent change*) locks consent mutation
audit as a release gate under **NFR-008**: primary JUnit class
`AuditLogIsCreatedAfterConsentChangeTests` (package `consent`); companion
`ConsentChangeCreatesAuditLogTests` (items 524/525). Frontend catalog:
`frontend/src/features/customers/auditLogIsCreatedAfterConsentChange.ts`.
| Products | CREATE, UPDATE, DELETE | `products` |
| Campaigns | SUBMIT, APPROVE, REJECT, LAUNCH | `campaigns` |
| Reports | EXPORT_REPORT (success only) | `report_exports` |

## REST Surface

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/audit-logs` | Recent logs; optional `actorUserId`, `action`, `entityType`, `entityId`, `createdFrom`, `createdTo` |
| `GET` | `/api/audit-logs/entity-history?entityType=&entityId=` | History for one entity |

Response shape: `ApiResponse<List<AuditLogView>>` with actor, action, entityType, entityId,
oldValue, newValue, ipAddress, createdAt.

## Related Documentation

- [`campaign-audit-logging.md`](campaign-audit-logging.md) — campaign lifecycle audits
- [`product-audit-logging.md`](product-audit-logging.md) — product catalog audits
- [`report-export.md`](report-export.md) — item 531 `EXPORT_REPORT`
