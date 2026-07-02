# Initial Architecture Diagram

This diagram captures the first implementation architecture for the Bayer-Westphalian Campaign Management Platform in accordance with the knowledge base.

```mermaid
flowchart LR
    users["Internal Users<br/>Admin, Campaign Manager, BI Analyst,<br/>Product Manager, Compliance Officer,<br/>Customer Service Agent, Sales Agent,<br/>Marketing Analyst, Executive Viewer, System Auditor"]

    subgraph client["Frontend - React, TypeScript, Vite"]
        web["Single Page App"]
        routes["React Router Pages"]
        query["TanStack Query API State"]
        forms["React Hook Form + Zod Validation"]
        charts["Recharts Dashboards"]
    end

    subgraph edge["Deployment Edge"]
        proxy["Nginx or Caddy Reverse Proxy"]
    end

    subgraph api["Backend - Java 21, Spring Boot"]
        controllers["REST Controllers"]
        security["Spring Security<br/>JWT or Secure Session"]
        validation["Jakarta Validation"]
        openapi["OpenAPI / Swagger"]

        subgraph domain["Domain Modules"]
            auth["Auth"]
            user["User and Roles"]
            customer["Customer"]
            beneficiary["Beneficiary"]
            consent["Consent and Opt-Out"]
            product["Product"]
            campaign["Campaign"]
            segment["Segment"]
            schedule["Schedule and Reminder"]
            communication["Communication"]
            analytics["Analytics and Reports"]
            audit["Audit"]
            ai["AI-Assisted Recommendations"]
        end

        persistence["Spring Data JPA / Hibernate"]
        migrations["Flyway Migrations"]
    end

    subgraph data["Data Layer"]
        postgres[("PostgreSQL")]
    end

    subgraph external["External / Replaceable Providers"]
        email["Email Provider"]
        sms["SMS Provider"]
        aiProvider["AI Provider"]
        fileStorage["File Storage"]
    end

    users --> web
    web --> routes
    routes --> query
    routes --> forms
    routes --> charts
    query --> proxy
    proxy --> controllers
    controllers --> security
    security --> validation
    validation --> domain
    domain --> persistence
    persistence --> postgres
    migrations --> postgres
    controllers --> openapi
    communication --> email
    communication --> sms
    ai --> aiProvider
    analytics --> fileStorage
    audit --> postgres
```

## Initial Runtime View

- The frontend is a React single-page application served locally by Vite during development and later by a reverse proxy or static container.
- The backend exposes REST JSON APIs, OpenAPI documentation, validation, authorization, and business workflows through Spring Boot.
- PostgreSQL is the system of record for customer, consent, product, campaign, schedule, communication, analytics, report, and audit data.
- Flyway owns all database schema changes so database evolution is version-controlled.
- External email, SMS, AI, and file-storage integrations are replaceable adapters. Mock provider modes are allowed only for development and testing.
- Nginx or Caddy is the planned deployment edge for production routing, HTTPS, and frontend/API traffic separation.

## Quality and Control Boundaries

- Spring Security protects API access for internal users and role-based workflows.
- Consent, campaign eligibility, approval, audit, and customer data rules remain backend-owned.
- Frontend validation improves user experience, but backend validation remains authoritative.
- Audit logging is required for sensitive actions and production evidence.
