# 08 — Configuration & Deployment

## Purpose

This document covers all application configuration properties, available developer tooling URLs, build instructions, and run instructions needed to get the Guest Book application running from a fresh clone.

---

## Prerequisites

| Requirement | Minimum Version | Notes |
|---|---|---|
| Java (JDK) | 21 | `java -version` must report 21+ |
| Maven | 3.6+ | Use the included `./mvnw` wrapper — no separate Maven install required |
| Git | any | For cloning the repository |

No external database, message broker, or service is required — the application is fully self-contained.

---

## Build

### Compile and package (skip tests)
```bash
./mvnw package -DskipTests
```

### Compile, run all tests, and package
```bash
./mvnw verify
```

### Clean build output
```bash
./mvnw clean
```

The packaged artifact is produced at:
```
target/demo-sdd-spring-0.0.1-SNAPSHOT.jar
```

---

## Run

### Via Maven (development — live reload enabled)
```bash
./mvnw spring-boot:run
```

### Via the packaged JAR
```bash
java -jar target/demo-sdd-spring-0.0.1-SNAPSHOT.jar
```

The application starts on port **8080** by default.

### Startup sequence
1. Spring Boot initialises all beans.
2. Flyway runs pending migrations against the H2 in-memory database (`V1__Create_guest_book_entry_table.sql`).
3. Hibernate validates the schema against the entity mappings.
4. `DataInitializer` (`CommandLineRunner`) seeds 3 sample guest book entries.
5. Embedded Tomcat begins accepting requests on port 8080.

---

## Application Properties

Full contents of `src/main/resources/application.properties`:

```properties
# ── Application ────────────────────────────────────────────────
spring.application.name=demo-sdd-spring

# ── H2 Database ────────────────────────────────────────────────
spring.datasource.url=jdbc:h2:mem:guestbookdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# ── JPA / Hibernate ────────────────────────────────────────────
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ── H2 Console ─────────────────────────────────────────────────
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# ── Flyway ─────────────────────────────────────────────────────
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# ── Thymeleaf ──────────────────────────────────────────────────
spring.thymeleaf.cache=false

# ── OpenAPI / Swagger ──────────────────────────────────────────
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.api-docs.groups.enabled=true
```

---

## Property Reference

### Datasource

| Property | Value | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:h2:mem:guestbookdb` | In-memory H2 database named `guestbookdb`. Destroyed on shutdown. |
| `spring.datasource.driverClassName` | `org.h2.Driver` | H2 JDBC driver class. |
| `spring.datasource.username` | `sa` | Default H2 superuser. |
| `spring.datasource.password` | (empty) | No password for H2 in-memory. |

### JPA / Hibernate

| Property | Value | Description |
|---|---|---|
| `spring.jpa.database-platform` | `org.hibernate.dialect.H2Dialect` | Generates H2-compatible SQL. |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate only validates schema; never creates or alters tables. Schema is owned by Flyway. |
| `spring.jpa.show-sql` | `true` | Prints all SQL to stdout. Disable in production. |
| `spring.jpa.properties.hibernate.format_sql` | `true` | Pretty-prints multi-line SQL. Disable in production. |

### H2 Console

| Property | Value | Description |
|---|---|---|
| `spring.h2.console.enabled` | `true` | Enables the browser-based H2 admin console. **Disable in production.** |
| `spring.h2.console.path` | `/h2-console` | URL path for the H2 console. |

### Flyway

| Property | Value | Description |
|---|---|---|
| `spring.flyway.enabled` | `true` | Enables Flyway on startup. |
| `spring.flyway.baseline-on-migrate` | `true` | Allows Flyway to baseline an existing schema (useful when adding Flyway to an existing project). |

### Thymeleaf

| Property | Value | Description |
|---|---|---|
| `spring.thymeleaf.cache` | `false` | Disables template caching. Changes to `.html` files are reflected without restart (requires Spring DevTools). **Set to `true` in production.** |

### OpenAPI / Springdoc

| Property | Value | Description |
|---|---|---|
| `springdoc.api-docs.path` | `/api-docs` | Path for the OpenAPI 3 JSON specification. |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Path for the interactive Swagger UI. |
| `springdoc.swagger-ui.enabled` | `true` | Enables Swagger UI. |
| `springdoc.swagger-ui.operationsSorter` | `method` | Sorts endpoints by HTTP method in Swagger UI. |
| `springdoc.swagger-ui.tagsSorter` | `alpha` | Sorts API tags alphabetically. |
| `springdoc.api-docs.groups.enabled` | `true` | Enables API grouping support. |

---

## Accessible URLs (Runtime)

| URL | Description |
|---|---|
| `http://localhost:8080/` | Root — redirects to `/entries` |
| `http://localhost:8080/entries` | Guest book web UI |
| `http://localhost:8080/entries/new` | Add new entry form |
| `http://localhost:8080/api/entries` | REST API — list all entries |
| `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| `http://localhost:8080/api-docs` | OpenAPI 3 JSON specification |
| `http://localhost:8080/h2-console` | H2 database admin console |

### H2 Console Login

Navigate to `http://localhost:8080/h2-console` and enter:

| Field | Value |
|---|---|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:guestbookdb` |
| User Name | `sa` |
| Password | (leave blank) |

---

## Spring DevTools (Development)

`spring-boot-devtools` is included as an optional runtime dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

DevTools provides:
- **Automatic restart** when class files on the classpath change (triggered by IDE recompile).
- **LiveReload** integration — browser refreshes automatically on static resource changes.
- **Thymeleaf cache disabled** by default in dev (overrides `spring.thymeleaf.cache=true` if set).

DevTools is **excluded from the packaged JAR** by `spring-boot-maven-plugin` — it does not affect production builds.

---

## Production Considerations

The following settings are appropriate for development/demo only and must be changed for production:

| Setting | Dev Value | Production Recommendation |
|---|---|---|
| `spring.datasource.url` | H2 in-memory | Replace with a persistent JDBC URL (PostgreSQL, MySQL, etc.) |
| `spring.h2.console.enabled` | `true` | Set to `false` — exposes direct DB access |
| `spring.jpa.show-sql` | `true` | Set to `false` — SQL in logs is a performance and security concern |
| `spring.jpa.properties.hibernate.format_sql` | `true` | Set to `false` |
| `spring.thymeleaf.cache` | `false` | Set to `true` for performance |
| `springdoc.swagger-ui.enabled` | `true` | Consider disabling or securing behind authentication |

### Example: Switching to PostgreSQL

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/guestbook
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=guestbook_user
spring.datasource.password=secret
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.h2.console.enabled=false
spring.jpa.show-sql=false
spring.thymeleaf.cache=true
```

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Remove or set to `runtime` scope the H2 dependency.

---

## Maven Build Plugins

### `spring-boot-maven-plugin`

Packages the application as an executable "fat JAR" containing all dependencies:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-devtools</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

### `maven-compiler-plugin`

Enables the Spring Boot Configuration Processor annotation processor (generates `spring-configuration-metadata.json` for IDE autocompletion of `application.properties`):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-configuration-processor</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```
