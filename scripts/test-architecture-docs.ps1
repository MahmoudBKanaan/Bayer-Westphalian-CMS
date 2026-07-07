$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$architectureDoc = Join-Path $projectRoot "docs\architecture\initial-architecture.md"
$docsIndex = Join-Path $projectRoot "docs\README.md"

if (-not (Test-Path $architectureDoc)) {
    throw "Initial architecture document was not found at $architectureDoc"
}

if (-not (Test-Path $docsIndex)) {
    throw "Documentation index was not found at $docsIndex"
}

$architecture = Get-Content $architectureDoc -Raw
$index = Get-Content $docsIndex -Raw

$requiredTerms = @(
    "```mermaid",
    "React, TypeScript, Vite",
    "Spring Boot",
    "Spring Security",
    "PostgreSQL",
    "Flyway",
    "OpenAPI / Swagger",
    "Consent and Opt-Out",
    "Campaign",
    "Segment",
    "Schedule and Reminder",
    "Communication",
    "Analytics and Reports",
    "Audit",
    "AI-Assisted Recommendations",
    "Nginx or Caddy"
)

foreach ($term in $requiredTerms) {
    if (-not $architecture.Contains($term)) {
        throw "Initial architecture document is missing required term: $term"
    }
}

if (-not $index.Contains("architecture/initial-architecture.md")) {
    throw "docs/README.md does not link to the initial architecture document"
}

Write-Host "Architecture documentation test passed."
