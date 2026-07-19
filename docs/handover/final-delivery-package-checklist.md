# Final Delivery Package Checklist

This checklist fulfills KB item **861**. It verifies the university/source handover package without
falsely approving the separate production `v1.0` release gate.

## 1. Source and Build Inputs

- [x] Root `README.md` identifies the project, stack, roles, status and entry points.
- [x] Backend source, `pom.xml`, configuration examples and Dockerfile are present.
- [x] Frontend source, `package.json`, lock file, configuration examples and Dockerfile are present.
- [x] PostgreSQL schema is reproducible through versioned Flyway migrations.
- [x] Local and production Compose definitions are present.
- [x] GitHub Actions workflows and verification scripts are present.
- [ ] Final package is generated from a reviewed, committed SHA rather than the dirty workspace.

## 2. Documentation

- [x] Complete final report and appendices are under `docs/report`.
- [x] Developer setup and troubleshooting guide is present.
- [x] Architecture, role, eligibility and security decisions are documented.
- [x] User and role-specific guides are present.
- [x] Administrator and operations manuals are present.
- [x] API guide and exported OpenAPI 3.1 JSON are present.
- [x] Test plan, maps and latest recorded execution report are present.
- [x] Production deployment, backup, restore, smoke, rollback and incident guides are present.
- [x] Demo dataset and timed demo script use synthetic data.
- [x] Maintainer guide and documentation-readiness map are present.

## 3. Design and Project Evidence

- [x] Eight required UML subjects are available as SVG under `UMLs`.
- [x] Release plan, backlog, story map, risk register and decision log are present.
- [x] Implementation, testing, Scrum, deployment, release and diagram appendices are present.
- [x] Final submission and handover checklists are present.
- [ ] Final screenshots or other binaries required by the evaluator are opened and visually checked.

## 4. Quality and Reproducibility

- [x] Test commands are documented for backend, frontend and E2E suites.
- [x] CI is configured to fail on lint, build, test and Docker verification errors.
- [x] Latest recorded release-candidate report contains zero failures.
- [ ] CI passes for the exact final submitted commit.
- [ ] A clean checkout completes the documented setup and verification commands.
- [ ] Generated OpenAPI remains synchronized with the final backend commit.

## 5. Security and Data Hygiene

- [x] Safe `.env` examples exist; real values are excluded by `.gitignore`.
- [x] Secrets, customer data, consent evidence and production dumps are prohibited from delivery.
- [x] Production security and provider fail-closed policies are documented.
- [ ] Review `git status`, tracked files and archive contents for secrets and personal data.
- [ ] Exclude `.env.production`, `node_modules`, build targets, caches, logs, temporary files,
  test reports containing tokens, database dumps and Office lock files.
- [ ] Run the institution-approved malware/archive scan before upload.

## 6. Delivery Archive Verification

Create the final archive only from the reviewed commit. After creation:

- [ ] Extract it into an empty directory.
- [ ] Confirm `README.md`, `backend`, `frontend`, `docs`, `UMLs`, migrations and workflows exist.
- [ ] Open the final report and every SVG diagram.
- [ ] Parse `docs/api/openapi.json` and confirm it is valid JSON/OpenAPI.
- [ ] Follow the setup guide far enough to confirm dependencies and commands resolve.
- [ ] Record archive checksum and exact Git SHA below.

| Delivery record | Value |
| --- | --- |
| Archive filename | `<pending>` |
| SHA-256 checksum | `<pending>` |
| Exact Git commit | `<pending>` |
| Branch | `<pending>` |
| Exact-commit CI URL/status | `<pending>` |
| Created at (UTC) | `<pending>` |
| Verified by | `<pending>` |

## 7. Production Release Separation

The delivery package may be submitted for evaluation while production release remains blocked.
Do not mark these items complete unless runtime evidence exists:

- [ ] Item 770 evidence manifest passes for the exact commit and environment.
- [ ] HTTPS deployment and all critical smoke workflows pass.
- [ ] Fresh backup, encrypted off-host copy and restore rehearsal pass.
- [ ] Provider policy, monitoring, rollback and ownership are approved.
- [ ] Independent human approver authorizes release.
- [ ] Annotated `v1.0` tag is created on approved clean `main`.

Unchecked production items are intentional blockers, not missing university documentation.
