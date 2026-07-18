# Communication Provider Configuration

KB configuration guide for **optional external provider adapters** (email and SMS) used by the
Bayer-Westphalian Campaign Management Platform.

The knowledge base treats email/SMS gateways as **replaceable adapters**: real SMTP or SMS vendor
integration is optional until production integration is approved. Mock adapters are allowed only for
**development, testing, and demonstration**. Production must not treat mock acceptance as customer
delivery evidence, and core compliance (consent, opt-out, eligibility, approval, audit, permissions,
contact history) must remain real.

| Related deep-dive | Path |
| --- | --- |
| Module behavior & contact events | [modules/communication-tracking.md](modules/communication-tracking.md) |
| Production email gate (item **726**) | [deployment/email-provider.md](deployment/email-provider.md) |
| Production SMS gate (item **727**) | [deployment/sms-provider.md](deployment/sms-provider.md) |
| Environment variable catalog | [deployment/environment-variables.md](deployment/environment-variables.md) |
| Secrets handling | [deployment/secrets.md](deployment/secrets.md) |

## Package and adapters

Primary package:

```text
com.bayerwestphalian.campaign.communication
```

| Interface / class | Role |
| --- | --- |
| `EmailProvider` / `SmsProvider` | Delivery adapter contracts |
| `MockEmailProvider` / `MockSmsProvider` | In-memory mocks (`mode=mock`, non-prod profiles) |
| `SmtpEmailProvider` | SMTP-ready **placeholder** (`mode=smtp`) — does not transmit until a real adapter replaces it |
| `ProviderReadySmsProvider` | Vendor-ready **placeholder** (`mode=provider`) — does not transmit until a real adapter replaces it |
| `DisabledEmailProvider` / `DisabledSmsProvider` | Explicit production-safe no-send (`mode=disabled`, `prod` profile) |
| `SendRetryService` | Retries sends up to Admin System Settings **send retry limit** (KB item **536**) |
| `CommunicationService` | Send-and-record workflow → `contact_events` (`SENT` / `FAILED` and engagement types) |

Spring selects the active bean with `@ConditionalOnProperty` on `app.providers.email.mode` /
`app.providers.sms.mode` (and profile constraints for disabled/mock beans).

## Configuration properties

Spring binds environment variables into `app.providers.*` (see `application.yml` /
`application-prod.yml`).

| Environment variable | Spring property | Purpose | Default (dev) | Default (prod) |
| --- | --- | --- | --- | --- |
| `PROVIDER_REAL_SENDING_ENABLED` | `app.providers.real-sending-enabled` | Master switch for real external delivery | `false` | `false` |
| `EMAIL_PROVIDER_MODE` | `app.providers.email.mode` | `mock` \| `smtp` \| `disabled` | `mock` | `disabled` |
| `SMTP_HOST` | `app.providers.email.smtp-host` | SMTP host (when mode is `smtp`) | empty | empty |
| `SMTP_PORT` | `app.providers.email.smtp-port` | SMTP port | `587` | `587` |
| `SMTP_USERNAME` | `app.providers.email.smtp-username` | SMTP username | empty | empty |
| `SMTP_PASSWORD` | `app.providers.email.smtp-password` | SMTP secret | empty | empty |
| `SMS_PROVIDER_MODE` | `app.providers.sms.mode` | `mock` \| `provider` \| `disabled` | `mock` | `disabled` |
| `SMS_API_KEY` | `app.providers.sms.api-key` | SMS vendor API key | empty | empty |
| `CONTACT_RETRY_LIMIT` | `app.contact.retry-limit` | Seed/default for send retries (Admin may override via system settings) | `3` | profile-dependent |

**Never commit real** `SMTP_PASSWORD` or `SMS_API_KEY`. Use the secret manager or deployment
environment. Templates: `backend/.env.example`, root `.env.example`, `.env.production.example`.

## Profile matrix (KB-aligned)

| Profile | Email mode | SMS mode | Real sending | Behavior |
| --- | --- | --- | --- | --- |
| `dev` / `test` | `mock` (default) | `mock` (default) | `false` | Mocks accept messages in memory for demos and automated tests. Not proof of delivery. |
| `prod` | `disabled` (default) | `disabled` (default) | `false` | `Disabled*Provider` returns `EMAIL_SENDING_DISABLED` / `SMS_SENDING_DISABLED`. No network send. |
| Future prod (after real adapters) | `smtp` (only when implemented) | `provider` (only when implemented) | `true` only after approval | Real TLS/API delivery; secrets required; integration tests must pass. |

### Rules that must always hold

1. **Mock is not production delivery.** Do not set `EMAIL_PROVIDER_MODE=mock` or
   `SMS_PROVIDER_MODE=mock` in production. Mock acceptance must not be presented as a production
   `SENT` customer outcome for compliance demos of external channels.
2. **Placeholders cannot be enabled as real senders.** `SmtpEmailProvider` and
   `ProviderReadySmsProvider` remain non-delivering. Startup validation via
   `SecretPresenceValidator` **rejects** enabling `PROVIDER_REAL_SENDING_ENABLED=true` with
   `EMAIL_PROVIDER_MODE=smtp` or `SMS_PROVIDER_MODE=provider` until the placeholders are replaced
   by real delivery implementations (see items **726** / **727**).
3. **Disabled is the safe production default.** Prefer explicit disable over leaving mock beans
   active when profile is `prod`.
4. **Consent and contact history stay real.** Provider adapters only affect external transport.
   Contact events, opt-outs, eligibility, campaign approval, and audit remain implemented business
   logic regardless of mock/placeholder/disabled mode.

## Local development setup

```powershell
# From repository root — templates already default to mock + real sending off
Copy-Item backend\.env.example backend\.env
# Ensure:
#   PROVIDER_REAL_SENDING_ENABLED=false
#   EMAIL_PROVIDER_MODE=mock
#   SMS_PROVIDER_MODE=mock
```

Start PostgreSQL and the backend with the `dev` profile (see
[Developer Setup](development/developer-setup.md)). Outbound send APIs will use mock providers and
still record contact events through `CommunicationService` / `SendRetryService`.

## Production setup (current repository state)

1. Leave `PROVIDER_REAL_SENDING_ENABLED=false`.
2. Leave `EMAIL_PROVIDER_MODE=disabled` and `SMS_PROVIDER_MODE=disabled`.
3. Leave `SMTP_*` and `SMS_API_KEY` empty unless preparing a future real integration (secrets only in
   the secret manager).
4. Confirm [production email](deployment/email-provider.md) and [SMS](deployment/sms-provider.md)
   checklists before any enablement attempt.

Compose and `application-prod.yml` already default to the disabled state.

## Enabling a future real provider (checklist)

Do **not** flip environment flags alone. Complete both channels’ production guides:

1. Implement authenticated delivery (SMTP TLS and/or SMS HTTPS API) with timeouts and error mapping.
2. Add controlled integration tests (success, failure, retries, consent, do-not-contact, unsubscribe).
3. Configure secrets via secret manager; verify SPF/DKIM/DMARC (email) and sender identity (SMS).
4. Send only to approved non-customer test recipients and keep provider-side delivery evidence.
5. Only then set modes + `PROVIDER_REAL_SENDING_ENABLED=true` in the approved deployment, after
   removing the startup rejection that currently blocks placeholder “enablement.”

## Send path and contact events

Outbound email/SMS must go through the communication **send-and-record** path:

1. `CommunicationService` validates context and delegates to `SendRetryService`.
2. `SendRetryService` calls the active `EmailProvider` / `SmsProvider` up to the configured retry
   limit (Admin system settings **send retry limit**).
3. Final **accepted** result → `contact_events` with type `SENT` (provider message id in notes when
   present).
4. Final **failed** result → `contact_events` with type `FAILED` (failure code/message).
5. Engagement types (`OPENED`, `CLICKED`, `REPLIED`, …) are recorded separately and update campaign
   metrics (KB **BR-034**).

Error codes operators may see from adapters:

| Code | Meaning |
| --- | --- |
| `EMAIL_SENDING_DISABLED` / `SMS_SENDING_DISABLED` | Explicit production disabled mode |
| `REAL_SENDING_DISABLED` | Placeholder selected but master switch is off |
| `SMTP_PROVIDER_NOT_CONFIGURED` / `SMS_PROVIDER_NOT_CONFIGURED` | Placeholder still non-delivering |
| `INVALID_*` | Message validation failed before transport |

## Incident response

If credentials, quotas, or provider status are uncertain:

1. Set `PROVIDER_REAL_SENDING_ENABLED=false`.
2. Set `EMAIL_PROVIDER_MODE=disabled` and `SMS_PROVIDER_MODE=disabled`.
3. Recreate/restart the backend with the safe configuration.
4. Rotate `SMTP_PASSWORD` / `SMS_API_KEY` at the vendor when compromise is possible.
5. Preserve **value-free** logs and audit evidence (never paste secrets into tickets).

## Automated evidence

| Area | Tests / artifacts |
| --- | --- |
| Disabled production email | `DisabledEmailProviderTests`, `ProductionEmailProviderDocumentationTests` |
| Disabled production SMS | `DisabledSmsProviderTests`, `ProductionSmsProviderDocumentationTests` |
| Secret / enablement gate | `SecretPresenceValidatorTests` |
| Retry orchestration | `SendRetryServiceTests`, `ConfigurableSendRetryLimitTests` |
| Contact events → analytics | `ContactEventsUpdateAnalyticsTests`, `EngagementCountsUpdateFromContactEventsTests` |
| This guide | `CommunicationProviderConfigurationDocumentationTests` |

## Traceability

| KB / backlog theme | How this guide maps |
| --- | --- |
| Optional provider adapters (email, SMS) | Adapter interfaces + env mode selection |
| Mock only for dev/test | Profile matrix; production defaults to `disabled` |
| Real SMTP / real SMS provider integration | Placeholder classes + enablement checklist; deep dives **726** / **727** |
| Contact history & outcomes | Send-and-record path; module doc |
| Secrets never committed | Env catalog + secrets doc; validator gates |

See also [Communication Tracking Module](modules/communication-tracking.md) for event types, API
surface, and analytics pipeline.
