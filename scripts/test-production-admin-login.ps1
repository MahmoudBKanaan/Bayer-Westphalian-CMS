param(
    [Parameter(Mandatory = $true)]
    [uri]$BaseUrl,
    [Parameter(Mandatory = $true)]
    [System.Management.Automation.PSCredential]$Credential,
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
$plainPassword = $null
$accessToken = $null
$authorization = $null
$loggedIn = $false

function ConvertFrom-Base64UrlJson {
    param([Parameter(Mandatory = $true)][string]$Value)

    $normalized = $Value.Replace('-', '+').Replace('_', '/')
    switch ($normalized.Length % 4) {
        2 { $normalized += '==' }
        3 { $normalized += '=' }
        1 { throw "Access token payload has invalid base64url length" }
    }
    $json = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($normalized))
    return $json | ConvertFrom-Json
}

try {
    $plainPassword = $Credential.GetNetworkCredential().Password
    $loginBody =
        @{ email = $Credential.UserName; password = $plainPassword } | ConvertTo-Json -Compress
    $login =
        Invoke-RestMethod `
            -Method Post `
            -Uri "$origin/api/auth/login" `
            -ContentType "application/json" `
            -Body $loginBody `
            -TimeoutSec $TimeoutSeconds

    if (-not $login.success -or -not $login.data.tokens.accessToken) {
        throw "Admin login did not return a successful authenticated session"
    }
    if ($login.data.user.status -ne "ACTIVE") {
        throw "Authenticated admin account is not ACTIVE"
    }

    $accessToken = [string]$login.data.tokens.accessToken
    $parts = $accessToken.Split('.')
    if ($parts.Count -ne 3) {
        throw "Access token is not a JWT"
    }
    $claims = ConvertFrom-Base64UrlJson -Value $parts[1]
    if (@($claims.roles) -notcontains "ADMIN") {
        throw "Authenticated account does not have the ADMIN role"
    }

    $authorization = "Bearer $accessToken"
    $me =
        Invoke-RestMethod `
            -Method Get `
            -Uri "$origin/api/auth/me" `
            -Headers @{ Authorization = $authorization } `
            -TimeoutSec $TimeoutSeconds
    if (-not $me.success -or $me.data.id -ne $login.data.user.id) {
        throw "Authenticated /api/auth/me session verification failed"
    }
    if ($me.data.status -ne "ACTIVE") {
        throw "Current admin session is not ACTIVE"
    }
    $loggedIn = $true

    Write-Host "Production admin login verification passed."
    Write-Host "Transport: trusted HTTPS"
    Write-Host "Identity: active authenticated user"
    Write-Host "Authorization: ADMIN role present"
    Write-Host "Session: /api/auth/me verified"
}
finally {
    if ($accessToken) {
        try {
            Invoke-RestMethod `
                -Method Post `
                -Uri "$origin/api/auth/logout" `
                -Headers @{ Authorization = "Bearer $accessToken" } `
                -TimeoutSec $TimeoutSeconds | Out-Null
        }
        catch {
            if ($loggedIn) {
                Write-Warning "Login passed but logout verification failed; revoke the test session"
            }
        }
    }
    $plainPassword = $null
    $accessToken = $null
    $authorization = $null
    $loginBody = $null
    $login = $null
    $me = $null
    $claims = $null
}
