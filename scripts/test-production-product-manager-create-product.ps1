param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [Parameter(Mandatory = $true)][System.Management.Automation.PSCredential]$ProductManagerCredential,
    [int]$TimeoutSeconds = 15
)

$ErrorActionPreference = "Stop"
if ($BaseUrl.Scheme -ne "https" -or $BaseUrl.AbsolutePath -ne "/" -or
    $BaseUrl.Query -or $BaseUrl.Fragment) {
    throw "BaseUrl must be an HTTPS origin"
}

$origin = $BaseUrl.GetLeftPart([System.UriPartial]::Authority)
$accessToken = $null
$productId = $null
$productDeleted = $false
$suffix = "$([DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss'))-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
$syntheticName = "SMOKE-$suffix Product Manager Product"

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
    $password = $ProductManagerCredential.GetNetworkCredential().Password
    $loginBody = @{
        email = $ProductManagerCredential.UserName
        password = $password
    } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Method Post -Uri "$origin/api/auth/login" `
        -ContentType "application/json" -Body $loginBody -TimeoutSec $TimeoutSeconds
    if (-not $login.success -or $login.data.user.status -ne "ACTIVE") {
        throw "Product Manager authentication failed"
    }
    $accessToken = [string]$login.data.tokens.accessToken
    $parts = $accessToken.Split('.')
    if ($parts.Count -ne 3 -or
        @((ConvertFrom-Base64UrlJson $parts[1]).roles) -notcontains "PRODUCT_MANAGER") {
        throw "Authenticated account does not have the PRODUCT_MANAGER role"
    }
    $headers = @{ Authorization = "Bearer $accessToken" }

    $productBody = @{
        name = $syntheticName
        productType = "OTHER"
        description = "Synthetic production Product Manager creation verification"
        price = 0.00
        durationMonths = 1
        expirationPolicy = "SMOKE_TEST_ONLY"
    } | ConvertTo-Json -Compress
    $createResponse = Invoke-WebRequest -Method Post -Uri "$origin/api/products" `
        -Headers $headers -ContentType "application/json" -Body $productBody `
        -TimeoutSec $TimeoutSeconds -UseBasicParsing
    $created = $createResponse.Content | ConvertFrom-Json
    $parsedProductId = [Guid]::Empty
    if ($createResponse.StatusCode -ne 201 -or -not $created.success -or
        -not [Guid]::TryParse([string]$created.data.id, [ref]$parsedProductId)) {
        throw "Product creation did not return HTTP 201 with a valid UUID"
    }
    $productId = $parsedProductId.ToString()
    if ($created.data.name -ne $syntheticName -or $created.data.productType -ne "OTHER" -or
        -not $created.data.active -or $created.data.deleted) {
        throw "Created product response does not match the synthetic request"
    }

    $persisted = Invoke-RestMethod -Method Get -Uri "$origin/api/products/$productId" `
        -Headers $headers -TimeoutSec $TimeoutSeconds
    if (-not $persisted.success -or $persisted.data.name -ne $syntheticName) {
        throw "Created synthetic product could not be read back"
    }

    $disabled = Invoke-RestMethod -Method Patch -Uri "$origin/api/products/$productId/disable" `
        -Headers $headers -TimeoutSec $TimeoutSeconds
    if (-not $disabled.success -or $disabled.data.active) {
        throw "Synthetic product disable cleanup failed"
    }

    $deleted = Invoke-RestMethod -Method Delete -Uri "$origin/api/products/$productId" `
        -Headers $headers -TimeoutSec $TimeoutSeconds
    if (-not $deleted.success -or -not $deleted.data.deleted -or -not $deleted.data.deletedAt) {
        throw "Synthetic product soft-delete cleanup failed"
    }
    $productDeleted = $true

    Write-Host "Production Product Manager create-product verification passed."
    Write-Host "Authorization: active PRODUCT_MANAGER session"
    Write-Host "Creation: HTTP 201 with valid product UUID"
    Write-Host "Persistence: Product Manager read-back succeeded"
    Write-Host "Cleanup: product disabled and soft-deleted"
}
finally {
    if ($productId -and $accessToken -and -not $productDeleted) {
        try {
            Invoke-RestMethod -Method Patch -Uri "$origin/api/products/$productId/disable" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Invoke-RestMethod -Method Delete -Uri "$origin/api/products/$productId" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
            Write-Warning "Synthetic product was disabled and soft-deleted during failure cleanup"
        }
        catch { Write-Warning "Product cleanup failed; disable and soft-delete the smoke product" }
    }
    if ($accessToken) {
        try {
            Invoke-RestMethod -Method Post -Uri "$origin/api/auth/logout" `
                -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec $TimeoutSeconds |
                Out-Null
        }
        catch { Write-Warning "Logout failed; revoke the Product Manager smoke session" }
    }
    $password = $null
    $accessToken = $null
    $headers = $null
    $loginBody = $null
    $productBody = $null
    $syntheticName = $null
}
