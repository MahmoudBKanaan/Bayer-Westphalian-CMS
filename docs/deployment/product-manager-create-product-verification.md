# Production Product Manager Create-Product Verification

**Sprint 18 item 749** requires an authorized Product Manager to create a product after deployment.

## Current Execution

Item 749 is **BLOCKED** because no production backend or HTTPS deployment is running. No Product
Manager credential was requested, and no product was created. Admin activity, direct SQL, mocked
tests, or a visible frontend button do not prove the role-specific deployed workflow.

## Safe Verification

Run after item 744 passes with a dedicated approved Product Manager smoke account:

```powershell
$productManagerCredential = Get-Credential -Message "Approved Product Manager smoke account"
.\scripts\test-production-product-manager-create-product.ps1 `
  -BaseUrl https://campaign.example.com `
  -ProductManagerCredential $productManagerCredential
$productManagerCredential = $null
```

The verifier authenticates an active `PRODUCT_MANAGER`, creates a uniquely named synthetic `OTHER`
product using harmless zero price and smoke-only metadata, requires HTTP 201 and a valid UUID, and
reads it back through the Product Manager-authorized API. The same Product Manager then disables and
soft-deletes the product. Failure cleanup repeats both safe lifecycle actions in `finally`.

Product create, deactivate, and delete operations are expected to create immutable audit events.
The script prints no credential, token, account email, product name, request payload, or response.
Record sanitized outcomes, release SHA/image digests, environment, product UUID only in controlled
audit evidence, request IDs, cleanup state, operator, and approver.

## Acceptance

Item 749 passes only when an active `PRODUCT_MANAGER` receives HTTP 201, the server assigns a valid
UUID and persists the expected product type/active state, a separate read succeeds, create audit
evidence exists, and the same role successfully disables and soft-deletes the synthetic product with
corresponding audit evidence. No Admin substitution is allowed for the capability under test.

Related documentation: [Product Manager Guide](../user-guides/product-manager-guide.md),
[Product Audit Logging](../modules/product-audit-logging.md),
[Production Smoke Checklist](production-smoke-test-checklist.md), and
[Incident Response](incident-response-notes.md).
