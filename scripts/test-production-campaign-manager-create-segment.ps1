param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$CampaignManagerCredential,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https" -or $BaseUrl.AbsolutePath -ne "/" -or
    $BaseUrl.Query -or $BaseUrl.Fragment) {
    throw "BaseUrl must be an HTTPS origin"
}

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$accessToken = $null
$segmentId = $null
$segmentDeleted = $false
$suffix = "$([DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss'))-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$syntheticName = "SMOKE-$suffix Campaign Manager Segment"

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

try {
    $password = $CampaignManagerCredential.GetNetworkCredential().Password
    $loginBody = @{
        email = $CampaignManagerCredential.UserName
        password = $password
    } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Method Post -Uri "$origin/api/auth/login" `
        -ContentType "application/json" -Body $loginBody -TimeoutSec $TimeoutSeconds
    if (-not $login.success -or $login.data.user.status -ne "ACTIVE") {
        throw "Campaign Manager authentication failed"
    }
    $ownerUserId = [string]$login.data.user.id
    $accessToken = [string]$login.data.tokens.accessToken
    $parts = $accessToken.Split('.')
    if ($parts.Count -ne 3 -or
        @((ConvertFrom-Base64UrlJson $parts[1]).roles) -notcontains "CAMPAIGN_MANAGER") {
        throw "Authenticated account does not have the CAMPAIGN_MANAGER role"
    }
    $headers = @{ Authorization = "Bearer $accessToken" }

    $segmentBody = @{
        name = $syntheticName
        description = "Synthetic production Campaign Manager segment creation verification"
        visibility = "PRIVATE"
        criteria = @()
    } | ConvertTo-Json -Compress
    $createResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/segments" `
        -Headers $headers -ContentType "application/json" -Body $segmentBody `
        -TimeoutSec $TimeoutSeconds -UseBasicParsing
    $created = $createResponse.Content | ConvertFrom-Json
    $parsedSegmentId = [Guid]::Empty
    if ($createResponse.StatusCode -ne 201 -or -not $created.success -or
        -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedSegmentId)) {
        throw "Segment creation did not return HTTP 201 with a valid UUID"
    }
    $segmentId = $parsedSegmentId.ToString()
    if ($created.data.name -ne $syntheticName -or $created.data.visibility -ne "PRIVATE" -or
        [string]$created.data.ownerUserId -ne $ownerUserId -or @($created.data.criteria).Count -ne 0) {
        throw "Created segment does not preserve owner, visibility, or criteria safety fields"
    }

    $persisted = Invoke-RestMethod -Method Get -Uri "$origin/api/segments/$segmentId" `
        -Headers $headers -TimeoutSec $TimeoutSeconds
    if (-not $persisted.success -or $persisted.data.id -ne $segmentId -or
        $persisted.data.name -ne $syntheticName) {
        throw "Created synthetic segment could not be read back"
    }

    $deleted = Invoke-RestMethod -Method Delete -Uri "$origin/api/segments/$segmentId" `
        -Headers $headers -TimeoutSec $TimeoutSeconds
    if (-not $deleted.success) {
        throw "Synthetic segment deletion cleanup failed"
    }
    $segmentDeleted = $true

    Write-Host "Production Campaign Manager create-segment verification passed."
    Write-Host "Authorization: active CAMPAIGN_MANAGER session"
    Write-Host "Creation: HTTP 201 with valid segment UUID"
    Write-Host "Ownership: segment owner matches authenticated user"
    Write-Host "Safety: private segment with no audience criteria"
    Write-Host "Cleanup: Campaign Manager deletion succeeded"
}
finally {
    if ($segmentId -and $accessToken -and -not $segmentDeleted) {
        try {
            Invoke-RestMethod -Method Delete -Uri "$origin/api/segments/$segmentId" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Write-Warning "Synthetic segment was deleted during failure cleanup"
        }
        catch { Write-Warning "Segment cleanup failed; delete the private smoke segment" }
    }
    if ($accessToken) {
        try {
            Invoke-RestMethod -Method Post -Uri "$origin/api/auth/logout" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
        }
        catch { Write-Warning "Logout failed; revoke the Campaign Manager smoke session" }
    }
    $password = $null
    $accessToken = $null
    $headers = $null
    $loginBody = $null
    $segmentBody = $null
    $syntheticName = $null
}
