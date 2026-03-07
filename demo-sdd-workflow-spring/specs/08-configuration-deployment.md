# 08 — Configuration and Deployment

## Overview

The application is configured via `src/main/resources/application.properties`. It uses an H2 in-memory database and requires no external services to run locally. The application starts on port `8080` by default.

---

## application.properties

```properties
# Application
spring.application.name=demo-sdd-workflow-spring

# Server
server.port=8080

# H2 In-Memory Database
spring.datasource.url=jdbc:h2:mem:workflowdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
# Schema is managed by Flyway — do not let Hibernate modify it
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# H2 Console (development only)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Springdoc / OpenAPI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
```

---

## pom.xml Dependencies

In addition to the dependencies included by Spring Initializr, add:

```xml
<!-- YAML parsing for workflow DSL files -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

The version is managed by the Spring Boot BOM — no explicit version needed.

Full dependency list:

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | REST controllers, embedded Tomcat |
| `spring-boot-starter-data-jpa` | JPA / Hibernate |
| `spring-boot-starter-thymeleaf` | Present in pom.xml — unused, safe to leave |
| `flyway-core` | Database migrations |
| `h2` | In-memory database (runtime/test scope) |
| `jackson-dataformat-yaml` | Parse YAML workflow definition files |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI and OpenAPI spec generation |
| `spring-boot-devtools` | Live reload during development |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |

---

## Local Development Quick Start

```bash
# Clone and enter project
cd demo-sdd-workflow-spring

# Build (skip tests for speed)
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Or run the JAR directly
java -jar target/demo-sdd-workflow-spring-*.jar
```

The application will:
1. Run Flyway migrations (creates 3 tables)
2. Start `WorkflowDefinitionLoader` — loads 3 YAML files from `classpath:workflows/`
3. Start listening on `http://localhost:8080`

---

## Runtime URLs

| URL | Description |
|-----|-------------|
| `http://localhost:8080/api/workflow-definitions` | List all loaded definitions |
| `http://localhost:8080/api/workflow-definitions/{name}` | Get definition by name |
| `http://localhost:8080/api/workflow-instances` | List / create instances |
| `http://localhost:8080/api/workflow-instances/{id}` | Get instance with history |
| `http://localhost:8080/api/workflow-instances/{id}/transitions` | Trigger a transition |
| `http://localhost:8080/api/workflow-instances/{id}/history` | Get history only |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/api-docs` | Raw OpenAPI JSON spec |
| `http://localhost:8080/h2-console` | H2 database console |

---

## H2 Console

Access at `http://localhost:8080/h2-console` with:

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:workflowdb` |
| Username | `sa` |
| Password | *(leave empty)* |

Useful queries:

```sql
-- All loaded definitions
SELECT id, name, description FROM workflow_definitions;

-- All instances with their current state
SELECT wi.id, wd.name, wi.current_state, wi.status
FROM workflow_instances wi
JOIN workflow_definitions wd ON wi.workflow_definition_id = wd.id;

-- Full history for instance id=1
SELECT * FROM workflow_history_entries WHERE workflow_instance_id = 1 ORDER BY occurred_at;
```

---

## Production Considerations

To deploy against a real database (e.g. PostgreSQL), override the datasource in `application.properties` or via environment variables:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/workflowdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# Disable H2 console in production
spring.h2.console.enabled=false
```

Add the PostgreSQL driver to `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

The Flyway migrations are database-agnostic and will run unchanged against PostgreSQL.

---

## Building a Docker Image

```bash
# Build JAR
./mvnw clean package -DskipTests

# Build image (requires Docker)
docker build -t demo-sdd-workflow-spring .

# Run container
docker run -p 8080:8080 demo-sdd-workflow-spring
```

A `Dockerfile` is not included by default — create one based on the standard Spring Boot layered JAR pattern if needed.
