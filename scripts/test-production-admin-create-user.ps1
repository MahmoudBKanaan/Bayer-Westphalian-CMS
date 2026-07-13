param(
    [Parameter(Mandatory = $true)]
    [uri]$BaseUrl,
    [Parameter(Mandatory = $true)]
    [System.Management.Automation.PSCredential]$AdminCredential,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https") {
    throw "BaseUrl must use https"
}
if ($BaseUrl.AbsolutePath -ne "/" -or $BaseUrl.Query -or $BaseUrl.Fragment) {
    throw "BaseUrl must be an HTTPS origin without path, query, or fragment"
}

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$adminPassword = $null
$accessToken = $null
$createdUserId = $null
$cleanupCompleted = $false
$utcStamp = [DateTimeOffset]::UtcNow.ToString("yyyyMMddHHmmss")
$suffix = "$utcStamp-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$syntheticEmail = "smoke-admin-create-$suffix@example.invalid"
$syntheticPassword = "Smoke-$([Guid]::NewGuid().ToString('N'))-Aa1!"
$syntheticName = "SMOKE-$suffix Admin User Creation"

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
    $adminPassword = $AdminCredential.GetNetworkCredential().Password
    $loginBody =
        @{ email = $AdminCredential.UserName; password = $adminPassword } | ConvertTo-Json -Compress
    $login =
        Invoke-RestMethod -Method Post -Uri "$origin/api/auth/login" -ContentType "application/json" `
            -Body $loginBody -TimeoutSec $TimeoutSeconds
    if (-not $login.success -or $login.data.user.status -ne "ACTIVE") {
        throw "Admin authentication failed"
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
    $headers = @{ Authorization = "Bearer $accessToken" }

    $createBody =
        @{ email = $syntheticEmail; password = $syntheticPassword; fullName = $syntheticName } |
        ConvertTo-Json -Compress
    $createResponse =
        Invoke-WebRequest -Method Post -Uri "$origin/api/users" -Headers $headers `
            -ContentType "application/json" -Body $createBody -TimeoutSec $TimeoutSeconds `
            -UseBasicParsing
    if ($createResponse.StatusCode -ne 201) {
        throw "User creation did not return HTTP 201"
    }
    $created = $createResponse.Content | ConvertFrom-Json
    $parsedUserId = [Guid]::Empty
    if (-not $created.success -or -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedUserId)) {
        throw "Created user does not have a valid UUID"
    }
    $createdUserId = $parsedUserId.ToString()
    if ($created.data.email -ne $syntheticEmail -or $created.data.status -ne "ACTIVE") {
        throw "Created user response does not match the synthetic request"
    }

    $persisted =
        Invoke-RestMethod -Method Get -Uri "$origin/api/users/$createdUserId" -Headers $headers `
            -TimeoutSec $TimeoutSeconds
    if (-not $persisted.success -or $persisted.data.email -ne $syntheticEmail) {
        throw "Created synthetic user could not be read back"
    }

    $disabled =
        Invoke-RestMethod -Method Patch -Uri "$origin/api/users/$createdUserId/disable" `
            -Headers $headers -TimeoutSec $TimeoutSeconds
    if (-not $disabled.success -or $disabled.data.status -ne "DISABLED") {
        throw "Synthetic user cleanup did not disable the account"
    }
    $cleanupCompleted = $true

    Write-Host "Production Admin create-user verification passed."
    Write-Host "Authorization: active ADMIN session"
    Write-Host "Creation: HTTP 201 with valid user UUID"
    Write-Host "Persistence: created user read back successfully"
    Write-Host "Cleanup: synthetic account disabled"
}
finally {
    if ($createdUserId -and $accessToken -and -not $cleanupCompleted) {
        try {
            Invoke-RestMethod -Method Patch -Uri "$origin/api/users/$createdUserId/disable" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Write-Warning "Synthetic user was disabled during failure cleanup"
        }
        catch {
            Write-Warning "Synthetic user cleanup failed; disable the recorded smoke account through the approved Admin workflow"
        }
    }
    if ($accessToken) {
        try {
            Invoke-RestMethod -Method Post -Uri "$origin/api/auth/logout" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
        }
        catch {
            Write-Warning "Logout failed; revoke the Admin smoke session"
        }
    }
    $adminPassword = $null
    $syntheticPassword = $null
    $accessToken = $null
    $headers = $null
    $loginBody = $null
    $createBody = $null
    $login = $null
    $claims = $null
    $utcStamp = $null
}
