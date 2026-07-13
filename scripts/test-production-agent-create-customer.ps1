param(
    [Parameter(Mandatory = $true)]
    [uri]$BaseUrl,
    [Parameter(Mandatory = $true)]
    [System.Management.Automation.PSCredential]$AgentCredential,
    [Parameter(Mandatory = $true)]
    [System.Management.Automation.PSCredential]$AdminCleanupCredential,
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
$agentToken = $null
$adminToken = $null
$customerId = $null
$cleanupCompleted = $false
$utcStamp = [DateTimeOffset]::UtcNow.ToString("yyyyMMddHHmmss")
$suffix = "$utcStamp-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$syntheticEmail = "smoke-agent-customer-$suffix@example.invalid"
$syntheticFirstName = "SMOKE-$suffix"
$syntheticLastName = "Agent-Created"

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

function New-RoleSession {
    param(
        [System.Management.Automation.PSCredential]$Credential,
        [string]$RequiredRole
    )

    $password = $Credential.GetNetworkCredential().Password
    try {
        $body = @{ email = $Credential.UserName; password = $password } | ConvertTo-Json -Compress
        $login =
            Invoke-RestMethod -Method Post -Uri "$origin/api/auth/login" `
                -ContentType "application/json" -Body $body -TimeoutSec $TimeoutSeconds
        if (-not $login.success -or $login.data.user.status -ne "ACTIVE") {
            throw "Role session authentication failed"
        }
        $token = [string]$login.data.tokens.accessToken
        $parts = $token.Split('.')
        if ($parts.Count -ne 3) {
            throw "Access token is not a JWT"
        }
        $claims = ConvertFrom-Base64UrlJson -Value $parts[1]
        if (@($claims.roles) -notcontains $RequiredRole) {
            throw "Authenticated account does not have required role"
        }
        return $token
    }
    finally {
        $password = $null
        $body = $null
        $login = $null
        $claims = $null
    }
}

try {
    $agentToken = New-RoleSession -Credential $AgentCredential -RequiredRole "CUSTOMER_SERVICE_AGENT"
    $agentHeaders = @{ Authorization = "Bearer $agentToken" }
    $customerBody =
        @{
            customerType = "PROSPECT"
            firstName = $syntheticFirstName
            lastName = $syntheticLastName
            email = $syntheticEmail
            status = "INACTIVE"
            doNotContact = $true
            source = "PRODUCTION_SMOKE_TEST"
        } | ConvertTo-Json -Compress

    $createResponse =
        Invoke-WebRequest -Method Post -Uri "$origin/api/customers" -Headers $agentHeaders `
            -ContentType "application/json" -Body $customerBody -TimeoutSec $TimeoutSeconds `
            -UseBasicParsing
    if ($createResponse.StatusCode -ne 201) {
        throw "Customer creation did not return HTTP 201"
    }
    $created = $createResponse.Content | ConvertFrom-Json
    $parsedCustomerId = [Guid]::Empty
    if (-not $created.success -or -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedCustomerId)) {
        throw "Created customer does not have a valid UUID"
    }
    $customerId = $parsedCustomerId.ToString()
    if ($created.data.email -ne $syntheticEmail -or
        $created.data.customerType -ne "PROSPECT" -or
        $created.data.status -ne "INACTIVE" -or
        -not $created.data.doNotContact) {
        throw "Created customer response does not preserve synthetic safety fields"
    }

    $persisted =
        Invoke-RestMethod -Method Get -Uri "$origin/api/customers/$customerId" `
            -Headers $agentHeaders -TimeoutSec $TimeoutSeconds
    if (-not $persisted.success -or $persisted.data.email -ne $syntheticEmail) {
        throw "Created synthetic customer could not be read by Customer Service Agent"
    }

    $adminToken = New-RoleSession -Credential $AdminCleanupCredential -RequiredRole "ADMIN"
    $deleted =
        Invoke-RestMethod -Method Delete -Uri "$origin/api/customers/$customerId" `
            -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec $TimeoutSeconds
    if (-not $deleted.success -or -not $deleted.data.deletedAt -or $deleted.data.active) {
        throw "Admin cleanup did not soft-delete the synthetic customer"
    }
    $cleanupCompleted = $true

    Write-Host "Production Customer Service Agent create-customer verification passed."
    Write-Host "Authorization: active CUSTOMER_SERVICE_AGENT session"
    Write-Host "Creation: HTTP 201 with valid customer UUID"
    Write-Host "Safety: synthetic prospect is INACTIVE and do-not-contact"
    Write-Host "Persistence: Agent read-back succeeded"
    Write-Host "Cleanup: Admin soft-delete succeeded"
}
finally {
    if ($customerId -and -not $cleanupCompleted) {
        try {
            if (-not $adminToken) {
                $adminToken =
                    New-RoleSession -Credential $AdminCleanupCredential -RequiredRole "ADMIN"
            }
            Invoke-RestMethod -Method Delete -Uri "$origin/api/customers/$customerId" `
                -Headers @{ Authorization = "Bearer $adminToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Write-Warning "Synthetic customer was soft-deleted during failure cleanup"
        }
        catch {
            Write-Warning "Synthetic customer cleanup failed; soft-delete the recorded smoke customer through the approved Admin workflow"
        }
    }
    foreach ($token in @($agentToken, $adminToken)) {
        if ($token) {
            try {
                Invoke-RestMethod -Method Post -Uri "$origin/api/auth/logout" `
                    -Headers @{ Authorization = "Bearer $token" } -TimeoutSec $TimeoutSeconds |
                    Out-Null
            }
            catch {
                Write-Warning "Logout failed; revoke the affected smoke session"
            }
        }
    }
    $agentToken = $null
    $adminToken = $null
    $agentHeaders = $null
    $customerBody = $null
    $syntheticEmail = $null
}
