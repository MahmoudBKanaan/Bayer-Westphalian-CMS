# Release Tagging Process

**Sprint 17 item 696** — Add release tagging process.

The Bayer-Westphalian Campaign Management Platform records **release versions** as Git tags on the
releasable **`main`** branch after CI is green. Tags map to the Knowledge Base release plan
(`v0.1` … `v1.0`). Related: [CI/CD](ci-cd.md), [branch protection](branch-protection.md) (**695**),
release artifacts (**691**), production gate (**714**).

Item **711** may expand this guide further; this document is the process baseline for **696**.

## Scope and solo-project honesty (KB)

| Practice | Solo interpretation |
| --- | --- |
| Tag only from green `main` | Do not tag `dev` or a red CI commit as a release |
| Annotated tags | Prefer annotated tags with a short release message |
| Version scheme | Follow KB **v0.x / v1.0** labels (not free-form marketing names) |
| Evidence | Keep tag name, commit SHA, and CI run link for the university report |

## Version scheme (KB release plan)

| Tag | Goal (KB) | When to apply |
| --- | --- | --- |
| `v0.1` | Project foundation | Repo, architecture, Docker, base apps ready |
| `v0.2` | Secure access | Auth, users, roles, protected routes |
| `v0.3` | CRM and compliance base | Customers, beneficiaries, consent, opt-outs |
| `v0.4` | Products and segmentation | Products, ownership, payments, segments |
| `v0.5` | Campaign lifecycle | Builder, approval, recipient preview |
| `v0.6` | Communication and reminders | Contact history, providers, reminders |
| `v0.7` | Analytics and AI | Dashboards, reports, AI-assisted features |
| `v0.8` | Audit and hardening | Audit logs, security hardening, access tests |
| `v0.9` | Production candidate | Full tests, CI/CD, deployment prep, docs |
| `v1.0` | Production-ready MVP | Deployed, documented, tested, demo-ready |

### Tag naming rules

1. Use a leading **`v`** and the KB version: `v0.9`, `v1.0`.
2. Prefer **exact** KB milestones for course releases; optional patch tags later as `v0.9.1` only if
   a fix release is needed after a milestone tag (document the reason in the tag message).
3. Tags are **immutable** release markers: do not move or force-update an existing published tag.
4. Tag the **merge commit on `main`** that represents the release (not an intermediate feature
   branch tip).

## Preconditions (before tagging)

1. Working tree for the release commit is on **`main`** and matches the intended release content.
2. **CI is green** for that commit (workflow **CI** / `ci.yml`) — items **693** / **694** / **714**.
3. Branch protection for `main` is applied when available — [branch-protection.md](branch-protection.md).
4. No intentional red or WIP commits in the tag history for that release.
5. Release artifacts from CI (**691**: `bwc-backend-jar`, `bwc-frontend-dist`) are available for the
   run when packaging evidence is required.
6. Secrets remain outside Git — [secrets.md](secrets.md).

## Process (Git CLI)

From a clean checkout of `main` at the release commit:

```powershell
# 1. Confirm branch and status
git checkout main
git pull origin main
git status

# 2. Confirm the commit you will tag (SHA)
git log -1 --oneline

# 3. Create an annotated tag (example: production candidate)
git tag -a v0.9 -m "Release v0.9: Production candidate (CI/CD, tests, deployment prep)"

# 4. Push the tag to origin (does not force-update)
git push origin v0.9

# 5. Optional: list tags
git tag -l "v*"
```

GitHub UI alternative: **Releases → Draft a new release → Choose a tag → Create new tag** on
`main`, paste the same version and notes, publish. Prefer **annotated** tags (GitHub Releases
creates them).

### Annotated tag message template

```text
Release vX.Y: <KB goal short name>

- Milestone: <v0.x / v1.0 KB description>
- Branch: main @ <short-sha>
- CI: green (workflow CI)
- Notes: <optional>
```

## What not to do

| Anti-pattern | Why |
| --- | --- |
| Tag a red CI commit | Violates pass-on-green / item **714** |
| Tag only on `dev` as the official release | `main` is the releasable line |
| Lightweight tag without message for course milestones | Annotated tags carry release intent |
| `git tag -f` / force-push tags after share | Breaks immutable release references |
| Put secrets or env values in the tag message | Secrets stay in secret managers only |
| Reuse an existing tag name for a different commit | Confuses consumers and evidence |

## GitHub Release notes (optional but recommended)

After pushing the tag:

1. Open **Releases** for the repository.
2. Select the tag (`v0.9`, …).
3. Summarize changes vs the previous tag (modules, CI, docs).
4. Attach or link CI artifacts if needed for demo (download from Actions run **691**).
5. Keep notes free of secrets and production credentials.

## Rollback note

Tags mark history; they do not deploy by themselves. To stop using a bad release:

1. Do **not** delete a published course milestone tag unless the tag was created in error and never
   shared; prefer a **new** fix tag (e.g. `v0.9.1`) on a corrected `main` commit.
2. Operational rollback of a deployment is a Sprint 18 concern (rollback plan / prod Compose).

## Checklist (operator)

1. Merge release content to `main` with green CI.
2. Choose the next unused KB version tag (`v0.1` … `v1.0` or documented patch).
3. Create annotated tag on the release commit.
4. Push tag to `origin`.
5. Optionally publish a GitHub Release with notes.
6. Record tag name + SHA + CI run URL for Sprint 17 evidence.
7. Proceed to deploy only when ops readiness allows (Sprint 18 / item **716+**).

## Related items

| Item | Topic |
| --- | --- |
| **691** | Release artifact generation (JAR + dist) |
| **692** | CI badge on README |
| **693–694** | Fail-on-red / pass-on-green |
| **695** | Branch protection recommendation |
| **696** | This release tagging process |
| **697** | [Deployment workflow placeholder](ci-cd.md#deployment-workflow-placeholder-item-697) |
| **711** | Release tagging guide expansion (docs) |
| **714** | Main not releasable unless CI passes |

Automated documentation evidence: `ReleaseTaggingProcessDocumentationTests` (backend),
`releaseTaggingProcess.ts` (frontend catalog).
