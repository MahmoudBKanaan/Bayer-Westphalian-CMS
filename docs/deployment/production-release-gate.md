# Production Release Gate

**Sprint 18 item 770** enforces the KB rule that production release is allowed only after smoke
tests, backups, security configuration, environment configuration, provider configuration policy,
rollback readiness, and critical workflows pass.

Green CI remains mandatory under item 714, but green CI alone is not production approval. Runtime
evidence must describe the exact release commit, tag, and environment and must be approved by a
human release approver.

## Required gates

| Gate | PASS requires |
| --- | --- |
| `smokeTests` | Complete items 737/763 checklist; every Critical check passes; synthetic cleanup and sign-off complete |
| `backups` | Fresh checksum-valid archive, encrypted off-host copy, matching consent-evidence recovery point, successful restore rehearsal |
| `securityConfiguration` | Approved security checklist: HTTPS/HSTS, restricted CORS, safe secrets/errors/logging, RBAC/audit controls |
| `environmentConfiguration` | Production profile and required variables validated for the exact environment without exposing values |
| `providerConfigurationPolicy` | Real email/SMS is approved and safely configured, or sending is explicitly disabled; ambiguous/mock production state fails |
| `rollbackPlan` | Known-good immutable images, schema compatibility decision, recovery owner, RPO/RTO, and rollback readiness reviewed |
| `criticalWorkflows` | Role-specific deployed acceptance checks pass, including authorization, consent/eligibility, human approval, launch, contact/audit, and analytics |

`PENDING`, `BLOCKED`, `FAIL`, `N/A`, missing gates, prose claims, local-only tests, stale evidence,
or evidence from another commit/environment are not `PASS`.

## Evidence manifest

Use `config/production-release-evidence.example.json` as the schema reference. Do not overwrite the
example with a false pass. Create the completed manifest in the approved release evidence system or
an access-controlled CI artifact. It contains references and identities only, never credentials,
JWTs, customer data, dumps, consent files, signed storage URLs, or unrestricted logs.

Each gate needs a concrete immutable evidence reference. The overall `decision` becomes `PASS` only
after all seven gate statuses are `PASS`. Record `operator`, independent human `approvedBy`, UTC
evaluation time, exact full commit SHA, release tag, and environment.

## Fail-closed validation

Run immediately before production release/tag publication, after item 714 confirms successful CI on
the exact `main` commit:

```powershell
.\scripts\assert-production-release-gate.ps1 `
  -EvidenceFile <approved-evidence-manifest.json> `
  -ExpectedCommit <40-character-final-main-sha> `
  -ExpectedTag v1.0
```

The validator rejects malformed JSON, placeholders, mismatched commit/tag, missing human approval,
non-UTC evaluation time, a non-PASS overall decision, any absent/non-PASS gate, or missing concrete
evidence reference. It prints gate names and release identity, not evidence contents or secrets.

The validator does not execute smoke tests, inspect production, approve evidence, or deploy. It
prevents incomplete evidence from being represented as release approval. A human approver remains
accountable for evidence authenticity and freshness.

## Release decision

Only when exact-commit CI and this validator both pass may the authorized release operator remove
the v1.0 draft banner, create the immutable annotated tag, publish release notes, and authorize the
production release. A validator failure blocks release; correct the underlying failed control and
regenerate approved evidence rather than editing a status to `PASS`.

Related procedures: [release tagging](release-tagging.md),
[production deployment](production-deployment-guide.md),
[smoke checklist](production-smoke-test-checklist.md), [backup guide](backup-guide.md),
[security checklist](production-security-checklist.md), [rollback plan](rollback-plan.md), and
[v1.0 release notes](../releases/v1.0-draft.md).

Automated evidence: `ProductionReleaseGateDocumentationTests`.
