# Communication Tracking Module

The communication tracking module manages customer contact history, email/SMS delivery retries, opt-out propagation on unsubscribe events, and adapter interfaces for message delivery providers.

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.communication
```

The module contains:
- `CommunicationService`: backend logic for recording milestones (sends, opens, clicks, replies, unsubscribes), creating sent/failed events from provider send results, and managing opt-out flags.
- `SendRetryService`: delivery retry orchestrator; maximum attempts from Admin System Settings
  (`SystemSettingsService.sendRetryLimit()`, item **536**).
- `EmailProvider` / `SmsProvider`: interfaces for message delivery adapters.
- `MockEmailProvider` / `MockSmsProvider`: dev/test mock implementations.
- `SmtpEmailProvider` / `ProviderReadySmsProvider`: production-ready placeholder adapters.
- `CommunicationController`: REST API endpoints under `/api/contact-events`.

## Contact Event Model

Communication milestones are recorded in the `contact_events` database table. The `ContactEvent` JPA entity maps to this table and represents a single communication instance.

### Critical test evidence (item 656)

**BR-034 / Sprint 16 item 656:** Contact events update analytics.

| Layer | Location |
| --- | --- |
| Backend critical suite | `ContactEventsUpdateAnalyticsTests` |
| Companion suite (item 450) | `EngagementCountsUpdateFromContactEventsTests` |
| Implementation | `CommunicationService.applyCampaignMetricsFromContactEvent` |
| Analytics | `AnalyticsService.getDashboard` aggregates `campaign_metrics` |
| Frontend catalog | `frontend/src/features/analytics/contactEventsUpdateAnalytics.ts` |
| KPI formulas | [kpi-definitions.md](kpi-definitions.md) |

Pipeline: record contact event → increment `campaign_metrics` (`SENT` / `OPENED` / `CLICKED` /
`REPLIED`; `CONVERTED` outcome) → dashboard KPIs and rates (**FR-103**–**FR-106**).

### Event Types
The event types are defined in `ContactEventType`:
- `SENT`: Message was successfully accepted by the provider.
- `FAILED`: Delivery attempt failed.
- `OPENED`: Customer opened the email.
- `CLICKED`: Customer clicked a tracked link.
- `REPLIED`: Customer replied to an SMS message.
- `UNSUBSCRIBED`: Customer opted out of marketing communications.

### Communication Channels
Defined in `CommunicationChannel`:
- `EMAIL`
- `SMS`
- `PHONE`
- `IN_APP`

## Send Result Event Recording

Outbound email and SMS sending must go through the communication service send-and-record workflow. The service delegates delivery to `SendRetryService` and records the final provider outcome:
- Accepted email or SMS result creates a `SENT` contact event with the provider message id when available.
- Final failed email or SMS result creates a `FAILED` contact event with failure code/message details.
- Send attempts are retried up to the Admin-configured retry limit (System Settings **send retry
  limit**, item 536; default/seed `app.contact.retry-limit`) before the final contact event is
  recorded. The limit is read at send time so settings changes apply without restart.

## Provider Adapter Architecture

Full environment and profile matrix: [Communication Provider Configuration](../communication-provider-configuration.md).

Delivery logic is decoupled from the campaign engine using replaceable adapter interfaces.

- **EmailProvider**: Defines `EmailDeliveryResult send(EmailMessage message)`.
- **SmsProvider**: Defines `SmsDeliveryResult send(SmsMessage message)`.

### Mock Providers
- `MockEmailProvider` and `MockSmsProvider` are enabled only under `dev` and `test` profiles with configuration mode `mock`.
- They store sent messages in memory for validation and do not perform real sending.

### Production Providers
- `SmtpEmailProvider` (for email mode `smtp`) and `ProviderReadySmsProvider` (for SMS mode `provider`) serve as production placeholders.
- Real delivery is disabled by default in these adapters unless explicitly enabled.

## Production Configuration Notes

### Sending Control
Real external sending is disabled by default in production. It is controlled by the property:
```properties
app.providers.real-sending-enabled=false
```
If real sending is disabled, production adapters return a failed result with error code `REAL_SENDING_DISABLED`.

### Configuring Providers
Before enabling real sending in production, the actual credentials must be configured:
- For SMTP: `app.providers.email.smtp-host`, `app.providers.email.smtp-port`, and `app.providers.email.smtp-username`.
- For SMS: `app.providers.sms.api-key`.
If these properties are not configured, attempting to send will return the error code `SMTP_PROVIDER_NOT_CONFIGURED` or `SMS_PROVIDER_NOT_CONFIGURED` even if `real-sending-enabled` is set to `true`.
