# 01 — System Overview

## Purpose

`demo-sdd-workflow-spring` is a **REST API-only backend workflow engine** built with Spring Boot 3.5.11 and Java 21. It allows developers to define multi-step business processes as YAML DSL files and execute them via HTTP. There is no web UI; all interaction is through JSON REST endpoints documented via Swagger UI.

The project demonstrates how a lightweight, embedded workflow engine can be built on standard Spring Boot components — JPA, Flyway, and Jackson — without any external workflow runtime.

---

## Functional Scope

| Feature | In Scope |
|---------|----------|
| Load workflow definitions from YAML at startup | Yes |
| Start workflow instances via REST | Yes |
| Drive instances through transitions via REST | Yes |
| Persist instance state and full history | Yes |
| Query definitions and instances via REST | Yes |
| Reject invalid/illegal transitions | Yes |
| Web UI / Thymeleaf views | No |
| External message queues (Kafka, RabbitMQ) | No |
| Multi-tenant isolation | No |
| Authentication / Authorization | No |

---

## Key Concepts

### Workflow Definition

A workflow definition is a named blueprint loaded from a YAML file. It declares:
- **States** — the possible stages of a process (one initial, one or more terminal)
- **Transitions** — directed edges between states, each labelled with an **action** name and an optional **taskType**

Definitions are read-only after startup; they cannot be modified via REST.

### Workflow Instance

A workflow instance is a running (or completed) execution of a definition. It tracks:
- The **current state** of the process
- Its **status**: `RUNNING` or `COMPLETED`
- A full **history** of every state change

### TaskType

Each transition carries an optional `taskType` that classifies who or what performs the step:

| Value | Meaning |
|-------|---------|
| `HUMAN` | A human actor performs the task (default) |
| `SERVICE` | An automated service or integration performs it |
| `GATEWAY` | A conditional routing decision |
| `EVENT` | An external event triggers the transition |

### State Machine

The engine implements a simple deterministic state machine:
- For a given instance in state `S`, there is at most one transition matching action `A`
- Triggering action `A` moves the instance from `S` to the target state `T`
- If the target state is terminal, the instance is marked `COMPLETED`

---

## System Components

```
┌──────────────────────────────────────────────────────┐
│                  Spring Boot Application              │
│                                                      │
│  ┌─────────────────┐     ┌──────────────────────┐   │
│  │ WorkflowDef     │     │ WorkflowInstance      │   │
│  │ ApiController   │     │ ApiController         │   │
│  └────────┬────────┘     └──────────┬───────────┘   │
│           │                         │                │
│  ┌────────▼────────┐     ┌──────────▼───────────┐   │
│  │ WorkflowDef     │     │ WorkflowInstance      │   │
│  │ Service         │     │ Service               │   │
│  └────────┬────────┘     └──────────┬───────────┘   │
│           │                         │                │
│  ┌────────▼─────────────────────────▼───────────┐   │
│  │            Spring Data JPA Repositories       │   │
│  └────────────────────────┬──────────────────────┘   │
│                           │                          │
│  ┌────────────────────────▼──────────────────────┐   │
│  │              H2 In-Memory Database             │   │
│  │         (schema managed by Flyway)             │   │
│  └───────────────────────────────────────────────┘   │
│                                                      │
│  ┌───────────────────────────────────────────────┐   │
│  │  WorkflowDefinitionLoader (CommandLineRunner)  │   │
│  │  Reads classpath:workflows/*.yml at startup    │   │
│  └───────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

---

## Bundled Example Workflows

Three example YAML workflows are included under `src/main/resources/workflows/`:

| File | Name | Description |
|------|------|-------------|
| `order-processing.yml` | `order-processing` | Bestellabwicklung — 5 states, 4 transitions (from issue #7) |
| `employee-onboarding.yml` | `employee-onboarding` | Mitarbeiter-Onboarding — 7 states with approval and withdraw path |
| `support-ticket.yml` | `support-ticket` | Support-Ticket — multiple paths: escalation cycle and re-open path |

---

## Runtime URLs

| URL | Description |
|-----|-------------|
| `http://localhost:8080/api/workflow-definitions` | List all definitions |
| `http://localhost:8080/api/workflow-instances` | List/create instances |
| `http://localhost:8080/swagger-ui.html` | Swagger UI (OpenAPI docs) |
| `http://localhost:8080/h2-console` | H2 database console |

---

## Source & References

- GitHub issue (user story): https://github.com/lofidewanto/java-ai-simple-demos/issues/8
- Reference workflow (Bestellabwicklung): https://github.com/lofidewanto/java-ai-simple-demos/issues/7
- Implementation plan: `userstories/US-008-backend-workflow-engine.md`
