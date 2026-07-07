$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$reviewDoc = Join-Path $projectRoot "docs\agile\sprint-1-review-notes.md"
$docsIndex = Join-Path $projectRoot "docs\README.md"

if (-not (Test-Path $reviewDoc)) {
    throw "Sprint 1 review notes were not found at $reviewDoc"
}

if (-not (Test-Path $docsIndex)) {
    throw "Documentation index was not found at $docsIndex"
}

$review = Get-Content $reviewDoc -Raw
$index = Get-Content $docsIndex -Raw

$requiredTerms = @(
    "Sprint 1",
    "Sprint Summary",
    "Completed Scope",
    "Demo Notes",
    "Test Evidence",
    "Definition of Done Check",
    "Risks and Issues",
    "Stakeholder Feedback",
    "Follow-Up Actions",
    "Review Outcome",
    "S1-001",
    "S1-009",
    "Frontend unit and integration tests",
    "Backend unit and integration tests",
    "Docker PostgreSQL integration test"
)

foreach ($term in $requiredTerms) {
    if (-not $review.Contains($term)) {
        throw "Sprint 1 review notes are missing required term: $term"
    }
}

if (-not $index.Contains("agile/sprint-1-review-notes.md")) {
    throw "docs/README.md does not link to the Sprint 1 review notes"
}

Write-Host "Sprint 1 review documentation test passed."
