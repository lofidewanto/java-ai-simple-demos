# 03 — REST API Specification

## Purpose

This document specifies the complete REST API contract for the Order Processing application. It covers all endpoints, HTTP methods, request/response shapes, status codes, and error cases.

---

## Base URL

```
http://localhost:8080/api/orders
```

---

## Controller

**Class:** `com.example.demo_sdd_spring.controller.api.OrderApiController`
**Annotations:** `@RestController`, `@RequestMapping("/api/orders")`
**OpenAPI tag:** `Orders` — `"Order Processing API"`
**Dependency:** Constructor-injected `OrderService`

---

## OpenAPI / Swagger UI

| Resource | URL |
|---|---|
| Interactive Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI 3 JSON spec | `http://localhost:8080/api-docs` |

Configuration in `application.properties`:
```properties
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
```

---

## Endpoints

### GET /api/orders

**Description:** Retrieve all orders, ordered by creation date descending (newest first).

**Request**
- Body: none
- Parameters: none

**Response — 200 OK**
```json
[
  {
    "id": 3,
    "customerName": "Carol Williams",
    "productName": "USB-C Hub",
    "quantity": 3,
    "status": "SUBMITTED",
    "notes": "2026-03-07 10:02 — Order submitted by customer",
    "createdAt": "2026-03-07T10:02:00",
    "updatedAt": "2026-03-07T10:02:00"
  },
  {
    "id": 1,
    "customerName": "Alice Johnson",
    "productName": "Wireless Headphones",
    "quantity": 2,
    "status": "SHIPPED",
    "notes": "2026-03-07 10:00 — Order submitted by customer\n2026-03-07 10:01 — Status changed to CHECKING_INVENTORY\n2026-03-07 10:01 — Status changed to PAYMENT_COLLECTED\n2026-03-07 10:01 — Status changed to SHIPPED",
    "createdAt": "2026-03-07T10:00:00",
    "updatedAt": "2026-03-07T10:01:00"
  }
]
```

Returns an empty array `[]` when no orders exist.

---

### GET /api/orders/{id}

**Description:** Retrieve a single order by its ID.

**Path Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Long` | yes | The numeric ID of the order |

**Response — 200 OK**
```json
{
  "id": 1,
  "customerName": "Alice Johnson",
  "productName": "Wireless Headphones",
  "quantity": 2,
  "status": "SHIPPED",
  "notes": "2026-03-07 10:00 — Order submitted by customer\n...",
  "createdAt": "2026-03-07T10:00:00",
  "updatedAt": "2026-03-07T10:01:00"
}
```

**Response — 404 Not Found**

Empty body. Returned when no order exists with the given `id`.

---

### POST /api/orders

**Description:** Submit a new order. The order is created with status `SUBMITTED`.

**Request Headers**
```
Content-Type: application/json
```

**Request Body**

| Field | Type | Required | Notes |
|---|---|---|---|
| `customerName` | `String` | yes | Non-null, non-blank |
| `productName` | `String` | yes | Non-null, non-blank |
| `quantity` | `Integer` | yes | Must be >= 1 |

```json
{
  "customerName": "Dave Miller",
  "productName": "Noise-Cancelling Earbuds",
  "quantity": 1
}
```

**Response — 201 Created**

Returns the persisted order with `id`, `status`, `notes`, `createdAt`, `updatedAt` populated by the server:

```json
{
  "id": 4,
  "customerName": "Dave Miller",
  "productName": "Noise-Cancelling Earbuds",
  "quantity": 1,
  "status": "SUBMITTED",
  "notes": "2026-03-07 11:00 — Order submitted by customer",
  "createdAt": "2026-03-07T11:00:00",
  "updatedAt": "2026-03-07T11:00:00"
}
```

---

### POST /api/orders/{id}/transition

**Description:** Advance the order to the next workflow state by applying a named action.

**Path Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Long` | yes | The numeric ID of the order to transition |

**Request Headers**
```
Content-Type: application/json
```

**Request Body**

| Field | Type | Required | Description |
|---|---|---|---|
| `action` | `String` | yes | The transition action to apply (see valid actions below) |

```json
{
  "action": "CHECK_INVENTORY"
}
```

**Valid Actions**

| Action | Required Current Status | Resulting Status |
|---|---|---|
| `CHECK_INVENTORY` | `SUBMITTED` | `CHECKING_INVENTORY` |
| `MARK_AVAILABLE` | `CHECKING_INVENTORY` | `PAYMENT_COLLECTED` |
| `MARK_UNAVAILABLE` | `CHECKING_INVENTORY` | `CUSTOMER_NOTIFIED` |
| `SHIP` | `PAYMENT_COLLECTED` | `SHIPPED` |

**Response — 200 OK**

Returns the updated order:

```json
{
  "id": 4,
  "customerName": "Dave Miller",
  "productName": "Noise-Cancelling Earbuds",
  "quantity": 1,
  "status": "CHECKING_INVENTORY",
  "notes": "2026-03-07 11:00 — Order submitted by customer\n2026-03-07 11:05 — Status changed to CHECKING_INVENTORY",
  "createdAt": "2026-03-07T11:00:00",
  "updatedAt": "2026-03-07T11:05:00"
}
```

**Response — 404 Not Found**

Empty body. Returned when no order exists with the given `id`.

**Response — 409 Conflict**

Empty body. Returned when the action is not valid for the order's current status (e.g., calling `SHIP` on a `SUBMITTED` order, or attempting any action on a terminal state).

---

## Error Handling Summary

| Scenario | HTTP Status | Body |
|---|---|---|
| Order not found (GET by ID) | 404 | empty |
| Order not found (transition) | 404 | empty |
| Invalid transition action | 409 | empty |
| Order in terminal state | 409 | empty |
| Successful creation | 201 | order JSON |
| Malformed JSON body | 400 | Spring default error body |

Error mapping in the controller:

```java
// 404 mapping
try {
    // service call
} catch (RuntimeException e) {
    return ResponseEntity.notFound().build();
}

// 409 mapping for invalid transitions
try {
    Order updated = orderService.transitionOrder(id, action);
    return ResponseEntity.ok(updated);
} catch (IllegalStateException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
} catch (RuntimeException e) {
    return ResponseEntity.notFound().build();
}
```

---

## curl Examples

### List all orders
```bash
curl -X GET http://localhost:8080/api/orders
```

### Get order by ID
```bash
curl -X GET http://localhost:8080/api/orders/1
```

### Submit a new order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Dave Miller","productName":"Noise-Cancelling Earbuds","quantity":1}'
```

### Advance order to next state
```bash
curl -X POST http://localhost:8080/api/orders/4/transition \
  -H "Content-Type: application/json" \
  -d '{"action":"CHECK_INVENTORY"}'
```

### Full happy-path walkthrough (order id=4)
```bash
# 1. Check inventory
curl -X POST http://localhost:8080/api/orders/4/transition \
  -H "Content-Type: application/json" \
  -d '{"action":"CHECK_INVENTORY"}'

# 2. Mark available
curl -X POST http://localhost:8080/api/orders/4/transition \
  -H "Content-Type: application/json" \
  -d '{"action":"MARK_AVAILABLE"}'

# 3. Ship
curl -X POST http://localhost:8080/api/orders/4/transition \
  -H "Content-Type: application/json" \
  -d '{"action":"SHIP"}'
```

---

## Controller Implementation Skeleton

```java
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order Processing API")
public class OrderApiController {

    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order created = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<Order> transitionOrder(
            @PathVariable Long id,
            @RequestBody TransitionRequest request) {
        try {
            Order updated = orderService.transitionOrder(id, request.getAction());
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

### `TransitionRequest` DTO

```java
public class TransitionRequest {
    private String action;

    public TransitionRequest() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
```

Placed in `com.example.demo_sdd_spring.controller.api.TransitionRequest`.
