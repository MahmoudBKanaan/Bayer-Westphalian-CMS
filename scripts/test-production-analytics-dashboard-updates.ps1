param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$CampaignManagerCredential,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$ComplianceCredential,
    [Parameter(Mandatory = $true)][switch]$ProviderSendingConfirmedDisabled,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https" -or $BaseUrl.AbsolutePath -ne "/" -or
    $BaseUrl.Query -or $BaseUrl.Fragment) { throw "BaseUrl must be an HTTPS origin" }
if (-not $ProviderSendingConfirmedDisabled.IsPresent) {
    throw "Provider sending must be confirmed disabled before analytics lifecycle verification"
}

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$managerToken = $null
$complianceToken = $null
$campaignId = $null
$campaignStatus = $null
$suffix = "$([DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss'))-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"

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
        $token = [string]$login.data.tokens.accessToken
        $parts = $token.Split('.')
        if (-not $login.success -or $login.data.user.status -ne "ACTIVE" -or $parts.Count -ne 3 -or
            @((ConvertFrom-Base64UrlJson $parts[1]).roles) -notcontains $RequiredRole) {
            throw "Required active role session could not be established"
        }
        return $token
    }
    finally { $password = $null; $body = $null; $login = $null }
}

try {
    $managerToken = New-RoleSession $CampaignManagerCredential "CAMPAIGN_MANAGER"
    $managerHeaders = @{ Authorization = "Bearer $managerToken" }
    $baselineDashboard = Invoke-RestMethod -Method Get -Uri "$origin/api/analytics/dashboard" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    $baselineExecutive = Invoke-RestMethod -Method Get -Uri "$origin/api/analytics/executive" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (-not $baselineDashboard.success -or -not $baselineExecutive.success) {
        throw "Analytics baseline could not be loaded"
    }

    $campaignBody = @{
        name = "SMOKE-$suffix Analytics Update"
        objective = "Synthetic zero-recipient analytics verification"
        channel = "EMAIL"
        messageSubject = "Never delivered"
        messageBody = "Analytics lifecycle verification only."
        productIds = @()
    } | ConvertTo-Json -Compress
    $create = Invoke-WebRequest -Method Post -Uri "$origin/api/campaigns" -Headers $managerHeaders `
        -ContentType "application/json" -Body $campaignBody -TimeoutSec $TimeoutSeconds `
        -UseBasicParsing
    $created = $create.Content | ConvertFrom-Json
    $parsedId = [Guid]::Empty
    if ($create.StatusCode -ne 201 -or
        -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedId)) {
        throw "Synthetic campaign setup failed"
    }
    $campaignId = $parsedId.ToString()
    $campaignStatus = "DRAFT"

    $afterCreate = Invoke-RestMethod -Method Get -Uri "$origin/api/analytics/dashboard" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ([long]$afterCreate.data.campaignTotal -ne [long]$baselineDashboard.data.campaignTotal + 1) {
        throw "Dashboard campaign total did not update after creation"
    }

    $submitted = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/submit" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ($submitted.data.status -ne "SUBMITTED") { throw "Campaign submission failed" }
    $campaignStatus = "SUBMITTED"
    $complianceToken = New-RoleSession $ComplianceCredential "COMPLIANCE_OFFICER"
    $approvalBody = @{ complianceReviewNotes = "Item 755 zero-recipient analytics approval" } |
        ConvertTo-Json -Compress
    $approved = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/approve" `
        -Headers @{ Authorization = "Bearer $complianceToken" } -ContentType "application/json" `
        -Body $approvalBody -TimeoutSec $TimeoutSeconds
    if ($approved.data.status -ne "APPROVED") { throw "Campaign approval failed" }
    $campaignStatus = "APPROVED"

    $eligible = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/campaigns/$campaignId/recipients/eligible" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    $excluded = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/campaigns/$campaignId/recipients/excluded" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (@($eligible.data).Count -ne 0 -or @($excluded.data).Count -ne 0) {
        throw "Analytics launch blocked because recipient rows exist"
    }

    $launched = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/launch" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ($launched.data.status -ne "ACTIVE") { throw "Campaign launch failed" }
    $campaignStatus = "ACTIVE"
    $activeDashboard = Invoke-RestMethod -Method Get -Uri "$origin/api/analytics/dashboard" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ([long]$activeDashboard.data.activeCampaigns -ne
            [long]$baselineDashboard.data.activeCampaigns + 1 -or
        [long]$activeDashboard.data.messagesSent -ne [long]$baselineDashboard.data.messagesSent -or
        [long]$activeDashboard.data.audienceSize -ne [long]$baselineDashboard.data.audienceSize) {
        throw "Dashboard active/sent/audience KPIs did not update safely after launch"
    }

    $detail = Invoke-RestMethod -Method Get -Uri "$origin/api/analytics/campaigns/$campaignId" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ($detail.data.status -ne "ACTIVE" -or [long]$detail.data.metrics.audienceSize -ne 0 -or
        [long]$detail.data.metrics.sentCount -ne 0) {
        throw "Campaign analytics detail did not show ACTIVE zero-recipient metrics"
    }

    $completed = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/complete" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ($completed.data.status -ne "COMPLETED") { throw "Campaign completion failed" }
    $campaignStatus = "COMPLETED"
    $completedDashboard = Invoke-RestMethod -Method Get -Uri "$origin/api/analytics/dashboard" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    $completedExecutive = Invoke-RestMethod -Method Get -Uri "$origin/api/analytics/executive" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ([long]$completedDashboard.data.activeCampaigns -ne
            [long]$baselineDashboard.data.activeCampaigns -or
        [long]$completedExecutive.data.completedCampaigns -ne
            [long]$baselineExecutive.data.completedCampaigns + 1) {
        throw "Dashboard did not update active/completed KPIs after completion"
    }

    $archived = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/archive" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ($archived.data.status -ne "ARCHIVED") { throw "Campaign archive cleanup failed" }
    $campaignStatus = "ARCHIVED"

    Write-Host "Production analytics dashboard update verification passed."
    Write-Host "Creation: campaign total increased by one"
    Write-Host "Launch: active increased by one; audience and sent remained unchanged"
    Write-Host "Detail: ACTIVE campaign metrics remained zero"
    Write-Host "Completion: active returned to baseline and completed increased by one"
    Write-Host "Cleanup: campaign archived"
}
finally {
    if ($campaignId -and $campaignStatus -eq "ACTIVE" -and $managerToken) {
        try {
            Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/complete" `
                -Headers @{ Authorization = "Bearer $managerToken" } | Out-Null
            Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/archive" `
                -Headers @{ Authorization = "Bearer $managerToken" } | Out-Null
        }
        catch { Write-Warning "Analytics smoke campaign cleanup failed" }
    }
    foreach ($token in @($managerToken, $complianceToken)) {
        if ($token) {
            try { Invoke-RestMethod -Method Post -Uri "$origin/api/auth/logout" `
                -Headers @{ Authorization = "Bearer $token" } | Out-Null } catch {}
        }
    }
    $managerToken = $null; $complianceToken = $null; $campaignBody = $null; $approvalBody = $null
}
