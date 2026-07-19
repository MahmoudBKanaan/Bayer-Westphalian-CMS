# Documentation Readiness and Audience Map

This document fulfills KB item **864** by giving developers, evaluators and operations personnel a
single route through the project documentation.

## Start by Audience

| Audience | Start here | Then use |
| --- | --- | --- |
| Developer | `README.md`, developer setup | architecture, API, module docs, maintainer guide, tests |
| Evaluator | final report | appendices, UMLs, test report, demo script and completion matrices |
| Operations | production operations manual | deployment, environment/secrets, backup/restore, smoke, rollback, incident guides |
| Administrator | administrator manual | user/role guide, settings, audit and bootstrap guides |
| Business user | employee user manual | role-specific guides and demo workflow |
| Security/compliance reviewer | security hardening and role access | eligibility, audit, production checklist and release gate |

## Required Understanding Path

| Need | Authoritative documentation | Practical evidence |
| --- | --- | --- |
| Understand purpose and scope | `README.md`, final report | use-case and architecture diagrams |
| Understand design | architecture docs, decision log | class diagram, ERD and source packages |
| Run locally | developer setup guide | local Compose, health endpoint and login screen |
| Test | master test plan and test maps | Maven, Vitest, Playwright and CI workflow |
| Deploy | production deployment guide | Compose, Nginx, env templates and health checks |
| Review behavior | user/admin manuals, API guide | demo script, acceptance maps and audit records |
| Maintain | maintainer guide | migrations, module docs, tests and decision log |
| Operate/recover | operations manual | monitoring, backup, restore, rollback and incident runbooks |
| Release | release notes and production gate | exact-commit CI and approved item 770 manifest |

## Minimal Reproduction Commands

From the repository root on Windows PowerShell:

```powershell
docker compose up -d postgres

Set-Location backend
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

In a second terminal:

```powershell
Set-Location frontend
npm install
npm run dev
```

Verification entry points:

```powershell
Set-Location frontend
npm run verify
npm run test:e2e

Set-Location ../backend
mvn verify
```

The detailed setup guide remains authoritative for prerequisites, environment files, ports and
troubleshooting. Production uses the production deployment guide, never these development commands.

## Review Questions

A complete handover should let the reader answer:

1. Which role creates, approves and launches a campaign?
2. Where are consent and recipient eligibility enforced?
3. Why can AI not approve copy or bypass DNC?
4. How is a database change introduced safely?
5. Which command runs each quality gate?
6. How are secrets and real providers configured or disabled?
7. How are health, logs, backup, restore and rollback handled?
8. What evidence is required before `v1.0` may be tagged?

If any answer cannot be found through the links above, update the relevant guide and this map before
handover.

## Current Readiness

Documentation coverage for understand, run, test, deploy, review and maintain is **PASS**. Production
release authorization remains **BLOCKED** until the exact-commit item 770 runtime evidence passes.
Documentation completeness must not be used as a substitute for that operational evidence.
