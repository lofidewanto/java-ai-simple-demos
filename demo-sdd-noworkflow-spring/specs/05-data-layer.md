# 05 — Data Layer Design

## Purpose

This document specifies the data access layer: the repository interface, Spring Data JPA patterns used, Flyway migration strategy, H2 database configuration, indexing rationale, and the `DataInitializer` seed data.

---

## Repository Interface

**Class:** `com.example.demo_sdd_spring.repository.OrderRepository`
**Annotation:** `@Repository`
**Extends:** `JpaRepository<Order, Long>`

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByCreatedAtDesc();
}
```

---

## Inherited JPA Repository Methods

By extending `JpaRepository`, the following methods are available without any implementation:

| Method | Description |
|---|---|
| `save(S entity)` | Insert (if new) or update (if managed). Triggers `@PrePersist` / `@PreUpdate`. Returns the saved entity. |
| `findById(ID id)` | Returns `Optional<Order>`. Empty if not found. |
| `findAll()` | Returns all orders (unordered). |
| `existsById(ID id)` | Returns `boolean` — true if a record with the given ID exists. |
| `deleteById(ID id)` | Deletes by primary key. |
| `count()` | Total number of records. |

---

## Custom Query Method

### `findAllByOrderByCreatedAtDesc()`

```java
List<Order> findAllByOrderByCreatedAtDesc();
```

- **Type:** Spring Data JPA derived query — no `@Query` annotation required.
- **Parsing:** Spring Data parses the method name: `findAll` + `By` (no filter) + `OrderBy` + `CreatedAt` + `Desc`.
- **Generated SQL (approximate):**
  ```sql
  SELECT * FROM orders ORDER BY created_at DESC;
  ```
- **Purpose:** Powers both the web UI order list and the REST `GET /api/orders` endpoint.
- **Performance:** Backed by `idx_orders_created_at` index.

---

## Schema Management: Flyway

### Strategy

Flyway is the **sole owner** of the database schema. Hibernate is configured to **validate only**.

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

### Migration File Location

```
src/main/resources/db/migration/
└── V1__Create_orders_table.sql
```

Spring Boot auto-configures Flyway to scan `classpath:db/migration` by default.

### Naming Convention

| Part | Example | Meaning |
|---|---|---|
| `V` | `V1` | Version prefix — unique and monotonically increasing |
| `__` | `__` | Double underscore separator (required by Flyway) |
| Description | `Create_orders_table` | Human-readable description |
| Extension | `.sql` | Plain SQL file |

### Adding a New Migration

To evolve the schema (e.g., add a `priority` column):

1. Create `src/main/resources/db/migration/V2__Add_priority_to_orders.sql`
2. Write forward-only SQL:
   ```sql
   ALTER TABLE orders ADD COLUMN priority VARCHAR(20) DEFAULT 'NORMAL';
   ```
3. Update the `Order` entity to include the new field.
4. Restart — Flyway applies the new migration automatically.

> Never modify an already-applied migration file. Flyway checksums each file; a change causes a startup failure (`FlywayException: Validate failed`).

---

## Migration V1 — Full SQL

```sql
CREATE TABLE orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    product_name  VARCHAR(255) NOT NULL,
    quantity      INTEGER NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'SUBMITTED',
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_status     ON orders(status);
```

### Column Notes

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `id` | `BIGINT` | `AUTO_INCREMENT PRIMARY KEY` | H2 identity column; maps to `GenerationType.IDENTITY` |
| `customer_name` | `VARCHAR(255)` | `NOT NULL` | Maps to `customerName` |
| `product_name` | `VARCHAR(255)` | `NOT NULL` | Maps to `productName` |
| `quantity` | `INTEGER` | `NOT NULL` | Must be >= 1; enforced by service layer |
| `status` | `VARCHAR(50)` | `NOT NULL` | Holds `OrderStatus` enum name (max length: `CHECKING_INVENTORY` = 18 chars) |
| `notes` | `TEXT` | nullable | Growing audit trail; unbounded length |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Set by `@PrePersist` |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Set by `@PrePersist` / `@PreUpdate` |

### Index Rationale

```sql
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_status     ON orders(status);
```

- `idx_orders_created_at` — optimises `findAllByOrderByCreatedAtDesc()`, the primary read query.
- `idx_orders_status` — optimises filtering by workflow state (useful for future queries like "all open orders").

---

## H2 In-Memory Database Configuration

```properties
spring.datasource.url=jdbc:h2:mem:orderdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### H2 Console Access

Navigate to `http://localhost:8080/h2-console` and connect with:
- **JDBC URL:** `jdbc:h2:mem:orderdb`
- **Username:** `sa`
- **Password:** (leave blank)

---

## Data Initializer

**Class:** `com.example.demo_sdd_spring.config.DataInitializer`
**Annotation:** `@Configuration`
**Bean:** `CommandLineRunner` — executes after all Spring beans are initialised.

The initializer creates 3 seed orders via `OrderService`, then applies transitions to demonstrate different workflow states:

```java
@Bean
public CommandLineRunner initData(OrderService orderService) {
    return args -> {
        // Order 1: fully processed — SHIPPED
        Order o1 = orderService.createOrder(
            new Order("Alice Johnson", "Wireless Headphones", 2));
        orderService.transitionOrder(o1.getId(), "CHECK_INVENTORY");
        orderService.transitionOrder(o1.getId(), "MARK_AVAILABLE");
        orderService.transitionOrder(o1.getId(), "SHIP");

        // Order 2: item unavailable — CUSTOMER_NOTIFIED
        Order o2 = orderService.createOrder(
            new Order("Bob Smith", "Mechanical Keyboard", 1));
        orderService.transitionOrder(o2.getId(), "CHECK_INVENTORY");
        orderService.transitionOrder(o2.getId(), "MARK_UNAVAILABLE");

        // Order 3: freshly submitted — SUBMITTED
        orderService.createOrder(
            new Order("Carol Williams", "USB-C Hub", 3));
    };
}
```

- Uses `OrderService` (not raw SQL), so the full JPA lifecycle and audit trail fire correctly.
- Because H2 is in-memory, these entries are recreated on every application restart.
- The three orders land in different states, making the UI immediately demonstrable without manual interaction.
