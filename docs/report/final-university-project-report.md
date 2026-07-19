# Bayer-Westphalian Campaign Management Platform

## Final University Project Report

This document fulfills KB item **860**: create, complete and polish the final report.

| Report field | Value |
| --- | --- |
| Project | Bayer-Westphalian Campaign Management Platform |
| Domain | Insurance marketing, CRM, compliance and business intelligence |
| Project type | Full-stack enterprise software engineering project |
| Delivery model | Solo-adapted Scrum |
| Principal technologies | React, TypeScript, Spring Boot, Java 21, PostgreSQL, Docker and Nginx |
| Evidence baseline | Commit `0277a591499ec1ebd90df2083c05cf6f8a51bfe6`, audited 2026-07-19 |
| Candidate version | `v0.9.0-rc.1` |
| Report status | Final submission candidate |
| Production status | `v1.0` not released; operational release gate remains blocked |

> This report describes the implemented and verified repository state. It does not claim a
> production deployment or final release where the required runtime evidence is absent.

## Abstract

Insurance campaign management combines customer relationship management, product knowledge,
marketing execution and legal compliance. A campaign platform must therefore do more than compose
messages: it must control who may act, establish whether a customer may be contacted, preserve human
approval, prevent excessive or duplicate contact, and retain evidence for later review.

This project delivers an internal campaign-management platform for Bayer-Westphalian Insurance. The
system integrates employee authentication and authorization, customer and beneficiary records,
consent, products and ownership, reusable audience segments, campaign creation and compliance review,
recipient eligibility, controlled launch, communication tracking, reminders, follow-up work,
analytics, reports, immutable audit records and explainable AI assistance. It is implemented as a
React and TypeScript frontend, a Spring Boot modular monolith and a PostgreSQL database managed by
Flyway. A documented Docker Compose and Nginx deployment model supports production preparation.

The principal technical contribution is a set of non-bypassable control boundaries. Centralized
eligibility evaluates consent, guardian consent, do-not-contact, opt-out, duplicate-recipient and
contact-limit rules. AI remains advisory: its results include explanations, campaign copy requires
human approval, and it cannot approve or launch a campaign. The recorded release-candidate suite
contains 4,168 backend, 967 frontend and 36 Playwright executions with zero failures. The application
is a strong operation-ready MVP submission, while final production release is intentionally withheld
until exact-commit CI and runtime security, backup, restore, smoke, provider and approval gates pass.

**Keywords:** campaign management, insurance CRM, consent, eligibility, role-based access control,
audit logging, explainable AI, Spring Boot, React, PostgreSQL, Scrum.

## Table of Contents

1. Introduction
2. Problem Definition and Objectives
3. Scope, Stakeholders and Requirements
4. Development Methodology
5. System Architecture and Design
6. Implementation
7. Verification and Validation
8. Security, Privacy, Compliance and AI Ethics
9. Deployment and Operations
10. Results and Evaluation
11. Limitations and Future Work
12. Lessons Learned
13. Conclusion
14. References and Appendices

## 1. Introduction

### 1.1 Background

Insurance organizations maintain long-lived relationships involving customers, beneficiaries,
products, payments, renewals and changing communication preferences. Marketing activity must use
this information without weakening consent or regulatory controls. Separate spreadsheets and tools
make it difficult to enforce consistent eligibility, separate campaign creation from approval, and
explain later why a customer was contacted or excluded.

The Bayer-Westphalian Campaign Management Platform addresses this problem as an internal employee
application. It creates one traceable workflow from customer and product context to segmentation,
campaign review, launch, communication history and analytics.

### 1.2 Project Motivation

The project was motivated by four connected needs:

1. **Operational efficiency:** employees need one interface for customers, products, segments,
   campaigns, reminders, follow-ups and reports.
2. **Compliance by design:** consent, do-not-contact, guardian consent and contact-frequency rules
   must be mandatory system behavior rather than optional user checks.
3. **Accountability:** campaign approval, launch and other sensitive actions must be attributable and
   auditable.
4. **Responsible assistance:** AI can improve search and content drafting, but must remain explainable
   and subordinate to authorization, eligibility and human approval.

### 1.3 Report Purpose

This report explains the problem, requirements, engineering method, architecture, implementation,
tests, security controls, deployment model and project results. The appendices provide direct links
to source-backed evidence. Claims are intentionally separated into implemented behavior, recorded
test evidence and outstanding production-runtime evidence.

## 2. Problem Definition and Objectives

### 2.1 Problem Statement

The business needs to create relevant insurance campaigns while preventing unauthorized or
non-compliant contact. Without an integrated system, customer/product data becomes inconsistent,
audience rules are difficult to reuse, approval may be informal, contact attempts can be duplicated,
and reporting lacks traceability. Automation can amplify these risks if it is allowed to override
deterministic compliance rules.

### 2.2 Project Aim

The aim was to design, implement and document an operation-oriented MVP that supports the complete
employee-managed campaign lifecycle and makes its compliance boundaries testable and auditable.

### 2.3 Objectives and Outcomes

| Objective | Outcome and evidence |
| --- | --- |
| Secure employee access | JWT authentication, account controls, backend RBAC and protected routes |
| Maintain CRM and product context | customer, beneficiary, product, ownership and payment modules |
| Record consent and restrictions | consent evidence, withdrawal, opt-out and do-not-contact controls |
| Build reusable audiences | saved segments, criteria builder, preview and exclusion summaries |
| Control campaign lifecycle | draft, submission, human compliance review, approval/rejection and launch |
| Prevent ineligible contact | centralized `EligibilityService` and stored recipient reasons |
| Track outcomes and work | contact events, follow-up tasks and reminder scheduling |
| Support decisions | analytics, report export and explainable AI recommendations |
| Preserve accountability | immutable sensitive-action audit logs |
| Demonstrate quality | unit, integration, security, frontend, accessibility and E2E tests |
| Prepare operations | CI/CD, production Compose, HTTPS, health, logging, backup/restore and rollback docs |

### 2.4 Success Criteria

Success requires more than screens existing. Core workflows must enforce backend authorization,
validate input, handle failures safely, audit sensitive changes, provide automated tests and be
documented for users and operators. Production release additionally requires exact-commit green CI,
secure environment configuration, backup and restore evidence, deployed smoke tests, provider-policy
confirmation, rollback readiness and human approval.

## 3. Scope, Stakeholders and Requirements

### 3.1 Stakeholders and Roles

| Role | Principal responsibility |
| --- | --- |
| Admin | employee accounts, roles, settings and administration |
| Campaign Manager | reusable segments, campaign drafts, previews and approved launch |
| Compliance Officer | consent oversight and independent campaign approval/rejection |
| Product Manager | products, ownership and product-change workflows |
| Customer Service Agent | customer data, consent support and contact history |
| Sales Agent | contact outcomes and assigned follow-up tasks |
| BI/Marketing Analyst | campaign, product, segment and channel analysis |
| Executive Viewer | read-only executive metrics and reports |
| System Auditor | read-only audit, consent, approval and user-activity review |

Separation of duties is deliberate. A Campaign Manager cannot convert AI output into compliance
approval, and analytical/read-only roles cannot gain mutation rights through hidden UI actions.

### 3.2 Functional Scope

| Capability group | Included functions |
| --- | --- |
| Identity | login, access/refresh tokens, account status, role assignment and password administration |
| CRM | customer/prospect CRUD, search, import, beneficiaries and soft deletion |
| Consent | record, withdraw, inspect evidence, channel mapping, opt-out and DNC |
| Products | catalog, ownership, payments and product-change requests |
| Segmentation | reusable segments, UUIDs, AND/OR criteria, previews and exclusion reasons |
| Campaigns | product/segment selection, copy, schedule, preview, submit, review and launch |
| Communications | provider abstraction, retry policy, contact events and history |
| Work management | reminders, scheduler/manual processing and follow-up task assignment/completion |
| Intelligence | dashboards, KPIs, CSV/PDF reports, fuzzy search and recommendations |
| Governance | audit log, system settings, role guides and operational evidence |

### 3.3 Core Business Rules

- A campaign cannot launch before human compliance approval.
- A customer marked do-not-contact or marketing opt-out is excluded.
- Contact requires valid consent for the selected channel.
- A minor beneficiary requires valid guardian consent where applicable.
- A customer cannot be duplicated in the same campaign.
- The configurable monthly contact limit is enforced.
- Paid payments do not generate payment reminders; expiration reminders support 3/6/12 months.
- AI cannot approve campaigns, override consent, bypass DNC/opt-out or bypass `EligibilityService`.
- Sensitive actions create immutable audit records that normal users cannot edit.

### 3.4 Non-Functional Requirements

| Quality attribute | Project response |
| --- | --- |
| Security | BCrypt, JWT, RBAC, safe errors, secret validation, restricted CORS and HTTPS/HSTS model |
| Privacy/compliance | least privilege, consent evidence, eligibility and synthetic demo data policy |
| Reliability | validation, transactions, health endpoints, retries, scheduler logs and backups |
| Usability | application shell, headings, feedback states, badges, validation and confirmations |
| Accessibility | labels, keyboard operation, table naming, contrast-oriented status and responsive layout |
| Maintainability | modular packages, typed DTOs, migrations, tests, OpenAPI and decision records |
| Testability | isolated profiles, provider abstractions, extensive unit/integration/E2E coverage |
| Operability | Compose, Nginx, logging, monitoring notes, backup/restore, smoke and rollback procedures |

### 3.5 Out of Scope and Boundaries

The repository does not bundle a commercial email/SMS account, hosted secrets manager, hosted
monitoring platform, paging system or cloud point-in-time recovery. These are controlled integration
points. Real sending remains disabled until approved and configured. The application is an internal
employee platform, not a public customer portal. Demo and committed evidence use synthetic data.

## 4. Development Methodology

### 4.1 Solo-Adapted Scrum

The project applies Scrum transparently in a solo setting. The developer performs Product Owner,
Scrum Master, Developer and QA responsibilities. Stakeholder perspectives are represented by the KB
roles, not by fictional team members. The process uses Product Backlog, Sprint Backlog, In Progress,
Blocked, Self Review, Testing and Done states.

### 4.2 Incremental Delivery

The release strategy progresses from foundation and secure access through CRM, products,
segmentation, campaign lifecycle, communications, analytics/AI, audit/hardening and production
preparation. The existing `v0.9.0-rc.1` tag represents the release candidate. `v1.0` is reserved for
the approved production-ready MVP.

### 4.3 Definition of Ready and Done

A backlog item is ready when its business value, role, acceptance conditions and dependencies are
understood. It is done when relevant API/database/UI behavior, validation, authorization, errors,
audit, tests, documentation and demonstration evidence exist. A feature-level Done state does not
automatically satisfy the production release gate.

### 4.4 Project Evidence

The [release plan](../agile/release-plan-evidence.md),
[product backlog](../agile/product-backlog-evidence.md),
[story map](../agile/user-story-map-evidence.md),
[risk register](../agile/risk-register.md) and
[decision log](../agile/decision-log.md) preserve planning and rationale. This provides an honest,
reviewable process trail suitable for the university assessment.

## 5. System Architecture and Design

### 5.1 Architectural Style

The backend is a Spring Boot modular monolith. This architecture was selected because it supports
transactional consistency and straightforward deployment while retaining domain package boundaries.
Controllers and request DTOs form the API boundary; services implement workflows; repositories use
Spring Data JPA; PostgreSQL is the system of record; Flyway owns forward-only schema migration.

The React frontend uses typed API modules, React Router, TanStack Query, React Hook Form and Zod.
Role-aware navigation improves usability, while the backend remains the authority for access.

### 5.2 Runtime Components

1. An authorized employee connects through HTTPS.
2. Nginx serves built frontend assets and proxies `/api` to Spring Boot.
3. Spring Security validates JWT identity and role authorization.
4. Controllers validate requests and call domain services.
5. Services enforce business rules and persist through repositories.
6. PostgreSQL stores operational data; consent evidence uses durable storage.
7. Scheduler/provider adapters process controlled reminder and communication work.
8. Logs, health checks, backups and monitoring support operations.

### 5.3 Domain and Data Design

The data model includes users and roles; customers and beneficiaries; consent; products, ownership,
payments and change requests; segments and criteria; campaigns, products and recipients; contact
events; reminders; follow-up tasks; metrics and report exports; AI recommendations; settings and
audit logs. UUID identifiers avoid environment-dependent sequence assumptions and provide valid IDs
for API/UI references.

Twenty-five Flyway migrations define the audited schema evolution. Applied migrations are immutable;
new changes use a new versioned migration.

### 5.4 Eligibility as a Control Boundary

`EligibilityService` evaluates active status, DNC, opt-out, channel consent, guardian consent,
duplicate campaign membership, monthly contact limits, temporary lack of interest and prior
conversion. Recipient preview stores both eligible and excluded snapshots with reasons. This gives
business users an explanation and creates a controlled launch input. AI, UI, reminders and campaign
launch cannot create an alternate route around eligibility.

### 5.5 Campaign State and Human Approval

Campaigns progress through draft, submission, pending approval, approval/rejection and launch states.
The Campaign Manager creates and submits; the Compliance Officer performs the human review; launch is
restricted to an approved campaign. AI copy has its own human-review state and does not change the
campaign's compliance status.

### 5.6 Design Evidence

The `UMLs` directory contains the use-case, campaign-creation activity, consent/eligibility activity,
campaign-launch sequence, ERD, backend class, system architecture and deployment diagrams. SVG makes
the diagrams scalable for submission; Mermaid sources are retained for newly created diagrams.

## 6. Implementation

### 6.1 Repository Scale

At the report evidence baseline, the repository contains:

| Measure | Count |
| --- | ---: |
| Backend production Java files | 381 |
| Backend Java test files | 516 |
| Frontend non-test TypeScript/TSX files | 152 |
| Frontend Vitest/Playwright test files | 165 |
| Flyway migrations | 25 |
| Exported OpenAPI paths | 89 |

Counts communicate scale but are not quality claims; behavior is evaluated through traceability and
tests.

### 6.2 Authentication, Users and Roles

The authentication module issues and validates access/refresh JWTs, loads enabled users and applies
rate/lock controls. User administration supports creation, editing, role changes, password reset and
disable/enable behavior. Disabled accounts cannot authenticate. Backend method/endpoint rules prevent
UI manipulation from granting access.

### 6.3 Customers, Beneficiaries and Consent

Employees can maintain customer and prospect profiles, search/filter records, manage beneficiaries,
record/withdraw consent and review evidence. Soft deletion preserves references. Consent updates are
audited and feed directly into eligibility rather than remaining informational fields.

### 6.4 Products, Ownership and Payments

Product Managers maintain products, ownerships, payment history and change requests. These records
support segment criteria, campaign context, default-risk assistance and expiration/payment reminders.

### 6.5 Segments and Campaigns

Campaign Managers create reusable segments with valid generated UUIDs, criteria and audience
previews. The campaign builder provides product and segment selectors for create and edit workflows,
copy fields, objective, channel, schedule and budget. Submission creates a controlled recipient
snapshot and enters compliance review. Approval and launch are separate sensitive actions with audit
records and confirmations.

### 6.6 Communications, Reminders and Follow-Up

Contact events record channel, direction, outcome and campaign/customer context. Provider interfaces
permit mock development/test adapters while production policy fails closed. Retry limits are
configurable. Reminder scheduling supports payment and 3/6/12-month expiration cases, Green/Yellow/Red
levels, scheduler logs and guarded manual triggering. Follow-up tasks support creation, assignment,
filtering and completion.

### 6.7 Analytics and Reports

Dashboards expose campaign and product performance according to role. Metrics derive from campaign
recipient and contact-event data. CSV/PDF export and export history support business reporting and
auditability.

### 6.8 AI-Assisted Features

AI functionality includes fuzzy/weighted customer search with score explanation, product
recommendations, segment suggestions, default-risk scoring from payment history, duplicate-contact
risk warnings and campaign-copy suggestions. Recommendations store explanation and confidence when
available. Campaign copy is visibly a draft and requires an explicit user decision to approve/apply,
edit or reject it.

### 6.9 Audit and System Settings

Audit records capture actor, action, entity and timestamp context for sensitive operations. Normal
users cannot edit the audit trail, and audit reads are role restricted. System settings expose the
documented monthly contact limit, retry limit and uninterested-exclusion period and wire those values
into domain decisions.

### 6.10 User Experience

The frontend provides a consistent shell, sidebar, top bar/user menu, page headings, responsive
desktop/tablet behavior and minimum mobile usability. Loading, empty, error and success states make
asynchronous workflows understandable. Forms use labels and validation messages; sensitive actions
use confirmations; campaign, customer, consent, reminder and audit states use accessible badges.

## 7. Verification and Validation

### 7.1 Test Strategy

Verification follows a layered strategy:

- backend unit tests for domain rules and service behavior;
- repository and integration tests for persistence and API behavior;
- security tests for authentication, authorization, CORS and safe errors;
- scheduler tests for reminders and operational logging;
- frontend unit and integration tests for forms, pages, routing and feedback states;
- Playwright E2E tests for role-based business workflows;
- accessibility tests for labels, tables, keyboard operation and main screens;
- CI checks for lint, formatting, build, tests, Docker images and Compose configuration.

### 7.2 Recorded Release-Candidate Results

Execution date: 2026-07-17. Release candidate: `v0.9.0-rc.1`.

| Category | Executed | Passed | Failed | Skipped | Result |
| --- | ---: | ---: | ---: | ---: | --- |
| Backend unit and integration | 4,168 | 3,954 | 0 | 214 | PASS |
| Frontend unit and integration | 967 | 967 | 0 | 0 | PASS |
| Playwright E2E | 36 | 36 | 0 | 0 | PASS |

Security tests are included in the backend total; accessibility checks are included in frontend and
Playwright totals. Skipped backend cases are recorded by the suite and are not failures. The report
recorded zero critical defects and zero release-blocking test failures for that candidate.

### 7.3 Critical Acceptance Coverage

The critical map verifies that campaigns cannot launch without approval; DNC, missing consent,
missing guardian consent, duplicate recipients and monthly limits cause exclusion; paid payments do
not receive reminders; expiration windows work; restricted roles cannot mutate or launch; consent
changes are audited; and disabled users cannot log in. AI-specific security tests verify that AI
cannot approve, override consent/DNC or bypass eligibility.

### 7.4 CI/CD Quality Gate

GitHub Actions runs the backend, frontend and Docker gates for pull requests and pushes to `main` and
`dev`. Jobs do not use soft-fail behavior for test failures. A historical local/RC pass cannot make a
new commit releasable; CI must pass for the exact final `main` SHA.

### 7.5 Evidence Limitations

Automated tests provide strong repeatable evidence but do not prove the actual production hostname,
TLS certificate, external providers, monitoring or off-host recovery. These require environment-
specific operational evidence.

## 8. Security, Privacy, Compliance and AI Ethics

### 8.1 Security Controls

Passwords are hashed; JWTs are validated; disabled users are rejected; backend roles protect
endpoints and services. Production configuration restricts CORS, requires HTTPS/HSTS, validates
secrets and environment values, suppresses stack traces and sanitizes logs. Swagger/OpenAPI exposure
is disabled by default in production.

### 8.2 Privacy and Data Handling

Customer identity, contact preferences and consent evidence are sensitive. Access is least privilege,
audit reads are restricted and evidence storage is durable. Real customer data, credentials, JWTs,
private keys, database dumps and consent files must not enter Git or university screenshots. The
demo dataset is synthetic.

### 8.3 Compliance Controls

Consent is enforced at the point of eligibility, not only displayed. Human campaign approval and
recipient eligibility are distinct mandatory controls. Audit logs make sensitive actions
attributable. Contact-frequency and duplicate-contact rules protect customers from excessive
outreach.

### 8.4 Responsible AI

AI outputs can be uncertain or incomplete. The design responds through explanation, confidence when
available, visible suggestion status and explicit human review. AI cannot expand permissions or
alter deterministic compliance outcomes. This reduces automation bias and ensures a named employee
remains accountable for campaign copy and campaign approval.

### 8.5 Threat and Risk Treatment

The risk register covers privilege escalation, invalid contact, unauthorized launch, AI automation
bias, secret leakage, data loss, provider misconfiguration, regression, environment drift, evidence
gaps and false release claims. Open operational risks remain release blockers rather than being
silently accepted.

## 9. Deployment and Operations

### 9.1 Production Model

The deployment model uses PostgreSQL 16, Spring Boot, built React assets and Nginx in Docker Compose.
Nginx terminates HTTPS and proxies the API. Persistent volumes protect database and consent evidence.
Health endpoints, bounded application/scheduler logs and monitoring procedures support diagnosis.

### 9.2 Configuration and Secrets

Production uses environment variables for database credentials, JWT secrets, explicit CORS origins,
contact/retry settings, scheduler policy, provider mode and storage locations. Secrets are supplied
outside Git. Real email/SMS must be approved and configured, or sending remains explicitly disabled.

### 9.3 Backup, Restore and Continuity

The operations package documents scheduled PostgreSQL backups, checksums, encrypted off-host copies,
matching consent-evidence recovery points and non-production restore rehearsal. Rollback considers
immutable images and schema compatibility. Incident and monitoring notes define escalation and
evidence preservation.

### 9.4 Production Release Gate

The KB permits release only when all of the following pass for the exact commit and environment:

1. production smoke tests;
2. fresh backup and successful restore rehearsal;
3. security configuration;
4. environment configuration;
5. real-provider or explicitly-disabled provider policy;
6. rollback readiness;
7. critical role workflows and human release approval.

At report preparation, these runtime conditions are not all evidenced. The repository is on `dev`
with working changes and no approved exact-final-`main` result. Consequently, no `v1.0` tag was
created. This is a correct fail-closed outcome, not an undocumented omission.

## 10. Results and Evaluation

### 10.1 Achievement Against Objectives

The system implements the KB minimum operation-ready MVP modules and integrates them through one
role-aware workflow. Eligibility is centralized, approval is human-controlled, sensitive actions are
auditable and AI is explainable and constrained. The application is supported by a large automated
suite, OpenAPI export, user/admin/operations manuals, production runbooks and eight final diagrams.

### 10.2 Technical Quality

Strengths include clear domain boundaries, typed contracts, versioned schema changes, broad test
coverage and explicit control points. Fail-closed production configuration and release evidence
reduce the risk of treating a development system as production-ready.

### 10.3 Business Value

The platform gives campaign teams a shared source for targeting, approval and performance while
giving compliance/audit roles visible evidence. Customer service and sales roles can record outcomes
and follow-up work; product and analyst roles can contribute without receiving campaign-launch
authority. This improves traceability and reduces manual coordination.

### 10.4 Evaluation Summary

| Dimension | Assessment |
| --- | --- |
| Functional breadth | Strong: all KB MVP domains represented |
| Compliance design | Strong: eligibility, role separation, human approval and audit are central |
| Automated verification | Strong RC evidence; exact final-commit rerun still required |
| Documentation | Strong: setup, deployment, users, admin, operations, API, demo and report package |
| Production execution | Incomplete evidence; release remains correctly blocked |
| External integration | Adapter-ready; real providers/hosted operations remain deployment-owned |

## 11. Limitations and Future Work

1. Complete the production `v1.0` gate on an approved HTTPS environment and exact final commit.
2. Integrate approved real email/SMS providers or retain explicit disabled mode.
3. Connect an enterprise secret manager, centralized observability, paging and certificate alerts.
4. Verify encrypted off-host backups and repeat restore exercises against agreed RPO/RTO targets.
5. Conduct user acceptance and accessibility evaluation with representative employees and assistive
   technologies.
6. Evaluate AI relevance, fairness and calibration using governed business datasets without
   weakening deterministic controls.
7. Add performance/load baselines based on expected production customer and campaign volumes.
8. Review modular-monolith boundaries if scaling or independent deployment requirements emerge.

## 12. Lessons Learned

- Compliance rules are most reliable when centralized and reused by every workflow.
- Frontend role visibility improves usability but cannot replace backend authorization.
- AI-human interaction requires explicit states; silently applying suggestions obscures accountability.
- Test counts are useful evidence only when linked to critical business rules and an exact commit.
- Deployment documentation is not deployment evidence; runtime gates must remain separate.
- A solo Scrum project is credible when responsibilities and evidence are transparent.
- Editable diagrams and decision records reduce drift between implementation and final documentation.

## 13. Conclusion

The Bayer-Westphalian Campaign Management Platform demonstrates the design and implementation of a
substantial consent-aware insurance campaign system. It combines CRM, product and campaign
capabilities with role separation, centralized eligibility, human approval, communication tracking,
analytics, auditability and constrained AI assistance. The architecture and test strategy make the
critical compliance behavior explicit and reviewable.

The implementation meets the intended university-project and operation-ready MVP scope. Its most
important quality is that convenience and automation do not supersede authorization, consent,
eligibility or human accountability. The project is ready as a final submission candidate. A final
production `v1.0` release remains intentionally conditional on the documented exact-commit and
runtime gates.

## 14. References and Appendices

### 14.1 Project References

- Project Knowledge Base: `KnowledgeBase.txt` and `KnowledgeBase.docx`.
- [Repository README](../../README.md).
- [Documentation index](../README.md).
- [Architecture overview](../architecture/initial-architecture.md).
- [Eligibility rules](../architecture/eligibility-rules.md).
- [Role-based access](../architecture/role-based-access.md).
- [Security hardening](../architecture/security-hardening.md).
- [API and OpenAPI guide](../api/openapi.md).
- [Master test plan](../testing/master-test-plan.md).
- [Test execution report](../testing/test-execution-report.md).
- [Production deployment guide](../deployment/production-deployment-guide.md).
- [Production release gate](../deployment/production-release-gate.md).
- [Operations manual](../operations/operations-guide.md).
- [Draft v1.0 release notes](../releases/v1.0-draft.md).

### 14.2 Appendix Package

- [Appendix index](appendices/README.md).
- [Appendix A: Implementation evidence](appendices/implementation-evidence.md).
- [Appendix B: Testing evidence](appendices/testing-evidence.md).
- [Appendix C: Scrum process evidence](appendices/scrum-process-evidence.md).
- [Appendix D: Deployment evidence](appendices/deployment-evidence.md).
- [Appendix E: Release evidence](appendices/release-evidence.md).
- [Appendix F: Diagram evidence](appendices/diagram-evidence.md).
- [Final submission checklist](final-submission-checklist.md).
- [Final handover checklist](../handover/final-handover-checklist.md).
- [Final delivery package checklist](../handover/final-delivery-package-checklist.md).
- [Documentation readiness map](../documentation-readiness.md).
- [Maintainer guide](../maintenance/maintainer-guide.md).

## Glossary

| Term | Meaning |
| --- | --- |
| AI | Artificial intelligence used for advisory search, recommendation and copy assistance |
| API | Application Programming Interface |
| CI/CD | Continuous Integration and Continuous Delivery/Deployment |
| CRM | Customer Relationship Management |
| DNC | Do Not Contact |
| DTO | Data Transfer Object |
| E2E | End-to-end browser testing |
| HSTS | HTTP Strict Transport Security |
| JWT | JSON Web Token |
| KPI | Key Performance Indicator |
| MVP | Minimum Viable Product |
| RBAC | Role-Based Access Control |
| RPO/RTO | Recovery Point Objective / Recovery Time Objective |
| UUID | Universally Unique Identifier |
