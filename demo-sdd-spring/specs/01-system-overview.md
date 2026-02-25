# 01 — System Overview

## Purpose

This document describes the high-level architecture, technology stack, component relationships, and key design decisions for the **demo-sdd-spring** Guest Book application.

---

## Application Summary

A full-stack Spring Boot CRUD web application that allows visitors to create, read, update, and delete guest book entries. It exposes two access surfaces:

1. **Browser-based UI** — Thymeleaf server-side rendered HTML pages
2. **REST API** — JSON endpoints consumed by any HTTP client

---

## Technology Stack

| Concern | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.10 |
| Web MVC | Spring Web (embedded Tomcat) | (Boot-managed) |
| Templating | Thymeleaf | (Boot-managed) |
| Persistence | Spring Data JPA / Hibernate | (Boot-managed) |
| Database | H2 (in-memory) | (Boot-managed) |
| Schema migrations | Flyway | (Boot-managed) |
| API documentation | Springdoc OpenAPI 3 + Swagger UI | 2.3.0 |
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
│  │  GuestBookWebController│  │ GuestBookApiController   │   │
│  │  (@Controller)        │  │ (@RestController)         │   │
│  │  /entries, /         │  │ /api/entries              │   │
│  └──────────┬───────────┘   └────────────┬─────────────┘   │
└─────────────┼────────────────────────────┼─────────────────┘
              │                            │
              ▼                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Service Layer                         │
│                                                             │
│              ┌──────────────────────────┐                  │
│              │     GuestBookService     │                  │
│              │     (@Service,           │                  │
│              │      @Transactional)     │                  │
│              └─────────────┬────────────┘                  │
└────────────────────────────┼────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Access Layer                     │
│                                                             │
│         ┌────────────────────────────────────┐             │
│         │  GuestBookEntryRepository           │             │
│         │  (JpaRepository<GuestBookEntry,Long>│             │
│         │   @Repository)                      │             │
│         └──────────────────┬─────────────────┘             │
└────────────────────────────┼────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain / DB Layer                     │
│                                                             │
│   ┌──────────────────────┐   ┌──────────────────────────┐  │
│   │   GuestBookEntry     │   │  H2 in-memory database   │  │
│   │   (@Entity)          │   │  managed by Flyway       │  │
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
GuestBookWebController   ──→  GuestBookService  ──→  GuestBookEntryRepository  ──→  H2
  │
  │  ModelAndView (Thymeleaf render)
  ▼
Browser (HTML response)
```

### REST API Flow

```
HTTP Client (curl / Postman / Frontend SPA)
  │
  │  HTTP GET/POST/PUT/DELETE (JSON)
  ▼
GuestBookApiController  ──→  GuestBookService  ──→  GuestBookEntryRepository  ──→  H2
  │
  │  ResponseEntity<GuestBookEntry> (JSON)
  ▼
HTTP Client
```

---

## Package Structure

```
com.example.demo_sdd_spring/
├── DemoSddSpringApplication.java    # @SpringBootApplication entry point
├── config/
│   └── DataInitializer.java         # CommandLineRunner — seeds sample data on startup
├── model/
│   └── GuestBookEntry.java          # @Entity — the sole domain object
├── repository/
│   └── GuestBookEntryRepository.java # JpaRepository extension
├── service/
│   └── GuestBookService.java         # Transactional business logic
└── controller/
    ├── api/
    │   └── GuestBookApiController.java  # REST surface (/api/entries)
    └── web/
        └── GuestBookWebController.java  # Thymeleaf surface (/entries)
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| No service interface | Application has a single service implementation with no runtime polymorphism requirement. An interface would add boilerplate without value. |
| Flyway owns the schema | `spring.jpa.hibernate.ddl-auto=validate` — Hibernate only validates against Flyway-managed schema. This ensures schema changes are versioned and reproducible. |
| H2 in-memory database | Development/demo simplicity. No external DB process required; data resets on each restart. See `specs/08-configuration-deployment.md` for production swap guidance. |
| Dual controller surfaces | The web controller handles browser form submissions (POST with `@ModelAttribute`, redirect-after-POST pattern). The API controller handles JSON for programmatic clients. Both share the same service layer. |
| Class-level `@Transactional` | `GuestBookService` is annotated at class level, with individual read methods overriding to `readOnly = true`. This ensures no method is accidentally non-transactional. |
| Sample data via `CommandLineRunner` | `DataInitializer` inserts 3 sample entries at startup via the service layer (not SQL), exercising the full stack during startup and guaranteeing valid timestamps. |

---

## Exposed URLs

| URL | Purpose |
|---|---|
| `http://localhost:8080/` | Redirects to `/entries` |
| `http://localhost:8080/entries` | Guest book web UI |
| `http://localhost:8080/api/entries` | REST API root |
| `http://localhost:8080/swagger-ui.html` | Swagger UI (interactive API docs) |
| `http://localhost:8080/api-docs` | OpenAPI 3 JSON spec |
| `http://localhost:8080/h2-console` | H2 database console (dev only) |

---

## Maven Artifact Identity

```xml
<groupId>com.example</groupId>
<artifactId>demo-sdd-spring</artifactId>
<version>0.0.1-SNAPSHOT</version>
<packaging>jar</packaging>
```

> **Note on package naming:** The artifact ID `demo-sdd-spring` contains hyphens (invalid in Java package names). The root Java package is therefore `com.example.demo_sdd_spring` (underscores).
