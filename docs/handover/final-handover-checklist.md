# Final Handover Checklist

## Product and Source

- [x] Scope, roles, architecture and current release status are described in `README.md`.
- [x] Backend, frontend, migrations, tests and safe environment examples are present.
- [x] Required UML diagrams are under `UMLs`.
- [x] Final university report and appendices are under `docs/report`.
- [ ] Final reviewed changes are merged to protected `main` with exact-commit green CI.

## Access and Security

- [x] Role and authorization model is documented.
- [x] Secret, CORS, HTTPS, error handling and production security procedures are documented.
- [ ] Production secrets are transferred through the approved secret manager, never this repository.
- [ ] Named operational, security/compliance and release approvers accept ownership.

## Data and Operations

- [x] Database migration, backup, restore, monitoring, incident and rollback procedures exist.
- [x] Admin bootstrap and provider policy are documented.
- [ ] Approved production environment and hostname are recorded.
- [ ] Fresh encrypted off-host backup and matching consent-evidence recovery point are verified.
- [ ] Restore rehearsal and full production smoke checklist pass for the exact release.
- [ ] Monitoring, paging, certificate and backup-failure alerts have named recipients.

## Release and Support

- [x] Draft `v1.0` notes and immutable tagging procedure exist.
- [x] Known limitations and external integration boundaries are explicit.
- [ ] Item 770 evidence manifest passes fail-closed validation.
- [ ] Independent human release approval is recorded.
- [ ] Annotated `v1.0` tag and GitHub release are created from approved `main`.
- [ ] Support owner, escalation route, maintenance window and review date are recorded.

## Handover Sign-Off

| Responsibility | Owner | Date (UTC) | Signature / reference |
| --- | --- | --- | --- |
| Application maintenance | `<owner>` | `<date>` | `<reference>` |
| Database and backup | `<owner>` | `<date>` | `<reference>` |
| Security and compliance | `<owner>` | `<date>` | `<reference>` |
| Infrastructure/monitoring | `<owner>` | `<date>` | `<reference>` |
| Product/business ownership | `<owner>` | `<date>` | `<reference>` |
| Release approval | `<approver>` | `<date>` | `<reference>` |

Unchecked items are genuine handover blockers and must not be converted to completed status without
the referenced operational evidence.
