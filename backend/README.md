# Backend

Spring Boot backend application for the Bayer-Westphalian Campaign Management Platform.

The backend owns APIs, business logic, authorization, validation, scheduled jobs, audit logging, campaign eligibility, consent enforcement, reporting, and persistence.

## Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA / Hibernate
- Jakarta Bean Validation
- PostgreSQL
- Flyway
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- JUnit, Spring Boot Test, Spring Security Test, Testcontainers

The Java package root is:

```text
com.bayerwestphalian.campaign
```

Database migrations belong in:

```text
src/main/resources/db/migration
```

## API Documentation

OpenAPI/Swagger is configured with Springdoc.

When the backend is running locally:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Details are documented in `docs/api/openapi.md`.

## Development Commands

Use the Maven wrapper once it is added, or a local Maven installation.

```bash
mvn spring-boot:run
mvn test
mvn spotless:apply
mvn spotless:check
mvn checkstyle:check
mvn package
```

On Windows PowerShell:

```powershell
mvn spring-boot:run
mvn test
mvn spotless:apply
mvn spotless:check
mvn checkstyle:check
mvn package
```

## Environment Variables

Configuration is supplied through environment variables. Commit only `.env.example`; never commit real `.env` files or secrets.

Start from the example file:

```powershell
Copy-Item .env.example .env
```

Core variables:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Selects `dev`, `test`, or `prod` |
| `SERVER_PORT` | Backend HTTP port |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_ISSUER` | JWT issuer |
| `JWT_SECRET` | JWT signing secret |
| `CONTACT_MONTHLY_LIMIT` | Maximum monthly marketing contacts |
| `CONTACT_RETRY_LIMIT` | Communication retry limit |
| `EMAIL_PROVIDER_MODE` | `mock` for dev/test or real provider mode |
| `SMS_PROVIDER_MODE` | `mock` for dev/test or real provider mode |
| `FILE_STORAGE_MODE` | Local or external file storage mode |

Production must provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` through the deployment environment or secret manager.

The `dev` profile defaults to the local Docker PostgreSQL database:

```text
jdbc:postgresql://localhost:5432/bwc_campaign
```

## Build Configuration

The Maven build is configured in `pom.xml` with Java 21, Spring Boot packaging, compiler release `21`, Surefire for tests, and UTF-8 encoding.

Formatting and linting are part of the backend verification baseline:

- Spotless formats Java with Google Java Format in AOSP style and checks selected YAML/Markdown files for trailing whitespace and final newlines.
- Checkstyle enforces import hygiene, line length, brace/statement basics, tabs, final newlines, and Java naming rules.
- `mvn verify` runs tests, Spotless checks, and Checkstyle checks.

Maven defaults are stored in `.mvn/`:

- `.mvn/maven.config`
- `.mvn/jvm.config`
