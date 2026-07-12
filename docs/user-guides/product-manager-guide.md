# Product Manager User Guide

This guide describes the MVP workflows available to employees with the `PRODUCT_MANAGER` role.

## Scope

Product Managers maintain the insurance and investment product catalog used by campaigns,
segmentation, reminders, and analytics. They can create, edit, search, and disable products;
manage product details such as price, duration, and expiration rules; assign products to customers
through product ownership records; and create, update, and track product-change requests.

Product Managers can view customer profiles, campaigns linked to products, and product performance
reports for planning and catalog maintenance.

Product Managers cannot manage employee users, assign roles, reset passwords, launch campaigns,
approve compliance-controlled marketing campaigns, or manage payment records unless they also have
an additional authorized role.

## Dashboard Workflow

Product Managers can use the dashboard to:

- View product-related KPIs and catalog health indicators.
- Identify products requiring updates or change requests.
- Navigate to Products, Product Details, Product Change Requests, Campaigns, Analytics, and
  Reports.

## Product Catalog Workflows

Product Managers can use the Products area to:

- View the searchable product catalog.
- Search by term and filter by `productType` and `active` state.
- Create insurance and investment products with name, product type, description, price,
  `durationMonths`, and `expirationPolicy`.
- Edit product details, pricing, duration, expiration policy, and active state.
- Disable products without deleting historical records.
- Soft-delete products when catalog retirement is required.
- Open the product details page for a selected product.

Supported product types are `HOMEOWNER_INSURANCE`, `LIFE_INSURANCE`, `INVESTMENT_FUND`,
`HEALTH_INSURANCE`, `AUTO_INSURANCE`, and `OTHER`.

Product catalog APIs are exposed under `/api/products`. Catalog behavior is documented in the
[`Product Module Documentation`](../modules/product-module.md).

## Product Details Workflows

Product Managers can use the product details page to:

- Review full product metadata and active or disabled state.
- Edit price, duration, expiration rules, and status.
- Disable or soft-delete a product when it must be removed from active promotion.
- Create a product-change request directly from the selected product.
- Review recent change requests linked to the product.

## Product Ownership Workflows

Product Managers can assign products to customers so campaign targeting and reminder workflows
have accurate ownership and expiration data. Authorized Product Managers can use product ownership
workflows to:

- Assign a product to a customer with `startDate`, `expirationDate`, and optional `policyNumber`.
- List owned products on the customer profile product ownership tab.

Ownership assignment uses `POST /api/product-ownerships`. Ownership behavior is documented in the
[`Product Ownership Documentation`](../modules/product-ownership.md).

## Product Change Request Workflows

Product Managers can use the Product Change Requests area to:

- View product-change requests with optional `productId` and `status` filters.
- Create requests with `requestType` and `description`.
- Update the description of open requests.
- Track request status through `OPEN`, `APPROVED`, `REJECTED`, and `IMPLEMENTED`.
- Approve, reject, or mark implemented requests when the workflow is complete.

Supported request types are `PRICE_CHANGE`, `DURATION_CHANGE`, `EXPIRATION_RULE_CHANGE`, and
`STATUS_CHANGE`.

Product-change request APIs are exposed under `/api/product-change-requests`.

## Campaign And Analytics Visibility

Product Managers can use read-only campaign and analytics areas to:

- View campaigns linked to promoted products.
- Review product performance and reporting summaries needed for catalog decisions.
- Confirm which products are currently used in campaign planning without launching campaigns.

Product Managers cannot submit, approve, or launch campaigns. Campaign launch remains a Campaign
Manager workflow after Compliance Officer approval.

## Access And Error Handling

Backend authorization is authoritative. Frontend role-based controls improve usability, but the
backend must still enforce the `PRODUCT_MANAGER` permissions for every protected workflow.

Expected responses:

- Missing authentication returns an unauthorized response.
- Authenticated users without the correct role receive `403 Forbidden`.
- Validation failures return backend validation errors.
- Attempts to create or mutate products without `PRODUCT_MANAGER` or `ADMIN` authorization must be
  rejected by the backend.

Unauthorized roles cannot create products. Product Managers cannot launch campaigns
(Sprint 16 critical item **653** / TC-013; `ProductManagerCannotLaunchCampaignsTests`).

## Audit Expectations

Product create, edit, disable, soft-delete, ownership assignment, ownership update, and
product-change request workflows are auditable. Product Managers can trigger product and
product-change audit entries. User administration and campaign approval audit entries remain
Admin or Compliance Officer workflows.

Product audit behavior is documented in the
[`Product Audit Logging Documentation`](../modules/product-audit-logging.md).

## KB Traceability

This guide preserves the KB Product Manager expectations:

- Role description: manage insurance and investment products and product-change requests.
- Allowed functions: create, edit, and disable products; manage product details; create
  product-change requests; view product performance.
- Screens: Dashboard, Products, Product Details, Product Change Requests, Campaigns, Analytics,
  and Reports.
- `FR-040`: Users can view products.
- `FR-041`: Product Manager or Admin can create products.
- `FR-042`: Product Manager or Admin can edit products.
- `FR-043`: Product Manager or Admin can disable or delete products.
- `FR-044`: Users can search products.
- `FR-045`: Product Manager can create product-change requests.
- `TC-010`: Product Manager can create a product.
- `TC-013` / item **653**: Product Manager cannot launch campaigns.
