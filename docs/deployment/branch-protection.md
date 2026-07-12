# Branch Protection Recommendation

**Sprint 17 item 695** — Add branch protection recommendation.

The Bayer-Westphalian Campaign Management Platform uses GitHub as the source of truth. This document
recommends **branch protection** (or modern **rulesets**) so that the releasable line cannot be
updated without CI and a deliberate merge. Settings live in the GitHub repository UI (or org
rulesets); they are **not** enforced by application code. Related: [CI/CD](ci-cd.md), fail-on-red
(**693**), pass-on-green (**694**), production gate item **714**.

## Scope and solo-project honesty (KB)

This is a **solo-adapted** Scrum project. Recommendations still apply:

| Practice | Solo interpretation |
| --- | --- |
| Protect `main` | Releasable branch; prefer merge via PR even when you are the only author |
| Required CI | Do not merge red pipelines; self-review still waits for green checks |
| Force push | Disallowed on `main` so history and CI evidence stay trustworthy |
| Admin bypass | Prefer **not** bypassing required checks on release commits |

`dev` may stay less strict for day-to-day work; **`main` is the protected releasable line**.

## Recommended rules for `main`

Apply under **Settings → Branches → Branch protection rules** (classic) or **Settings → Rules →
Rulesets** (preferred on current GitHub). Target branch: **`main`**.

| Setting | Recommended value | Rationale |
| --- | --- | --- |
| Require a pull request before merging | **On** (1 approval optional for solo; enable if multi-contributor) | Forces a merge commit / review surface |
| Require status checks to pass before merging | **On** | Aligns with fail-on-red (**693**) and item **714** |
| Status checks to require | All jobs from the **CI** workflow (see below) | Full build/test/package gate |
| Require branches to be up to date before merging | **On** when practical | Avoids merging stale green on an outdated base |
| Do not allow bypassing the above settings | **On** for non-admin automation; solo may keep emergency admin access documented | Prevents silent force-merge of red work |
| Restrict who can push to matching branches | No direct push to `main` (PR only) when using PR requirement | Keeps CI on `pull_request` as the primary gate |
| Allow force pushes | **Off** | Protects release history and audit trail |
| Allow deletions | **Off** | Prevents accidental branch delete |
| Require conversation resolution before merging | **On** if using PR review comments | Keeps open threads from being ignored |
| Require linear history | Optional | Prefer squash or rebase merges if enabled |

### Required status checks (CI job display names)

The workflow [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) is named **`CI`**. After
at least one successful run on a PR, select these **check names** (job `name:` fields) as required:

| Job id | Check name (require) |
| --- | --- |
| `backend-build` | Backend build |
| `backend-test` | Backend test |
| `backend-integration-test` | Backend integration test |
| `frontend-install` | Frontend install |
| `frontend-lint` | Frontend lint |
| `frontend-test` | Frontend test |
| `frontend-build` | Frontend build |
| `docker-backend` | Docker backend image |
| `docker-frontend` | Docker frontend image |
| `docker-compose-validate` | Docker Compose validation |
| `production-config-validate` | Production config validation |

If GitHub only lists a subset until each job has reported once, re-edit the rule after a full green
PR. Do **not** require checks that are not defined in `ci.yml`.

## Recommended rules for `dev` (optional)

| Setting | Recommendation |
| --- | --- |
| Branch protection | Optional lighter ruleset |
| Required CI | Prefer requiring the same **CI** checks before merge into `dev` when sharing work |
| Force push | Prefer **Off**; allow only if a temporary rewrite policy is documented |

CI still runs on `push` to `dev` and on PRs targeting `dev` ([ci-cd.md](ci-cd.md) triggers).

## How this interacts with the pipeline

1. Open a PR into `main` → GitHub Actions **CI** runs (**698**).
2. Fail-on-red (**693**): any red quality-gate job blocks merge when checks are required.
3. Pass-on-green (**694**): a clean tree can go green; only then should merge proceed.
4. After merge, push to `main` runs CI again (**699**); badge tracks `main` (**692**).
5. Main is not releasable unless CI passes (**714**).

## Setup checklist (operator)

1. Ensure [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) is on the default branch and
   has produced at least one green run so check names appear.
2. Open repository **Settings → Rules** (or **Branches**) for
   `MahmoudBKanaan/Bayer-Westphalian-CMS` (or your fork).
3. Create a ruleset / protection rule for **`main`** with the table above.
4. Add all **CI** job check names listed in this document.
5. Disable force push and branch deletion for `main`.
6. Merge a test PR only when checks are green; confirm merge is blocked when a check is red
   (optional dry-run; intentional red evidence is item **706**).
7. Record a screenshot for university evidence if required (Sprint 17 review).

## What this recommendation is not

- It is **not** a GitHub App or Terraform that applies rules automatically.
- It does **not** replace local testing or item **715** full suite runs.
- It does **not** store secrets; CI remains `permissions: contents: read` ([secrets.md](secrets.md)).

## Related items

| Item | Topic |
| --- | --- |
| **677–691** | CI workflow, jobs, artifacts |
| **692** | CI badge on README |
| **693** | Fail-on-red |
| **694** | Pass-on-green for clean `main` |
| **695** | This branch protection recommendation |
| **696** | [Release tagging process](release-tagging.md) |
| **714** | Main not releasable unless CI passes |

Automated documentation evidence: `BranchProtectionRecommendationDocumentationTests` (backend),
`branchProtectionRecommendation.ts` (frontend catalog).
