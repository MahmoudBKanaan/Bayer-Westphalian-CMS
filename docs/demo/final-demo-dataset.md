# Final Demo Dataset

**Item 781** prepares the deterministic synthetic dataset for the final Bayer-Westphalian Campaign
Management Platform demonstration. It is loaded only by the `dev` and `test` Flyway locations from
`backend/src/main/resources/db/demo/R__controlled_demo_data.sql`. Production configuration must not
include `classpath:db/demo`.

## Safety and repeatability

- All customer contact data is synthetic and uses the reserved `example.test` domain.
- Fixed valid UUIDs make screen navigation, evidence, and the demo script repeatable.
- The repeatable Flyway script uses conflict-safe upserts so startup restores anchor values.
- Real provider sending is not required and must remain disabled during the demo.
- No production customer, credential, token, consent file, dump, or provider key belongs here.
- Demo accounts from versioned migrations use `.test` identities and are quarantined/disabled by
  the production Admin bootstrap process.

The machine-readable scenario/anchor manifest is
[`config/final-demo-dataset.json`](../../config/final-demo-dataset.json). It contains IDs and scenario
names, not passwords.

## Included scenarios

| Scenario | Prepared evidence |
| --- | --- |
| Role navigation | Seeded role accounts for Admin, Campaign Manager, BI Analyst, Product Manager, Compliance Officer, Customer Service Agent, and extended roles |
| Customer and beneficiary | Policyholder, beneficiary relationship, prospects, guardian cases, multiple locations/statuses |
| Consent and exclusion | Given/withdrawn/rejected/expired consent, opt-out, do-not-contact, guardian and eligibility examples |
| Products/payments | Products, ownership, payment status/history, expiration context |
| Segmentation | Reusable segment with deterministic UUID and criteria for preview |
| Campaign | Draft/review-ready campaign data, products, segment, eligible/excluded recipients and reasons |
| Communication work | Contact events, follow-up task, and reminder schedule |
| Insights | Campaign metrics, report-export history, and explainable AI recommendation |
| Audit | Synthetic `LOAD_DEMO_DATA` audit event plus normal workflow audit history |

## Anchor records

| Record | UUID |
| --- | --- |
| Policyholder Anna Keller | `20000000-0000-0000-0000-000000000101` |
| Eligible beneficiary Lena Keller | `20000000-0000-0000-0000-000000000102` |
| Do-not-contact prospect Jonas Weber | `20000000-0000-0000-0000-000000000103` |
| Demo product | `30000000-0000-0000-0000-000000000101` |
| Demo segment | `40000000-0000-0000-0000-000000000101` |
| Demo campaign | `50000000-0000-0000-0000-000000000101` |
| Payment record | `32000000-0000-0000-0000-000000000101` |
| Contact event | `52000000-0000-0000-0000-000000000101` |
| Follow-up | `53000000-0000-0000-0000-000000000101` |
| Reminder | `54000000-0000-0000-0000-000000000101` |
| AI recommendation | `57000000-0000-0000-0000-000000000101` |

## Load or reset locally

Start a clean local database and the backend with `dev`:

```powershell
docker compose down -v
docker compose up -d postgres
Set-Location backend
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

`docker compose down -v` destroys local data and is appropriate only for an intentional disposable
demo reset. It is never a production command. On an existing dev database, Flyway reruns the
repeatable seed when its checksum changes; the upserts restore anchor records.

## Verify

After Flyway startup completes, run from the repository root:

```powershell
.\scripts\verify-final-demo-dataset.ps1
```

The verifier is read-only. It checks the deterministic anchors across users, customers,
beneficiaries, consent, products, ownership, payment, segment, campaign/recipients, contacts,
follow-ups, reminders, metrics, reports, AI, and audit. It prints only group status/counts and no
customer rows or credentials.

## Demo hygiene

Use the prepared anchors for read-only walkthroughs. If the final demo creates or changes records,
prefix them with `DEMO-<UTC timestamp>` and use only synthetic destinations. Reset the disposable
database afterward or clean up through audited application workflows. Do not present local/dev data
or screenshots as production release evidence.

Automated evidence: `FinalDemoDatasetDocumentationTests` plus the existing Flyway demo migration
resource and integration contracts.
