# Architecture and Product Decision Log

| ID | Decision | Rationale | Consequence |
| --- | --- | --- | --- |
| ADR-001 | Use a modular Spring Boot monolith | simpler deployment and transactional consistency for university/MVP scope | packages enforce domain boundaries; later extraction remains possible |
| ADR-002 | Use React/TypeScript with REST | typed, testable internal UI and clear backend boundary | frontend and backend deploy independently behind Nginx |
| ADR-003 | PostgreSQL plus forward-only Flyway migrations | relational integrity and reproducible schema history | never edit applied migrations or use production `flyway clean` |
| ADR-004 | Centralize contact rules in `EligibilityService` | consent and DNC controls must be consistent | UI, AI, reminders and launch cannot bypass it |
| ADR-005 | Require human campaign and AI-copy approval | KB compliance rule and accountable decision-making | AI output remains a suggestion and cannot change campaign approval |
| ADR-006 | Store recipient preview snapshots and exclusion reasons | explainability and launch traceability | launch uses the approved stored eligible snapshot |
| ADR-007 | Use immutable audit records | sensitive actions must be attributable | normal users have no audit mutation operation |
| ADR-008 | Abstract email/SMS providers and fail closed | real credentials/providers may be unavailable | production sending is disabled until explicitly approved/configured |
| ADR-009 | Use Docker Compose and Nginx HTTPS as deployment baseline | portable, inspectable project-scale operations | hosted monitoring/secrets remain operator integrations |
| ADR-010 | Use solo-adapted Scrum transparently | project is implemented by one developer | no fictional team members or ceremonies are claimed |
| ADR-011 | Gate releases on exact-commit CI and runtime evidence | green local tests alone cannot prove production readiness | `v1.0` remains blocked until item 770 passes |
| ADR-012 | Publish Mermaid source with SVG diagrams | diagrams stay editable and versionable | SVGs are regenerated and visually reviewed after changes |
