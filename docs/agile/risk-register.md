# Risk Register

Scale: probability and impact are Low, Medium, or High. Owners are project roles, not fictional people.

| ID | Risk | Probability | Impact | Mitigation / control | Owner | Status |
| --- | --- | --- | --- | --- | --- | --- |
| R-01 | Unauthorized access or privilege escalation | Medium | High | backend RBAC, JWT validation, route guards, security tests | Developer/Admin | Controlled |
| R-02 | Contact without valid consent or despite DNC | Medium | High | centralized `EligibilityService`, preview reasons, no-bypass tests | Compliance | Controlled |
| R-03 | Campaign launched without human approval | Low | High | state machine, role checks, confirmation and audit | Compliance | Controlled |
| R-04 | AI suggestion treated as an autonomous decision | Medium | High | explanation, confidence, explicit human copy approval | Campaign Manager | Controlled |
| R-05 | Secret or stack-trace exposure | Medium | High | prod validation, safe errors/logging, ignored env files | Operations | Controlled |
| R-06 | Data loss or failed restore | Medium | High | scheduled backups, checksums, off-host copy and restore rehearsal | Operations | Open gate |
| R-07 | External email/SMS misconfiguration | Medium | High | fail-closed provider policy; disabled until approved | Operations | Open gate |
| R-08 | Regression in large test suite | Medium | Medium | exact-commit CI, unit/integration/E2E suites | Developer/QA | Controlled |
| R-09 | Production differs from local environment | Medium | High | production Compose, health checks and deployed smoke tests | Operations | Open gate |
| R-10 | Incomplete university evidence | Medium | Medium | traceability index, appendices and submission checklist | Developer | Mitigated |
| R-11 | Solo-project knowledge concentration | High | Medium | setup, API, user, admin, deployment and operations manuals | Developer | Mitigated |
| R-12 | False release claim | Medium | High | fail-closed item 770 manifest and immutable tag procedure | Release approver | Blocked safely |

Review risks at sprint review, before release-candidate creation, and at every production gate.
