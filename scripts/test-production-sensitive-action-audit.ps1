param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$AdminCredential,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https" -or $BaseUrl.AbsolutePath -ne "/" -or
    $BaseUrl.Query -or $BaseUrl.Fragment) { throw "BaseUrl must be an HTTPS origin" }

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$accessToken = $null
$userId = $null
$userDisabled = $false
$suffix = "$([DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss'))-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$syntheticEmail = "smoke-audit-$suffix@example.invalid"
$syntheticPassword = "Smoke-$([Guid]::NewGuid().ToString('N'))-Aa1!"

function ConvertFrom-Base64UrlJson([string]$Value) {
    $normalized = $Value.Replace('-', '+').Replace('_', '/')
    switch ($normalized.Length % 4) {
        2 { $normalized += '==' }
        3 { $normalized += '=' }
        1 { throw "Access token payload has invalid base64url length" }
    }
    return [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($normalized)) |
        ConvertFrom-Json
}

function Get-UserAuditHistory {
    param([string]$Id, [hashtable]$Headers)
    return Invoke-RestMethod -Method Get -Uri "$origin/api/audit-logs/entities/users/$Id" `
        -Headers $Headers -TimeoutSec $TimeoutSeconds
}

try {
    $password = $AdminCredential.GetNetworkCredential().Password
    $loginBody = @{ email = $AdminCredential.UserName; password = $password } |
        ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Method Post -Uri "$origin/api/auth/login" `
        -ContentType "application/json" -Body $loginBody -TimeoutSec $TimeoutSeconds
    if (-not $login.success -or $login.data.user.status -ne "ACTIVE") {
        throw "Admin authentication failed"
    }
    $adminUserId = [string]$login.data.user.id
    $accessToken = [string]$login.data.tokens.accessToken
    $parts = $accessToken.Split('.')
    if ($parts.Count -ne 3 -or
        @((ConvertFrom-Base64UrlJson $parts[1]).roles) -notcontains "ADMIN") {
        throw "Authenticated account does not have the ADMIN role"
    }
    $headers = @{ Authorization = "Bearer $accessToken" }

    $createBody = @{
        email = $syntheticEmail
        password = $syntheticPassword
        fullName = "SMOKE-$suffix Audit Sensitive Action"
    } | ConvertTo-Json -Compress
    $createResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/users" -Headers $headers `
        -ContentType "application/json" -Body $createBody -TimeoutSec $TimeoutSeconds `
        -UseBasicParsing
    $created = $createResponse.Content | ConvertFrom-Json
    $parsedUserId = [Guid]::Empty
    if ($createResponse.StatusCode -ne 201 -or
        -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedUserId)) {
        throw "Sensitive user-creation action failed"
    }
    $userId = $parsedUserId.ToString()

    $creationHistory = Get-UserAuditHistory -Id $userId -Headers $headers
    $createEvents = @($creationHistory.data) | Where-Object {
        $_.action -eq "CREATE" -and $_.entityType -eq "users" -and
        [string]$_.entityId -eq $userId -and [string]$_.actorUserId -eq $adminUserId
    }
    if (-not $creationHistory.success -or $createEvents.Count -ne 1) {
        throw "Expected immutable CREATE audit event was not found"
    }
    $creationAuditJson = $createEvents[0] | ConvertTo-Json -Depth 10 -Compress
    if ($creationAuditJson -match '(?i)password|passwordHash|rawPassword' -or
        $creationAuditJson.Contains($syntheticPassword)) {
        throw "Audit payload contains prohibited password material"
    }

    $disabled = Invoke-RestMethod -Method Patch -Uri "$origin/api/users/$userId/disable" `
        -Headers $headers -TimeoutSec $TimeoutSeconds
    if (-not $disabled.success -or $disabled.data.status -ne "DISABLED") {
        throw "Sensitive user-disable action failed"
    }
    $userDisabled = $true

    $disableHistory = Get-UserAuditHistory -Id $userId -Headers $headers
    $disableEvents = @($disableHistory.data) | Where-Object {
        $_.action -eq "DISABLE_USER" -and [string]$_.actorUserId -eq $adminUserId
    }
    if (-not $disableHistory.success -or $disableEvents.Count -ne 1) {
        throw "Expected immutable DISABLE_USER audit event was not found"
    }

    Write-Host "Production sensitive-action audit verification passed."
    Write-Host "Sensitive action: Admin user creation recorded once"
    Write-Host "Audit identity: actor, action, entity type, and entity UUID match"
    Write-Host "Audit privacy: no password material present"
    Write-Host "Cleanup: user disabled and DISABLE_USER recorded once"
}
finally {
    if ($userId -and $accessToken -and -not $userDisabled) {
        try {
            Invoke-RestMethod -Method Patch -Uri "$origin/api/users/$userId/disable" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Write-Warning "Synthetic user was disabled during failure cleanup"
        }
        catch { Write-Warning "User cleanup failed; disable the synthetic audit user" }
    }
    if ($accessToken) {
        try {
            Invoke-RestMethod -Method Post -Uri "$origin/api/auth/logout" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
        }
        catch { Write-Warning "Logout failed; revoke the Admin smoke session" }
    }
    $password = $null
    $syntheticPassword = $null
    $accessToken = $null
    $headers = $null
    $loginBody = $null
    $createBody = $null
    $creationAuditJson = $null
}
