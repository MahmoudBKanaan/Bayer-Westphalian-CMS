# Production SMS Provider

**Sprint 18 item 727** - Configure a real SMS provider or disable sending until configured.

The repository's `ProviderReadySmsProvider` is a provider placeholder and does not transmit SMS.
Production therefore uses the safe branch of the KB requirement: SMS sending is explicitly
disabled until a real, tested provider adapter replaces it.

## Current production state

- `PROVIDER_REAL_SENDING_ENABLED=false`
- `SMS_PROVIDER_MODE=disabled`
- `DisabledSmsProvider` is active only under the `prod` profile and always returns
  `SMS_SENDING_DISABLED` without network delivery.
- `SMS_API_KEY` remains blank and unused.
- Compose and `application-prod.yml` default to disabled; the development/test mock provider is not
  a production bean.

Do not set `SMS_PROVIDER_MODE=mock` in production. Mock acceptance is not evidence that an SMS was
delivered and must never create a production `SENT` outcome.

## Enabling a future real provider

1. Replace the provider placeholder with an implementation using the chosen vendor's authenticated
   HTTPS API and bounded connection/read timeouts.
2. Add controlled provider integration tests for delivery IDs, failures, retries, rate limits,
   unsubscribe/do-not-contact, consent, and international phone formatting.
3. Store `SMS_API_KEY` in the deployment secret manager and configure sender identity, region,
   quota, and callback/webhook verification.
4. Send only to an approved non-customer test number and verify provider delivery evidence.
5. Set `SMS_PROVIDER_MODE=provider` and `PROVIDER_REAL_SENDING_ENABLED=true` only after approval.

Current startup validation intentionally rejects that enabled provider combination even with an API
key because the installed adapter remains non-delivering. This prevents configuration alone from
misrepresenting a placeholder as a real SMS channel.

## Incident response

Set `PROVIDER_REAL_SENDING_ENABLED=false` and `SMS_PROVIDER_MODE=disabled` whenever provider status
or credentials are uncertain. Recreate the backend, rotate `SMS_API_KEY` at the provider when
compromise is possible, and retain only value-free logs and audit evidence.

Automated evidence: `DisabledSmsProviderTests`, `SecretPresenceValidatorTests`, and
`ProductionSmsProviderDocumentationTests`.
