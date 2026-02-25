# 04 — Service Layer Design

## Purpose

This document specifies the business logic layer of the application — `GuestBookService` — including its responsibilities, method contracts, transaction boundaries, and error handling strategy.

---

## Service Class

**Class:** `com.example.demo_sdd_spring.service.GuestBookService`  
**Annotations:** `@Service`, `@Transactional` (class-level)  
**Dependency:** Constructor-injected `GuestBookEntryRepository`

---

## Design Decisions

### No Service Interface

`GuestBookService` is a concrete class — there is no `IGuestBookService` or `GuestBookServiceImpl` pair. The rationale:

- There is exactly one implementation; no runtime polymorphism is needed.
- Spring can proxy the concrete class directly for transaction management (CGLIB proxy).
- An interface would add indirection and boilerplate with no architectural benefit at this scale.

### Class-Level `@Transactional`

All methods are transactional by default (applies the `@Transactional` annotation from the class). Individual read methods override this with `@Transactional(readOnly = true)` to:

- Signal intent clearly to the reader.
- Allow Hibernate/JDBC to optimise read-only connections (e.g. skip dirty checking, flush).
- Prevent accidental writes in read operations.

---

## Method Contracts

### `getAllEntries()`

```java
@Transactional(readOnly = true)
public List<GuestBookEntry> getAllEntries()
```

| Aspect | Detail |
|---|---|
| Transaction | Read-only |
| Delegates to | `repository.findAllByOrderByCreatedAtDesc()` |
| Returns | `List<GuestBookEntry>` — all entries, newest first. Empty list if none exist. |
| Throws | Nothing — an empty list is the normal "no data" case. |

---

### `getEntryById(Long id)`

```java
@Transactional(readOnly = true)
public Optional<GuestBookEntry> getEntryById(Long id)
```

| Aspect | Detail |
|---|---|
| Transaction | Read-only |
| Delegates to | `repository.findById(id)` |
| Returns | `Optional<GuestBookEntry>` — present if found, empty if not. |
| Throws | Nothing — callers must check `Optional.isPresent()`. |

The controller maps an empty Optional to HTTP 404:

```java
return service.getEntryById(id)
    .map(ResponseEntity::ok)
    .orElse(ResponseEntity.notFound().build());
```

---

### `createEntry(GuestBookEntry entry)`

```java
public GuestBookEntry createEntry(GuestBookEntry entry)
```

| Aspect | Detail |
|---|---|
| Transaction | Read-write (class-level default) |
| Delegates to | `repository.save(entry)` |
| Returns | The persisted `GuestBookEntry` with `id`, `createdAt`, `updatedAt` populated. |
| Side effects | `@PrePersist` callback fires, setting `createdAt` and `updatedAt`. |
| Throws | Nothing explicitly — DB constraint violations propagate as unchecked exceptions. |

---

### `updateEntry(Long id, GuestBookEntry updatedEntry)`

```java
public GuestBookEntry updateEntry(Long id, GuestBookEntry updatedEntry)
```

| Aspect | Detail |
|---|---|
| Transaction | Read-write (class-level default) |
| Steps | 1. `repository.findById(id)` — fetch existing entry. 2. If absent, throw `RuntimeException`. 3. Copy `name`, `email`, `message` from `updatedEntry` onto the managed entity. 4. `repository.save(existingEntry)`. |
| Returns | The updated, persisted `GuestBookEntry`. |
| Throws | `RuntimeException("Entry not found with id: " + id)` if entry does not exist. |
| Side effects | `@PreUpdate` callback fires, refreshing `updatedAt`. `createdAt` is never modified. |

**Important:** Only `name`, `email`, and `message` are copied from the incoming entry. `id`, `createdAt`, and `updatedAt` are **never overwritten** from the request — they are owned by the persistence layer.

```java
existingEntry.setName(updatedEntry.getName());
existingEntry.setEmail(updatedEntry.getEmail());
existingEntry.setMessage(updatedEntry.getMessage());
return repository.save(existingEntry);
```

---

### `deleteEntry(Long id)`

```java
public void deleteEntry(Long id)
```

| Aspect | Detail |
|---|---|
| Transaction | Read-write (class-level default) |
| Steps | 1. `repository.existsById(id)` — confirm entry exists. 2. If absent, throw `RuntimeException`. 3. `repository.deleteById(id)`. |
| Returns | `void` |
| Throws | `RuntimeException("Entry not found with id: " + id)` if entry does not exist. |

> **Why check existence before delete?** `JpaRepository.deleteById()` silently succeeds even if the record does not exist (it wraps `findById` + delete). The explicit `existsById` check ensures callers (controllers) get a meaningful error they can map to HTTP 404.

---

## Error Handling Strategy

The service layer uses **unchecked `RuntimeException`** for "not found" scenarios rather than a custom checked exception. This is a deliberate simplicity trade-off for a demo application.

| Scenario | Exception Thrown | Controller Response |
|---|---|---|
| `updateEntry` — ID not found | `RuntimeException("Entry not found with id: X")` | 404 Not Found |
| `deleteEntry` — ID not found | `RuntimeException("Entry not found with id: X")` | 404 Not Found |

Both controllers wrap the service call in a `try/catch (RuntimeException e)` block and return `ResponseEntity.notFound().build()`.

> **Extension point:** In a production application, replace `RuntimeException` with a custom `EntryNotFoundException extends RuntimeException` and use a `@ControllerAdvice` / `@ExceptionHandler` for centralised error mapping.

---

## Transaction Boundary Diagram

```
Controller (no transaction)
    │
    │ calls service method
    ▼
GuestBookService (@Transactional)
    │  ┌─────────────────────────────────────┐
    │  │  Transaction begins                 │
    │  │                                     │
    │  │  repository.findById / save /       │
    │  │  deleteById / existsById            │
    │  │                                     │
    │  │  @PrePersist / @PreUpdate fire      │
    │  │  (still inside transaction)         │
    │  │                                     │
    │  │  Transaction commits (or rolls back │
    │  │  on unchecked exception)            │
    │  └─────────────────────────────────────┘
    │
    ▼
Controller (receives result / exception)
```

---

## Constructor and Dependency Injection

```java
@Service
@Transactional
public class GuestBookService {

    private final GuestBookEntryRepository guestBookEntryRepository;

    public GuestBookService(GuestBookEntryRepository guestBookEntryRepository) {
        this.guestBookEntryRepository = guestBookEntryRepository;
    }
    // ...
}
```

Constructor injection (not field injection) is used throughout, making the class unit-testable without a Spring context — the repository can be passed as a Mockito mock directly.

---

## Test Coverage

`GuestBookServiceTest` covers all 5 methods with 8 unit tests. See `specs/07-testing-strategy.md` for details.
