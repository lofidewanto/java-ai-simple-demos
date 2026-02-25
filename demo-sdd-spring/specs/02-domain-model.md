# 02 — Domain Model

## Purpose

This document specifies the sole domain entity of the application — `GuestBookEntry` — including its fields, constraints, JPA mapping, lifecycle behaviour, and the database schema it maps to.

---

## Entity: `GuestBookEntry`

**Class:** `com.example.demo_sdd_spring.model.GuestBookEntry`  
**Table:** `guest_book_entry`

### Class-level Annotations

```java
@Entity
@Table(name = "guest_book_entry")
@Schema(description = "Guest Book Entry")  // OpenAPI documentation
```

---

## Field Specifications

| Field | Java Type | Column | Nullable | Notes |
|---|---|---|---|---|
| `id` | `Long` | `id` | NO | Primary key, auto-increment (`IDENTITY` strategy). Read-only in API (schema `accessMode = READ_ONLY`). |
| `name` | `String` | `name` | NO | Visitor's display name. `VARCHAR(255)`. Required. |
| `email` | `String` | `email` | YES | Visitor's email address. `VARCHAR(255)`. Optional. |
| `message` | `String` | `message` | NO | Guest book message body. Stored as `TEXT` (no length limit). Required. |
| `createdAt` | `LocalDateTime` | `created_at` | NO | Set automatically on first persist. Never updated after creation (`updatable = false`). |
| `updatedAt` | `LocalDateTime` | `updated_at` | NO | Set on first persist; updated on every subsequent save. |

### Detailed Annotations per Field

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
private Long id;

@Column(nullable = false)
@Schema(description = "Name of the guest", requiredMode = Schema.RequiredMode.REQUIRED)
private String name;

@Column
@Schema(description = "Email address of the guest")
private String email;

@Column(nullable = false, columnDefinition = "TEXT")
@Schema(description = "Message left by the guest", requiredMode = Schema.RequiredMode.REQUIRED)
private String message;

@Column(name = "created_at", nullable = false, updatable = false)
@Schema(description = "Timestamp when the entry was created", accessMode = Schema.AccessMode.READ_ONLY)
private LocalDateTime createdAt;

@Column(name = "updated_at", nullable = false)
@Schema(description = "Timestamp when the entry was last updated", accessMode = Schema.AccessMode.READ_ONLY)
private LocalDateTime updatedAt;
```

---

## Lifecycle Callbacks

The entity manages its own timestamps via JPA lifecycle hooks — no external trigger or default column value is relied upon at the application level.

### `@PrePersist — onCreate()`

Called by JPA immediately before an `INSERT`. Sets both `createdAt` and `updatedAt` to the current system time.

```java
@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
}
```

### `@PreUpdate — onUpdate()`

Called by JPA immediately before an `UPDATE`. Refreshes `updatedAt` to the current system time. `createdAt` is never touched (it is marked `updatable = false` at the column level as an additional guard).

```java
@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

---

## Constructors

```java
// Required by JPA spec — must have a no-arg constructor
public GuestBookEntry() {}

// Convenience constructor for programmatic creation (e.g. DataInitializer, tests)
public GuestBookEntry(String name, String email, String message) {
    this.name = name;
    this.email = email;
    this.message = message;
}
```

> `id`, `createdAt`, and `updatedAt` are **never set via constructor** — they are managed by JPA/Hibernate.

---

## Accessors

All fields have conventional Java Bean-style getters and setters. No Lombok is used.

| Method | Returns |
|---|---|
| `getId()` | `Long` |
| `setId(Long)` | `void` |
| `getName()` | `String` |
| `setName(String)` | `void` |
| `getEmail()` | `String` |
| `setEmail(String)` | `void` |
| `getMessage()` | `String` |
| `setMessage(String)` | `void` |
| `getCreatedAt()` | `LocalDateTime` |
| `setCreatedAt(LocalDateTime)` | `void` |
| `getUpdatedAt()` | `LocalDateTime` |
| `setUpdatedAt(LocalDateTime)` | `void` |

---

## Database Schema

The schema is owned and versioned by Flyway. See `specs/05-data-layer.md` for migration strategy.

### Migration: `V1__Create_guest_book_entry_table.sql`

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

### Schema Notes

- `DEFAULT CURRENT_TIMESTAMP` on the DB columns is a safety net; the application always sets these values explicitly via `@PrePersist` / `@PreUpdate`.
- The index on `created_at DESC` optimises the primary query pattern: `findAllByOrderByCreatedAtDesc()`.
- `message` is `TEXT` (not `VARCHAR`) to avoid length constraints on guest messages.
- `email` is intentionally nullable — guests are not required to provide contact information.

---

## JSON Representation

When serialised as JSON (REST API responses), the entity maps as follows:

```json
{
  "id": 1,
  "name": "Alice Smith",
  "email": "alice@example.com",
  "message": "What a wonderful application!",
  "createdAt": "2026-02-14T10:30:00",
  "updatedAt": "2026-02-14T10:30:00"
}
```

- `id`, `createdAt`, and `updatedAt` are read-only — clients should not send them in `POST`/`PUT` request bodies (they are ignored if present).
- `email` may be `null` in the response if the guest did not provide one.

### Minimal Request Body (POST / PUT)

```json
{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "message": "What a wonderful application!"
}
```

---

## Sample Data

`DataInitializer` seeds the following 3 entries at application startup:

| name | email | message |
|---|---|---|
| `Alice Johnson` | `alice@example.com` | `What a wonderful guest book! Great job on the implementation.` |
| `Bob Smith` | `bob@example.com` | `Hello everyone! Happy to be here and leave my mark.` |
| `Carol Williams` | `carol@example.com` | `Excellent work on this Spring Boot application. Very clean code!` |
