param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$AgentCredential,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$AdminCleanupCredential,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https" -or $BaseUrl.AbsolutePath -ne "/" -or
    $BaseUrl.Query -or $BaseUrl.Fragment) { throw "BaseUrl must be an HTTPS origin" }

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$agentToken = $null
$adminToken = $null
$customerId = $null
$customerDeleted = $false
$suffix = "$([DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss'))-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$syntheticEmail = "smoke-contact-$suffix@example.invalid"
$eventNotes = "Synthetic item 754 contact-history verification; no communication occurred"

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
    $agent = New-RoleSession -Credential $AgentCredential -RequiredRole "CUSTOMER_SERVICE_AGENT"
    $agentToken = $agent.Token
    $agentHeaders = @{ Authorization = "Bearer $agentToken" }
    $customerBody = @{
        customerType = "PROSPECT"
        firstName = "SMOKE-$suffix"
        lastName = "Contact-Event"
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

    $occurredAt = [DateTimeOffset]::UtcNow.ToString("o")
    $eventBody = @{
        customerId = $customerId
        channel = "PHONE"
        eventType = "NOTE"
        outcome = "NO_RESPONSE"
        notes = $eventNotes
        occurredAt = $occurredAt
    } | ConvertTo-Json -Compress
    $eventResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/contact-events" `
        -Headers $agentHeaders -ContentType "application/json" -Body $eventBody `
        -TimeoutSec $TimeoutSeconds -UseBasicParsing
    $event = $eventResponse.Content | ConvertFrom-Json
    $parsedEventId = [Guid]::Empty
    if ($eventResponse.StatusCode -ne 201 -or -not $event.success -or
        -not [Guid]::TryParse([string]$event.data.id, [ref]$parsedEventId)) {
        throw "Contact event did not return HTTP 201 with a valid UUID"
    }
    $eventId = $parsedEventId.ToString()
    if ($event.data.customerId -ne $customerId -or $event.data.campaignId -or
        $event.data.channel -ne "PHONE" -or $event.data.eventType -ne "NOTE" -or
        [string]$event.data.createdByUserId -ne $agent.UserId) {
        throw "Recorded contact event does not match customer, creator, or provider-free type"
    }

    $timeline = Invoke-RestMethod -Method Get `
        -Uri "$origin/api/contact-events/timeline?customerId=$customerId&eventType=NOTE" `
        -Headers $agentHeaders -TimeoutSec $TimeoutSeconds
    $matching = @($timeline.data) | Where-Object { [string]$_.id -eq $eventId }
    if (-not $timeline.success -or $matching.Count -ne 1) {
        throw "Recorded contact event was not found in the customer timeline"
    }

    $admin = New-RoleSession -Credential $AdminCleanupCredential -RequiredRole "ADMIN"
    $adminToken = $admin.Token
    $deleted = Invoke-RestMethod -Method Delete -Uri "$origin/api/customers/$customerId" `
        -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec $TimeoutSeconds
    if (-not $deleted.success -or -not $deleted.data.deletedAt) {
        throw "Synthetic customer cleanup failed"
    }
    $customerDeleted = $true

    Write-Host "Production contact-event recording verification passed."
    Write-Host "Authorization: active CUSTOMER_SERVICE_AGENT session"
    Write-Host "Recording: HTTP 201 with valid contact-event UUID"
    Write-Host "Safety: provider-free NOTE; no campaign or communication send"
    Write-Host "Persistence: customer timeline contains exactly one matching event"
    Write-Host "Cleanup: synthetic customer soft-deleted; immutable event retained"
}
finally {
    if ($customerId -and -not $customerDeleted) {
        try {
            if (-not $adminToken) {
                $adminToken =
                    (New-RoleSession -Credential $AdminCleanupCredential -RequiredRole "ADMIN").Token
            }
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
    $eventNotes = $null
    $customerBody = $null
    $eventBody = $null
}
