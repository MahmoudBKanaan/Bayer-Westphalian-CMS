param(
    [Parameter(Mandatory = $true)]
    [string]$EvidenceFile,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$ExpectedCommit,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^v[0-9]+\.[0-9]+(?:\.[0-9]+)?$')]
    [string]$ExpectedTag
)

$ErrorActionPreference = "Stop"
$requiredGates = @(
    "smokeTests",
    "backups",
    "securityConfiguration",
    "environmentConfiguration",
    "providerConfigurationPolicy",
    "rollbackPlan",
    "criticalWorkflows"
)

if (-not (Test-Path -LiteralPath $EvidenceFile -PathType Leaf)) {
    throw "Production release gate rejected: evidence file does not exist"
}

try {
    $evidence = Get-Content -LiteralPath $EvidenceFile -Raw | ConvertFrom-Json
}
catch {
    throw "Production release gate rejected: evidence file is not valid JSON"
}

if ($evidence.schemaVersion -ne 1) {
    throw "Production release gate rejected: unsupported evidence schema version"
}
if ([string]$evidence.commitSha -ine $ExpectedCommit) {
    throw "Production release gate rejected: evidence commit does not match the release commit"
}
if ($evidence.releaseTag -cne $ExpectedTag) {
    throw "Production release gate rejected: evidence tag does not match the release tag"
}
if ([string]::IsNullOrWhiteSpace($evidence.environment) -or
    $evidence.environment -like "REPLACE_*") {
    throw "Production release gate rejected: approved environment is missing"
}
if ([string]::IsNullOrWhiteSpace($evidence.operator) -or
    [string]::IsNullOrWhiteSpace($evidence.approvedBy) -or
    $evidence.operator -like "REPLACE_*" -or $evidence.approvedBy -like "REPLACE_*") {
    throw "Production release gate rejected: operator and human approver are required"
}
if ($evidence.decision -cne "PASS") {
    throw "Production release gate rejected: overall decision is not PASS"
}

$evaluatedAt = [DateTimeOffset]::MinValue
if (-not [DateTimeOffset]::TryParse($evidence.evaluatedAtUtc, [ref]$evaluatedAt) -or
    $evaluatedAt.Offset -ne [TimeSpan]::Zero) {
    throw "Production release gate rejected: evaluatedAtUtc must be a valid UTC timestamp"
}

foreach ($gateName in $requiredGates) {
    $gateProperty = $evidence.gates.PSObject.Properties[$gateName]
    if ($null -eq $gateProperty) {
        throw "Production release gate rejected: required gate '$gateName' is missing"
    }
    $gate = $gateProperty.Value
    if ($gate.status -cne "PASS") {
        throw "Production release gate rejected: gate '$gateName' is not PASS"
    }
    if ([string]::IsNullOrWhiteSpace($gate.evidence) -or
        $gate.evidence -match '(?i)references?$' -or $gate.evidence -like "REPLACE_*") {
        throw "Production release gate rejected: gate '$gateName' lacks a concrete evidence reference"
    }
}

Write-Host "Production release gate passed."
Write-Host "Release: $ExpectedTag"
Write-Host "Commit: $ExpectedCommit"
Write-Host "Environment: $($evidence.environment)"
Write-Host "Validated gates: $($requiredGates -join ', ')"
Write-Host "Approval: operator and human approver recorded"
