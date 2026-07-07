# Customer CSV Import Guide

Customer CSV import supports controlled loading of customers and prospects for the MVP.
It is intended for authorized customer-management users and keeps backend validation as the
authoritative rule set.

## Endpoint

`POST /api/customers/import`

The request must use multipart form data with a `file` part containing a CSV document.
The frontend must let the browser set the multipart boundary and must not force a manual
`Content-Type` header.

## Required Headers

The CSV file must include these headers:

```text
customer_type,first_name,last_name,email,phone,address_line,city,country,date_of_birth,age_group,status,do_not_contact,source
```

The backend rejects files that do not include the required header set. Header names are matched
case-insensitively after trimming whitespace.

## CSV Format

Files must be UTF-8 encoded comma-separated values. The first non-empty line is treated as the
header row. Blank lines are ignored. Fields may be quoted with double quotes, and escaped double
quotes inside a quoted field must be written as two double quotes.

| Field | Required | Format |
| --- | --- | --- |
| `customer_type` | Yes | `CUSTOMER`, `PROSPECT`, or `BENEFICIARY`. |
| `first_name` | Yes | Text, maximum 100 characters. |
| `last_name` | Yes | Text, maximum 100 characters. |
| `email` | No | Valid email address, maximum 255 characters. |
| `phone` | No | Optional leading `+`, digits, spaces, parentheses, or hyphens; 7 to 50 characters. |
| `address_line` | No | Text, maximum 255 characters. |
| `city` | No | Text, maximum 100 characters. |
| `country` | No | Text, maximum 100 characters. |
| `date_of_birth` | No | ISO date format `yyyy-MM-dd`; future dates are rejected. |
| `age_group` | No | `MINOR`, `AGE_18_25`, `AGE_26_40`, `AGE_41_60`, `AGE_60_PLUS`, `18_25`, `26_40`, `41_60`, or `60_PLUS`. |
| `status` | No | `ACTIVE`, `INACTIVE`, `INTERESTED`, `UNINTERESTED`, or `CONVERTED`; blank defaults to `ACTIVE`. |
| `do_not_contact` | No | `true` or `false`; blank defaults to `false`. |
| `source` | No | Text, maximum 100 characters. |

Example file:

```csv
customer_type,first_name,last_name,email,phone,address_line,city,country,date_of_birth,age_group,status,do_not_contact,source
CUSTOMER,Ada,Policyholder,ada@bayer-westphalian.test,+49-555-0100,Insurance Street 1,Berlin,Germany,1984-08-21,41_60,ACTIVE,false,LIFE_INSURANCE_BENEFICIARY
PROSPECT,Ben,Prospect,ben@bayer-westphalian.test,+49-555-0200,,Munich,Germany,,AGE_26_40,INTERESTED,true,CSV_IMPORT
```

## Row Validation

Rows are processed independently.

- Valid rows are imported and returned in `customers`.
- Invalid rows are rejected and do not create customer records.
- A file can partially succeed when at least one row is valid and another row is invalid.
- The response includes `importedCount`, `failedCount`, `customers`, and `errors`.

Each row-level error includes:

- `lineNumber`: source CSV line number for the rejected row.
- `field`: CSV field that failed validation.
- `message`: validation reason.
- `value`: submitted value that failed validation.

Validation errors are reported for required fields and invalid formats or enum values, including
`first_name`, `last_name`, `email`, `phone`, `date_of_birth`, `age_group`, `status`, and
`do_not_contact`.

## Response Example

```json
{
  "success": true,
  "message": "Customers imported",
  "data": {
    "importedCount": 1,
    "failedCount": 1,
    "customers": [
      {
        "fullName": "Ada Policyholder",
        "email": "ada@bayer-westphalian.test"
      }
    ],
    "errors": [
      {
        "lineNumber": 3,
        "field": "email",
        "message": "must be a valid email",
        "value": "bad-email"
      }
    ]
  }
}
```

This behavior supports KB evidence that CSV import rejects invalid rows and reports row-level
errors while preserving valid rows from the same file.
