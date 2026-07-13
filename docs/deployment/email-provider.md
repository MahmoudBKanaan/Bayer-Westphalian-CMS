# Production Email Provider

**Sprint 18 item 726** - Configure a real email provider or disable sending until configured.

The repository's `SmtpEmailProvider` is a provider-ready placeholder and does not transmit email.
Therefore production uses the safe branch of the KB requirement: email sending is explicitly
disabled until a real, tested provider adapter replaces it.

## Current production state

- `PROVIDER_REAL_SENDING_ENABLED=false`
- `EMAIL_PROVIDER_MODE=disabled`
- `DisabledEmailProvider` is active only under the `prod` profile and always returns
  `EMAIL_SENDING_DISABLED` without network delivery.
- SMTP host, port, username, and password remain blank and unused.
- Compose and `application-prod.yml` default to the disabled state; development/test mock providers
  are not production beans.

Do not set `EMAIL_PROVIDER_MODE=mock` in production. Mock acceptance is not delivery evidence and
must never create a `SENT` production outcome.

## Enabling a future real provider

Real sending may be enabled only after all of the following are complete:

1. Replace the SMTP placeholder with an implementation that performs authenticated TLS delivery.
2. Add provider integration tests using a controlled SMTP test server and verify timeout/error
   handling, retry limits, unsubscribe behavior, consent, do-not-contact, and contact-event results.
3. Configure `SMTP_HOST`, valid `SMTP_PORT`, `SMTP_USERNAME`, and `SMTP_PASSWORD` through the
   deployment environment/secret manager.
4. Verify sender identity, SPF/DKIM/DMARC, provider quotas, bounce handling, and a non-customer test
   recipient.
5. Set `EMAIL_PROVIDER_MODE=smtp` and `PROVIDER_REAL_SENDING_ENABLED=true` only in the approved
   deployment.

Current startup validation intentionally rejects that enabled SMTP combination even when fields are
present because the installed adapter is still a placeholder. This prevents configuration from
turning a non-delivering adapter into apparent production sending.

## Incident response

If provider configuration becomes incomplete or suspect, set `PROVIDER_REAL_SENDING_ENABLED=false`
and `EMAIL_PROVIDER_MODE=disabled`, recreate the backend, preserve value-free logs/audit evidence,
and rotate `SMTP_PASSWORD` at the provider when compromise is possible.

Automated evidence: `DisabledEmailProviderTests`, `SecretPresenceValidatorTests`, and
`ProductionEmailProviderDocumentationTests`.
