param(
    [switch]$PlanOnly
)

$ErrorActionPreference = "Stop"

# Item 707: Pipeline passes on clean main branch.
# Script: verify-pipeline-passes-on-clean-main.ps1
# CI parity commands include: mvn -B -DskipTests package, mvn -B test,
# npm ci, npm run lint, npm test, npm run build, Docker image builds, and Compose validation.
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path

function Assert-CleanMainBranch {
    Push-Location -LiteralPath $repoRoot
    try {
        $branch = (& git branch --show-current).Trim()
        if ($branch -ne "main") {
            throw "Item 707 requires branch 'main'; current branch is '$branch'."
        }

        $status = & git status --porcelain
        if ($status.Count -gt 0) {
            throw "Item 707 requires a clean worktree. Commit, stash, or revert pending changes first."
        }
    }
    finally {
        Pop-Location
    }
}

function Invoke-CiParityStep {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [string]$Command,
        [string[]]$Arguments
    )

    $fullWorkingDirectory = Join-Path $repoRoot $WorkingDirectory
    $displayCommand = "$Command $($Arguments -join ' ')".Trim()

    if ($PlanOnly) {
        Write-Host "[plan] $Name :: cd $WorkingDirectory; $displayCommand"
        return
    }

    Write-Host "[run] $Name"
    Push-Location -LiteralPath $fullWorkingDirectory
    try {
        & $Command @Arguments
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne 0) {
            throw "$Name failed with exit code $exitCode."
        }
    }
    finally {
        Pop-Location
    }
}

function Assert-RequiredFile {
    param(
        [string]$RelativePath
    )

    $path = Join-Path $repoRoot $RelativePath
    if ($PlanOnly) {
        Write-Host "[plan] Assert file exists :: $RelativePath"
        return
    }
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required CI parity file is missing: $RelativePath"
    }
}

if (-not $PlanOnly) {
    Assert-CleanMainBranch
}

Assert-RequiredFile "backend/src/main/resources/application-prod.yml"
Assert-RequiredFile "backend/src/main/java/com/bayerwestphalian/campaign/common/config/ProductionEnvironmentPostProcessor.java"
Assert-RequiredFile "backend/src/main/java/com/bayerwestphalian/campaign/common/config/SecretPresenceValidator.java"

Invoke-CiParityStep "Backend build" "backend" "mvn.cmd" @("-B", "-DskipTests", "package")
Invoke-CiParityStep "Backend tests" "backend" "mvn.cmd" @("-B", "test")
Invoke-CiParityStep "Backend integration tests" "backend" "mvn.cmd" @("-B", "test", "-Dtest=*IntegrationTests")
Invoke-CiParityStep "Frontend install" "frontend" "npm.cmd" @("ci")
Invoke-CiParityStep "Frontend lint" "frontend" "npm.cmd" @("run", "lint")
Invoke-CiParityStep "Frontend tests" "frontend" "npm.cmd" @("test")
Invoke-CiParityStep "Frontend build" "frontend" "npm.cmd" @("run", "build")
Invoke-CiParityStep "Docker backend image" "." "docker" @("build", "-t", "bwc-backend:ci", "-f", "backend/Dockerfile", "backend")
Invoke-CiParityStep "Docker frontend image" "." "docker" @("build", "-t", "bwc-frontend:ci", "-f", "frontend/Dockerfile", "frontend")
Invoke-CiParityStep "Docker Compose validation" "." "powershell" @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", ".\scripts\test-docker-compose-config.ps1")

if ($PlanOnly) {
    Write-Host "Item 707 clean-main CI parity plan listed successfully; no runtime pass evidence was recorded."
}
else {
    Write-Host "Item 707 clean-main CI parity completed successfully."
}
