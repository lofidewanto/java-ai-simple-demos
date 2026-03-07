# 07 — Testing Strategy

## Purpose

This document describes the test pyramid, all test classes, tools and annotations used, mocking strategy, and the expected coverage for the Order Processing application.

---

## Test Pyramid

```
           ┌──────────────────────┐
           │   Integration Tests  │  1 test
           │  (@SpringBootTest)   │  Full Spring context
           └──────────────────────┘
          ┌────────────────────────────┐
          │    Controller Slice Tests  │  15 tests
          │  (@WebMvcTest — 2 classes) │  Web layer only
          └────────────────────────────┘
         ┌──────────────────────────────────┐
         │       Repository Slice Tests     │  5 tests
         │         (@DataJpaTest)           │  JPA layer only
         └──────────────────────────────────┘
        ┌────────────────────────────────────────┐
        │           Unit Tests                   │  10 tests
        │  (@ExtendWith(MockitoExtension.class))  │  No Spring context
        └────────────────────────────────────────┘
```

**Total: ~31 tests across 5 test classes.**

---

## Test Tools

| Tool | Version | Purpose |
|---|---|---|
| JUnit 5 (Jupiter) | Boot-managed | Test runner and assertions |
| Mockito | Boot-managed | Mocking dependencies in unit tests |
| MockMvc | Boot-managed | Simulating HTTP requests in controller slice tests |
| Spring Boot Test | Boot-managed | `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest` slices |
| H2 (in-memory) | Boot-managed | Embedded DB for repository and integration tests |
| Jackson `ObjectMapper` | Boot-managed | Serialising/deserialising JSON in API controller tests |

All are provided by `spring-boot-starter-test` — no additional test dependencies are required.

---

## Test Classes

### 1. `DemoSddNoworkflowSpringApplicationTests` — Integration Test

**File:** `src/test/java/com/example/demo_sdd_spring/DemoSddNoworkflowSpringApplicationTests.java`
**Annotation:** `@SpringBootTest`
**Tests:** 1

| Test | Description |
|---|---|
| `contextLoads()` | Starts the full Spring context (Flyway migrations, DataInitializer with all transitions, all beans). Passes if no exception is thrown. |

**Purpose:** Smoke test — verifies the entire wiring is correct and the application can start with the seeded data.

---

### 2. `OrderServiceTest` — Unit Tests

**File:** `src/test/java/com/example/demo_sdd_spring/service/OrderServiceTest.java`
**Annotation:** `@ExtendWith(MockitoExtension.class)`
**Tests:** 10

**Setup:**
```java
@Mock
OrderRepository orderRepository;

@InjectMocks
OrderService orderService;
```

No Spring context loaded. Mockito creates the mock and injects it via constructor.

| Test | Method Under Test | Scenario | Verification |
|---|---|---|---|
| `getAllOrders_ShouldReturnAllOrders` | `getAllOrders()` | Repository returns 2 orders | `findAllByOrderByCreatedAtDesc()` called once; list size = 2 |
| `getOrderById_WhenExists_ShouldReturnOrder` | `getOrderById(Long)` | Repository returns `Optional.of(order)` | Optional is present; fields match |
| `getOrderById_WhenNotExists_ShouldReturnEmpty` | `getOrderById(Long)` | Repository returns `Optional.empty()` | Optional is empty |
| `createOrder_ShouldSetSubmittedStatusAndSave` | `createOrder(order)` | `repository.save()` returns the order | Status is `SUBMITTED`; notes contain initial audit line; `save()` called once |
| `transitionOrder_CheckInventory_ShouldSucceed` | `transitionOrder(id, "CHECK_INVENTORY")` | Order is `SUBMITTED` | Status becomes `CHECKING_INVENTORY`; notes appended; `save()` called |
| `transitionOrder_MarkAvailable_ShouldSucceed` | `transitionOrder(id, "MARK_AVAILABLE")` | Order is `CHECKING_INVENTORY` | Status becomes `PAYMENT_COLLECTED`; notes appended |
| `transitionOrder_MarkUnavailable_ShouldSucceed` | `transitionOrder(id, "MARK_UNAVAILABLE")` | Order is `CHECKING_INVENTORY` | Status becomes `CUSTOMER_NOTIFIED`; notes appended |
| `transitionOrder_Ship_ShouldSucceed` | `transitionOrder(id, "SHIP")` | Order is `PAYMENT_COLLECTED` | Status becomes `SHIPPED`; notes appended |
| `transitionOrder_InvalidAction_ShouldThrowIllegalStateException` | `transitionOrder(id, "SHIP")` | Order is `SUBMITTED` (wrong state) | `IllegalStateException` thrown |
| `transitionOrder_TerminalState_ShouldThrowIllegalStateException` | `transitionOrder(id, "SHIP")` | Order is `SHIPPED` (terminal) | `IllegalStateException` thrown |

**Additional edge case (optional):**

| Test | Scenario | Verification |
|---|---|---|
| `transitionOrder_WhenNotFound_ShouldThrowRuntimeException` | `findById` returns empty | `RuntimeException` thrown |

---

### 3. `OrderRepositoryTest` — Repository Slice Tests

**File:** `src/test/java/com/example/demo_sdd_spring/repository/OrderRepositoryTest.java`
**Annotation:** `@DataJpaTest`
**Tests:** 5

`@DataJpaTest` configures:
- An embedded H2 database (separate from the main app instance).
- Flyway or `ddl-auto=create-drop` (Spring Boot auto-configures for test slice).
- Only JPA-related beans — no web layer, no services.
- Each test runs in a transaction rolled back after the test.

| Test | Scenario | Verification |
|---|---|---|
| `saveOrder_ShouldPersistWithSubmittedStatus` | Save a new order with status `SUBMITTED` | ID is assigned; `createdAt` / `updatedAt` are not null; status is `SUBMITTED` |
| `findById_WhenExists_ShouldReturnOrder` | Save then find by ID | Optional is present; `customerName`, `productName`, `quantity` match |
| `findById_WhenNotExists_ShouldReturnEmpty` | Find by non-existent ID | Optional is empty |
| `findAllByOrderByCreatedAtDesc_ShouldReturnNewestFirst` | Save 2 orders with `Thread.sleep(100)` between them | List has 2 entries; first entry has later `createdAt` than second |
| `updateOrder_ShouldPersistStatusChange` | Save order with `SUBMITTED`, update status to `CHECKING_INVENTORY`, save again | Retrieved order has `status = CHECKING_INVENTORY`; `updatedAt` is refreshed |

> **Note on `Thread.sleep(100)`:** Used in the ordering test to guarantee two distinct `createdAt` timestamps, since `LocalDateTime.now()` resolution may not distinguish two rapid consecutive inserts on all JVMs.

---

### 4. `OrderApiControllerTest` — API Controller Slice Tests

**File:** `src/test/java/com/example/demo_sdd_spring/controller/api/OrderApiControllerTest.java`
**Annotation:** `@WebMvcTest(OrderApiController.class)`
**Tests:** 8

`@WebMvcTest` configures:
- Only the web layer.
- `MockMvc` auto-wired for HTTP simulation.
- `OrderService` replaced by `@MockBean`.

**Setup:**
```java
@Autowired MockMvc mockMvc;
@Autowired ObjectMapper objectMapper;
@MockBean  OrderService orderService;
```

| Test | HTTP | Path | Mock Setup | Expected Status | Verification |
|---|---|---|---|---|---|
| `getAllOrders_ShouldReturnListOfOrders` | GET | `/api/orders` | service returns 2-order list | 200 | JSON array length = 2; first order's `customerName` correct |
| `getOrderById_WhenExists_ShouldReturnOrder` | GET | `/api/orders/1` | service returns `Optional.of(order)` | 200 | JSON `id`, `customerName`, `status` match |
| `getOrderById_WhenNotExists_ShouldReturn404` | GET | `/api/orders/999` | service returns `Optional.empty()` | 404 | Empty body |
| `createOrder_ShouldReturnCreatedOrder` | POST | `/api/orders` | service returns saved order with `SUBMITTED` | 201 | JSON body matches; `Content-Type: application/json` |
| `transitionOrder_ValidAction_ShouldReturnUpdatedOrder` | POST | `/api/orders/1/transition` | service returns updated order | 200 | JSON `status` reflects new state |
| `transitionOrder_InvalidAction_ShouldReturn409` | POST | `/api/orders/1/transition` | service throws `IllegalStateException` | 409 | Empty body |
| `transitionOrder_OrderNotFound_ShouldReturn404` | POST | `/api/orders/999/transition` | service throws `RuntimeException` | 404 | Empty body |
| `transitionOrder_TerminalState_ShouldReturn409` | POST | `/api/orders/1/transition` | service throws `IllegalStateException` (terminal) | 409 | Empty body |

---

### 5. `OrderWebControllerTest` — Web Controller Slice Tests

**File:** `src/test/java/com/example/demo_sdd_spring/controller/web/OrderWebControllerTest.java`
**Annotation:** `@WebMvcTest(OrderWebController.class)`
**Tests:** 7

Same slice setup as the API controller test, but targeting the Thymeleaf controller. MockMvc simulates browser form submissions.

| Test | HTTP | Path | Mock Setup | Expected Status | Verification |
|---|---|---|---|---|---|
| `redirectToOrders_ShouldRedirectToOrdersPage` | GET | `/` | — | 3xx | Redirect location = `/orders` |
| `listOrders_ShouldShowOrdersList` | GET | `/orders` | service returns 2-order list | 200 | View name = `orders/list`; model contains `orders` attribute |
| `showNewOrderForm_ShouldShowForm` | GET | `/orders/new` | — | 200 | View = `orders/form`; model contains empty `order` |
| `createOrder_ShouldRedirectToList` | POST | `/orders` | service returns saved order | 3xx | Redirect to `/orders` |
| `showOrderDetail_WhenExists_ShouldShowDetail` | GET | `/orders/1` | service returns `Optional.of(order)` | 200 | View = `orders/detail`; model contains `order` |
| `showOrderDetail_WhenNotExists_ShouldRedirectWithError` | GET | `/orders/999` | service returns `Optional.empty()` | 3xx | Redirect to `/orders` |
| `transitionOrder_ShouldRedirectToDetail` | POST | `/orders/1/transition` | service transitions successfully | 3xx | Redirect to `/orders/1` |

---

## Mocking Strategy Summary

| Test Class | Spring Context | Service Mock | Repository Mock |
|---|---|---|---|
| `DemoSddNoworkflowSpringApplicationTests` | Full (`@SpringBootTest`) | Real bean | Real bean |
| `OrderServiceTest` | None (pure unit) | `@InjectMocks` (real) | `@Mock` (Mockito) |
| `OrderRepositoryTest` | JPA slice (`@DataJpaTest`) | Not loaded | Real bean (H2) |
| `OrderApiControllerTest` | Web slice (`@WebMvcTest`) | `@MockBean` (Mockito) | Not loaded |
| `OrderWebControllerTest` | Web slice (`@WebMvcTest`) | `@MockBean` (Mockito) | Not loaded |

---

## Running Tests

### Run all tests
```bash
./mvnw test
```

### Run a specific test class
```bash
./mvnw test -Dtest=OrderServiceTest
./mvnw test -Dtest=OrderApiControllerTest
./mvnw test -Dtest=OrderRepositoryTest
```

### Run with verbose output
```bash
./mvnw test -Dsurefire.useFile=false
```

---

## Coverage Goals

| Layer | Approach | Target |
|---|---|---|
| Service | Pure unit tests with mocked repository | All public methods; all valid transition paths; all invalid/terminal transition error paths |
| Repository | JPA slice tests with real H2 | All CRUD operations + custom query method + status update persistence |
| API Controller | Web slice with MockMvc | All endpoints × success + error paths (404, 409) |
| Web Controller | Web slice with MockMvc | All routes × primary paths |
| Application startup | Full integration test | Context loads without error; DataInitializer runs all transitions successfully |

> **Extension:** Add `jacoco-maven-plugin` to `pom.xml` to generate HTML coverage reports with `./mvnw verify`.
