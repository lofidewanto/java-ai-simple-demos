# 01 — System Overview

## Purpose

This document describes the high-level architecture, technology stack, component relationships, and key design decisions for the **demo-sdd-noworkflow-spring** Order Processing application.

---

## Application Summary

A full-stack Spring Boot web application that implements the **Bestellabwicklung** (order processing) business workflow defined in issue [#7](https://github.com/lofidewanto/java-ai-simple-demos/issues/7). The business process is encoded directly in the service layer as a state machine — no external workflow engine is used.

The application exposes two access surfaces:

1. **Browser-based UI** — Thymeleaf server-side rendered HTML pages with Bootstrap 5 styling
2. **REST API** — JSON endpoints consumed by any HTTP client

### Business Process

```
Customer submits order
        │
        ▼
Shop checks inventory
        │
   ┌────┴─────────────────┐
   │ Available            │ Not Available
   ▼                      ▼
Payment collected   Customer notified
   │
   ▼
Item shipped
```

### Order States

| State | Description |
|---|---|
| `SUBMITTED` | Customer has submitted the order; awaiting inventory check |
| `CHECKING_INVENTORY` | Shop is verifying product availability |
| `PAYMENT_COLLECTED` | Item is available; payment has been collected |
| `SHIPPED` | Item has been dispatched to the customer *(terminal)* |
| `CUSTOMER_NOTIFIED` | Item was unavailable; customer has been informed *(terminal)* |

### Valid Transitions

| From | Action | To |
|---|---|---|
| `SUBMITTED` | `CHECK_INVENTORY` | `CHECKING_INVENTORY` |
| `CHECKING_INVENTORY` | `MARK_AVAILABLE` | `PAYMENT_COLLECTED` |
| `CHECKING_INVENTORY` | `MARK_UNAVAILABLE` | `CUSTOMER_NOTIFIED` |
| `PAYMENT_COLLECTED` | `SHIP` | `SHIPPED` |

`SHIPPED` and `CUSTOMER_NOTIFIED` are terminal states — no further transitions are possible.

---

## Technology Stack

| Concern | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.x |
| Web MVC | Spring Web (embedded Tomcat) | (Boot-managed) |
| Templating | Thymeleaf | (Boot-managed) |
| Persistence | Spring Data JPA / Hibernate | (Boot-managed) |
| Database | H2 (in-memory) | (Boot-managed) |
| Schema migrations | Flyway | (Boot-managed) |
| API documentation | Springdoc OpenAPI 3 + Swagger UI | 2.8.x |
| Frontend styling | Bootstrap 5.3 | CDN |
| Build tool | Maven (wrapper `mvnw`) | 3.x |
| Testing | JUnit 5, Mockito, MockMvc | (Boot-managed) |

---

## Architectural Style

**Layered MVC** with a strict separation of concerns across four layers:

```
┌─────────────────────────────────────────────────────────────┐
│                       Presentation Layer                    │
│                                                             │
│  ┌──────────────────────┐   ┌──────────────────────────┐   │
│  │  OrderWebController  │   │   OrderApiController     │   │
│  │  (@Controller)       │   │   (@RestController)      │   │
│  │  /orders, /          │   │   /api/orders            │   │
│  └──────────┬───────────┘   └────────────┬─────────────┘   │
└─────────────┼────────────────────────────┼─────────────────┘
              │                            │
              ▼                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Service Layer                         │
│                                                             │
│              ┌──────────────────────────┐                  │
│              │       OrderService       │                  │
│              │  (@Service,              │                  │
│              │   @Transactional)        │                  │
│              │  State machine logic     │                  │
│              └─────────────┬────────────┘                  │
└────────────────────────────┼────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Access Layer                     │
│                                                             │
│         ┌────────────────────────────────────┐             │
│         │  OrderRepository                   │             │
│         │  (JpaRepository<Order, Long>        │             │
│         │   @Repository)                     │             │
│         └──────────────────┬─────────────────┘             │
└────────────────────────────┼────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain / DB Layer                     │
│                                                             │
│   ┌──────────────────────┐   ┌──────────────────────────┐  │
│   │   Order (@Entity)    │   │  H2 in-memory database   │  │
│   │   OrderStatus (enum) │   │  managed by Flyway       │  │
│   └──────────────────────┘   └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Component Interactions

### Web UI Flow

```
Browser
  │
  │  HTTP GET/POST (HTML forms)
  ▼
OrderWebController   ──→  OrderService  ──→  OrderRepository  ──→  H2
  │
  │  ModelAndView (Thymeleaf render)
  ▼
Browser (HTML response)
```

### REST API Flow

```
HTTP Client (curl / Postman / Frontend SPA)
  │
  │  HTTP GET/POST (JSON)
  ▼
OrderApiController  ──→  OrderService  ──→  OrderRepository  ──→  H2
  │
  │  ResponseEntity<Order> (JSON)
  ▼
HTTP Client
```

---

## Package Structure

```
com.example.demo_sdd_spring/
├── DemoSddNoworkflowSpringApplication.java   # @SpringBootApplication entry point
├── config/
│   └── DataInitializer.java                  # CommandLineRunner — seeds sample orders on startup
├── model/
│   ├── Order.java                            # @Entity — the sole domain object
│   └── OrderStatus.java                      # Enum — the five workflow states
├── repository/
│   └── OrderRepository.java                  # JpaRepository extension
├── service/
│   └── OrderService.java                     # Transactional business logic + state machine
└── controller/
    ├── api/
    │   └── OrderApiController.java           # REST surface (/api/orders)
    └── web/
        └── OrderWebController.java           # Thymeleaf surface (/orders)
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| No workflow engine | The business process has a fixed, well-understood state graph. Encoding it as a `switch` statement in the service layer is simpler, testable, and has no external dependency. |
| No service interface | Single implementation; Spring CGLIB proxies the concrete class for `@Transactional`. No interface adds boilerplate without value. |
| Flyway owns the schema | `spring.jpa.hibernate.ddl-auto=validate` — schema changes are versioned SQL files, not Hibernate auto-DDL. |
| H2 in-memory database | Development/demo simplicity. No external DB process required; data resets on each restart. |
| Dual controller surfaces | Web controller handles browser form submissions (PRG pattern). API controller handles JSON. Both share the same service layer and state machine. |
| Class-level `@Transactional` | `OrderService` annotated at class level; read methods override with `readOnly = true`. |
| `notes` as audit trail | The `notes` field is system-managed. Each state transition appends a timestamped line. Never editable by the user. |
| Bootstrap 5 via CDN | Clean, modern UI with no local asset bundling. Status badges, workflow stepper, and card layout provide a professional look and feel. |

---

## Exposed URLs

| URL | Purpose |
|---|---|
| `http://localhost:8080/` | Redirects to `/orders` |
| `http://localhost:8080/orders` | Order list web UI |
| `http://localhost:8080/orders/new` | New order submission form |
| `http://localhost:8080/orders/{id}` | Order detail + workflow action buttons |
| `http://localhost:8080/api/orders` | REST API — list all orders |
| `http://localhost:8080/api/orders/{id}` | REST API — get single order |
| `http://localhost:8080/api/orders/{id}/transition` | REST API — advance workflow state |
| `http://localhost:8080/swagger-ui.html` | Swagger UI (interactive API docs) |
| `http://localhost:8080/api-docs` | OpenAPI 3 JSON spec |
| `http://localhost:8080/h2-console` | H2 database console (dev only) |

---

## Maven Artifact Identity

```xml
<groupId>com.example</groupId>
<artifactId>demo-sdd-noworkflow-spring</artifactId>
<version>0.0.1-SNAPSHOT</version>
<packaging>jar</packaging>
```

> **Note on package naming:** The artifact ID `demo-sdd-noworkflow-spring` contains hyphens (invalid in Java package names). The root Java package is `com.example.demo_sdd_spring` (underscores).
