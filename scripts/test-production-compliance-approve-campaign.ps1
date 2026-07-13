param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$CampaignManagerCredential,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$ComplianceCredential,
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
$syntheticName = "SMOKE-$suffix Compliance Approval"

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
        name = $syntheticName
        objective = "Synthetic compliance approval verification; never launch"
        channel = "EMAIL"
        messageSubject = "Synthetic approval evidence"
        messageBody = "No segment, products, recipients, schedule, or sending."
        productIds = @()
    } | ConvertTo-Json -Compress
    $createResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/campaigns" `
        -Headers $managerHeaders -ContentType "application/json" -Body $campaignBody `
        -TimeoutSec $TimeoutSeconds -UseBasicParsing
    $created = $createResponse.Content | ConvertFrom-Json
    $parsedCampaignId = [Guid]::Empty
    if ($createResponse.StatusCode -ne 201 -or
        -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedCampaignId)) {
        throw "Synthetic campaign setup failed"
    }
    $campaignId = $parsedCampaignId.ToString()
    $campaignStatus = "DRAFT"

    $submitted = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/submit" `
        -Headers $managerHeaders -TimeoutSec $TimeoutSeconds
    if (-not $submitted.success -or $submitted.data.status -ne "SUBMITTED") {
        throw "Campaign submission failed"
    }
    $campaignStatus = "SUBMITTED"

    $compliance = New-RoleSession $ComplianceCredential "COMPLIANCE_OFFICER"
    $complianceToken = $compliance.Token
    $complianceHeaders = @{ Authorization = "Bearer $complianceToken" }
    $approvalBody = @{
        complianceReviewNotes = "Synthetic item 752 human approval verification"
    } | ConvertTo-Json -Compress
    $approved = Invoke-RestMethod -Method Post -Uri "$origin/api/campaigns/$campaignId/approve" `
        -Headers $complianceHeaders -ContentType "application/json" -Body $approvalBody `
        -TimeoutSec $TimeoutSeconds
    if (-not $approved.success -or $approved.data.status -ne "APPROVED" -or
        [string]$approved.data.approvedByUserId -ne $compliance.UserId -or
        -not $approved.data.approvedAt -or
        $approved.data.complianceReviewNotes -ne "Synthetic item 752 human approval verification") {
        throw "Compliance approval response does not identify the human approver and notes"
    }
    $campaignStatus = "APPROVED"

    $persisted = Invoke-RestMethod -Method Get -Uri "$origin/api/campaigns/$campaignId" `
        -Headers $complianceHeaders -TimeoutSec $TimeoutSeconds
    if (-not $persisted.success -or $persisted.data.status -ne "APPROVED" -or
        [string]$persisted.data.approvedByUserId -ne $compliance.UserId) {
        throw "Approved campaign could not be read back"
    }

    $history = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/audit-logs/entities/campaigns/$campaignId" `
        -Headers $complianceHeaders -TimeoutSec $TimeoutSeconds
    $approvalAudit = @($history.data) | Where-Object {
        $_.action -eq "APPROVE" -and [string]$_.actorUserId -eq $compliance.UserId
    }
    if (-not $history.success -or $approvalAudit.Count -lt 1) {
        throw "Immutable APPROVE audit event for the Compliance Officer was not found"
    }

    Write-Host "Production Compliance Officer approve-campaign verification passed."
    Write-Host "Authorization: active COMPLIANCE_OFFICER session"
    Write-Host "Approval: persisted APPROVED status with human approver and timestamp"
    Write-Host "Audit: immutable APPROVE event matches the Compliance Officer"
    Write-Host "Safety: targetless and unscheduled; never preview, launch, or send"
    Write-Host "Retained synthetic campaign UUID: $campaignId"
}
finally {
    if ($campaignId -and $campaignStatus -in @("DRAFT", "SUBMITTED") -and $managerToken) {
        Write-Warning "Synthetic campaign remains non-approved; reject and archive it through the approved workflow"
    }
    if ($campaignStatus -eq "APPROVED") {
        Write-Warning "Approved synthetic campaign intentionally retained; never launch it without a separately approved item 753 test"
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
    $syntheticName = $null
}
