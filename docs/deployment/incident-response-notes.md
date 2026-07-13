# Production Incident Response Notes

**Sprint 18 item 740** defines the operational incident notes and minimum response process for the
Bayer-Westphalian Campaign Management Platform. These notes complement, but do not replace, the
organization's legal, privacy, security, and business-continuity policies.

**Sprint 18 item 766** finalizes this document as the production incident-notes guide. Completed
incident records belong in the approved access-controlled incident system, not in this repository.

## Response Principles

1. Protect people, customer rights, consent, and data before availability or release deadlines.
2. Contain first, preserve evidence second, then diagnose and recover through approved procedures.
3. Use UTC timestamps and request IDs to build one factual timeline. Separate observations from
   hypotheses and decisions.
4. Never bypass RBAC, consent, do-not-contact, `EligibilityService`, audit logging, or migration
   controls to restore service faster.
5. Never place secrets, JWTs, credentials, full customer records, message bodies, consent evidence,
   or raw database dumps in incident notes, chat, screenshots, or source control.
6. AI may summarize sanitized evidence, but may not declare impact, approve communication, rotate
   production access independently, approve rollback, or close an incident.

## Severity And Escalation

| Severity | Examples | Initial response target | Required escalation |
| --- | --- | --- | --- |
| `SEV-1 Critical` | Active unauthorized access, secret compromise with exploitation, consent/eligibility bypass, unintended mass sending, destructive data loss, public outage of critical workflows | Immediate containment | Incident commander, system owner, security/privacy/compliance owner, provider owner, executive/business owner as applicable |
| `SEV-2 High` | Production unavailable or materially degraded, repeated failed sends, audit trail gap, restore/backup failure affecting recoverability, suspected limited data exposure | Begin response promptly and stop affected workflow | System owner plus security/privacy/compliance or operations owner according to impact |
| `SEV-3 Medium` | Non-critical feature unavailable, bounded scheduler delay, elevated errors with workaround and no rights/data impact | Triage in normal operational window | Feature/operations owner; raise severity if scope grows |
| `SEV-4 Low` | Cosmetic or low-risk operational defect with no workflow, security, privacy, or data-integrity impact | Track and prioritize | Product/engineering owner |

Severity may only stay the same or increase while facts are uncertain. Lowering severity requires a
recorded human decision and evidence that the suspected high-impact condition is excluded.

Do not invent statutory notification deadlines in this repository. The designated privacy/legal
owner determines whether an event is a personal-data breach, who must be notified, and the applicable
deadline based on jurisdiction, contracts, and organizational policy. Escalate suspected exposure
immediately so that assessment can begin.

## Roles

| Role | Responsibility |
| --- | --- |
| Reporter | Opens the incident with timestamp, symptom, environment, and safe evidence reference |
| Incident commander | Owns severity, priorities, decisions, handoffs, and communication cadence |
| Technical lead | Diagnoses, proposes containment/recovery, and records command outcomes |
| Security/privacy/compliance owner | Assesses access, personal data, consent, audit, and notification obligations |
| Communications/business owner | Approves user, stakeholder, provider, or executive communication |
| System Auditor / scribe | Maintains the immutable timeline and sanitized evidence index |
| Recovery approver | Authorizes rollback, restore, provider re-enable, and return to service |

In a small project one person may fill multiple roles, but the notes must identify each decision in
the appropriate role. Automation never fills incident commander or recovery approver roles.

## First 15 Minutes

1. Create an incident identifier such as `INC-YYYYMMDD-NNN`; open the timeline in the approved
   access-controlled incident system, not in Git when it may contain production information.
2. Record detection time, reporter, environment, release image digests, observable symptom, request
   IDs, affected workflow, and initial scope without copying sensitive payloads.
3. Assign provisional severity and incident commander. Escalate uncertain security/privacy impact.
4. Contain the smallest affected surface: maintenance mode, provider disable, scheduler stop, account
   lock, token revocation, secret rotation, or traffic isolation as appropriate.
5. Preserve bounded logs, audit records, provider event identifiers, image/config identifiers, and
   backup metadata before restarting or replacing containers.
6. Establish the next update time and who receives it. State what is known, unknown, contained, and
   being investigated; do not speculate about cause or affected people.

## Safe Evidence Collection

Collect references and metadata, not unrestricted copies:

- UTC timestamp range, service/container, release tag/commit/image digest, request ID, bounded log
  line numbers, HTTP status, sanitized error code, and command exit code.
- Relevant immutable `AuditLog` identifiers and action/entity metadata without exporting unrelated
  customer history.
- Provider message/event identifiers and delivery status, not full message content or recipient list.
- Backup filename, UTC recovery point, byte size, SHA-256 verification result, retention location,
  and restore-rehearsal result; never attach the dump.
- Who collected each item, when, from which system, where it is access-controlled, and every transfer
  or redaction. Preserve originals according to policy and work from copies.

Do not run `env`, `printenv`, unrestricted database exports, SQL parameter logging, full HTTP header
capture, or recursive consent-file copies for convenience. Never paste `Authorization`, `Cookie`,
`Set-Cookie`, `DB_PASSWORD`, `JWT_SECRET`, SMTP credentials, or SMS keys into evidence.

## Scenario Playbooks

### Secret or credential exposure

1. Treat the value as compromised even if a commit, log, or screenshot was later deleted.
2. Disable/revoke it at the source, rotate through the approved secret manager, and restart only the
   services that consume it.
3. For JWT compromise, rotate `JWT_SECRET`, invalidate sessions, require sign-in, and review access
   and audit history. For database credentials, coordinate pool restart and least-privilege review.
4. For provider keys, disable real sending until replacement credentials and destination policy are
   verified. Search Git history, CI logs, tickets, screenshots, and shared artifacts for copies.

### Unauthorized access or suspected personal-data exposure

1. Block the account/session/source and preserve authentication, request-ID, and audit evidence.
2. Do not notify customers or authorities independently; immediately engage the designated
   security/privacy/legal owner for scope and notification assessment.
3. Determine affected data categories, people, time range, actions, exports, consent evidence, and
   whether access changed data. Minimize queries to the necessary scope.
4. Rotate affected credentials and correct authorization without deleting audit history.

### Consent, do-not-contact, eligibility, or unintended sending failure

1. Disable real email/SMS sending at both application policy and provider where possible.
2. Stop campaign launch/retry and reminder scheduling; preserve campaign, recipient, contact-event,
   eligibility-decision, provider-event, consent, and audit identifiers.
3. Identify affected recipients and messages under compliance supervision. Do not automatically
   resend, retract, or alter consent records.
4. Re-enable only after the safeguard is corrected, regression-tested, smoke-tested with synthetic
   recipients, and approved by compliance and recovery owners.

### Availability, bad deployment, or restart loop

1. Enter maintenance mode and preserve health, container state, image digest, request IDs, and recent
   logs. Keep PostgreSQL running when safe for diagnosis.
2. Use the [Production Rollback Plan](rollback-plan.md). Do not rebuild an uncommitted working tree or
   use mutable `latest` images during incident recovery.
3. Verify readiness, HTTPS, CORS, RBAC, and Critical smoke checks before reopening traffic.

### Database corruption, migration failure, or data loss

1. Stop writers, scheduler, providers, and backup retention cleanup. Preserve the current state with
   a final incident backup only when integrity and capacity permit.
2. Never run `flyway clean`, alter `flyway_schema_history`, edit an applied migration, or improvise
   reverse DDL.
3. Select a checksum-verified backup and matching consent-evidence recovery point that passed the
   non-production restore rehearsal. Obtain explicit RPO approval.
4. Follow the [Production Database Restore](backup-and-restore.md#production-database-restore-item-734)
   and rollback plan; keep maintenance mode active until validation passes.

### Backup, storage, scheduler, or provider degradation

1. Stop the affected automated workflow if repeated operation can amplify loss, duplicate contact,
   overwrite evidence, or consume quota.
2. Record last successful run/recovery point, failure counts, duration, configuration name (never
   value), and downstream impact.
3. Do not mark the incident resolved merely because a retry succeeded. Verify retention, checksum,
   restore readability, deduplication/contact limits, and monitoring recovery as applicable.

## Communication Notes

Every update should include incident ID, UTC time, severity, affected environment/workflow, user
impact stated conservatively, containment status, known/unknown facts, next action, owner, and next
update time. Only approved owners communicate externally. Avoid blame, unsupported root-cause claims,
affected-person estimates without evidence, and promises about recovery time before validation.

Use this short update format:

```text
INCIDENT: <INC-ID> | <UTC> | <SEV-N>
Impact: <confirmed impact; no sensitive details>
Status: <investigating / contained / recovering / monitoring>
Known: <facts with evidence references>
Unknown: <material unanswered questions>
Actions/owners: <next action and accountable role>
Next update: <UTC>
```

## Live Note Quality

The System Auditor/scribe maintains one chronological UTC timeline throughout the incident. Every
entry records the timestamp, actor, type (`OBSERVATION`, `ACTION`, `DECISION`, `RESULT`, or
`COMMUNICATION`), factual statement, evidence reference, and next owner when applicable.

- Separate confirmed facts, hypotheses, decisions, and outcomes. Correct an error with a new entry;
  do not silently rewrite history.
- Record command intent and exit status, not secret-bearing command lines or unrestricted output.
- Record every severity, containment, maintenance, provider, rollback/restore, communication, and
  return-to-service decision with its human approver.
- Use stable request IDs, scheduler run IDs, audit IDs, provider event IDs, image digests, and
  backup filenames to reference evidence without copying sensitive payloads.
- Mark handoffs with outgoing/incoming owner, current state, active risks, next update time, and
  uncompleted actions. No critical action may be left without an owner.
- Restrict and audit access to incident records; apply the approved retention and legal-hold policy.

An empty template, chat transcript, raw log bundle, or retrospective written from memory is not an
adequate incident record.

## Recovery And Closure Gate

Recovery requires a human-approved target, successful health/Flyway checks, all Critical item 737
smoke checks, restored audit logging, safe provider/scheduler state, current backup evidence, and a
documented monitoring window. Keep maintenance mode active if any Critical check fails.

Close only when containment is durable, affected data/workflows and communications are accounted for,
credentials are rotated where needed, monitoring is stable, evidence is retained, recovery is
approved, and follow-up owners/dates exist. Reclassify to monitoring before closure; do not close at
the moment service first returns.

## Incident Record Template

```text
Incident ID:
Title:
Environment / release tag / commit / image digests:
Detected UTC / reporter / detection source:
Severity history and approvers:
Incident commander / technical lead / scribe / recovery approver:

Confirmed impact:
Potential impact (clearly labeled):
Affected workflows and time window:
Security/privacy/compliance assessment owner and reference:

Timeline (UTC; observation, action, decision, result, actor, evidence reference):
-

Containment actions and current state:
Secrets/accounts/providers/schedulers affected (names only, never values):
Data and consent-evidence scope:
Backup recovery point / checksum result / restore rehearsal:

Root cause (after evidence review):
Contributing factors:
Recovery path and approval:
Smoke execution and monitoring result:
Actual RPO / RTO:

External/internal communications and approvers:
Residual risk:
Follow-up action / owner / due date:
Closure UTC / closer / closure approver:
```

Store completed production notes in the approved incident system. This repository contains the
template and process only, not production incident payloads.

## Post-Incident Review

After recovery and the monitoring window, hold a blameless evidence-based review. Record the
detection path, confirmed impact and time window, root cause and contributing factors, control and
response effectiveness, actual RPO/RTO, communication decisions, and why recovery/closure was safe.
Create corrective actions with priority, owner, due date, verification method, and linkage to the
incident. Track them to completion; closing the incident does not close unfinished actions.

Review whether monitoring, smoke tests, access controls, consent/eligibility, audit logging,
provider safeguards, backup/restore, runbooks, and training should change. Any production change
follows normal review, CI, deployment, and rollback gates. Do not delete incident or audit evidence
after fixing the defect.

Related controls: [Production Logging](production-logging.md),
[Scheduler Logging](scheduler-logging.md), [Secrets Documentation](secrets.md),
[Production Security Checklist](production-security-checklist.md),
[Production Smoke Test Checklist](production-smoke-test-checklist.md),
[Production Rollback Plan](rollback-plan.md), and [Backup and Restore](backup-and-restore.md).
Signal definitions, initial thresholds, and alert ownership are maintained in
[Production Operational Monitoring Notes](operational-monitoring-notes.md) (item **741**).

Automated item 766 evidence: `FinalIncidentNotesDocumentationTests`.
