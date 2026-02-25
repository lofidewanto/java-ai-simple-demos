# 05 — Data Layer Design

## Purpose

This document specifies the data access layer: the repository interface, Spring Data JPA patterns used, Flyway migration strategy, H2 database configuration, and indexing rationale.

---

## Repository Interface

**Class:** `com.example.demo_sdd_spring.repository.GuestBookEntryRepository`  
**Annotation:** `@Repository`  
**Extends:** `JpaRepository<GuestBookEntry, Long>`

```java
@Repository
public interface GuestBookEntryRepository extends JpaRepository<GuestBookEntry, Long> {
    List<GuestBookEntry> findAllByOrderByCreatedAtDesc();
}
```

---

## Inherited JPA Repository Methods

By extending `JpaRepository`, the following methods are available without any implementation:

| Method | Description |
|---|---|
| `save(S entity)` | Insert (if new) or update (if managed). Triggers `@PrePersist` / `@PreUpdate`. Returns the saved entity. |
| `findById(ID id)` | Returns `Optional<GuestBookEntry>`. Empty if not found. |
| `findAll()` | Returns all entries (unordered). |
| `existsById(ID id)` | Returns `boolean` — true if a record with the given ID exists. |
| `deleteById(ID id)` | Deletes by primary key. No-op if ID does not exist. |
| `count()` | Total number of records. |
| `findAll(Sort sort)` | Returns all entries with a given sort specification. |

---

## Custom Query Method

### `findAllByOrderByCreatedAtDesc()`

```java
List<GuestBookEntry> findAllByOrderByCreatedAtDesc();
```

- **Type:** Spring Data JPA derived query — no `@Query` annotation required.
- **Parsing:** Spring Data parses the method name: `findAll` + `By` (no filter predicate) + `OrderBy` + `CreatedAt` + `Desc`.
- **Generated SQL (approximate):**
  ```sql
  SELECT * FROM guest_book_entry ORDER BY created_at DESC;
  ```
- **Purpose:** The primary read operation — powers both the web UI list page and the REST `GET /api/entries` endpoint.
- **Performance:** Backed by `idx_guest_book_entry_created_at` index (see Schema section below).

---

## Schema Management: Flyway

### Strategy

Flyway is the **sole owner** of the database schema. Hibernate is configured to **validate only** — it checks that the schema matches the entity mappings but never creates, alters, or drops tables.

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

### Why Flyway?

| Concern | Flyway approach |
|---|---|
| Reproducibility | Every environment (dev, CI, prod) gets the exact same schema via versioned SQL files. |
| Auditability | Each change is a numbered, named SQL file committed to source control. |
| Safety | `ddl-auto=validate` prevents Hibernate from making unintended schema modifications. |
| H2 compatibility | Flyway runs the same SQL against H2 in dev and can target any JDBC-compatible DB in production. |

### Migration File Location

```
src/main/resources/db/migration/
└── V1__Create_guest_book_entry_table.sql
```

Spring Boot auto-configures Flyway to scan `classpath:db/migration` by default.

### Naming Convention

Flyway migration files follow the pattern: `V{version}__{description}.sql`

| Part | Example | Meaning |
|---|---|---|
| `V` | `V1` | Version prefix — must be unique and monotonically increasing |
| `__` | `__` | Double underscore separator (required) |
| Description | `Create_guest_book_entry_table` | Human-readable description (underscores become spaces in the Flyway report) |
| Extension | `.sql` | Plain SQL file |

### Adding a New Migration

To evolve the schema (e.g., add a `phone` column):

1. Create `src/main/resources/db/migration/V2__Add_phone_to_guest_book_entry.sql`
2. Write forward-only SQL:
   ```sql
   ALTER TABLE guest_book_entry ADD COLUMN phone VARCHAR(50);
   ```
3. Update the `GuestBookEntry` entity to include the new field.
4. Restart the application — Flyway applies the new migration automatically.

> Never modify an already-applied migration file. Flyway checksums each file; a change will cause a startup failure (`FlywayException: Validate failed`).

---

## Migration V1 — Full SQL

```sql
CREATE TABLE guest_book_entry (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    message    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_guest_book_entry_created_at ON guest_book_entry(created_at DESC);
```

### Column Notes

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `id` | `BIGINT` | `AUTO_INCREMENT PRIMARY KEY` | H2 identity column; maps to `GenerationType.IDENTITY` in JPA |
| `name` | `VARCHAR(255)` | `NOT NULL` | Max 255 chars; Hibernate validates this at mapping level |
| `email` | `VARCHAR(255)` | nullable | Optional contact info |
| `message` | `TEXT` | `NOT NULL` | Unlimited length text; no `VARCHAR` cap |
| `created_at` | `TIMESTAMP` | `NOT NULL` | DB default is a safety net; app always sets via `@PrePersist` |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | DB default is a safety net; app always sets via `@PrePersist` / `@PreUpdate` |

### Index Rationale

```sql
CREATE INDEX idx_guest_book_entry_created_at ON guest_book_entry(created_at DESC);
```

- The dominant read pattern (`findAllByOrderByCreatedAtDesc`) sorts by `created_at DESC`.
- An index on this column in descending order allows the database to serve this query via an index scan rather than a full table sort.
- For H2 in-memory (dev/demo), this makes negligible difference with small data sets, but the index documents intent and is correct for any production-scale RDBMS.

---

## H2 In-Memory Database Configuration

```properties
spring.datasource.url=jdbc:h2:mem:guestbookdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### Key Properties

| Property | Value | Meaning |
|---|---|---|
| `jdbc:h2:mem:guestbookdb` | in-memory | Database lives in JVM heap; destroyed on shutdown |
| `driverClassName` | `org.H2.Driver` | H2 JDBC driver |
| `username` / `password` | `sa` / (empty) | H2 default credentials |
| `H2Dialect` | Hibernate dialect | Generates H2-compatible SQL |
| `show-sql=true` | enabled | All SQL statements printed to stdout (dev only) |
| `format_sql=true` | enabled | Pretty-prints multi-line SQL in logs |
| `h2-console` | `/h2-console` | Browser-accessible DB admin UI (dev only) |

### H2 Console Access

Navigate to `http://localhost:8080/h2-console` and connect with:
- **JDBC URL:** `jdbc:h2:mem:guestbookdb`
- **Username:** `sa`
- **Password:** (leave blank)

### Switching to a Persistent / Production Database

To replace H2 with PostgreSQL (example):

1. Add `spring-boot-starter-data-jpa` and the PostgreSQL JDBC driver to `pom.xml`.
2. Update `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/guestbook
   spring.datasource.username=youruser
   spring.datasource.password=yourpassword
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   spring.h2.console.enabled=false
   ```
3. Remove the H2 dependency from `pom.xml`.
4. Flyway migrations will run unchanged (standard SQL).

---

## Data Initializer

**Class:** `com.example.demo_sdd_spring.config.DataInitializer`  
**Annotation:** `@Configuration`  
**Bean:** `CommandLineRunner` — executes after all Spring beans are initialised, before the application starts accepting requests.

```java
@Bean
public CommandLineRunner initData(GuestBookService service) {
    return args -> {
        service.createEntry(new GuestBookEntry("Alice Johnson", "alice@example.com", "..."));
        service.createEntry(new GuestBookEntry("Bob Smith",     "bob@example.com",   "..."));
        service.createEntry(new GuestBookEntry("Carol Williams","carol@example.com", "..."));
    };
}
```

- Uses `GuestBookService.createEntry()` (not raw SQL), so the full JPA lifecycle fires.
- Guarantees `createdAt` / `updatedAt` are set correctly.
- Because H2 is in-memory, these entries are recreated on every application restart.
