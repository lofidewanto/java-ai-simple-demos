# User Story #10 — Task Plan
# Create a Simple Web Application for Order Processing

**GitHub Issue:** [#10](https://github.com/lofidewanto/java-ai-simple-demos/issues/10)
**Depends on:** [#7 — Prozess: Bestellabwicklung](https://github.com/lofidewanto/java-ai-simple-demos/issues/7)
**Full specifications:** See `specs/` directory (01 through 08)

---

## User Story

> Create a simple web application that implements the order process defined in #7.
> The application should directly implement the business process in code instead of building a generic workflow engine.
> The goal is to demonstrate how a business process can be implemented as a simple state-based web application using a lightweight Spring Boot architecture.

---

## Business Process (from #7)

```
Customer submits order
        │
        ▼
Shop checks inventory
        │
   ┌────┴──────────────────────┐
   │ Available                 │ Not Available
   ▼                           ▼
Payment collected        Customer notified
   │                           (terminal)
   ▼
Item shipped
(terminal)
```

---

## Implementation Overview

- **Stack:** Java 21, Spring Boot 3.5.x, Thymeleaf, Bootstrap 5.3, Spring Data JPA, H2, Flyway, Springdoc OpenAPI
- **Architecture:** 4-layer MVC — Presentation → Service (state machine) → Data Access → Domain
- **UI:** Thymeleaf server-side rendered, Bootstrap 5.3 via CDN, clean and modern look & feel
- **No external workflow engine** — state transitions encoded as a `switch` statement in `OrderService`

---

## Detailed Task List

### Phase 1 — Project Foundation

| # | Task | Spec Reference | Complexity |
|---|---|---|---|
| 1.1 | Update `application.properties` with full H2, JPA, Flyway, Thymeleaf, and Springdoc configuration | `specs/08` | Low |
| 1.2 | Create Flyway migration `src/main/resources/db/migration/V1__Create_orders_table.sql` | `specs/05` | Low |

**Acceptance criteria:**
- Application starts with `./mvnw spring-boot:run` without errors.
- H2 console accessible at `http://localhost:8080/h2-console`.
- `orders` table visible in H2 console with all columns.

---

### Phase 2 — Domain Layer

| # | Task | Spec Reference | Complexity |
|---|---|---|---|
| 2.1 | Create `OrderStatus` enum with 5 values: `SUBMITTED`, `CHECKING_INVENTORY`, `PAYMENT_COLLECTED`, `SHIPPED`, `CUSTOMER_NOTIFIED` | `specs/02` | Low |
| 2.2 | Create `Order` JPA entity with all fields, `@Enumerated(EnumType.STRING)`, and `@PrePersist` / `@PreUpdate` lifecycle callbacks | `specs/02` | Medium |

**File locations:**
```
src/main/java/com/example/demo_sdd_spring/model/OrderStatus.java
src/main/java/com/example/demo_sdd_spring/model/Order.java
```

**Acceptance criteria:**
- `Order` entity maps to `orders` table — Hibernate `validate` passes on startup.
- `@PrePersist` sets `createdAt`, `updatedAt`, and defaults `status` to `SUBMITTED` if null.
- `@PreUpdate` refreshes `updatedAt` only.

---

### Phase 3 — Data Access Layer

| # | Task | Spec Reference | Complexity |
|---|---|---|---|
| 3.1 | Create `OrderRepository` extending `JpaRepository<Order, Long>` with `findAllByOrderByCreatedAtDesc()` | `specs/05` | Low |
| 3.2 | Create `DataInitializer` `@Configuration` bean with `CommandLineRunner` that seeds 3 orders in different states | `specs/05` | Medium |

**File locations:**
```
src/main/java/com/example/demo_sdd_spring/repository/OrderRepository.java
src/main/java/com/example/demo_sdd_spring/config/DataInitializer.java
```

**Seed data (3 orders, different states for demonstration):**
| Customer | Product | Qty | Final State |
|---|---|---|---|
| Alice Johnson | Wireless Headphones | 2 | `SHIPPED` (full happy path) |
| Bob Smith | Mechanical Keyboard | 1 | `CUSTOMER_NOTIFIED` (unavailable path) |
| Carol Williams | USB-C Hub | 3 | `SUBMITTED` (freshly submitted) |

**Acceptance criteria:**
- Application starts and `DataInitializer` runs without errors.
- H2 console shows 3 rows in `orders` table with correct statuses.
- Audit trail in `notes` column populated for Alice and Bob's orders.

---

### Phase 4 — Service Layer (State Machine)

| # | Task | Spec Reference | Complexity |
|---|---|---|---|
| 4.1 | Create `OrderService` with `@Service @Transactional` class-level annotation and constructor-injected `OrderRepository` | `specs/04` | Low |
| 4.2 | Implement `getAllOrders()` — `@Transactional(readOnly=true)`, delegates to `findAllByOrderByCreatedAtDesc()` | `specs/04` | Low |
| 4.3 | Implement `getOrderById(Long id)` — returns `Optional<Order>` | `specs/04` | Low |
| 4.4 | Implement `createOrder(Order order)` — sets status to `SUBMITTED`, appends initial audit line, saves | `specs/04` | Low |
| 4.5 | Implement `transitionOrder(Long id, String action)` — state machine `switch`, appends audit line, saves | `specs/04` | **High** |
| 4.6 | Implement private helpers `appendAuditLine(Order, String)` and `timestamp()` | `specs/04` | Low |

**File location:**
```
src/main/java/com/example/demo_sdd_spring/service/OrderService.java
```

**State machine transition table (must be exact):**

| Current Status | Action | New Status | Error |
|---|---|---|---|
| `SUBMITTED` | `CHECK_INVENTORY` | `CHECKING_INVENTORY` | — |
| `SUBMITTED` | anything else | — | `IllegalStateException` |
| `CHECKING_INVENTORY` | `MARK_AVAILABLE` | `PAYMENT_COLLECTED` | — |
| `CHECKING_INVENTORY` | `MARK_UNAVAILABLE` | `CUSTOMER_NOTIFIED` | — |
| `CHECKING_INVENTORY` | anything else | — | `IllegalStateException` |
| `PAYMENT_COLLECTED` | `SHIP` | `SHIPPED` | — |
| `PAYMENT_COLLECTED` | anything else | — | `IllegalStateException` |
| `SHIPPED` | any | — | `IllegalStateException` (terminal) |
| `CUSTOMER_NOTIFIED` | any | — | `IllegalStateException` (terminal) |
| _(not found)_ | any | — | `RuntimeException` |

**Acceptance criteria:**
- All transitions produce the correct `OrderStatus` change.
- `notes` field has a new timestamped line appended after each transition.
- Invalid transitions throw `IllegalStateException`.
- Not-found throws `RuntimeException`.

---

### Phase 5 — REST API Layer

| # | Task | Spec Reference | Complexity |
|---|---|---|---|
| 5.1 | Create `TransitionRequest` DTO with `action` field | `specs/03` | Low |
| 5.2 | Create `OrderApiController` with `@RestController @RequestMapping("/api/orders")` | `specs/03` | Medium |
| 5.3 | Implement `GET /api/orders` — returns `ResponseEntity<List<Order>>` 200 | `specs/03` | Low |
| 5.4 | Implement `GET /api/orders/{id}` — returns 200 or 404 | `specs/03` | Low |
| 5.5 | Implement `POST /api/orders` — returns 201 with created order | `specs/03` | Low |
| 5.6 | Implement `POST /api/orders/{id}/transition` — returns 200, 404, or 409 | `specs/03` | Medium |

**File locations:**
```
src/main/java/com/example/demo_sdd_spring/controller/api/TransitionRequest.java
src/main/java/com/example/demo_sdd_spring/controller/api/OrderApiController.java
```

**Error mapping:**
- `RuntimeException` from service → 404 Not Found
- `IllegalStateException` from service → 409 Conflict

**Acceptance criteria:**
- Swagger UI at `http://localhost:8080/swagger-ui.html` shows all endpoints.
- All curl examples from `specs/03` work correctly.
- 409 returned for invalid transitions; 404 for unknown IDs.

---

### Phase 6 — Web UI Layer

| # | Task | Spec Reference | Complexity |
|---|---|---|---|
| 6.1 | Create `src/main/resources/static/css/style.css` with custom overrides (stepper, audit trail, card shadows) | `specs/06` | Medium |
| 6.2 | Create `templates/layout.html` with Bootstrap 5.3 CDN, Bootstrap Icons CDN, sticky navbar, footer | `specs/06` | Medium |
| 6.3 | Create `templates/fragments/status-badge.html` fragment for reusable color-coded status badges | `specs/06` | Low |
| 6.4 | Create `templates/orders/list.html` — table with status badges, empty state, flash messages, New Order button | `specs/06` | Medium |
| 6.5 | Create `templates/orders/form.html` — Bootstrap card form for new order (customer, product, quantity) | `specs/06` | Low |
| 6.6 | Create `templates/orders/detail.html` — order info card, workflow action buttons, stepper, audit trail pre block | `specs/06` | **High** |
| 6.7 | Create `OrderWebController` with all 6 routes and PRG pattern | `specs/06` | Medium |

**File locations:**
```
src/main/resources/static/css/style.css
src/main/resources/templates/layout.html
src/main/resources/templates/fragments/status-badge.html
src/main/resources/templates/orders/list.html
src/main/resources/templates/orders/form.html
src/main/resources/templates/orders/detail.html
src/main/java/com/example/demo_sdd_spring/controller/web/OrderWebController.java
```

**Route map:**
| Method | Path | Handler | Result |
|---|---|---|---|
| `GET` | `/` | `redirectToOrders()` | `redirect:/orders` |
| `GET` | `/orders` | `listOrders(Model)` | `orders/list` |
| `GET` | `/orders/new` | `showNewOrderForm(Model)` | `orders/form` |
| `POST` | `/orders` | `createOrder(...)` | `redirect:/orders` |
| `GET` | `/orders/{id}` | `showOrderDetail(...)` | `orders/detail` |
| `POST` | `/orders/{id}/transition` | `transitionOrder(...)` | `redirect:/orders/{id}` |

**Workflow action buttons on detail page (per state):**
| Status | Button(s) shown | Bootstrap color |
|---|---|---|
| `SUBMITTED` | "Check Inventory" | `btn-warning` |
| `CHECKING_INVENTORY` | "Mark Available" + "Mark Unavailable" | `btn-success` + `btn-danger` |
| `PAYMENT_COLLECTED` | "Ship Order" | `btn-primary` |
| `SHIPPED` | No buttons — "Order complete" alert | `alert-success` |
| `CUSTOMER_NOTIFIED` | No buttons — "Order closed" alert | `alert-secondary` |

**Status badge colors:**
| Status | Badge class |
|---|---|
| `SUBMITTED` | `bg-secondary` |
| `CHECKING_INVENTORY` | `bg-info text-dark` |
| `PAYMENT_COLLECTED` | `bg-primary` |
| `SHIPPED` | `bg-success` |
| `CUSTOMER_NOTIFIED` | `bg-warning text-dark` |

**Acceptance criteria:**
- Order list renders with correct status badges and no errors.
- New order form submits and redirects with success flash message.
- Detail page shows only the valid action buttons for the current order state.
- Action buttons trigger transitions and redirect back to detail page.
- Terminal state orders show info alert instead of action buttons.
- Audit trail displayed in monospace pre block.
- Workflow stepper highlights active state.

---

### Phase 7 — Tests

| # | Task | Spec Reference | Complexity |
|---|---|---|---|
| 7.1 | Update `DemoSddNoworkflowSpringApplicationTests` — verify context loads with new beans | `specs/07` | Low |
| 7.2 | Create `OrderServiceTest` — 10 unit tests covering all methods and all transition paths | `specs/07` | **High** |
| 7.3 | Create `OrderRepositoryTest` — 5 JPA slice tests | `specs/07` | Medium |
| 7.4 | Create `OrderApiControllerTest` — 8 MockMvc tests covering all endpoints and error responses | `specs/07` | Medium |
| 7.5 | Create `OrderWebControllerTest` — 7 MockMvc tests covering all routes | `specs/07` | Medium |

**File locations:**
```
src/test/java/com/example/demo_sdd_spring/DemoSddNoworkflowSpringApplicationTests.java
src/test/java/com/example/demo_sdd_spring/service/OrderServiceTest.java
src/test/java/com/example/demo_sdd_spring/repository/OrderRepositoryTest.java
src/test/java/com/example/demo_sdd_spring/controller/api/OrderApiControllerTest.java
src/test/java/com/example/demo_sdd_spring/controller/web/OrderWebControllerTest.java
```

**`OrderServiceTest` must cover these transitions:**
- Happy path: `CHECK_INVENTORY` → `CHECKING_INVENTORY`
- Happy path: `MARK_AVAILABLE` → `PAYMENT_COLLECTED`
- Happy path: `MARK_UNAVAILABLE` → `CUSTOMER_NOTIFIED`
- Happy path: `SHIP` → `SHIPPED`
- Error: invalid action for state → `IllegalStateException`
- Error: action on terminal state → `IllegalStateException`
- Error: order not found → `RuntimeException`

**Acceptance criteria:**
- `./mvnw test` passes with 0 failures.
- All ~31 tests green.

---

## Implementation Order (Recommended)

```
Phase 1 (Foundation) → Phase 2 (Domain) → Phase 3 (Data) → Phase 4 (Service)
    → Phase 5 (REST API) → Phase 6 (Web UI) → Phase 7 (Tests)
```

Each phase can be verified independently before proceeding to the next.

---

## Files to Create / Modify Summary

### New files
```
src/main/resources/application.properties                               (update)
src/main/resources/db/migration/V1__Create_orders_table.sql             (new)
src/main/resources/static/css/style.css                                 (new)
src/main/resources/templates/layout.html                                (new)
src/main/resources/templates/fragments/status-badge.html                (new)
src/main/resources/templates/orders/list.html                           (new)
src/main/resources/templates/orders/form.html                           (new)
src/main/resources/templates/orders/detail.html                         (new)
src/main/java/com/example/demo_sdd_spring/model/OrderStatus.java        (new)
src/main/java/com/example/demo_sdd_spring/model/Order.java              (new)
src/main/java/com/example/demo_sdd_spring/repository/OrderRepository.java (new)
src/main/java/com/example/demo_sdd_spring/config/DataInitializer.java   (new)
src/main/java/com/example/demo_sdd_spring/service/OrderService.java     (new)
src/main/java/com/example/demo_sdd_spring/controller/api/TransitionRequest.java (new)
src/main/java/com/example/demo_sdd_spring/controller/api/OrderApiController.java (new)
src/main/java/com/example/demo_sdd_spring/controller/web/OrderWebController.java (new)
src/test/java/com/example/demo_sdd_spring/service/OrderServiceTest.java (new)
src/test/java/com/example/demo_sdd_spring/repository/OrderRepositoryTest.java (new)
src/test/java/com/example/demo_sdd_spring/controller/api/OrderApiControllerTest.java (new)
src/test/java/com/example/demo_sdd_spring/controller/web/OrderWebControllerTest.java (new)
```

### Modified files
```
src/main/resources/application.properties
src/test/java/com/example/demo_sdd_spring/DemoSddNoworkflowSpringApplicationTests.java
```

---

## Estimated Test Count

| Class | Tests |
|---|---|
| `DemoSddNoworkflowSpringApplicationTests` | 1 |
| `OrderServiceTest` | 10 |
| `OrderRepositoryTest` | 5 |
| `OrderApiControllerTest` | 8 |
| `OrderWebControllerTest` | 7 |
| **Total** | **31** |

---

### Phase 8 — End-to-End Browser Test (Chrome web MCP)

| # | Task | Scenario | Steps |
|---|---|---|---|
| 8.1 | Happy path — full workflow to `SHIPPED` | Create order → advance through all states | 1. Navigate to `http://localhost:8080/orders`. 2. Click "New Order". 3. Fill in customer name, product, quantity and submit. 4. Verify redirect to order list with new order in `SUBMITTED` state. 5. Click the new order. 6. Click "Check Inventory" → verify status changes to `CHECKING_INVENTORY`. 7. Click "Mark Available" → verify status changes to `PAYMENT_COLLECTED`. 8. Click "Ship Order" → verify status changes to `SHIPPED`. 9. Verify no action buttons shown — "Order complete" alert displayed. 10. Verify audit trail shows 4 timestamped lines. |
| 8.2 | Unavailable path — workflow to `CUSTOMER_NOTIFIED` | Create order → mark as unavailable | 1. Navigate to `http://localhost:8080/orders`. 2. Click "New Order". 3. Fill in customer name, product, quantity and submit. 4. Click the new order. 5. Click "Check Inventory" → verify status changes to `CHECKING_INVENTORY`. 6. Click "Mark Unavailable" → verify status changes to `CUSTOMER_NOTIFIED`. 7. Verify no action buttons shown — "Order closed" alert displayed. 8. Verify audit trail shows 3 timestamped lines. |
| 8.3 | List & navigation — seed data verification | Verify 3 seed orders are visible with correct badges | 1. Navigate to `http://localhost:8080/orders`. 2. Verify Alice Johnson's order shows `SHIPPED` badge (green). 3. Verify Bob Smith's order shows `CUSTOMER_NOTIFIED` badge (amber). 4. Verify Carol Williams's order shows `SUBMITTED` badge (gray). 5. Click each order and verify detail page loads with correct state, action buttons, and audit trail. |

**Tooling:** Chrome web MCP — browser-based click-through (not a shell script).

**Prerequisites:** Application must be running (`./mvnw spring-boot:run`) and `DataInitializer` seed data loaded before running scenarios 8.1–8.3.

**Acceptance criteria:**
- All 3 scenarios complete without errors.
- Status badges reflect correct Bootstrap color classes at each step.
- Audit trail pre block grows by one line after each transition.
- Terminal-state orders show info alert instead of action buttons.
- Swagger UI accessible at `http://localhost:8080/swagger-ui.html` and lists all endpoints.
