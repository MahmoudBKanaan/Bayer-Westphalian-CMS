# Final Submission Package Verification

Checked on 2026-07-19 against repository commit `0277a591499ec1ebd90df2083c05cf6f8a51bfe6`.
Re-run the checks after any later commit.

| Check | Status | Evidence / action |
| --- | --- | --- |
| Final report exists and has no unsupported release claim | PASS | `final-university-project-report.md` |
| Implementation/testing/Scrum/deployment/release/diagram appendices exist | PASS | `appendices/README.md` |
| Eight required UML subjects are in `UMLs` | PASS | `docs/report/appendices/diagram-evidence.md` |
| Setup, API, user, admin and operations documentation exists | PASS | `README.md`, `docs/README.md` |
| Another developer/evaluator/operator can understand, run, test, deploy, review and maintain | PASS | `docs/documentation-readiness.md`, maintainer guide |
| Release candidate test evidence exists | PASS | `docs/testing/test-execution-report.md` |
| Synthetic final demo data/script exists | PASS | `docs/demo` |
| Secrets and real customer data excluded | REVIEW | inspect staged files before submission |
| Temporary/cache/build outputs excluded | REVIEW | package from tracked source, not raw workspace |
| Exact final commit CI is green | BLOCKED | current work is on `dev` with uncommitted changes |
| Item 770 production gate passes | BLOCKED | runtime production evidence remains incomplete |
| Final `v1.0` tag exists on approved `main` SHA | BLOCKED | correctly withheld until the preceding gates pass |

## Packaging Procedure

1. Commit reviewed source and documentation on `dev`; open and review a pull request to `main`.
2. Require successful CI on the exact final `main` SHA.
3. Exclude `.env.production`, secrets, logs, caches, `node_modules`, build output, temporary files,
   database dumps, consent evidence and personal data from the submitted archive.
4. Include source, migrations, tests, `UMLs`, report, appendices, guides and safe configuration examples.
5. Open every SVG and key Markdown/PDF artifact from the final archive before submission.
6. Record archive name, SHA-256 checksum, commit SHA and submission timestamp.
7. Create `v1.0` only after the separate production gate passes; university submission does not
   override the release rule.
8. Complete `docs/handover/final-delivery-package-checklist.md` against the final archive.

## Final Record

| Field | Value |
| --- | --- |
| Archive | `<final-archive-name>` |
| SHA-256 | `<archive-checksum>` |
| Git commit | `<final-40-character-sha>` |
| CI run | `<successful-run-url>` |
| Submitted at (UTC) | `<timestamp>` |
| Submitted by | `<name>` |
