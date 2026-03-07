# 04 — Service Layer

## Overview

The service layer contains all business logic. Controllers delegate to services; services use repositories for persistence. Services are annotated with `@Service` and `@Transactional` where appropriate.

There are two service classes:

| Service | Responsibility |
|---------|---------------|
| `WorkflowDefinitionService` | Load, validate, persist, and query workflow definitions |
| `WorkflowInstanceService` | Start instances, execute state machine transitions, query instances and history |

---

## WorkflowDefinitionService

**Package:** `com.example.workflow.service`

### Responsibilities

- Parse `WorkflowDefinitionDto` objects from YAML input (delegated from `WorkflowDefinitionLoader`)
- Validate the DSL rules (see Validation section below)
- Persist validated definitions as `WorkflowDefinition` entities
- Serve definition queries from the REST controller

### Method Contracts

#### `loadFromDto(WorkflowDefinitionDto dto)`

```
Input:  WorkflowDefinitionDto (parsed from YAML)
Effect: Validates the DTO, then persists a new WorkflowDefinition entity.
        If a definition with the same name already exists, it is skipped (idempotent).
Throws: IllegalArgumentException if DSL validation fails
```

#### `findAll() → List<WorkflowDefinitionDto>`

```
Returns all persisted workflow definitions, deserialised from JSON blobs into DTOs.
```

#### `findByName(String name) → WorkflowDefinitionDto`

```
Returns the definition with the given name.
Throws: WorkflowNotFoundException if no definition matches
```

### DSL Validation Rules

Performed inside `loadFromDto` before any persistence:

| Rule | Error message |
|------|---------------|
| Exactly one state must have `initial: true` | "Workflow must have exactly one initial state" |
| At least one state must have `terminal: true` | "Workflow must have at least one terminal state" |
| Every transition `from` must reference a declared state name | "Transition 'from' references unknown state: {name}" |
| Every transition `to` must reference a declared state name | "Transition 'to' references unknown state: {name}" |
| No two transitions may share the same `(from, action)` pair | "Duplicate transition: from={from}, action={action}" |

### JSON Serialisation

States and transitions are stored in the database as JSON strings (TEXT columns). The service uses `ObjectMapper` (Jackson) to serialise/deserialise `List<StateDto>` and `List<TransitionDto>` when reading and writing entities.

---

## WorkflowInstanceService

**Package:** `com.example.workflow.service`

### Responsibilities

- Create new workflow instances and set their initial state
- Resolve and apply transitions (the state machine logic)
- Write history entries on every state change
- Detect terminal states and mark instances as `COMPLETED`
- Serve instance and history queries from the REST controller

### Method Contracts

#### `startInstance(String workflowName) → WorkflowInstanceResponse`

```
1. Look up the WorkflowDefinition by name (throws WorkflowNotFoundException if absent)
2. Deserialise states from statesJson; find the one with initial=true
3. Create a new WorkflowInstance:
     currentState = initial state name
     status       = RUNNING
4. Write the first WorkflowHistoryEntry:
     fromState  = null
     toState    = initial state name
     action     = "START"
     taskType   = HUMAN
5. Persist both entities and return WorkflowInstanceResponse
```

#### `triggerTransition(Long instanceId, String action) → WorkflowInstanceResponse`

```
1. Load instance by ID (throws InstanceNotFoundException if absent)
2. If instance.status == COMPLETED → throw WorkflowCompletedException
3. Deserialise transitions from the associated definition
4. Find a transition where from == instance.currentState AND action == action
   → if none found, throw InvalidTransitionException
5. Update the instance:
     currentState = transition.to
     updatedAt    = now
6. Write a WorkflowHistoryEntry:
     fromState  = previous currentState
     toState    = transition.to
     action     = action
     taskType   = transition.taskType (default HUMAN)
7. If the new currentState is terminal:
     instance.status = COMPLETED
8. Persist and return WorkflowInstanceResponse
```

#### `findById(Long id) → WorkflowInstanceResponse`

```
Load instance with its history.
Throws: InstanceNotFoundException if no instance with the given ID exists
```

#### `findAll(String workflowName) → List<WorkflowInstanceResponse>`

```
Return all instances.
If workflowName is non-null, filter to only instances of that definition.
Each response includes the full history.
```

---

## State Machine Logic

The core of the engine is the transition resolver inside `triggerTransition`. Given the current state and an action:

```
transitions
  .stream()
  .filter(t -> t.getFrom().equals(currentState) && t.getAction().equals(action))
  .findFirst()
  .orElseThrow(() -> new InvalidTransitionException(...))
```

This is a **deterministic** machine: at most one transition can match `(from, action)` — enforced by the DSL validation rule that prohibits duplicate pairs.

### Terminal State Detection

After applying a transition, the service checks whether the new `currentState` is marked `terminal: true` in the definition's state list. If so, it sets `instance.status = "COMPLETED"`.

---

## Transactions

| Operation | Transaction boundary |
|-----------|---------------------|
| `loadFromDto` | `@Transactional` — writes definition |
| `startInstance` | `@Transactional` — writes instance + first history entry atomically |
| `triggerTransition` | `@Transactional` — updates instance + writes history entry atomically |
| `findById` / `findAll` | `@Transactional(readOnly = true)` |
| `findByName` / `findAll` (definition) | `@Transactional(readOnly = true)` |

---

## Dependencies

```
WorkflowDefinitionService
  └── WorkflowDefinitionRepository
  └── ObjectMapper (Jackson, for JSON serialisation of states/transitions)

WorkflowInstanceService
  └── WorkflowInstanceRepository
  └── WorkflowHistoryEntryRepository
  └── WorkflowDefinitionRepository  (to look up definition for a new instance)
  └── ObjectMapper (for deserialising states/transitions from JSON blobs)
```
