param(
    [Parameter(Mandatory = $true)]
    [uri]$BaseUrl,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https") {
    throw "BaseUrl must use https"
}
if ($BaseUrl.AbsolutePath -ne "/" -or $BaseUrl.Query -or $BaseUrl.Fragment) {
    throw "BaseUrl must be an HTTPS origin without path, query, or fragment"
}
if ($TimeoutSeconds -lt 5) {
    throw "TimeoutSeconds must be at least 5"
}

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$httpOrigin = "http://$($BaseUrl.Authority)"
$tempDirectory =
    Join-Path ([System.IO.Path]::GetTempPath()) "bwc-https-$([Guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $tempDirectory | Out-Null

function Invoke-Curl {
    param([string[]]$Arguments)

    $output = & curl.exe --silent --show-error --max-time $TimeoutSeconds @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "HTTPS verification request failed (curl exit $LASTEXITCODE)"
    }
    return $output
}

try {
    $headersPath = Join-Path $tempDirectory "https-headers.txt"
    $bodyPath = Join-Path $tempDirectory "https-body.html"
    $status =
        Invoke-Curl @(
            "--dump-header", $headersPath,
            "--output", $bodyPath,
            "--write-out", "%{http_code}",
            "$origin/"
        )
    if (($status | Select-Object -Last 1).Trim() -ne "200") {
        throw "HTTPS application root did not return HTTP 200"
    }

    $body = Get-Content -LiteralPath $bodyPath -Raw
    if ($body -notmatch '<div\s+id=["'']root["'']') {
        throw "HTTPS response does not contain the React application root"
    }

    $headers = Get-Content -LiteralPath $headersPath -Raw
    if ($headers -notmatch '(?im)^strict-transport-security:\s*max-age=') {
        throw "HTTPS response is missing Strict-Transport-Security"
    }

    $redirectHeaders = Invoke-Curl @("--head", "$httpOrigin/")
    $redirectText = $redirectHeaders -join "`n"
    if ($redirectText -notmatch '(?im)^HTTP/\S+\s+30[178]\b') {
        throw "HTTP application root does not return a permanent HTTPS redirect"
    }
    $escapedAuthority = [regex]::Escape($BaseUrl.Authority)
    if ($redirectText -notmatch "(?im)^location:\s*https://$escapedAuthority/") {
        throw "HTTP redirect does not target the expected HTTPS origin"
    }

    $readinessStatus =
        Invoke-Curl @(
            "--output", "NUL",
            "--write-out", "%{http_code}",
            "$origin/readyz"
        )
    if (($readinessStatus | Select-Object -Last 1).Trim() -ne "200") {
        throw "HTTPS readiness endpoint did not return HTTP 200"
    }

    Write-Host "Production HTTPS application verification passed."
    Write-Host "Origin: $origin"
    Write-Host "Application root: HTTP 200 with React root"
    Write-Host "Transport: trusted TLS, HSTS present, HTTP permanently redirects to HTTPS"
    Write-Host "Readiness: HTTPS /readyz returned HTTP 200"
}
finally {
    Remove-Item -LiteralPath $tempDirectory -Recurse -Force -ErrorAction SilentlyContinue
}
