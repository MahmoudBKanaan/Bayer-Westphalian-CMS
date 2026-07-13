# Production CORS

**Sprint 18 item 723** - Configure production CORS.

The backend requires `CORS_ALLOWED_ORIGINS` whenever the `prod` profile is active. Both startup
validation and Spring Security use the same `ProductionCorsOrigins` rules, so unsafe configuration
fails before traffic is served.

## Allowed format

Supply a comma-separated allow-list of exact frontend origins:

```dotenv
CORS_ALLOWED_ORIGINS=https://campaign.example.com,https://www.campaign.example.com
```

Every entry must:

- Use `https://`.
- Include an explicit hostname and optional port.
- Represent an origin only, with no path, trailing slash, query string, fragment, or user info.
- Avoid wildcards, `localhost`, loopback addresses, and blank entries.

Do not include `/api`; CORS compares the browser page origin, not an API endpoint URL. For the
same-origin production frontend, configure the public HTTPS site origin even though ordinary API
requests do not require a cross-origin grant.

## Runtime policy

Spring Security allows `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, and `OPTIONS`. Request headers are
limited to `Authorization`, `Content-Type`, and `X-Request-Id`; only `X-Request-Id` is exposed.
Credentials are enabled only alongside the explicit origin list, and successful preflight results
may be cached for 3600 seconds.

Changing the list requires a backend restart. Verify allowed and denied origins without sending
credentials in shell history:

```powershell
curl.exe -i -X OPTIONS https://campaign.example.com/api/health `
  -H "Origin: https://campaign.example.com" `
  -H "Access-Control-Request-Method: GET"

curl.exe -i -X OPTIONS https://campaign.example.com/api/health `
  -H "Origin: https://unapproved.example.com" `
  -H "Access-Control-Request-Method: GET"
```

The first response should include the configured `Access-Control-Allow-Origin`; the second must not
grant the unapproved origin.

Automated evidence: `ProductionCorsConfigurationTests`, `EnvironmentVariableValidatorTests`, and
`ProductionCorsDeploymentDocumentationTests`.
