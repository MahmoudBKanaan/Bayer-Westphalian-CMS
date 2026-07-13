param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$CampaignManagerCredential,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$ComplianceCleanupCredential,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https" -or $BaseUrl.AbsolutePath -ne "/" -or
    $BaseUrl.Query -or $BaseUrl.Fragment) { throw "BaseUrl must be an HTTPS origin" }

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$managerToken = $null
$complianceToken = $null
$campaignId = $null
$campaignStatus = $null
$suffix = "$([DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss'))-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$syntheticName = "SMOKE-$suffix Campaign Manager Campaign"

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

function New-RoleSession($Credential, [string]$RequiredRole) {
    $password = $Credential.GetNetworkCredential().Password
    try {
        $body = @{ email = $Credential.UserName; password = $password } | ConvertTo-Json -Compress
        $login = Invoke-RestMethod -Method Post -Uri "$origin/api/auth/login" `
            -ContentType "application/json" -Body $body -TimeoutSec $TimeoutSeconds
        if (-not $login.success -or $login.data.user.status -ne "ACTIVE") {
            throw "Role session authentication failed"
        }
        $token = [string]$login.data.tokens.accessToken
        $parts = $token.Split('.')
        if ($parts.Count -ne 3 -or
            @((ConvertFrom-Base64UrlJson $parts[1]).roles) -notcontains $RequiredRole) {
            throw "Authenticated account does not have required role"
        }
        return [pscustomobject]@{ Token = $token; UserId = [string]$login.data.user.id }
    }
    finally { $password = $null; $body = $null; $login = $null }
}

try {
    $managerSession = New-RoleSession $CampaignManagerCredential "CAMPAIGN_MANAGER"
    $managerToken = $managerSession.Token
    $managerHeaders = @{ Authorization = "Bearer $managerToken" }
    $campaignBody = @{
        name = $syntheticName
        objective = "Synthetic production campaign creation verification; never launch"
        channel = "EMAIL"
        messageSubject = "Synthetic smoke draft"
        messageBody = "Synthetic smoke draft. No recipients, no sending."
        productIds = @()
    } | ConvertTo-Json -Compress

    $createResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/campaigns" `
        -Headers $managerHeaders -ContentType "application/json" -Body $campaignBody `
        -TimeoutSec $TimeoutSeconds -UseBasicParsing
    $created = $createResponse.Content | ConvertFrom-Json
    $parsedCampaignId = [Guid]::Empty
    if ($createResponse.StatusCode -ne 201 -or -not $created.success -or
        -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedCampaignId)) {
        throw "Campaign creation did not return HTTP 201 with a valid UUID"
    }
    $campaignId = $parsedCampaignId.ToString()
    $campaignStatus = [string]$created.data.status
    if ($created.data.name -ne $syntheticName -or $campaignStatus -ne "DRAFT" -or
        [string]$created.data.ownerUserId -ne $managerSession.UserId -or
        $created.data.segmentId -or @($created.data.productIds).Count -ne 0) {
        throw "Created campaign does not preserve owner, DRAFT state, or targetless safety fields"
    }

    $persisted = Invoke-RestMethod -Method Get -Uri "$origin/api/campaigns/$campaignId" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (-not $persisted.success -or $persisted.data.id -ne $campaignId -or
        $persisted.data.status -ne "DRAFT") {
        throw "Created synthetic campaign could not be read back as DRAFT"
    }

    $submitted = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/submit" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (-not $submitted.success -or $submitted.data.status -ne "SUBMITTED") {
        throw "Synthetic campaign cleanup submission failed"
    }
    $campaignStatus = "SUBMITTED"

    $complianceSession = New-RoleSession $ComplianceCleanupCredential "COMPLIANCE_OFFICER"
    $complianceToken = $complianceSession.Token
    $rejectBody = @{
        rejectionReason = "Synthetic smoke campaign cleanup; never approve or launch"
        complianceReviewNotes = "Item 751 controlled cleanup"
    } | ConvertTo-Json -Compress
    $rejected = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/reject" `
        -Headers @{ Authorization = "Bearer $complianceToken" } -ContentType "application/json" `
        -Body $rejectBody -TimeoutSec $TimeoutSeconds
    if (-not $rejected.success -or $rejected.data.status -ne "REJECTED") {
        throw "Compliance cleanup rejection failed"
    }
    $campaignStatus = "REJECTED"

    $archived = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/archive" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (-not $archived.success -or $archived.data.status -ne "ARCHIVED") {
        throw "Campaign Manager archive cleanup failed"
    }
    $campaignStatus = "ARCHIVED"

    Write-Host "Production Campaign Manager create-campaign verification passed."
    Write-Host "Authorization: active CAMPAIGN_MANAGER session"
    Write-Host "Creation: HTTP 201 with valid campaign UUID and DRAFT status"
    Write-Host "Ownership: campaign owner matches authenticated manager"
    Write-Host "Safety: no segment, products, schedule, recipients, approval, launch, or sending"
    Write-Host "Cleanup: human Compliance rejection followed by Campaign Manager archive"
}
finally {
    if ($campaignId -and $campaignStatus -eq "REJECTED" -and $managerToken) {
        try {
            Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/archive" `
                -Headers @{ Authorization = "Bearer $managerToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Write-Warning "Synthetic campaign was archived during failure cleanup"
        }
        catch { Write-Warning "Campaign archive cleanup failed; keep it non-approved and archive through the approved workflow" }
    }
    if ($campaignId -and $campaignStatus -in @("DRAFT", "SUBMITTED")) {
        Write-Warning "Synthetic campaign remains non-approved; complete Compliance rejection and archive cleanup"
    }
    foreach ($token in @($managerToken, $complianceToken)) {
        if ($token) {
            try {
                Invoke-RestMethod -Method Post -Uri "$origin/api/auth/logout" `
                    -Headers @{ Authorization = "Bearer $token" } -TimeoutSec $TimeoutSeconds |
                    Out-Null
            }
            catch { Write-Warning "Logout failed; revoke the affected smoke session" }
        }
    }
    $managerToken = $null
    $complianceToken = $null
    $managerHeaders = $null
    $campaignBody = $null
    $rejectBody = $null
    $syntheticName = $null
}
