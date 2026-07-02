# Scripts

Project helper scripts for local setup, verification, packaging, and deployment preparation.

Scripts added here must be safe to run locally and documented before use.

## Verification Scripts

Run from the project root on Windows PowerShell:

```powershell
.\scripts\test-docker-compose-config.ps1
.\scripts\test-docker-compose-postgres.ps1
```

- `test-docker-compose-config.ps1` validates the Docker Compose model for the PostgreSQL service, named volume, network, and health check.
- `test-docker-compose-postgres.ps1` starts PostgreSQL with Docker Compose, waits for health, runs `pg_isready`, and executes a smoke SQL query.
