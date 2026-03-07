# 02 — Domain Model

## Purpose

This document specifies the domain objects of the Order Processing application — the `Order` entity and the `OrderStatus` enum — including fields, constraints, JPA mapping, lifecycle behaviour, and the database schema they map to.

---

## Enum: `OrderStatus`

**Class:** `com.example.demo_sdd_spring.model.OrderStatus`

```java
public enum OrderStatus {
    SUBMITTED,
    CHECKING_INVENTORY,
    PAYMENT_COLLECTED,
    SHIPPED,
    CUSTOMER_NOTIFIED
}
```

| Value | Meaning | Terminal? |
|---|---|---|
| `SUBMITTED` | Customer has submitted the order; shop has not yet reviewed it | No |
| `CHECKING_INVENTORY` | Shop is verifying whether the product is in stock | No |
| `PAYMENT_COLLECTED` | Product is available; payment has been successfully collected | No |
| `SHIPPED` | Product has been dispatched to the customer | **Yes** |
| `CUSTOMER_NOTIFIED` | Product was unavailable; customer has been informed | **Yes** |

Stored as `VARCHAR` in the database via `@Enumerated(EnumType.STRING)` for readability and to decouple DB values from enum ordinal positions.

---

## Entity: `Order`

**Class:** `com.example.demo_sdd_spring.model.Order`
**Table:** `orders`

### Class-level Annotations

```java
@Entity
@Table(name = "orders")
@Schema(description = "Order in the order processing workflow")
```

---

## Field Specifications

| Field | Java Type | Column | Nullable | Notes |
|---|---|---|---|---|
| `id` | `Long` | `id` | NO | Primary key, auto-increment (`IDENTITY` strategy). Read-only in API. |
| `customerName` | `String` | `customer_name` | NO | Full name of the customer placing the order. `VARCHAR(255)`. |
| `productName` | `String` | `product_name` | NO | Name of the product being ordered. `VARCHAR(255)`. |
| `quantity` | `Integer` | `quantity` | NO | Number of units ordered. Must be >= 1. |
| `status` | `OrderStatus` | `status` | NO | Current workflow state. Stored as `VARCHAR(50)`. |
| `notes` | `String` | `notes` | YES | System-managed audit trail. Newline-delimited timestamped log of all state transitions. Never set by the user. `TEXT`. |
| `createdAt` | `LocalDateTime` | `created_at` | NO | Set automatically on first persist. Never updated after creation (`updatable = false`). |
| `updatedAt` | `LocalDateTime` | `updated_at` | NO | Set on first persist; updated on every subsequent save. |

### Detailed Annotations per Field

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
private Long id;

@Column(name = "customer_name", nullable = false)
@Schema(description = "Full name of the customer", requiredMode = Schema.RequiredMode.REQUIRED)
private String customerName;

@Column(name = "product_name", nullable = false)
@Schema(description = "Name of the product being ordered", requiredMode = Schema.RequiredMode.REQUIRED)
private String productName;

@Column(nullable = false)
@Schema(description = "Number of units ordered", requiredMode = Schema.RequiredMode.REQUIRED)
private Integer quantity;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 50)
@Schema(description = "Current workflow state of the order", accessMode = Schema.AccessMode.READ_ONLY)
private OrderStatus status;

@Column(columnDefinition = "TEXT")
@Schema(description = "System-managed audit trail of state transitions", accessMode = Schema.AccessMode.READ_ONLY)
private String notes;

@Column(name = "created_at", nullable = false, updatable = false)
@Schema(description = "Timestamp when the order was created", accessMode = Schema.AccessMode.READ_ONLY)
private LocalDateTime createdAt;

@Column(name = "updated_at", nullable = false)
@Schema(description = "Timestamp when the order was last updated", accessMode = Schema.AccessMode.READ_ONLY)
private LocalDateTime updatedAt;
```

---

## Lifecycle Callbacks

The entity manages its own timestamps via JPA lifecycle hooks.

### `@PrePersist — onCreate()`

Called by JPA immediately before an `INSERT`. Sets both `createdAt` and `updatedAt` to the current system time, and initialises `status` to `SUBMITTED` if not already set.

```java
@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
        status = OrderStatus.SUBMITTED;
    }
}
```

### `@PreUpdate — onUpdate()`

Called by JPA immediately before an `UPDATE`. Refreshes `updatedAt` only. `createdAt` is never touched.

```java
@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

---

## Constructors

```java
// Required by JPA spec
public Order() {}

// Convenience constructor for programmatic creation (DataInitializer, tests)
public Order(String customerName, String productName, Integer quantity) {
    this.customerName = customerName;
    this.productName  = productName;
    this.quantity     = quantity;
}
```

> `id`, `status`, `notes`, `createdAt`, and `updatedAt` are **never set via constructor** — they are managed by the service layer and JPA/Hibernate.

---

## Accessors

All fields have conventional Java Bean-style getters and setters. No Lombok is used.

| Method | Returns |
|---|---|
| `getId()` | `Long` |
| `setId(Long)` | `void` |
| `getCustomerName()` | `String` |
| `setCustomerName(String)` | `void` |
| `getProductName()` | `String` |
| `setProductName(String)` | `void` |
| `getQuantity()` | `Integer` |
| `setQuantity(Integer)` | `void` |
| `getStatus()` | `OrderStatus` |
| `setStatus(OrderStatus)` | `void` |
| `getNotes()` | `String` |
| `setNotes(String)` | `void` |
| `getCreatedAt()` | `LocalDateTime` |
| `setCreatedAt(LocalDateTime)` | `void` |
| `getUpdatedAt()` | `LocalDateTime` |
| `setUpdatedAt(LocalDateTime)` | `void` |

---

## Notes Field — Audit Trail Format

The `notes` field is managed exclusively by `OrderService.transitionOrder()`. Each state transition appends a line in the following format:

```
yyyy-MM-dd HH:mm — <message>
```

Example for a fully processed order:

```
2026-03-07 10:00 — Order submitted by customer
2026-03-07 10:05 — Status changed to CHECKING_INVENTORY
2026-03-07 10:10 — Status changed to PAYMENT_COLLECTED
2026-03-07 10:15 — Status changed to SHIPPED
```

Example for an unavailable item:

```
2026-03-07 09:00 — Order submitted by customer
2026-03-07 09:05 — Status changed to CHECKING_INVENTORY
2026-03-07 09:08 — Status changed to CUSTOMER_NOTIFIED
```

Implementation pattern in the service:

```java
String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
String line = timestamp + " — Status changed to " + newStatus.name();
String existing = order.getNotes() == null ? "" : order.getNotes();
order.setNotes(existing.isBlank() ? line : existing + "\n" + line);
```

---

## Database Schema

The schema is owned and versioned by Flyway. See `specs/05-data-layer.md` for migration strategy.

### Migration: `V1__Create_orders_table.sql`

```sql
CREATE TABLE orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    product_name  VARCHAR(255) NOT NULL,
    quantity      INTEGER NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_status     ON orders(status);
```

### Schema Notes

- `status` is `VARCHAR(50)` to hold the `OrderStatus` enum names (longest: `CHECKING_INVENTORY` = 18 chars).
- `notes` is `TEXT` (unbounded) to accommodate a growing audit trail.
- `DEFAULT 'SUBMITTED'` and `DEFAULT CURRENT_TIMESTAMP` are DB-level safety nets; the application always sets these values explicitly.
- The index on `status` optimises queries that filter by state (e.g., listing all open orders).

---

## JSON Representation

When serialised as JSON (REST API responses):

```json
{
  "id": 1,
  "customerName": "Alice Johnson",
  "productName": "Wireless Headphones",
  "quantity": 2,
  "status": "SUBMITTED",
  "notes": "2026-03-07 10:00 — Order submitted by customer",
  "createdAt": "2026-03-07T10:00:00",
  "updatedAt": "2026-03-07T10:00:00"
}
```

### Minimal Request Body (POST /api/orders)

```json
{
  "customerName": "Alice Johnson",
  "productName": "Wireless Headphones",
  "quantity": 2
}
```

`id`, `status`, `notes`, `createdAt`, and `updatedAt` are read-only — clients must not send them (they are ignored if present).

---

## Sample Data

`DataInitializer` seeds the following 3 orders at application startup, with transitions applied to place them in different states for demonstration:

| customerName | productName | quantity | status |
|---|---|---|---|
| `Alice Johnson` | `Wireless Headphones` | 2 | `SHIPPED` (fully processed) |
| `Bob Smith` | `Mechanical Keyboard` | 1 | `CUSTOMER_NOTIFIED` (item unavailable) |
| `Carol Williams` | `USB-C Hub` | 3 | `SUBMITTED` (freshly submitted, no action yet) |
