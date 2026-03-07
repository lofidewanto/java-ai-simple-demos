# 06 — Web UI / MVC Flow

## Purpose

This document specifies the Thymeleaf-based web UI layer: all routes, controller-to-template mappings, model attributes, form flows, Bootstrap component usage, and the redirect-after-POST pattern.

---

## Controller

**Class:** `com.example.demo_sdd_spring.controller.web.OrderWebController`
**Annotation:** `@Controller` (no base `@RequestMapping` — routes defined per method)
**Dependency:** Constructor-injected `OrderService`

---

## Templates

Located in `src/main/resources/templates/`:

```
templates/
├── layout.html           # Shared navbar + footer shell (Thymeleaf fragment)
└── orders/
    ├── list.html         # Order list with status badges and action links
    ├── form.html         # New order submission form
    └── detail.html       # Order detail view + workflow action buttons
```

Static assets in `src/main/resources/static/css/style.css`.
Bootstrap 5.3 loaded from CDN — no local asset bundling required.

---

## Route Map

| Method | Path | Handler | Template / Redirect | Description |
|---|---|---|---|---|
| `GET` | `/` | `redirectToOrders()` | `redirect:/orders` | Root URL redirect |
| `GET` | `/orders` | `listOrders(Model)` | `orders/list` | Show all orders |
| `GET` | `/orders/new` | `showNewOrderForm(Model)` | `orders/form` | Show blank order form |
| `POST` | `/orders` | `createOrder(...)` | `redirect:/orders` | Submit new order |
| `GET` | `/orders/{id}` | `showOrderDetail(...)` | `orders/detail` | Show order detail + action buttons |
| `POST` | `/orders/{id}/transition` | `transitionOrder(...)` | `redirect:/orders/{id}` | Advance workflow state |

> **Note:** HTML forms only support `GET` and `POST`. State transitions are submitted as `POST` with a hidden `action` field — no `_method` override filter is needed.

---

## Handler Details

### `GET /` — Redirect to Orders

```java
@GetMapping("/")
public String redirectToOrders() {
    return "redirect:/orders";
}
```

---

### `GET /orders` — List Orders

```java
@GetMapping("/orders")
public String listOrders(Model model) {
    model.addAttribute("orders", orderService.getAllOrders());
    return "orders/list";
}
```

**Model attributes:**

| Attribute | Type | Description |
|---|---|---|
| `orders` | `List<Order>` | All orders, newest first |

---

### `GET /orders/new` — Show New Order Form

```java
@GetMapping("/orders/new")
public String showNewOrderForm(Model model) {
    model.addAttribute("order", new Order());
    return "orders/form";
}
```

**Model attributes:**

| Attribute | Type | Description |
|---|---|---|
| `order` | `Order` | Empty object for Thymeleaf form binding |

---

### `POST /orders` — Create Order

```java
@PostMapping("/orders")
public String createOrder(@ModelAttribute Order order, RedirectAttributes redirectAttributes) {
    orderService.createOrder(order);
    redirectAttributes.addFlashAttribute("successMessage", "Order submitted successfully!");
    return "redirect:/orders";
}
```

PRG pattern: redirects to `/orders` on success with a flash message.

---

### `GET /orders/{id}` — Show Order Detail

```java
@GetMapping("/orders/{id}")
public String showOrderDetail(@PathVariable Long id, Model model,
                              RedirectAttributes redirectAttributes) {
    Optional<Order> order = orderService.getOrderById(id);
    if (order.isPresent()) {
        model.addAttribute("order", order.get());
        return "orders/detail";
    } else {
        redirectAttributes.addFlashAttribute("errorMessage", "Order not found!");
        return "redirect:/orders";
    }
}
```

**Model attributes (success path):**

| Attribute | Type | Description |
|---|---|---|
| `order` | `Order` | The order to display |

**Error path:** Order not found → redirect to `/orders` with `errorMessage`.

---

### `POST /orders/{id}/transition` — Advance Workflow State

```java
@PostMapping("/orders/{id}/transition")
public String transitionOrder(@PathVariable Long id,
                              @RequestParam String action,
                              RedirectAttributes redirectAttributes) {
    try {
        orderService.transitionOrder(id, action);
        redirectAttributes.addFlashAttribute("successMessage", "Order updated successfully!");
    } catch (IllegalStateException e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid action for current order status.");
    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Order not found!");
    }
    return "redirect:/orders/" + id;
}
```

Always redirects back to the order detail page (`/orders/{id}`) with the appropriate flash message.

---

## Thymeleaf Template Design

### `layout.html` — Shared Shell

All pages extend this layout via `th:replace` or `th:insert` fragments.

**Structure:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title th:text="${pageTitle} ?: 'Order Manager'">Order Manager</title>
    <!-- Bootstrap 5.3 CSS -->
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" />
    <!-- Bootstrap Icons -->
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" />
    <!-- Custom styles -->
    <link rel="stylesheet" th:href="@{/css/style.css}" />
</head>
<body>
    <!-- Sticky top navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark sticky-top">
        <div class="container">
            <a class="navbar-brand fw-bold" th:href="@{/orders}">
                <i class="bi bi-box-seam me-2"></i>Order Manager
            </a>
            <div class="navbar-nav ms-auto">
                <a class="nav-link" th:href="@{/orders}">
                    <i class="bi bi-list-ul me-1"></i>Orders
                </a>
                <a class="nav-link" th:href="@{/orders/new}">
                    <i class="bi bi-plus-circle me-1"></i>New Order
                </a>
            </div>
        </div>
    </nav>

    <!-- Page content slot -->
    <main class="container py-4" th:fragment="content">
        <!-- child templates insert here -->
    </main>

    <!-- Footer -->
    <footer class="border-top py-3 mt-auto bg-light">
        <div class="container text-center text-muted small">
            Order Manager &mdash; demo-sdd-noworkflow-spring
        </div>
    </footer>

    <!-- Bootstrap 5.3 JS bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

---

### `orders/list.html` — Order List

**Key Thymeleaf expressions:**

```html
<!-- Flash messages (dismissible) -->
<div th:if="${successMessage}"
     class="alert alert-success alert-dismissible fade show"
     role="alert">
    <i class="bi bi-check-circle me-2"></i>
    <span th:text="${successMessage}"></span>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
<div th:if="${errorMessage}"
     class="alert alert-danger alert-dismissible fade show"
     role="alert">
    <i class="bi bi-exclamation-triangle me-2"></i>
    <span th:text="${errorMessage}"></span>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>

<!-- Page header with "New Order" button -->
<div class="d-flex justify-content-between align-items-center mb-4">
    <h1 class="h3 mb-0">Orders</h1>
    <a th:href="@{/orders/new}" class="btn btn-primary">
        <i class="bi bi-plus-circle me-1"></i>New Order
    </a>
</div>

<!-- Empty state -->
<div th:if="${#lists.isEmpty(orders)}" class="text-center py-5 text-muted">
    <i class="bi bi-inbox display-4 d-block mb-3"></i>
    <p class="mb-3">No orders yet.</p>
    <a th:href="@{/orders/new}" class="btn btn-outline-primary">Place your first order</a>
</div>

<!-- Orders table -->
<div th:unless="${#lists.isEmpty(orders)}" class="card shadow-sm">
    <div class="card-body p-0">
        <table class="table table-hover table-striped mb-0">
            <thead class="table-dark">
                <tr>
                    <th>#</th>
                    <th>Customer</th>
                    <th>Product</th>
                    <th>Qty</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="order : ${orders}">
                    <td th:text="${order.id}"></td>
                    <td th:text="${order.customerName}"></td>
                    <td th:text="${order.productName}"></td>
                    <td th:text="${order.quantity}"></td>
                    <td>
                        <span th:replace="~{fragments/status-badge :: badge(${order.status})}"></span>
                    </td>
                    <td th:text="${#temporals.format(order.createdAt, 'yyyy-MM-dd HH:mm')}"></td>
                    <td>
                        <a th:href="@{/orders/{id}(id=${order.id})}"
                           class="btn btn-sm btn-outline-primary">
                            <i class="bi bi-eye me-1"></i>View
                        </a>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
```

---

### `orders/form.html` — New Order Form

```html
<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow-sm">
            <div class="card-header bg-primary text-white">
                <h5 class="mb-0"><i class="bi bi-plus-circle me-2"></i>New Order</h5>
            </div>
            <div class="card-body">
                <form th:action="@{/orders}" th:object="${order}" method="post">
                    <div class="mb-3">
                        <label for="customerName" class="form-label fw-semibold">
                            Customer Name <span class="text-danger">*</span>
                        </label>
                        <input type="text" class="form-control" id="customerName"
                               th:field="*{customerName}" required placeholder="Full name" />
                    </div>
                    <div class="mb-3">
                        <label for="productName" class="form-label fw-semibold">
                            Product Name <span class="text-danger">*</span>
                        </label>
                        <input type="text" class="form-control" id="productName"
                               th:field="*{productName}" required placeholder="Product name" />
                    </div>
                    <div class="mb-4">
                        <label for="quantity" class="form-label fw-semibold">
                            Quantity <span class="text-danger">*</span>
                        </label>
                        <input type="number" class="form-control" id="quantity"
                               th:field="*{quantity}" required min="1" value="1" />
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-success">
                            <i class="bi bi-send me-1"></i>Submit Order
                        </button>
                        <a th:href="@{/orders}" class="btn btn-outline-secondary">
                            <i class="bi bi-x-circle me-1"></i>Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
```

---

### `orders/detail.html` — Order Detail + Workflow

```html
<!-- Flash messages -->
<!-- (same pattern as list.html) -->

<!-- Order info card -->
<div class="row">
    <div class="col-lg-7 mb-4">
        <div class="card shadow-sm">
            <div class="card-header d-flex justify-content-between align-items-center">
                <h5 class="mb-0">Order #<span th:text="${order.id}"></span></h5>
                <span th:replace="~{fragments/status-badge :: badge(${order.status})}"></span>
            </div>
            <div class="card-body">
                <dl class="row mb-0">
                    <dt class="col-sm-4">Customer</dt>
                    <dd class="col-sm-8" th:text="${order.customerName}"></dd>

                    <dt class="col-sm-4">Product</dt>
                    <dd class="col-sm-8" th:text="${order.productName}"></dd>

                    <dt class="col-sm-4">Quantity</dt>
                    <dd class="col-sm-8" th:text="${order.quantity}"></dd>

                    <dt class="col-sm-4">Created</dt>
                    <dd class="col-sm-8"
                        th:text="${#temporals.format(order.createdAt, 'yyyy-MM-dd HH:mm')}"></dd>

                    <dt class="col-sm-4">Last Updated</dt>
                    <dd class="col-sm-8"
                        th:text="${#temporals.format(order.updatedAt, 'yyyy-MM-dd HH:mm')}"></dd>
                </dl>
            </div>
        </div>
    </div>

    <!-- Workflow action panel -->
    <div class="col-lg-5 mb-4">
        <div class="card shadow-sm">
            <div class="card-header">
                <h5 class="mb-0"><i class="bi bi-arrow-right-circle me-2"></i>Workflow Actions</h5>
            </div>
            <div class="card-body">

                <!-- SUBMITTED: one action -->
                <div th:if="${order.status.name() == 'SUBMITTED'}">
                    <p class="text-muted small mb-3">Awaiting inventory check.</p>
                    <form th:action="@{/orders/{id}/transition(id=${order.id})}" method="post">
                        <input type="hidden" name="action" value="CHECK_INVENTORY" />
                        <button type="submit" class="btn btn-warning w-100">
                            <i class="bi bi-search me-2"></i>Check Inventory
                        </button>
                    </form>
                </div>

                <!-- CHECKING_INVENTORY: two actions -->
                <div th:if="${order.status.name() == 'CHECKING_INVENTORY'}">
                    <p class="text-muted small mb-3">Is the product available?</p>
                    <form th:action="@{/orders/{id}/transition(id=${order.id})}" method="post"
                          class="mb-2">
                        <input type="hidden" name="action" value="MARK_AVAILABLE" />
                        <button type="submit" class="btn btn-success w-100">
                            <i class="bi bi-check-circle me-2"></i>Mark Available
                        </button>
                    </form>
                    <form th:action="@{/orders/{id}/transition(id=${order.id})}" method="post">
                        <input type="hidden" name="action" value="MARK_UNAVAILABLE" />
                        <button type="submit" class="btn btn-danger w-100">
                            <i class="bi bi-x-circle me-2"></i>Mark Unavailable
                        </button>
                    </form>
                </div>

                <!-- PAYMENT_COLLECTED: one action -->
                <div th:if="${order.status.name() == 'PAYMENT_COLLECTED'}">
                    <p class="text-muted small mb-3">Payment received. Ready to ship.</p>
                    <form th:action="@{/orders/{id}/transition(id=${order.id})}" method="post">
                        <input type="hidden" name="action" value="SHIP" />
                        <button type="submit" class="btn btn-primary w-100">
                            <i class="bi bi-truck me-2"></i>Ship Order
                        </button>
                    </form>
                </div>

                <!-- SHIPPED: terminal -->
                <div th:if="${order.status.name() == 'SHIPPED'}">
                    <div class="alert alert-success mb-0">
                        <i class="bi bi-check-circle-fill me-2"></i>
                        Order complete — item has been shipped.
                    </div>
                </div>

                <!-- CUSTOMER_NOTIFIED: terminal -->
                <div th:if="${order.status.name() == 'CUSTOMER_NOTIFIED'}">
                    <div class="alert alert-secondary mb-0">
                        <i class="bi bi-info-circle-fill me-2"></i>
                        Order closed — customer has been notified.
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>

<!-- Workflow stepper -->
<div class="card shadow-sm mb-4">
    <div class="card-header">
        <h5 class="mb-0"><i class="bi bi-diagram-3 me-2"></i>Workflow Progress</h5>
    </div>
    <div class="card-body">
        <div class="order-stepper">
            <div th:each="step : ${ {'SUBMITTED','CHECKING_INVENTORY','PAYMENT_COLLECTED','SHIPPED'} }"
                 th:class="${order.status.name() == step} ? 'step active' :
                            ${#strings.equals(order.status.name(), 'CUSTOMER_NOTIFIED') and step == 'SHIPPED'} ? 'step skipped' :
                            'step'">
                <span class="step-label" th:text="${step}"></span>
            </div>
        </div>
        <!-- CUSTOMER_NOTIFIED branch note -->
        <div th:if="${order.status.name() == 'CUSTOMER_NOTIFIED'}"
             class="mt-2 text-muted small">
            <i class="bi bi-arrow-return-right me-1"></i>
            Alternative path: item was unavailable — customer notified.
        </div>
    </div>
</div>

<!-- Audit trail -->
<div class="card shadow-sm mb-4" th:if="${order.notes != null}">
    <div class="card-header">
        <h5 class="mb-0"><i class="bi bi-journal-text me-2"></i>Audit Trail</h5>
    </div>
    <div class="card-body p-0">
        <pre class="audit-trail mb-0 p-3" th:text="${order.notes}"></pre>
    </div>
</div>

<!-- Back link -->
<a th:href="@{/orders}" class="btn btn-outline-secondary">
    <i class="bi bi-arrow-left me-1"></i>Back to Orders
</a>
```

---

## Status Badge Fragment

Defined in `templates/fragments/status-badge.html` and reused in both `list.html` and `detail.html`:

```html
<th:block th:fragment="badge(status)">
    <span th:switch="${status.name()}"
          class="badge rounded-pill fs-6">
        <span th:case="'SUBMITTED'"          class="badge rounded-pill bg-secondary">Submitted</span>
        <span th:case="'CHECKING_INVENTORY'" class="badge rounded-pill bg-info text-dark">Checking Inventory</span>
        <span th:case="'PAYMENT_COLLECTED'"  class="badge rounded-pill bg-primary">Payment Collected</span>
        <span th:case="'SHIPPED'"            class="badge rounded-pill bg-success">Shipped</span>
        <span th:case="'CUSTOMER_NOTIFIED'"  class="badge rounded-pill bg-warning text-dark">Customer Notified</span>
    </span>
</th:block>
```

| Status | Bootstrap class | Color |
|---|---|---|
| `SUBMITTED` | `bg-secondary` | Gray |
| `CHECKING_INVENTORY` | `bg-info text-dark` | Cyan |
| `PAYMENT_COLLECTED` | `bg-primary` | Blue |
| `SHIPPED` | `bg-success` | Green |
| `CUSTOMER_NOTIFIED` | `bg-warning text-dark` | Amber |

---

## `static/css/style.css` — Custom Overrides

```css
/* ── Card shadows ─────────────────────────────────────── */
.card {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* ── Navbar brand ─────────────────────────────────────── */
.navbar-brand {
    font-size: 1.25rem;
    letter-spacing: 0.02em;
}

/* ── Audit trail pre block ────────────────────────────── */
.audit-trail {
    font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
    font-size: 0.85rem;
    line-height: 1.6;
    background: #f8f9fa;
    color: #495057;
    white-space: pre-wrap;
    word-break: break-word;
}

/* ── Workflow stepper ─────────────────────────────────── */
.order-stepper {
    display: flex;
    align-items: center;
    gap: 0;
    overflow-x: auto;
    padding-bottom: 0.5rem;
}

.order-stepper .step {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    position: relative;
    padding: 0.5rem 0.25rem;
}

.order-stepper .step::before {
    content: '';
    width: 2rem;
    height: 2rem;
    border-radius: 50%;
    background: #dee2e6;
    border: 3px solid #adb5bd;
    display: block;
    margin-bottom: 0.4rem;
    z-index: 1;
}

.order-stepper .step.active::before {
    background: #0d6efd;
    border-color: #0d6efd;
}

.order-stepper .step.skipped::before {
    background: #6c757d;
    border-color: #6c757d;
}

/* Connecting line between steps */
.order-stepper .step + .step::after {
    content: '';
    position: absolute;
    top: 1.5rem;
    right: 50%;
    width: 100%;
    height: 3px;
    background: #dee2e6;
    z-index: 0;
}

.order-stepper .step-label {
    font-size: 0.7rem;
    text-align: center;
    color: #6c757d;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    word-break: break-word;
    max-width: 5rem;
}

.order-stepper .step.active .step-label {
    color: #0d6efd;
    font-weight: 600;
}

/* ── Table improvements ───────────────────────────────── */
.table th {
    font-size: 0.8rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

/* ── Badge sizing in table ────────────────────────────── */
.table .badge {
    font-size: 0.75rem;
}

/* ── Responsive body padding ──────────────────────────── */
body {
    font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
    background-color: #f5f6fa;
}

main.container {
    min-height: calc(100vh - 140px);
}
```

---

## Redirect-After-POST Pattern

All write operations (create order, transition) follow the **PRG (Post/Redirect/Get)** pattern:

```
Browser             Controller
  │                     │
  │  POST /orders       │
  │ ───────────────────▶│
  │                     │  orderService.createOrder(...)
  │                     │
  │  302 redirect       │
  │ ◀───────────────────│  Location: /orders
  │                     │
  │  GET /orders        │
  │ ───────────────────▶│
  │  200 HTML           │
  │ ◀───────────────────│
```

Flash attributes (`RedirectAttributes.addFlashAttribute`) survive exactly one redirect and are then discarded from the session.
