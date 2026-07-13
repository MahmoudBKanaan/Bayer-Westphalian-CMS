param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$AgentCredential,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$AdminCleanupCredential,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https" -or $BaseUrl.AbsolutePath -ne "/" -or
    $BaseUrl.Query -or $BaseUrl.Fragment) {
    throw "BaseUrl must be an HTTPS origin"
}

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$agentToken = $null
$adminToken = $null
$customerId = $null
$consentId = $null
$consentWithdrawn = $false
$customerDeleted = $false
$suffix = "$( [DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss') )-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$syntheticEmail = "smoke-consent-$suffix@example.invalid"

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
        return $token
    }
    finally {
        $password = $null
        $body = $null
        $login = $null
    }
}

try {
    $agentToken = New-RoleSession $AgentCredential "CUSTOMER_SERVICE_AGENT"
    $agentHeaders = @{ Authorization = "Bearer $agentToken" }

    $customerBody = @{
        customerType = "PROSPECT"
        firstName = "SMOKE-$suffix"
        lastName = "Consent-Record"
        email = $syntheticEmail
        status = "INACTIVE"
        doNotContact = $true
        source = "PRODUCTION_SMOKE_TEST"
    } | ConvertTo-Json -Compress
    $customerResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/customers" `
        -Headers $agentHeaders -ContentType "application/json" -Body $customerBody `
        -TimeoutSec $TimeoutSeconds -UseBasicParsing
    $customer = $customerResponse.Content | ConvertFrom-Json
    $parsedCustomerId = [Guid]::Empty
    if ($customerResponse.StatusCode -ne 201 -or
        -not [Guid]::TryParse([string]$customer.data.id, [ref]$parsedCustomerId)) {
        throw "Synthetic customer setup failed"
    }
    $customerId = $parsedCustomerId.ToString()

    $consentBody = @{
        customerId = $customerId
        consentType = "MARKETING_EMAIL"
        status = "GIVEN"
        purpose = "Synthetic production consent recording verification"
        source = "PRODUCTION_SMOKE_TEST"
        grantedAt = [DateTimeOffset]::UtcNow.ToString("o")
    } | ConvertTo-Json -Compress
    $consentResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/consents" `
        -Headers $agentHeaders -ContentType "application/json" -Body $consentBody `
        -TimeoutSec $TimeoutSeconds -UseBasicParsing
    $consent = $consentResponse.Content | ConvertFrom-Json
    $parsedConsentId = [Guid]::Empty
    if ($consentResponse.StatusCode -ne 201 -or -not $consent.success -or
        -not [Guid]::TryParse([string]$consent.data.id, [ref]$parsedConsentId)) {
        throw "Consent recording did not return HTTP 201 with a valid UUID"
    }
    $consentId = $parsedConsentId.ToString()
    if ($consent.data.customerId -ne $customerId -or
        $consent.data.consentType -ne "MARKETING_EMAIL" -or
        $consent.data.status -ne "GIVEN" -or -not $consent.data.valid) {
        throw "Recorded consent does not match the synthetic request"
    }

    $status = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/consents/status?customerId=$customerId&consentType=MARKETING_EMAIL" `
        -Headers $agentHeaders -TimeoutSec $TimeoutSeconds
    if (-not $status.success -or $status.data.id -ne $consentId) {
        throw "Recorded consent could not be read back as current status"
    }

    $withdrawBody = @{ consentRecordId = $consentId } | ConvertTo-Json -Compress
    $withdrawn = Invoke-RestMethod -Method Post -Uri "$origin/api/consents/withdraw" `
        -Headers $agentHeaders -ContentType "application/json" -Body $withdrawBody `
        -TimeoutSec $TimeoutSeconds
    if (-not $withdrawn.success -or $withdrawn.data.status -ne "WITHDRAWN" -or
        -not $withdrawn.data.withdrawnAt -or $withdrawn.data.valid) {
        throw "Consent withdrawal cleanup failed"
    }
    $consentWithdrawn = $true

    $adminToken = New-RoleSession $AdminCleanupCredential "ADMIN"
    $deleted = Invoke-RestMethod -Method Delete -Uri "$origin/api/customers/$customerId" `
        -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec $TimeoutSeconds
    if (-not $deleted.success -or -not $deleted.data.deletedAt) {
        throw "Synthetic customer cleanup failed"
    }
    $customerDeleted = $true

    Write-Host "Production consent recording verification passed."
    Write-Host "Authorization: Customer Service Agent consent-write role"
    Write-Host "Recording: HTTP 201 with valid consent UUID"
    Write-Host "Persistence: current consent status read-back succeeded"
    Write-Host "Cleanup: consent withdrawn and synthetic customer soft-deleted"
}
finally {
    if ($consentId -and $agentToken -and -not $consentWithdrawn) {
        try {
            $body = @{ consentRecordId = $consentId } | ConvertTo-Json -Compress
            Invoke-RestMethod -Method Post -Uri "$origin/api/consents/withdraw" `
                -Headers @{ Authorization = "Bearer $agentToken" } -ContentType "application/json" `
                -Body $body -TimeoutSec $TimeoutSeconds | Out-Null
            Write-Warning "Consent was withdrawn during failure cleanup"
        }
        catch { Write-Warning "Consent cleanup failed; withdraw the recorded smoke consent" }
    }
    if ($customerId -and -not $customerDeleted) {
        try {
            if (-not $adminToken) { $adminToken = New-RoleSession $AdminCleanupCredential "ADMIN" }
            Invoke-RestMethod -Method Delete -Uri "$origin/api/customers/$customerId" `
                -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Write-Warning "Synthetic customer was soft-deleted during failure cleanup"
        }
        catch { Write-Warning "Customer cleanup failed; soft-delete the smoke customer" }
    }
    foreach ($token in @($agentToken, $adminToken)) {
        if ($token) {
            try {
                Invoke-RestMethod -Method Post -Uri "$origin/api/auth/logout" `
                    -Headers @{ Authorization = "Bearer $token" } -TimeoutSec $TimeoutSeconds |
                    Out-Null
            }
            catch { Write-Warning "Logout failed; revoke the affected smoke session" }
        }
    }
    $agentToken = $null
    $adminToken = $null
    $agentHeaders = $null
    $syntheticEmail = $null
    $customerBody = $null
    $consentBody = $null
    $withdrawBody = $null
}
