# Reverse Proxy and HTTPS Configuration

**Sprint 18 items 721–722 / deployment evidence DEP-02 (config note).**

This note records how the Bayer-Westphalian Campaign Management Platform terminates TLS and
routes public traffic in the **production Compose model**. It is intended as screenshot evidence
when a **trusted public domain certificate** is not yet available for browser padlock capture.

> **Honesty rule:** A self-signed `https://localhost/` session is **not** production HTTPS
> evidence. Do not claim “secure production TLS” from local PEMs. Use this configuration note
> (plus `docker/nginx/nginx.conf` and [https.md](https.md)) until a real hostname with a CA-issued
> certificate is deployed.

## Production topology (Compose)

| Item | Production value |
| --- | --- |
| Production proxy | **Nginx** `1.27-alpine` service `reverse-proxy` in `docker-compose.prod.yml` |
| Public HTTP port | Host `HTTP_PORT` (default **80**) → container `8080` |
| Public HTTPS port | Host `HTTPS_PORT` (default **443**) → container `8443` |
| Frontend (SPA) | `https://campaign.example.com/` → upstream `frontend:80` |
| Backend API | `https://campaign.example.com/api/` → upstream `backend:8080` (prefix kept) |
| Readiness | `https://campaign.example.com/readyz` → `backend:8080/actuator/health/readiness` |
| Liveness | `https://campaign.example.com/livez` → `backend:8080/actuator/health/liveness` |
| Proxy process health | `/proxy-healthz` (Nginx local response; used by Compose healthcheck) |

PostgreSQL, backend, and frontend **do not** publish application ports to the host in production.
Only the reverse proxy is public.

## HTTP behavior

- Plain HTTP on the public edge is accepted only for:
  - Compose/internal **proxy liveness** (`/proxy-healthz`), and
  - **Permanent redirect** of all other paths to HTTPS (`301 https://$host$request_uri`).
- Application user traffic is expected on **HTTPS**.
- Backend `HTTPS_REQUIRED=true` (prod default) accepts traffic that is secure or carries
  `X-Forwarded-Proto: https` from this trusted proxy.

## TLS

| Concern | Production configuration |
| --- | --- |
| Termination | Nginx terminates TLS (not Spring Boot) |
| Certificate mount | `TLS_CERTIFICATE_PATH` → `/etc/nginx/tls/fullchain.pem` (read-only) |
| Private key mount | `TLS_PRIVATE_KEY_PATH` → `/etc/nginx/tls/privkey.pem` (read-only) |
| Protocols | TLS **1.2** and **1.3** |
| HSTS | `Strict-Transport-Security: max-age=31536000; includeSubDomains` on HTTPS responses |
| Certificate source | Operator-supplied PEMs from a trusted CA (or ACME via host tooling); **not** baked into images |
| Local screenshot PEMs | Self-signed files under `docker/tls/` are **dev/evidence only** |

Renew certificates on the host, then reload/recreate `reverse-proxy`. See [https.md](https.md) and
[docker/nginx/tls/README.md](../../docker/nginx/tls/README.md).

## CORS

| Environment | Rule |
| --- | --- |
| Production | `CORS_ALLOWED_ORIGINS` must list explicit **`https://`** origins only (e.g. `https://campaign.example.com`) |
| Forbidden in prod | `*`, `localhost`, `127.0.0.1`, plain `http://` origins |
| Same-origin SPA | Frontend is built with `VITE_API_BASE_URL=/api`, so browser calls to the API are same-origin through the proxy |

Detail: [production-cors.md](production-cors.md).

## Local development (not production HTTPS evidence)

| Mode | Transport | Evidence value |
| --- | --- | --- |
| Vite + local Spring Boot | HTTP `http://localhost:5173` → API `http://localhost:8080` | Development only |
| Prod-profile backend overlay | Direct HTTP on host `:8080` with `HTTPS_REQUIRED` relaxed | API testing only |
| Full prod Compose + self-signed | `https://localhost/` with browser warning | Proves routing/proxy wiring **only**, not public CA TLS |

The local environment **does not claim live production HTTPS evidence**.

## Actual production Nginx configuration

Source of truth: [`docker/nginx/nginx.conf`](../../docker/nginx/nginx.conf) (Sprint 18 items **721–722**).

```nginx
# Production reverse proxy and HTTPS termination - Sprint 18 items 721-722.
worker_processes auto;
pid /tmp/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    server_tokens off;
    sendfile on;
    keepalive_timeout 65;
    client_max_body_size 20m;

    proxy_connect_timeout 10s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;

    upstream frontend_upstream {
        server frontend:80;
        keepalive 16;
    }

    upstream backend_upstream {
        server backend:8080;
        keepalive 16;
    }

    # Internal/plain HTTP listener: health for Compose, redirect everything else to HTTPS.
    server {
        listen 8080;
        server_name _;

        location = /proxy-healthz {
            access_log off;
            default_type text/plain;
            return 200 "ok\n";
        }

        location / {
            return 301 https://$host$request_uri;
        }
    }

    server {
        listen 8443 ssl;
        server_name _;

        ssl_certificate /etc/nginx/tls/fullchain.pem;
        ssl_certificate_key /etc/nginx/tls/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_session_cache shared:TLS:10m;
        ssl_session_timeout 1d;
        ssl_session_tickets off;
        ssl_prefer_server_ciphers off;

        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "DENY" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

        location = /proxy-healthz {
            access_log off;
            default_type text/plain;
            return 200 "ok\n";
        }

        location = /healthz {
            proxy_pass http://backend_upstream/actuator/health/readiness;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port 443;
        }

        location = /readyz {
            proxy_pass http://backend_upstream/actuator/health/readiness;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port 443;
        }

        location = /livez {
            proxy_pass http://backend_upstream/actuator/health/liveness;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port 443;
        }

        location /api/ {
            proxy_pass http://backend_upstream;
            proxy_http_version 1.1;
            proxy_set_header Connection "";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port 443;
        }

        location / {
            proxy_pass http://frontend_upstream;
            proxy_http_version 1.1;
            proxy_set_header Connection "";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header X-Forwarded-Host $host;
        }
    }
}
```

Compose mounts this file and TLS PEMs into the `reverse-proxy` service (see
`docker-compose.prod.yml`).

## Optional Caddy shape (illustrative / non-Compose production alternative)

The repository includes a **local/placeholder** Caddyfile at
[`docker/caddy/Caddyfile`](../../docker/caddy/Caddyfile). The **production Compose stack ships
Nginx**, not Caddy. A production-style Caddy domain block would look like:

```caddy
campaign.example.com {
    encode gzip zstd

    handle /api/* {
        reverse_proxy backend:8080 {
            header_up X-Forwarded-Proto {scheme}
            header_up X-Forwarded-Host {host}
        }
    }

    handle /actuator/* {
        reverse_proxy backend:8080 {
            header_up X-Forwarded-Proto {scheme}
            header_up X-Forwarded-Host {host}
        }
    }

    handle {
        reverse_proxy frontend:80
    }
}
```

Do **not** present this Caddy block as the running Compose production proxy unless Caddy is
actually deployed. Prefer the Nginx configuration above for Sprint 18 item **721–722** evidence.

## Operator verification (no secrets)

```powershell
# Compose model
docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.production -f docker-compose.prod.yml ps reverse-proxy

# Against a real public hostname (trusted cert — production evidence)
curl.exe -I https://campaign.example.com/
curl.exe -i https://campaign.example.com/readyz
.\scripts\test-production-https.ps1 -BaseUrl https://campaign.example.com

# Local self-signed only (routing proof; not CA production evidence)
curl.exe -k -I https://localhost/
curl.exe -k -i https://localhost/readyz
```

## Screenshot guidance (item 743 / DEP-02)

| Situation | Capture | Suggested filename |
| --- | --- | --- |
| Real deployed domain + trusted cert | Browser padlock + URL + app shell | `03-reverse-proxy-https.png` |
| Local / no public CA yet | This document open in the editor (title + Production proxy/TLS/CORS + Nginx block) | `03-reverse-proxy-https-config-note.png` |

Never screenshot `.env.production`, private keys, or full certificate PEMs.

## Related documentation

- [Production Reverse Proxy](reverse-proxy.md) (item **721**)
- [Production HTTPS](https.md) (item **722**)
- [Production CORS](production-cors.md) (item **723**)
- [Production Security Configuration](../security/production-security-configuration.md) §6
- [Deployment Screenshot Evidence](deployment-screenshot-evidence.md) (item **743**)
