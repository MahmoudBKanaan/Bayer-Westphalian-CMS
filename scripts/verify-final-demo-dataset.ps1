param(
    [string]$ComposeFile = "docker-compose.yml"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$composePath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $ComposeFile))

if (-not (Test-Path -LiteralPath $composePath -PathType Leaf)) {
    throw "Compose file not found"
}

$sql = @'
WITH checks(name, present) AS (
    VALUES
      ('users', EXISTS (SELECT 1 FROM users WHERE id = '10000000-0000-0000-0000-000000000001')),
      ('customers', EXISTS (SELECT 1 FROM customers WHERE id = '20000000-0000-0000-0000-000000000101')),
      ('beneficiaries', EXISTS (SELECT 1 FROM beneficiaries WHERE policyholder_customer_id = '20000000-0000-0000-0000-000000000101')),
      ('consents', EXISTS (SELECT 1 FROM consent_records WHERE customer_id = '20000000-0000-0000-0000-000000000102')),
      ('products', EXISTS (SELECT 1 FROM products WHERE id = '30000000-0000-0000-0000-000000000101')),
      ('ownerships', EXISTS (SELECT 1 FROM product_ownerships WHERE customer_id = '20000000-0000-0000-0000-000000000101')),
      ('payments', EXISTS (SELECT 1 FROM payment_records WHERE id = '32000000-0000-0000-0000-000000000101')),
      ('segments', EXISTS (SELECT 1 FROM segments WHERE id = '40000000-0000-0000-0000-000000000101')),
      ('campaigns', EXISTS (SELECT 1 FROM campaigns WHERE id = '50000000-0000-0000-0000-000000000101')),
      ('recipients', EXISTS (SELECT 1 FROM campaign_recipients WHERE campaign_id = '50000000-0000-0000-0000-000000000101')),
      ('contacts', EXISTS (SELECT 1 FROM contact_events WHERE id = '52000000-0000-0000-0000-000000000101')),
      ('followups', EXISTS (SELECT 1 FROM follow_up_tasks WHERE id = '53000000-0000-0000-0000-000000000101')),
      ('reminders', EXISTS (SELECT 1 FROM reminder_schedules WHERE id = '54000000-0000-0000-0000-000000000101')),
      ('metrics', EXISTS (SELECT 1 FROM campaign_metrics WHERE campaign_id = '50000000-0000-0000-0000-000000000101')),
      ('reports', EXISTS (SELECT 1 FROM report_exports WHERE id = '56000000-0000-0000-0000-000000000101')),
      ('ai', EXISTS (SELECT 1 FROM ai_recommendations WHERE id = '57000000-0000-0000-0000-000000000101')),
      ('audit', EXISTS (SELECT 1 FROM audit_logs WHERE id = '58000000-0000-0000-0000-000000000101'))
)
SELECT name || '=' || CASE WHEN present THEN 'PASS' ELSE 'MISSING' END
FROM checks
ORDER BY name;
'@

$result = $sql | docker compose -f $composePath exec -T postgres `
    psql -X -v ON_ERROR_STOP=1 -U bwc_app -d bwc_campaign -t -A
if ($LASTEXITCODE -ne 0) {
    throw "Could not verify the final demo dataset"
}

$lines = @($result | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$missing = @($lines | Where-Object { $_ -like "*=MISSING" })
if ($missing.Count -gt 0) {
    throw "Final demo dataset is incomplete: $($missing -join ', ')"
}

Write-Host "Final demo dataset verification passed."
Write-Host "Scope: synthetic dev/test data only"
Write-Host "Entity groups: $($lines.Count)"
Write-Host "Provider sending: not required"
