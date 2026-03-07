# 04 — Service Layer Design

## Purpose

This document specifies the business logic layer of the Order Processing application — `OrderService` — including its responsibilities, method contracts, the state machine implementation, transaction boundaries, and error handling strategy.

---

## Service Class

**Class:** `com.example.demo_sdd_spring.service.OrderService`
**Annotations:** `@Service`, `@Transactional` (class-level)
**Dependency:** Constructor-injected `OrderRepository`

---

## Design Decisions

### No Workflow Engine

The order process has a fixed, well-understood state graph with 4 transitions. Encoding it as a `switch` statement in the service layer is:

- **Simpler** — no additional framework dependency or configuration.
- **Testable** — pure Java; all paths can be unit-tested with a mocked repository.
- **Transparent** — the business rules are visible directly in the source code, not buried in XML or a DSL.

### No Service Interface

`OrderService` is a concrete class — no `IOrderService` / `OrderServiceImpl` pair. Spring CGLIB-proxies the class for `@Transactional`.

### Class-Level `@Transactional`

All methods are transactional by default. Read methods override with `@Transactional(readOnly = true)`.

### Audit Trail in `notes`

The service appends a timestamped line to `notes` on every state transition. The entity never sets `notes` itself — this is purely a service-layer concern.

---

## Method Contracts

### `getAllOrders()`

```java
@Transactional(readOnly = true)
public List<Order> getAllOrders()
```

| Aspect | Detail |
|---|---|
| Transaction | Read-only |
| Delegates to | `repository.findAllByOrderByCreatedAtDesc()` |
| Returns | `List<Order>` — all orders, newest first. Empty list if none exist. |
| Throws | Nothing. |

---

### `getOrderById(Long id)`

```java
@Transactional(readOnly = true)
public Optional<Order> getOrderById(Long id)
```

| Aspect | Detail |
|---|---|
| Transaction | Read-only |
| Delegates to | `repository.findById(id)` |
| Returns | `Optional<Order>` — present if found, empty if not. |
| Throws | Nothing — callers must check `Optional.isPresent()`. |

---

### `createOrder(Order order)`

```java
public Order createOrder(Order order)
```

| Aspect | Detail |
|---|---|
| Transaction | Read-write (class-level default) |
| Steps | 1. Sets `status` to `SUBMITTED`. 2. Appends initial audit line to `notes`. 3. `repository.save(order)`. |
| Returns | The persisted `Order` with `id`, `status`, `notes`, `createdAt`, `updatedAt` populated. |
| Side effects | `@PrePersist` callback fires, setting timestamps. |
| Throws | Nothing explicitly. |

Implementation detail:

```java
public Order createOrder(Order order) {
    order.setStatus(OrderStatus.SUBMITTED);
    order.setNotes(timestamp() + " — Order submitted by customer");
    return orderRepository.save(order);
}
```

---

### `transitionOrder(Long id, String action)`

```java
public Order transitionOrder(Long id, String action)
```

| Aspect | Detail |
|---|---|
| Transaction | Read-write (class-level default) |
| Steps | 1. `repository.findById(id)` — fetch order. 2. If absent, throw `RuntimeException`. 3. Apply transition via switch logic (see below). 4. Append audit line to `notes`. 5. `repository.save(order)`. |
| Returns | The updated, persisted `Order`. |
| Throws | `RuntimeException("Order not found with id: " + id)` if order does not exist. |
| Throws | `IllegalStateException("Invalid action '" + action + "' for status " + order.getStatus())` for any invalid transition (wrong action for current state, or terminal state). |
| Side effects | `@PreUpdate` fires, refreshing `updatedAt`. |

### State Machine Switch Logic

```java
OrderStatus current = order.getStatus();
OrderStatus next;

switch (current) {
    case SUBMITTED:
        if ("CHECK_INVENTORY".equals(action)) {
            next = OrderStatus.CHECKING_INVENTORY;
        } else {
            throw new IllegalStateException(
                "Invalid action '" + action + "' for status " + current);
        }
        break;

    case CHECKING_INVENTORY:
        if ("MARK_AVAILABLE".equals(action)) {
            next = OrderStatus.PAYMENT_COLLECTED;
        } else if ("MARK_UNAVAILABLE".equals(action)) {
            next = OrderStatus.CUSTOMER_NOTIFIED;
        } else {
            throw new IllegalStateException(
                "Invalid action '" + action + "' for status " + current);
        }
        break;

    case PAYMENT_COLLECTED:
        if ("SHIP".equals(action)) {
            next = OrderStatus.SHIPPED;
        } else {
            throw new IllegalStateException(
                "Invalid action '" + action + "' for status " + current);
        }
        break;

    case SHIPPED:
    case CUSTOMER_NOTIFIED:
        throw new IllegalStateException(
            "Order is in a terminal state: " + current);

    default:
        throw new IllegalStateException("Unknown status: " + current);
}

order.setStatus(next);
appendAuditLine(order, "Status changed to " + next.name());
return orderRepository.save(order);
```

### `appendAuditLine(Order order, String message)` — Private Helper

```java
private void appendAuditLine(Order order, String message) {
    String line = timestamp() + " — " + message;
    String existing = order.getNotes();
    order.setNotes((existing == null || existing.isBlank()) ? line : existing + "\n" + line);
}

private String timestamp() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
}
```

---

## Error Handling Strategy

| Scenario | Exception Thrown | HTTP Mapping |
|---|---|---|
| `transitionOrder` — ID not found | `RuntimeException("Order not found with id: X")` | 404 Not Found |
| `transitionOrder` — invalid action for state | `IllegalStateException(...)` | 409 Conflict |
| `transitionOrder` — terminal state | `IllegalStateException(...)` | 409 Conflict |
| `getOrderById` — not found | _(none — returns `Optional.empty()`)_ | 404 Not Found |

Controllers use `try/catch` to map these to the appropriate HTTP responses. See `specs/03-api-spec.md`.

> **Extension point:** In a production application, replace `RuntimeException` with `OrderNotFoundException extends RuntimeException` and `IllegalStateException` with `InvalidTransitionException extends RuntimeException`, then use `@ControllerAdvice` / `@ExceptionHandler` for centralised error mapping.

---

## Transaction Boundary Diagram

```
Controller (no transaction)
    │
    │ calls service method
    ▼
OrderService (@Transactional)
    │  ┌─────────────────────────────────────┐
    │  │  Transaction begins                 │
    │  │                                     │
    │  │  repository.findById / save         │
    │  │                                     │
    │  │  State machine logic executes       │
    │  │  (pure Java — no I/O)              │
    │  │                                     │
    │  │  appendAuditLine modifies `notes`   │
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
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ...
}
```

Constructor injection makes the class unit-testable without a Spring context — the repository can be passed as a Mockito mock directly.

---

## Test Coverage

`OrderServiceTest` covers all 4 methods with 10 unit tests, including all valid transition paths and all invalid-transition / terminal-state error paths. See `specs/07-testing-strategy.md` for details.
