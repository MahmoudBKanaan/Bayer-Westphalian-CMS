# Scripts

Project helper scripts for local setup, verification, packaging, and deployment preparation.

Scripts added here must be safe to run locally and documented before use.

## Verification Scripts

Run from the project root on Windows PowerShell:

Production backup creation verification (requires an uncommitted production environment file):

```powershell
.\scripts\test-production-backup.ps1 -EnvFile .env.production
.\scripts\test-production-backup-exists.ps1 -BackupVolume bwc_postgres_backups -MaximumAgeHours 26
.\scripts\test-production-restore.ps1 -BackupVolume bwc_postgres_backups
.\scripts\test-production-https.ps1 -BaseUrl https://campaign.example.com
$adminCredential = Get-Credential
.\scripts\test-production-admin-login.ps1 -BaseUrl https://campaign.example.com -Credential $adminCredential
.\scripts\test-production-admin-create-user.ps1 -BaseUrl https://campaign.example.com -AdminCredential $adminCredential
$agentCredential = Get-Credential
.\scripts\test-production-agent-create-customer.ps1 -BaseUrl https://campaign.example.com -AgentCredential $agentCredential -AdminCleanupCredential $adminCredential
.\scripts\test-production-record-consent.ps1 -BaseUrl https://campaign.example.com -AgentCredential $agentCredential -AdminCleanupCredential $adminCredential
$productManagerCredential = Get-Credential
.\scripts\test-production-product-manager-create-product.ps1 -BaseUrl https://campaign.example.com -ProductManagerCredential $productManagerCredential
$campaignManagerCredential = Get-Credential
.\scripts\test-production-campaign-manager-create-segment.ps1 -BaseUrl https://campaign.example.com -CampaignManagerCredential $campaignManagerCredential
$complianceCredential = Get-Credential
.\scripts\test-production-campaign-manager-create-campaign.ps1 -BaseUrl https://campaign.example.com -CampaignManagerCredential $campaignManagerCredential -ComplianceCleanupCredential $complianceCredential
.\scripts\test-production-compliance-approve-campaign.ps1 -BaseUrl https://campaign.example.com -CampaignManagerCredential $campaignManagerCredential -ComplianceCredential $complianceCredential
.\scripts\test-production-campaign-manager-launch-approved.ps1 -BaseUrl https://campaign.example.com -CampaignManagerCredential $campaignManagerCredential -ComplianceCredential $complianceCredential -ProviderSendingConfirmedDisabled
.\scripts\test-production-record-contact-event.ps1 -BaseUrl https://campaign.example.com -AgentCredential $agentCredential -AdminCleanupCredential $adminCredential
.\scripts\test-production-analytics-dashboard-updates.ps1 -BaseUrl https://campaign.example.com -CampaignManagerCredential $campaignManagerCredential -ComplianceCredential $complianceCredential -ProviderSendingConfirmedDisabled
.\scripts\test-production-sensitive-action-audit.ps1 -BaseUrl https://campaign.example.com -AdminCredential $adminCredential
.\scripts\test-production-logs-accessible.ps1 -EnvFile .env.production -TailLines 50
```

```powershell
.\scripts\test-docker-compose-config.ps1
.\scripts\test-docker-compose-postgres.ps1
.\scripts\test-architecture-docs.ps1
.\scripts\test-sprint-1-review-docs.ps1
```

- `test-docker-compose-config.ps1` validates the Docker Compose model for the PostgreSQL service, named volume, network, and health check.
- `test-docker-compose-postgres.ps1` starts PostgreSQL with Docker Compose, waits for health, runs `pg_isready`, and executes a smoke SQL query.
- `test-production-backup.ps1` triggers a production-format backup and verifies its size, SHA-256 manifest, and `pg_restore` readability without exposing rows.
- `test-production-backup-exists.ps1` read-only verifies that a fresh completed backup and checksum exist and PostgreSQL can read the archive.
- `test-production-restore.ps1` restores a verified dump into an isolated PostgreSQL `tmpfs` rehearsal container and validates Flyway history and core tables.
- `test-production-https.ps1` verifies trusted TLS, the React application shell, HSTS, permanent HTTP-to-HTTPS redirect, and HTTPS readiness.
- `test-production-admin-login.ps1` verifies an active ADMIN session over trusted HTTPS without printing or persisting credentials or tokens.
- `test-production-admin-create-user.ps1` creates, reads, and disables a uniquely named synthetic user through the authorized Admin API.
- `test-production-agent-create-customer.ps1` verifies Customer Service Agent creation/read and Admin-only soft-delete cleanup of a non-contactable synthetic prospect.
- `test-production-record-consent.ps1` records, reads, and withdraws synthetic marketing consent, then soft-deletes its non-contactable customer.
- `test-production-product-manager-create-product.ps1` verifies Product Manager creation/read and audited disable/soft-delete cleanup of a synthetic product.
- `test-production-campaign-manager-create-segment.ps1` verifies Campaign Manager creation/read/delete of a private, empty-criteria synthetic segment with an automatic UUID.
- `test-production-campaign-manager-create-campaign.ps1` verifies Campaign Manager draft creation/read and controlled Compliance rejection/archive cleanup without recipients or launch.
- `test-production-compliance-approve-campaign.ps1` verifies separate human Compliance approval, persisted approver identity, and immutable approval audit evidence for a targetless campaign.
- `test-production-campaign-manager-launch-approved.ps1` launches only a newly approved zero-recipient campaign, verifies Manager launch/audit, then completes and archives it.
- `test-production-record-contact-event.ps1` records and reads a provider-free Agent NOTE for a non-contactable synthetic customer, then Admin soft-deletes the customer.
- `test-production-analytics-dashboard-updates.ps1` verifies deterministic dashboard and campaign-metric changes through a zero-recipient campaign lifecycle.
- `test-production-sensitive-action-audit.ps1` verifies Admin user creation/disable produce actor-linked immutable audit events without password material.
- `test-production-logs-accessible.ps1` read-only verifies bounded logs for every production service and prints only service/count metadata.
- `test-architecture-docs.ps1` validates that the initial architecture document contains the required KB architecture components and is linked from the docs index.
- `test-sprint-1-review-docs.ps1` validates that Sprint 1 review notes contain the required review, demo, test, DoD, risk, stakeholder, and follow-up sections and are linked from the docs index.
