# 02 — Domain Model

## Overview

The domain layer consists of three JPA entities and one enum. All entities are persisted in an H2 in-memory database, with the schema managed by Flyway.

```
WorkflowDefinition  1 ──────── * WorkflowInstance
                                        │
                                        1
                                        │
                                        *
                               WorkflowHistoryEntry
```

---

## Entities

### WorkflowDefinition

Represents a loaded and validated workflow blueprint. Created at startup by `WorkflowDefinitionLoader`; never modified at runtime.

**JPA table:** `workflow_definitions`

| Field | Column | Type | Constraints |
|-------|--------|------|-------------|
| `id` | `id` | `BIGINT` | PK, auto-generated |
| `name` | `name` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `description` | `description` | `VARCHAR(1000)` | nullable |
| `source` | `source` | `VARCHAR(1000)` | nullable |
| `statesJson` | `states_json` | `TEXT` | NOT NULL — JSON array of state objects |
| `transitionsJson` | `transitions_json` | `TEXT` | NOT NULL — JSON array of transition objects |
| `createdAt` | `created_at` | `TIMESTAMP` | NOT NULL, set on insert |

**Notes:**
- `name` is the natural key used throughout the API (e.g. `order-processing`)
- `statesJson` and `transitionsJson` store serialised `List<StateDto>` and `List<TransitionDto>` respectively; they are deserialised in the service layer when needed
- `source` is a free-text URL linking back to the origin of the workflow definition (e.g. a GitHub issue)

---

### WorkflowInstance

Represents a single execution of a workflow definition. Created via `POST /api/workflow-instances`. Each instance advances independently through the state machine.

**JPA table:** `workflow_instances`

| Field | Column | Type | Constraints |
|-------|--------|------|-------------|
| `id` | `id` | `BIGINT` | PK, auto-generated |
| `workflowDefinition` | `workflow_definition_id` | `BIGINT` | NOT NULL, FK → `workflow_definitions.id` |
| `currentState` | `current_state` | `VARCHAR(255)` | NOT NULL |
| `status` | `status` | `VARCHAR(50)` | NOT NULL, `RUNNING`, `PAUSED`, or `COMPLETED` |
| `createdAt` | `created_at` | `TIMESTAMP` | NOT NULL, set on insert |
| `updatedAt` | `updated_at` | `TIMESTAMP` | NOT NULL, updated on each transition |

**Lifecycle:**
1. Created with `currentState` = the initial state of the definition, `status = RUNNING`
2. Each successful transition updates `currentState` and `updatedAt`
3. When `currentState` becomes a terminal state, `status` is set to `COMPLETED`
4. `COMPLETED` instances are immutable — no further transitions are accepted
5. A `RUNNING` instance can be set to `PAUSED` via the pause endpoint; transitions are rejected while paused
6. A `PAUSED` instance can be set back to `RUNNING` via the resume endpoint

---

### WorkflowHistoryEntry

An append-only record of each state transition. One entry is written for the initial placement (action = `START`, `fromState` = `null`) and one for every subsequent transition.

**JPA table:** `workflow_history_entries`

| Field | Column | Type | Constraints |
|-------|--------|------|-------------|
| `id` | `id` | `BIGINT` | PK, auto-generated |
| `workflowInstance` | `workflow_instance_id` | `BIGINT` | NOT NULL, FK → `workflow_instances.id` |
| `fromState` | `from_state` | `VARCHAR(255)` | nullable (null for initial entry) |
| `toState` | `to_state` | `VARCHAR(255)` | NOT NULL |
| `action` | `action` | `VARCHAR(255)` | NOT NULL |
| `taskType` | `task_type` | `VARCHAR(50)` | NOT NULL, defaults to `HUMAN` |
| `occurredAt` | `occurred_at` | `TIMESTAMP` | NOT NULL, set on insert |

**Notes:**
- History entries are never updated or deleted
- `fromState` is `null` only for the very first entry (initial placement)
- The initial entry uses the synthetic action `START` and `taskType HUMAN`

---

## Enum

### TaskType

Classifies the nature of a transition. Stored as a string in the database.

```java
public enum TaskType {
    HUMAN,    // A human actor performs this step (default when omitted in YAML)
    SERVICE,  // An automated service call
    GATEWAY,  // A conditional routing / decision point
    EVENT     // Triggered by an external event
}
```

`TaskType` is declared on the **transition** (not on the state). It is optional in the YAML DSL; the parser defaults to `HUMAN` if omitted.

---

## Relationship Diagram (Entity Detail)

```
┌──────────────────────────────────┐
│       WorkflowDefinition         │
│ PK id                            │
│    name          (unique)        │
│    description                   │
│    source                        │
│    statesJson     (TEXT)         │
│    transitionsJson (TEXT)        │
│    createdAt                     │
└──────────────┬───────────────────┘
               │ 1
               │
               │ *
┌──────────────▼───────────────────┐
│        WorkflowInstance          │
│ PK id                            │
│ FK workflow_definition_id        │
│    currentState                  │
│    status  (RUNNING|PAUSED|      │
│             COMPLETED)           │
│    createdAt                     │
│    updatedAt                     │
└──────────────┬───────────────────┘
               │ 1
               │
               │ *
┌──────────────▼───────────────────┐
│     WorkflowHistoryEntry         │
│ PK id                            │
│ FK workflow_instance_id          │
│    fromState  (nullable)         │
│    toState                       │
│    action                        │
│    taskType  (TaskType enum)     │
│    occurredAt                    │
└──────────────────────────────────┘
```

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| States/transitions stored as JSON blobs | Avoids extra join tables; definitions are read-only so no normalisation benefit |
| `TaskType` on transition, not state | A state can be reached via different task types (e.g. manual or automated) |
| Append-only history | Provides a complete audit trail; no history entry is ever modified |
| `status` field on instance | Avoids repeatedly querying history to determine whether an instance is terminal or paused |
| Three-value status (`RUNNING`, `PAUSED`, `COMPLETED`) | `PAUSED` allows long-running workflows to be suspended without losing state; `COMPLETED` is irreversible |
| H2 in-memory database | Simplifies local development; swap `application.properties` for PostgreSQL/MySQL in production |
