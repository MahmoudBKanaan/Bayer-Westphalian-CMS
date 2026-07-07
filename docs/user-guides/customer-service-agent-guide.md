# Customer Service Agent User Guide

This guide describes the MVP workflows available to employees with the
`CUSTOMER_SERVICE_AGENT` role.

## Scope

Customer Service Agents support day-to-day customer and prospect profile maintenance. They can
search customer records, view customer details, create and update customer profiles, import
customers from CSV, and manage beneficiary relationship links.

Customer Service Agents can import customers from CSV when customer and prospect records need to
be loaded in bulk.

Customer Service Agents cannot manage employee users, assign roles, reset passwords, or
soft-delete customer profiles unless they also have an additional authorized role.

## Customer Workflows

Customer Service Agents can use the Customers area to:

- View the paginated customer and prospect list.
- Search by name, email, phone, city, country, and source.
- Filter by customer type, customer status, city, country, and contactability.
- Open the customer profile details page.
- Create a customer or prospect profile.
- Update customer contact details, demographics, status, source, and `doNotContact`.
- Import customers and prospects from CSV.

Soft delete is restricted to `ADMIN`. If a Customer Service Agent tries to delete a customer, the
backend must return a forbidden response.

## Beneficiary Workflows

Customer Service Agents can use the customer details beneficiaries tab to:

- Link a beneficiary customer to a policyholder customer.
- Update the beneficiary relationship.
- Add or update guardian name and guardian email.
- Save the `guardianConsentRequired` flag.
- Delete a beneficiary relationship link.

The policyholder and beneficiary must be different existing customer records. Duplicate
beneficiary links are rejected.

Duplicate beneficiary links are rejected.

## Consent Workflows

Customer Service Agents can use the customer details consent tab to:

- View the current consent status for each consent record.
- Review whether each consent record is valid or requires action.
- See consent type, purpose, source, granted date, withdrawn date, expiration date, evidence URL,
  and recorder information.
- Record new consent with type, status, purpose, source, and evidence.
- Mark marketing opt-outs and withdraw consent when required.
- Update the customer `doNotContact` override from the same compliance area.

The consent tab must display `GIVEN`, `REQUIRED`, `WITHDRAWN`, `EXPIRED`, and `REJECTED`
statuses using clear readable labels.

## CSV Import

Customer Service Agents can import customer and prospect records through
`POST /api/customers/import`.
The CSV format is documented in the
[`Customer CSV Import Guide`](../admin/customer-csv-import-guide.md).

Valid rows are imported. Invalid rows are rejected and returned as row-level errors with
`lineNumber`, `field`, `message`, and `value`.

## Access And Error Handling

Backend authorization is authoritative. Frontend role-based controls improve usability, but the
backend must still enforce the `CUSTOMER_SERVICE_AGENT` permissions for every protected workflow.

Expected responses:

- Missing authentication returns an unauthorized response.
- Authenticated users without the correct role receive `403 Forbidden`.
- Validation failures return backend validation errors.
- CSV row failures return row-level import errors while preserving valid rows.

## Audit Expectations

Customer creation, customer update, and customer soft-delete workflows are auditable. Customer
Service Agents can trigger customer creation and update audit entries. Role assignment and user
administration audit entries remain Admin workflows.
