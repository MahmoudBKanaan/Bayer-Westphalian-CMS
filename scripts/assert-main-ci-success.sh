#!/usr/bin/env bash
set -euo pipefail

# Sprint 17 item 714: main is releasable only when CI succeeded for the exact commit.
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${GITHUB_SHA:?GITHUB_SHA is required}"
: "${GITHUB_TOKEN:?GITHUB_TOKEN is required}"

release_branch="${RELEASE_BRANCH:-${GITHUB_REF_NAME:-}}"
if [[ "$release_branch" != "main" ]]; then
  echo "Release gate rejected branch '$release_branch': only main can be releasable."
  exit 1
fi

api_url="https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/workflows/ci.yml/runs?branch=main&head_sha=${GITHUB_SHA}&event=push&per_page=10"
response=$(curl --fail-with-body --silent --show-error \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "$api_url")

successful_run_url=$(jq -r \
  '[.workflow_runs[] | select(.name == "CI" and .status == "completed" and .conclusion == "success" and .head_sha == env.GITHUB_SHA)] | first | .html_url // empty' \
  <<<"$response")

if [[ -z "$successful_run_url" ]]; then
  echo "Release gate rejected ${GITHUB_SHA}: no completed successful CI push run exists for this exact main commit."
  exit 1
fi

echo "Release gate passed for main commit ${GITHUB_SHA}."
echo "CI evidence: ${successful_run_url}"
