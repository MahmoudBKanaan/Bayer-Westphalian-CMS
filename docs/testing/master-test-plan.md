# Master Test Plan

## Purpose

This plan defines the minimum verification required for the Bayer-Westphalian Campaign
Management Platform. It implements the KB Testing Plan, NFR-010 (testability), the Definition of
Done, and the production release gate.

## Scope

Testing covers authentication and roles, users, customers and beneficiaries, consent and
eligibility, products and payments, segments, campaigns and compliance approval, communications,
follow-ups, reminders, analytics and reports, AI-assisted features, audit logging, system settings,
security, accessibility, and production configuration.

External email and SMS delivery may use mock providers in development and test. Provider
configuration and smoke checks cover production integration; destructive production testing is
out of scope.

## Test Strategy

| Level | Scope | Primary tool |
| --- | --- | --- |
| Backend unit | Services, validation, eligibility, consent, workflows, schedulers, audit | JUnit 5, Mockito |
| Backend integration | Controllers, authorization, persistence, migrations, PostgreSQL where applicable | Spring Boot Test, Testcontainers |
| Security | Authentication, role access, forbidden actions, CORS, safe errors, secrets | Spring Security Test |
| Frontend unit/component | Forms, tables, charts, badges, dialogs, API clients | Vitest, React Testing Library |
| Frontend integration | Router, session, role menus, customer and campaign workflows | Vitest, React Testing Library |
| Browser E2E | Login -> customer -> consent -> campaign -> approval -> launch | Playwright |
| Non-functional | Accessibility, search performance smoke, health, logging, backup/restore | Automated checks and operational smoke tests |

Detailed traceability is maintained in:

- [Functional requirements test map](functional-requirements-test-map.md)
- [Business rules test map](business-rules-test-map.md)
- [Non-functional requirements test map](non-functional-requirements-test-map.md)

## Critical Test Cases

| ID | Expected result |
| --- | --- |
| TC-001 | Campaign cannot launch without compliance approval. |
| TC-002 | A do-not-contact customer is excluded. |
| TC-003 | A customer without valid consent is excluded. |
| TC-004 | A minor beneficiary without guardian consent is excluded. |
| TC-005 | The same customer cannot be duplicated in one campaign. |
| TC-006 | The configured monthly contact limit is enforced. |
| TC-007 | Red reminder follows earlier reminder levels. |
| TC-008 | Product-expiration reminders support 3, 6, and 12 months. |
| TC-009 | BI Analyst cannot edit customers. |
| TC-010 | Product Manager cannot launch campaigns. |
| TC-011 | Compliance Officer can approve and reject campaigns. |
| TC-012 | Contact events update campaign analytics. |
| TC-013 | Soft-deleted customers are absent from active lists. |
| TC-014 | Consent changes create immutable audit records. |
| TC-015 | Admin can disable a user and the disabled user cannot log in. |

## Environment And Data

- Use Java 21, Node.js 22, Maven, and the locked npm dependencies.
- Backend integration tests use isolated test data and PostgreSQL/Testcontainers where required.
- Frontend tests use deterministic fixtures and mocked HTTP responses.
- E2E tests use the controlled Playwright dataset; tests must not depend on execution order.
- Never use real customer data, production secrets, or live provider credentials in automated tests.

## Execution

Run from the repository root unless a command changes directory:

```powershell
mvn -f backend/pom.xml test
Set-Location frontend
npm test
npm run test:e2e
npm run lint
npm run build
```

CI additionally validates backend packaging, frontend installation, Docker images, Docker Compose,
and production configuration. See [CI/CD documentation](../deployment/ci-cd.md).

## Entry And Exit Criteria

Testing may begin when acceptance criteria, permissions, validation rules, required migrations, and
test data are known and the relevant code builds.

A change passes when:

- required unit, integration, security, frontend, and E2E tests pass;
- lint and production builds pass;
- critical workflows and role restrictions pass;
- no unresolved critical or high-severity defect remains;
- sensitive actions are auditable and production errors expose no secrets or stack traces;
- documentation and requirement traceability are current.

The main branch is not releasable unless CI passes. Production release also requires successful
smoke tests, backup verification, security and environment configuration, provider policy checks,
and an approved rollback plan.

## Responsibility And Evidence

This is a solo-adapted Scrum project: the developer performs development, self-review, test
execution, defect correction, and evidence capture. Failed tests are recorded with the command,
environment, failure output, root cause, fix, and rerun result. CI logs, test reports, screenshots,
and smoke-test records are release evidence.
