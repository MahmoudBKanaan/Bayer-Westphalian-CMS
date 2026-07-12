# Follow-Up Task Documentation

The follow-up task module owns operational work items created for interested prospects and
customers after campaign contact, reminders, or service interactions (KB epic **E17** Follow-up
management / **FR-093**). It lets Customer Service Agents, Sales Agents, Campaign Managers, and
Admins create, assign, update, complete, and filter follow-up work so leads do not stall after a
marketing or reminder touchpoint.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.followup
```

Follow-up components:

- `FollowUpTask`: JPA entity mapped to the `follow_up_tasks` table.
- `FollowUpRepository`: assignee, customer, open-task, and multi-criteria search access.
- `FollowUpService`: backend-owned validation, authorization, create/assign/complete/status
  workflows, and task search.
- `FollowUpController`: REST API boundary under `/api/follow-up-tasks`.
- `FollowUpTaskStatus`, `FollowUpTaskPriority`, request, command, search criteria, and view DTOs.

The module depends on:

- **Customer** module: every task requires an existing customer (`customer_id`).
- **User** module: optional assignee must be an active internal employee (`assigned_to`).
- **Campaign** module: optional link to the originating campaign (`campaign_id`).

## Data Model

Table: `follow_up_tasks` (KB entity fields).

| Field | Notes |
| --- | --- |
| `id` | UUID primary key |
| `customer_id` | Required FK to `customers` |
| `campaign_id` | Optional FK to `campaigns` |
| `assigned_to` | Optional FK to `users` |
| `title` | Required, max 255 characters |
| `description` | Optional free text |
| `due_date` | Optional date |
| `status` | `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `priority` | `LOW`, `MEDIUM`, `HIGH` |
| `created_at` | Set on create |
| `completed_at` | Set when status becomes `COMPLETED` |

### Status lifecycle

- New tasks start as `OPEN` with default priority `MEDIUM` when priority is omitted.
- `IN_PROGRESS` marks active work (`start()`); clears `completed_at`.
- `COMPLETED` records completion timestamp via `complete()` (KB item 392).
- `CANCELLED` ends work without completion timestamp.
- Status may be reopened to `OPEN` through `updateStatus(OPEN)`.

### Priority

Supported priorities are `LOW`, `MEDIUM`, and `HIGH`. Search sorts by due date ascending (nulls
last), then priority descending, then created time ascending so urgent due work surfaces first.

## REST API

Follow-up endpoints return the shared `ApiResponse` wrapper.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/follow-up-tasks` | Create a follow-up task. |
| `PUT` | `/api/follow-up-tasks/{id}/assign` | Assign the task to an active user. |
| `PUT` | `/api/follow-up-tasks/{id}/complete` | Complete the task (`COMPLETED` + `completed_at`). |
| `PUT` | `/api/follow-up-tasks/{id}/status` | Update status (`OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`). |
| `PUT` | `/api/follow-up-tasks/{id}` | Update description and/or priority. |
| `GET` | `/api/follow-up-tasks` | Search and list follow-up tasks. |

### Search filters

Supported query parameters on `GET /api/follow-up-tasks`:

- `customerId`
- `assignedTo`
- `priority`
- `status`
- `dueDateFrom`
- `dueDateTo`

Repository helpers used by the service layer:

- `findByAssignedTo(UUID userId)` — assignee worklist.
- `findOpenTasks()` — tasks in `OPEN` or `IN_PROGRESS`.
- `findByCustomerId(UUID customerId)` — customer profile task list.
- `search(FollowUpTaskSearchCriteria criteria)` — combined filters with worklist sort order.

### Create payload

`CreateFollowUpTaskRequest` fields:

- Required: `customerId`, `title`
- Optional: `campaignId`, `assignedTo`, `description`, `dueDate`, `priority` (defaults to `MEDIUM`)

### Assign payload

`AssignFollowUpTaskRequest` requires `assignedTo` (active user UUID). Assignment is required for
Sales Agent lead ownership and for filtering the follow-up worklist by assignee (KB item 391).

## Domain Rules

- `customerId` and `title` are required when creating a follow-up task.
- The customer must exist; missing customers raise not-found errors.
- Optional `campaignId` must reference an existing campaign when provided.
- Optional `assignedTo` on create, and required `assignedTo` on assign, must reference an existing
  user; assignment additionally requires the user to be **active**.
- Completion requires a valid task id and transitions status to `COMPLETED` with `completed_at`
  set so open worklists no longer treat the task as operational work.
- Status updates require both task id and a non-null status value.
- Backend validation is authoritative; frontend validation is only a user-experience layer.

## Authorization

Spring Security and method-level authorization are the backend access-control boundary. Frontend
route checks may hide screens, but every protected follow-up workflow must still be enforced by
backend role authorization.

| Operation | Allowed roles |
| --- | --- |
| Create task | `ADMIN`, `CUSTOMER_SERVICE_AGENT`, `CAMPAIGN_MANAGER` |
| Assign task | `ADMIN`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `CAMPAIGN_MANAGER` |
| Complete task | `ADMIN`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT` |
| Update status / description / priority | `ADMIN`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT` |
| List / search / assignee worklist | `ADMIN`, `CUSTOMER_SERVICE_AGENT`, `SALES_AGENT`, `CAMPAIGN_MANAGER` |

KB role summary alignment:

- **Campaign Manager**: create, assign, update follow-ups; view campaign-related work.
- **Customer Service Agent**: create/update customers and contact outcomes; manage follow-up tasks.
- **Sales Agent**: view assigned leads; complete follow-up tasks; update outcomes.
- **Admin**: full operational access to follow-up workflows.

## Frontend Boundary

- Follow-Up Tasks screen: `frontend/src/pages/FollowUpTasksPage.tsx`
- Customer Details follow-up tab / related UI: `frontend/src/pages/CustomerDetailsPage.tsx`
- API client: `frontend/src/api/followUpTasks.ts`
- Domain types for status and priority appear in frontend domain models where listed.

## Downstream Use

Follow-up tasks connect campaign and reminder outcomes to human action:

- After interested contact outcomes, agents create follow-up tasks (FR-093).
- KB FR-088 notes the system may create follow-up tasks after reminders; operational create/assign
  APIs support that workflow.
- Sales and CSA worklists filter by assignee, priority, status, and due date range.
- Optional `campaign_id` preserves lineage from a launched campaign to later sales work.

Related modules:

- [Communication Tracking Documentation](communication-tracking.md) — contact outcomes that often
  trigger follow-ups
- [Campaign Lifecycle Documentation](campaign-lifecycle.md) — optional campaign link
- [Reminder Scheduling Documentation](reminder-scheduling.md) — payment and product-expiration
  reminders that can drive follow-up creation after customer contact

## Acceptance Criteria (KB / Sprint 11)

- A follow-up task can be created for a customer (optional campaign and assignee).
- A follow-up task can be assigned to an active internal user.
- A follow-up task can be completed with `COMPLETED` status and `completed_at` recorded.
- Follow-up tasks can be filtered by assignee, priority, status, and due date range.
- Unauthorized roles cannot create, assign, complete, or list protected follow-up endpoints.
- Follow-Up Tasks UI and customer-profile task views consume `/api/follow-up-tasks`.

## Evidence For Demo And Review

- Follow-up task screen screenshot
- Follow-up assignment screenshot
- Completion / status update demonstration
- Filtered worklist by assignee or status
- Role permission checks for Sales Agent vs BI Analyst (or other unauthorized role)
- Sprint 11 review notes linking E17 follow-up management
