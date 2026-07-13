# Employee User Manual

**Item 776** is the shared user manual for the Bayer-Westphalian Campaign Management Platform. The
application is an internal employee system, not a public customer portal. Your assigned roles
determine which pages, records, and actions are available; backend authorization remains decisive.

## Before you begin

- Use only your named employee account. Never share passwords, sessions, exported reports, or
  customer records.
- Use the approved production HTTPS URL supplied by your administrator. Stop if the browser reports
  a certificate warning or redirects to an unexpected host.
- Use customer data only for your authorized business purpose. Follow organizational retention,
  privacy, consent, and incident policies.
- Contact an Admin when your role or account status is wrong. Hidden navigation or `403 Forbidden`
  usually means the account is not authorized for that function.

## Sign in and sign out

1. Open the approved application URL.
2. Enter your employee email and password, then select **Sign in**.
3. Confirm the top bar shows your identity and expected role(s).
4. At the end of work, open the user menu and select **Sign out**. Close shared-browser windows.

Repeated invalid passwords can trigger a temporary lockout. Do not keep retrying or ask another
employee for credentials; contact the administrator through the approved support channel.

## Application layout

The sidebar contains only pages allowed for at least one assigned role. The top bar shows the page
heading, breadcrumb, backend health, loading state, and user menu. Directly typing a restricted URL
does not bypass authorization.

Common interface behavior:

- **Loading:** wait for the status to finish before repeating a save or launch command.
- **Empty state:** no matching records were found; clear filters or create a record if authorized.
- **Error state:** read the safe message, preserve its request/time context, and retry only when the
  action is safe. Never paste tokens, customer payloads, or unrestricted logs into support notes.
- **Validation:** messages identify missing or invalid fields. Correct them before resubmitting.
- **Success notification:** confirms the operation completed; verify the resulting status/details.
- **Confirmation dialog:** re-read the object, impact, and counts before sensitive actions.
- **Status badge:** conveys lifecycle or compliance state; do not infer permission from color alone.

Keyboard users can use `Tab`/`Shift+Tab`, `Enter`/`Space`, arrow keys in supported controls, and the
skip link to reach main content. Every form control should have a readable label. Report inaccessible
controls rather than using an unsafe workaround.

## Roles and primary areas

| Role | Typical pages and work |
| --- | --- |
| Admin | Users, roles, settings, audit oversight |
| Campaign Manager | Segments, campaigns, builder, recipient preview, launch, follow-ups/reminders |
| Compliance Officer | Consent review, campaign review, recipient eligibility, audit |
| Customer Service Agent / Sales Agent | Customers, beneficiaries, consent/contact history, follow-ups |
| Product Manager | Products, ownership, payment/change workflows |
| BI Analyst / Marketing Analyst | Analytics, reports, segment/campaign insight |
| Executive Viewer | Read-only executive analytics and reports |
| System Auditor | Read-only audit history and audit export |

Users with multiple roles receive the union of allowed navigation, but separation-of-duty rules still
apply. A Campaign Manager cannot approve a campaign merely because they created it, and AI cannot
act as an approver.

## Customer and consent workflow

Authorized service/compliance users can search and filter Customers, open details, and work with
profiles, beneficiaries, consent, ownership, payments, contacts, reminders, and follow-ups.

When creating or updating a customer:

1. Search first to avoid duplicates.
2. Enter accurate type, identity, contact, source, status, and address/demographic data.
3. Set `doNotContact` only from verified customer/compliance information.
4. Save, confirm the success message, and verify the details and generated UUID.

For consent:

1. Select the correct customer and consent type/purpose.
2. Record status, source, dates, recorder context, and approved evidence reference/file.
3. Confirm guardian consent when required for a minor beneficiary.
4. Withdraw or record opt-out when instructed; never replace history to hide an earlier decision.
5. Verify the displayed badge/history and audit outcome.

`WITHDRAWN`, `EXPIRED`, `REJECTED`, marketing opt-out, or `doNotContact=true` prevents marketing
eligibility. Users and AI cannot override these controls.

## Products and ownership

Product Managers create and maintain products, pricing/duration/expiration rules, ownership, payment
records, and product-change requests. Search before creating, use the correct product type/status,
and disable rather than misrepresent an unavailable product. Campaign Managers can select active
products but cannot use campaign editing to change the product catalog.

Payment history supports reminders and default-risk recommendations. Record actual payment state;
do not alter history to obtain a preferred AI score.

## Segments

Campaign Managers can view, add, edit, delete, and preview reusable segments. A segment UUID is
generated automatically and appears in the details panel.

1. Open **Segments** and create a clearly named segment.
2. Add criteria such as age, location, customer type, product ownership, payment behavior/status,
   expiration, and AND/OR logic.
3. Preview matched, eligible, and excluded counts before saving or campaign use.
4. Save and verify the same valid UUID and criteria in details.

A segment match is not permission to contact. Final recipient preview applies deterministic consent,
do-not-contact, frequency, uninterested, duplicate, and other `EligibilityService` rules.

## Campaign lifecycle

Campaign Managers use **Campaign Builder** or Campaigns to create a draft:

1. Enter name, objective, and channel.
2. Select the target segment and promoted product(s).
3. Write the subject/body or request an AI copy suggestion.
4. Set the schedule and review all details.
5. Create the `DRAFT`, preview recipients, then submit for compliance review.

Draft and rejected campaigns can be revised. Submitted campaigns wait for a Compliance Officer.
The reviewer checks message, products, segment, schedule, preview counts/exclusions, consent,
do-not-contact, guardian consent, contact limits, and owner/reviewer separation. Approval or rejection
is a human decision; rejection requires a reason and may include revision notes.

Only an `APPROVED` campaign can launch. Before launch, the Campaign Manager must inspect the current
recipient preview and confirmation counts. Launch, pause/resume, complete, and archive only through
allowed lifecycle actions. Never launch to test a configuration or to real recipients during a demo.

## Contact history, reminders, and follow-ups

Authorized users record contact channel, outcome, campaign/customer context, and safe notes. Do not
place unnecessary personal data or secrets in notes. Repeated-contact warnings and configured retry
and monthly limits must be respected.

Reminders display Green/Yellow/Red levels and processing status. Scheduler/manual-trigger controls
are operational/admin functions, not a way to force duplicate communication. Follow-up tasks can be
created, assigned to an authorized employee, filtered, updated, and completed with accurate outcome
context.

## Analytics and reports

Dashboards show campaign totals, audience, sent/engagement/conversion counts and rates, product
performance, cost/revenue, and estimated ROI according to role. Interpret rates with sent volume;
zero or low volume can make percentages misleading. Executive views favor aggregates.

CSV/PDF and audit exports may contain sensitive information. Export only for an approved purpose,
store them in approved locations, restrict sharing, and delete them according to retention policy.
Unauthorized export requests are denied and must not be worked around.

## AI-assisted features

AI can assist with fuzzy customer search, product recommendations, segment suggestions, default-risk
scores, duplicate-contact warnings, and campaign copy. Review the explanation and confidence when
available and verify source business data.

AI cannot:

- approve/reject or launch a campaign;
- approve its own campaign-copy suggestion;
- override consent, opt-out, guardian consent, or do-not-contact;
- bypass `EligibilityService`, contact limits, retry limits, or authorization;
- edit immutable audit history.

Campaign copy remains pending until a human approves it. Reject unsafe, inaccurate, discriminatory,
or unsupported output and record the appropriate human decision.

## Audit and accountability

Sensitive actions create immutable audit records. Admin, Compliance Officer, and System Auditor
roles can read appropriate audit history; normal users cannot edit or delete it. Audit entries can
include actor, action, entity, time, previous/new values, and request context.

Do not perform another employee's action under your account. Report missing audit evidence or an
unexpected ability to access a restricted page/API immediately.

## Errors and support

| Situation | User action |
| --- | --- |
| `401 Unauthorized` / returned to login | Sign in again; report repeated session failures |
| `403 Forbidden` / redirected from page | Confirm role with Admin; do not try alternate URLs/APIs |
| Validation/business-rule error | Correct the fields or lifecycle state; do not bypass controls |
| Backend unavailable/health warning | Stop sensitive work, note UTC time/page/request ID, contact operations |
| Save outcome uncertain | Check the resulting record/history before retrying to avoid duplicates |
| Suspected exposure or unintended contact | Stop affected work and invoke the approved incident channel immediately |

Support evidence should include UTC time, page/workflow, safe error code, request ID, and expected
versus observed behavior. Never include passwords, JWTs, customer payloads, consent evidence,
message bodies, or unrestricted screenshots/logs.

## Role-specific guides

- [Customer Service Agent](customer-service-agent-guide.md)
- [Campaign Manager](campaign-manager-guide.md)
- [Compliance Officer](compliance-officer-guide.md)
- [Product Manager](product-manager-guide.md)
- [BI Analyst](bi-analyst-guide.md)
- [Executive Viewer](executive-viewer-guide.md)
- [System Auditor](system-auditor-guide.md)
- [Segmentation](segmentation-user-guide.md)

Admins should also use the [Administrator Manual](../admin/admin-manual.md) and existing
[User-Management Guide](../admin/user-management-guide.md). Technical setup and production operation
are outside this manual; the documentation index identifies the current operator guides.

Automated documentation evidence: `EmployeeUserManualDocumentationTests`.
