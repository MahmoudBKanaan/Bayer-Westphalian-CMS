param(
    [string]$ApiDocsUrl = "http://localhost:8080/v3/api-docs",
    [string]$OutputFile = "docs/api/openapi.json"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $OutputFile))
$outputDirectory = Split-Path -Parent $outputPath
$temporaryPath = "$outputPath.partial"

if (-not $ApiDocsUrl.StartsWith("http://localhost:") -and
    -not $ApiDocsUrl.StartsWith("http://127.0.0.1:")) {
    throw "OpenAPI export accepts local endpoints only; export production documentation inside its approved boundary"
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
try {
    $response = Invoke-WebRequest -Uri $ApiDocsUrl -TimeoutSec 30 -UseBasicParsing
    if ($response.StatusCode -ne 200) {
        throw "OpenAPI endpoint returned HTTP $($response.StatusCode)"
    }

    $document = $response.Content | ConvertFrom-Json
    if ([string]$document.openapi -notmatch '^3\.') {
        throw "Exported document is not OpenAPI 3.x"
    }
    if ([string]::IsNullOrWhiteSpace($document.info.title) -or $null -eq $document.paths) {
        throw "Exported document is missing required info or paths"
    }

    $requiredPaths = @(
        "/api/auth/login",
        "/api/customers",
        "/api/products",
        "/api/segments",
        "/api/campaigns",
        "/api/analytics/dashboard",
        "/api/audit-logs"
    )
    foreach ($requiredPath in $requiredPaths) {
        if ($null -eq $document.paths.PSObject.Properties[$requiredPath]) {
            throw "Exported document is missing required path: $requiredPath"
        }
    }

    $json = $document | ConvertTo-Json -Depth 100
    [System.IO.File]::WriteAllText($temporaryPath, "$json`n", [System.Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temporaryPath -Destination $outputPath -Force

    Write-Host "OpenAPI export completed."
    Write-Host "Output: $outputPath"
    Write-Host "Version: $($document.openapi)"
    Write-Host "Paths: $(@($document.paths.PSObject.Properties).Count)"
}
finally {
    Remove-Item -LiteralPath $temporaryPath -Force -ErrorAction SilentlyContinue
}
