# User-Story Map Evidence

| Activity | Discover / prepare | Perform | Control / learn |
| --- | --- | --- | --- |
| Access | log in | use role-aware navigation | lock/disable accounts; audit access |
| Customer management | search or create customer | maintain profile, beneficiary and ownership | review contact, payment and consent history |
| Consent | inspect channel status | record or withdraw consent and evidence | enforce opt-out, DNC and guardian rules |
| Products | browse catalog | create/update product and ownership | approve change workflow; monitor expiration/payment |
| Segmentation | define criteria | preview and save reusable segment | show exclusions; enforce `EligibilityService` |
| Campaigns | choose product, segment and objective | draft copy, schedule and submit | human compliance approval; controlled launch |
| Communications | inspect recipient snapshot | record event/send through configured adapter | retries, duplicate/contact-limit controls and audit |
| Follow-up/reminders | identify due work | assign/complete task or process reminder | escalation levels and scheduler logs |
| Insight | select dashboard/report scope | view/export KPIs | explain AI recommendations and preserve audit trail |
| Operations | configure environment | deploy and monitor | backup/restore, smoke, rollback and incident response |

The walking skeleton is login -> customer -> consent -> product -> segment -> campaign -> approval ->
launch -> analytics. Detailed acceptance evidence is mapped in `docs/testing` and role procedures in
`docs/user-guides`.
