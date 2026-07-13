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
    throw "Provider sending must be confirmed disabled before launch verification"
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
    $manager = New-RoleSession $CampaignManagerCredential "CAMPAIGN_MANAGER"
    $managerToken = $manager.Token
    $managerHeaders = @{ Authorization = "Bearer $managerToken" }
    $campaignBody = @{
        name = "SMOKE-$suffix Safe Launch"
        objective = "Synthetic targetless launch verification"
        channel = "EMAIL"
        messageSubject = "Never delivered: zero recipients"
        messageBody = "Synthetic lifecycle verification only."
        productIds = @()
    } | ConvertTo-Json -Compress
    $createResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/campaigns" `
        -Headers $managerHeaders -ContentType "application/json" -Body $campaignBody `
        -TimeoutSec $TimeoutSeconds -UseBasicParsing
    $created = $createResponse.Content | ConvertFrom-Json
    $parsedId = [Guid]::Empty
    if ($createResponse.StatusCode -ne 201 -or
        -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedId)) {
        throw "Synthetic campaign setup failed"
    }
    $campaignId = $parsedId.ToString()
    $campaignStatus = "DRAFT"

    $submitted = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/submit" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ($submitted.data.status -ne "SUBMITTED") { throw "Campaign submission failed" }
    $campaignStatus = "SUBMITTED"

    $compliance = New-RoleSession $ComplianceCredential "COMPLIANCE_OFFICER"
    $complianceToken = $compliance.Token
    $complianceHeaders = @{ Authorization = "Bearer $complianceToken" }
    $approvalBody = @{ complianceReviewNotes = "Item 753 zero-recipient launch approval" } |
        ConvertTo-Json -Compress
    $approved = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/approve" `
        -Headers $complianceHeaders -ContentType "application/json" -Body $approvalBody `
        -TimeoutSec $TimeoutSeconds
    if ($approved.data.status -ne "APPROVED" -or
        [string]$approved.data.approvedByUserId -ne $compliance.UserId) {
        throw "Human Compliance approval failed"
    }
    $campaignStatus = "APPROVED"

    $eligibleBefore = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/campaigns/$campaignId/recipients/eligible" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    $excludedBefore = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/campaigns/$campaignId/recipients/excluded" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (@($eligibleBefore.data).Count -ne 0 -or @($excludedBefore.data).Count -ne 0) {
        throw "Launch blocked: synthetic campaign unexpectedly has recipient rows"
    }

    $launched = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/launch" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (-not $launched.success -or $launched.data.status -ne "ACTIVE") {
        throw "Campaign Manager launch did not produce ACTIVE status"
    }
    $campaignStatus = "ACTIVE"

    $eligibleAfter = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/campaigns/$campaignId/recipients/eligible" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (@($eligibleAfter.data).Count -ne 0) {
        throw "Launch created an unexpected eligible/sent recipient"
    }

    $history = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/audit-logs/entities/campaigns/$campaignId" `
        -Headers $complianceHeaders -TimeoutSec $TimeoutSeconds
    $launchAudit = @($history.data) | Where-Object {
        $_.action -eq "LAUNCH" -and [string]$_.actorUserId -eq $manager.UserId
    }
    if (-not $history.success -or $launchAudit.Count -lt 1) {
        throw "Immutable LAUNCH audit event for the Campaign Manager was not found"
    }

    $completed = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/complete" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ($completed.data.status -ne "COMPLETED") { throw "Campaign completion cleanup failed" }
    $campaignStatus = "COMPLETED"
    $archived = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/archive" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if ($archived.data.status -ne "ARCHIVED") { throw "Campaign archive cleanup failed" }
    $campaignStatus = "ARCHIVED"

    Write-Host "Production Campaign Manager launch-approved verification passed."
    Write-Host "Approval: separate human COMPLIANCE_OFFICER"
    Write-Host "Precondition: zero eligible and zero excluded recipient rows"
    Write-Host "Launch: CAMPAIGN_MANAGER changed APPROVED to ACTIVE"
    Write-Host "Audit: immutable LAUNCH event matches the Campaign Manager"
    Write-Host "Sending: zero recipients and zero contact generation"
    Write-Host "Cleanup: completed and archived"
}
finally {
    if ($campaignId -and $campaignStatus -eq "ACTIVE" -and $managerToken) {
        try {
            Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/complete" `
                -Headers @{ Authorization = "Bearer $managerToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/archive" `
                -Headers @{ Authorization = "Bearer $managerToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Write-Warning "Synthetic campaign completed and archived during failure cleanup"
        }
        catch { Write-Warning "Active synthetic campaign cleanup failed; complete and archive it immediately" }
    }
    if ($campaignId -and $campaignStatus -in @("DRAFT", "SUBMITTED", "APPROVED")) {
        Write-Warning "Synthetic campaign remains non-active; resolve it through the approved lifecycle"
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
    $complianceHeaders = $null
    $campaignBody = $null
    $approvalBody = $null
}
