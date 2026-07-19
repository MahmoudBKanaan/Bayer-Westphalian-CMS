# Release Plan Evidence

The project follows the KB incremental release strategy and solo-adapted Scrum model.

| Release | Objective | Principal evidence | Status |
| --- | --- | --- | --- |
| v0.1 | Foundation | repository structure, React, Spring Boot, PostgreSQL, Docker | Complete |
| v0.2 | Secure access | JWT authentication, roles, protected routes, user administration | Complete |
| v0.3 | CRM and compliance | customers, beneficiaries, consent, opt-out | Complete |
| v0.4 | Products and segmentation | products, ownership, payments, segment builder | Complete |
| v0.5 | Campaign lifecycle | builder, preview, compliance review, launch controls | Complete |
| v0.6 | Communication and reminders | contact events, adapters, reminders, follow-up tasks | Complete |
| v0.7 | Analytics and AI | dashboards, exports, explainable AI assistance | Complete |
| v0.8 | Audit and hardening | immutable audit log, RBAC/security controls | Complete |
| v0.9 | Release candidate | full local suites, CI/CD and production documentation | RC tag exists |
| v1.0 | Production-ready MVP | exact-commit CI plus item 770 operational gates | Blocked |

The release candidate tag is `v0.9.0-rc.1`. The final `v1.0` tag is deliberately absent because
the deployed HTTPS, smoke, backup/restore, provider-policy, critical-workflow, and human-approval
evidence remains incomplete. See `docs/releases/v1.0-draft.md`.

Evidence anchors: `.github/workflows/ci.yml`, `docs/testing/test-execution-report.md`,
`docs/deployment/production-release-gate.md`, and `docs/deployment/release-tagging.md`.
