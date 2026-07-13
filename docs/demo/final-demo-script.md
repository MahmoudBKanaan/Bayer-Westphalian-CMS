# Final Demo Script

**Item 782** provides the final demonstration runbook for the Bayer-Westphalian Campaign Management
Platform. It uses the synthetic item 781 dataset and demonstrates the KB journey:

```text
Login -> customer/consent -> product -> segment -> campaign -> human approval -> launch -> analytics/audit
```

Target duration is **20 minutes**, plus 5 minutes for questions. This is a development/demo
walkthrough, not production-release or smoke-test evidence.

## Roles

| Demo responsibility | Persona |
| --- | --- |
| Presenter/operator | Drives the browser and narrates business value |
| Admin | Shows employee-only access, roles, settings, and auditability |
| Customer Service Agent | Shows customer/beneficiary/consent context |
| Product Manager | Shows product and payment context |
| Campaign Manager | Shows segment, campaign, recipient preview, and controlled launch |
| Compliance Officer | Explains human approval and exclusions |
| BI Analyst / Executive Viewer | Shows analytics and management reporting |
| System Auditor | Shows immutable sensitive-action evidence |

One presenter may switch between seeded role accounts. Credentials are obtained from the approved
demo secret/seed procedure and must not appear in this script, slides, terminal history, or screen
recording.

## Preflight (15-30 minutes before)

1. Use a disposable local/dev or isolated demo environment. Confirm it is not production.
2. Start PostgreSQL, backend with `dev`, and frontend. Verify liveness/readiness and browser access.
3. Run the read-only dataset verifier:

   ```powershell
   .\scripts\verify-final-demo-dataset.ps1
   ```

4. Confirm the item 781 manifest anchors, all role accounts, and the approved campaign are present.
5. Confirm `PROVIDER_REAL_SENDING_ENABLED=false`; email/SMS is mock/disabled. Do not continue to a
   launch if this cannot be proven.
6. Confirm every demo recipient uses synthetic `.test`/`example.test` data and no real destination.
7. Open the app in a clean browser profile, set desktop width near 1440px, close notifications and
   unrelated tabs, and disable password-manager overlays.
8. Keep the [User Manual](../user-guides/user-manual.md),
   [Demo Dataset](final-demo-dataset.md), and this script available on a second screen.
9. Prepare a unique mutation prefix such as `DEMO-20260713T100000Z` if creating records live.
10. Start recording only after checking that no credentials, tokens, customer data, terminal output,
    or private desktop content is visible.

### Hard no-go conditions

Do not begin or continue the mutation/launch portion when the environment identity is uncertain,
provider real sending is enabled/unverified, dataset verification fails, non-synthetic recipients
appear, backend readiness is down, or credentials/secrets are visible. Use the read-only fallback
instead and state the limitation honestly.

## Anchor records

| Record | Display / UUID |
| --- | --- |
| Policyholder | Anna Keller - `20000000-0000-0000-0000-000000000101` |
| Eligible beneficiary | Lena Keller - `20000000-0000-0000-0000-000000000102` |
| Do-not-contact prospect | Jonas Weber - `20000000-0000-0000-0000-000000000103` |
| Segment | Demo Beneficiaries With Consent - `40000000-0000-0000-0000-000000000101` |
| Campaign | Demo Beneficiary Investment Outreach - `50000000-0000-0000-0000-000000000101` |
| Follow-up | `53000000-0000-0000-0000-000000000101` |
| Reminder | `54000000-0000-0000-0000-000000000101` |

## Timed walkthrough

### 0:00-1:30 - Purpose and secure login

**Action:** Open `/login`, sign in as the Campaign Manager, and show the shell/user-role menu.

**Say:** “This is an internal employee platform for consent-aware insurance campaigns. Every page
and API is role-protected; the menu reflects the role, while Spring Security remains authoritative.”

**Show:** Application shell, top bar, health state, role-filtered navigation. Briefly explain that a
direct restricted Admin URL redirects/denies access rather than bypassing the menu.

**Expected:** Campaign Manager sees Segments/Campaigns/Builder but not Users/Settings.

### 1:30-4:00 - Customer, beneficiary, and consent

**Action:** Switch to Customer Service Agent. Search `Anna Keller`, open customer details, and show
the beneficiary link to Lena. Review consent/status/contactability context; then open Jonas Weber to
show do-not-contact.

**Say:** “Customer and beneficiary context is centralized. Consent history is preserved, and
do-not-contact is a deterministic exclusion, not a recommendation.”

**Show:** Customer UUID, beneficiary relationship, consent status badges/evidence metadata,
do-not-contact status, loading/empty/error-safe UI behavior where relevant.

**Expected:** Anna/Lena records are synthetic; Jonas is visibly not contactable.

### 4:00-5:30 - Product and payment context

**Action:** Switch to Product Manager. Open Products and the seeded product/ownership/payment data.

**Say:** “Products supply campaign context, while ownership and payment history drive reminders and
explainable risk recommendations. Product administration is separated from campaign approval.”

**Show:** Product status/details and payment due/history context without changing anchor values.

### 5:30-8:00 - Reusable segment and UUID

**Action:** Switch to Campaign Manager. Open Segments and select **Demo Beneficiaries With Consent**.

**Say:** “Segments are reusable targeting definitions. Their valid UUID is generated automatically
and visible in details. Matching a segment does not grant permission to contact anyone.”

**Show:** UUID `40000000-0000-0000-0000-000000000101`, criteria `customer_type=BENEFICIARY` and
`consent_status=GIVEN`, visibility, owner, and preview counts/explanations.

**Optional live action:** Create a prefixed private segment, show automatic UUID, edit one criterion,
then delete it after the demo. Do not alter the anchor segment.

### 8:00-10:30 - Campaign builder and human-approved AI

**Action:** Open Campaign Builder and walk through Basics, Audience & product, Message, Schedule,
and Review without submitting an incomplete mutation. Then open the anchor campaign.

**Say:** “Create and Edit both include product selection. AI may suggest copy with an explanation and
confidence, but the suggestion stays pending until a human approves it. AI cannot approve a campaign
or expand recipient eligibility.”

**Show:** Segment/product selection, message fields, schedule, validation, confirmation, anchor
campaign owner/status, promoted product, and approved-by metadata.

### 10:30-13:30 - Compliance and recipient exclusions

**Action:** Switch to Compliance Officer. Open Compliance and the campaign recipient preview.

**Say:** “A Compliance Officer, separate from the Campaign Manager, owns the approval decision.
Recipient preview applies `EligibilityService` after segment matching.”

**Show:** Eligible/excluded totals and examples:

- Lena Keller: eligible with valid email consent;
- Jonas Weber: `DO_NOT_CONTACT`;
- withdrawn/rejected consent: `MARKETING_OPT_OUT`;
- missing/expired or guardian consent: `INVALID_CONSENT`;
- repeated assignment: `DUPLICATE_CAMPAIGN_RECIPIENT`;
- configured frequency: `MONTHLY_CONTACT_LIMIT`.

**Expected:** The campaign is `APPROVED`, with Compliance Officer identity/time visible. Explain that
rejection requires a reason and approval/rejection creates immutable audit evidence.

### 13:30-15:00 - Controlled launch

**Safety gate:** Reconfirm this is dev/demo, provider real sending is false, and every recipient is
synthetic. If any check is uncertain, open the launch confirmation but select **Cancel**.

**Action:** Switch to Campaign Manager, reopen recipient preview, select Launch, read the confirmation
counts, and confirm only when the safety gate passed.

**Say:** “Only an approved campaign can launch. The confirmation exposes eligible and excluded
counts; deterministic safeguards are re-applied and cannot be overridden by AI or UI.”

**Expected:** Status moves from `APPROVED` to `ACTIVE`; no real message is sent. If the anchor was
already launched during rehearsal, use the read-only audit/status evidence or reset before the demo.

### 15:00-16:30 - Contact, reminder, and follow-up

**Action:** Show Contact History, Reminders, and Follow-ups using the anchor IDs.

**Say:** “Operational work remains traceable after launch. Reminder levels communicate urgency,
configured retry/contact limits apply, and follow-up tasks can be assigned and completed.”

**Show:** Controlled contact event, Yellow reminder, assigned open follow-up, status badges, and
scheduler/provider-safe state. Do not manually trigger production-like sending.

### 16:30-18:00 - Analytics, reports, and AI explanation

**Action:** Switch to BI Analyst or Executive Viewer. Open Dashboard, Analytics/Executive, and
Reports; then show the stored AI recommendation explanation/confidence where available.

**Say:** “Metrics summarize audience, eligibility, sent/engagement, conversion, cost, revenue, and
ROI. Executive views favor aggregates. AI is explainable decision support, not autonomous action.”

**Show:** Campaign metrics, product performance, report history, AI explanation and confidence.
Do not download/export sensitive files during a recorded demo unless the destination is approved.

### 18:00-19:30 - Audit and unauthorized access

**Action:** Switch to System Auditor. Open Audit, filter/select relevant campaign/user/consent events,
and show entity history. Then explain that normal roles cannot open Audit or mutate logs.

**Say:** “Sensitive actions are immutable and actor-linked. System Auditor access is read-only;
unauthorized page/API access is denied.”

**Show:** Action badge, actor, entity UUID, UTC time, previous/new values, approval/launch sequence,
and absence of edit/delete controls. Do not expose secret/customer payloads in detail panels.

### 19:30-20:00 - Close

**Say:** “The platform combines role-based CRM, deterministic consent and eligibility, human
campaign approval, explainable AI support, communications/reminders, analytics, and immutable audit
history. Production release is still allowed only when the item 770 evidence gate passes.”

Open the documentation index or release gate briefly. Do not claim that this local demo proves
production deployment, backups, HTTPS, provider readiness, smoke completion, or v1.0 release.

## Read-only fallback

If mutation or launch is unsafe/unavailable:

1. Use anchor details, recipient preview, existing approved-by metadata, contact history, metrics,
   and audit entries to narrate the same controls.
2. Use approved pre-captured synthetic screenshots only when the live UI is unavailable; label them
   as earlier evidence, not a live result.
3. State the exact failure/limitation and continue with unaffected read-only sections.
4. Never edit database rows, disable safeguards, expose credentials, or invent a successful result
   to keep the presentation moving.

## Question prompts

| Likely question | Short answer |
| --- | --- |
| Why are UUIDs long? | They are standard valid UUID identifiers generated/stored consistently and safe across distributed clients. |
| Can AI approve or contact customers? | No. AI provides explained suggestions; authorization, human approval, and deterministic eligibility remain mandatory. |
| Can a Campaign Manager bypass consent? | No. Backend `EligibilityService` enforces consent, do-not-contact, opt-out, guardian, duplicate, and frequency rules. |
| Why separate approval and launch? | Compliance Officer approves; Campaign Manager launches only an approved campaign, preserving separation and audit history. |
| Is this production-ready now? | The implementation/runbooks exist, but v1.0 remains draft until exact production evidence passes item 770. |
| Are demo records real customers? | No. They are deterministic synthetic dev/test records under reserved test domains. |

## Post-demo cleanup and evidence

1. Sign out all role sessions and stop screen recording before opening terminals/secrets.
2. If the anchor campaign was launched or anchors changed, reset the disposable demo database and
   rerun item 781 verification. Delete prefixed live-created records through audited UI workflows.
3. Confirm provider real sending remained false and no real destination/contact was used.
4. Store only approved redacted screenshots/video. Do not retain passwords, JWTs, customer payloads,
   consent evidence, raw exports, or unrestricted logs.
5. Record environment (`dev/demo`), commit, dataset verification result, presenter, UTC window,
   completed/skipped sections, limitations, and cleanup result.
6. Capture questions and follow-up actions with owner/due date. Demo completion does not change the
   blocked production release status.

## Presenter checklist

- [ ] Dataset verifier passed and anchor IDs are available.
- [ ] Backend/frontend health and role accounts are ready.
- [ ] Provider real sending is false; all records/destinations are synthetic.
- [ ] Credentials and unrelated desktop content are hidden.
- [ ] Role handoffs and timing have been rehearsed.
- [ ] Launch safety gate and read-only fallback are understood.
- [ ] Final message does not misrepresent production/release status.
- [ ] Cleanup and sanitized evidence record are completed.

Related: [Final Demo Dataset](final-demo-dataset.md), [Employee User Manual](../user-guides/user-manual.md),
[Core Workflow Screenshot Catalog](../testing/core-workflow-screenshots.md), and
[v1.0 Release Notes](../releases/v1.0-draft.md).

Automated documentation evidence: `FinalDemoScriptDocumentationTests`.
