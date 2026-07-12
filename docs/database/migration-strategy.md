# Database Migration Strategy

The KB defines PostgreSQL as the system of record and Flyway as the database migration tool. Sprint 2 also sets the production gate that database structure must be version-controlled through migrations and that manual production database changes are not allowed.

## Migration Location

All backend database migrations must be committed under:

```text
backend/src/main/resources/db/migration
```

## Naming Convention

Use Flyway versioned SQL migrations for schema changes:

```text
V<positive-integer>__<lower_snake_case_description>.sql
```

Examples:

```text
V1__create_initial_schema.sql
V2__create_user_role_tables.sql
V3__add_customer_consent_indexes.sql
```

Rules:

- Start version numbers at `1` and increment by one logical migration at a time.
- Use exactly two underscores between the version and the description.
- Use lowercase letters, numbers, and single underscores in the description.
- Start the description with a lowercase letter.
- Do not use spaces, hyphens, dates, timestamps, uppercase letters, or environment names.
- Do not edit an applied migration. Add a new migration that moves the schema forward.
- Keep each migration focused on one database change set.

Repeatable Flyway migrations are not part of the current Sprint 2 baseline. Add them only after a documented architecture decision.

## Relationship to backup and restore (item **666** / NFR-013)

Logical PostgreSQL dumps capture data **and** the Flyway history table. After restore, the
application must start against the same migration set that produced that history. Do not edit
applied migrations to “fix” a restored environment — add a new versioned script if the schema must
change. Full operator dump/restore steps: [Backup and Restore Process](../deployment/backup-and-restore.md).

## Verification

The backend test suite enforces this convention by checking the migration directory, validating unique version numbers, and confirming the scripts recorded by Flyway follow the same naming rule.

Run:

```powershell
cd backend
mvn test
mvn verify
```
