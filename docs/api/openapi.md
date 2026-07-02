# OpenAPI and Swagger

The backend uses Springdoc OpenAPI to generate API documentation from Spring MVC controllers.

## Local URLs

When the backend is running locally:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Maven Dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.17</version>
</dependency>
```

## Configuration

OpenAPI metadata is configured in:

```text
backend/src/main/java/com/bayerwestphalian/campaign/common/OpenApiConfiguration.java
```

Springdoc paths are configured in:

```text
backend/src/main/resources/application.yml
```

Swagger endpoints may need to be explicitly permitted when backend security rules are implemented.
