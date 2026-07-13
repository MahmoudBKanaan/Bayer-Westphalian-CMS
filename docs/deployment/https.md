# Production HTTPS

**Sprint 18 item 722** - Configure HTTPS.

Nginx terminates TLS for the production Compose stack. Public HTTP redirects permanently to HTTPS;
the only HTTP exception is the internal `/proxy-healthz` liveness endpoint used by Compose.

## TLS contract

- Public ports default to `80` and `443` and can be changed with `HTTP_PORT` / `HTTPS_PORT`.
- Nginx accepts TLS 1.2 and TLS 1.3 and disables session tickets.
- `TLS_CERTIFICATE_PATH` supplies the full certificate chain.
- `TLS_PRIVATE_KEY_PATH` supplies the matching private key.
- Compose requires both paths and mounts both files read-only. Certificates and private keys are
  never committed or baked into an image.
- HTTPS responses include HSTS for one year with subdomains.
- API requests receive `X-Forwarded-Proto: https` and `X-Forwarded-Port: 443`, allowing the
  backend `HttpsEnforcementFilter` to accept traffic while Spring Boot remains private HTTP inside
  the application network.

Use a certificate issued for the real production domain by a trusted CA. Self-signed certificates
are suitable only for isolated test environments where clients explicitly trust that test CA.

## Configure and verify

Set absolute host paths in the uncommitted deployment environment:

```dotenv
TLS_CERTIFICATE_PATH=/etc/letsencrypt/live/campaign.example.com/fullchain.pem
TLS_PRIVATE_KEY_PATH=/etc/letsencrypt/live/campaign.example.com/privkey.pem
```

Windows absolute paths are supported because Compose uses long bind-mount syntax. Use paths whose
files are shared with Docker Desktop and readable by its Linux VM.

Then validate and start the stack:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
curl.exe -I http://campaign.example.com/
curl.exe -I https://campaign.example.com/
curl.exe https://campaign.example.com/healthz
```

Expected evidence: HTTP returns `301` with an HTTPS location; HTTPS validates against the expected
hostname, serves the application, includes `Strict-Transport-Security`, and `/healthz` reports UP.
Certificate renewal must be monitored and followed by an Nginx reload or container recreation.

Automated static evidence: `ProductionHttpsConfigurationDocumentationTests`.

## Application Load Verification (Item 744)

After deployment, verify the public origin from a machine outside the deployment host:

```powershell
.\scripts\test-production-https.ps1 -BaseUrl https://campaign.example.com
```

The verifier rejects non-HTTPS origins and does not use `curl -k`, so an untrusted, expired, or
hostname-mismatched certificate fails. It requires HTTP 200 plus the React root at `/`, HSTS on the
HTTPS response, a permanent `301`/`307`/`308` redirect from HTTP to the exact HTTPS authority, and
HTTP 200 from HTTPS `/readyz`. It writes response data only to a temporary directory and removes it
in `finally`.

Current execution at `2026-07-13T00:02:04+03:00` is **BLOCKED**: no `bwc-production` Compose project
is running, only development PostgreSQL is present, and `https://localhost/` is unreachable with
curl exit 7. The reachable Vite server on HTTP port 5173 is not production HTTPS evidence. Rerun
against the approved deployed hostname and retain sanitized output, UTC time, release SHA/image
digests, certificate subject/expiry metadata, operator, and approver.
