# US-008: Backend Workflow Engine

## User Story

> **As a** business user / developer,  
> **I want** a REST API-based workflow engine that can define and execute multi-step business processes using a YAML DSL,  
> **so that** workflows—including long-running ones—can be managed reliably, and I can model, start, pause, resume, and drive stateful workflows (e.g. order processing, employee onboarding) through their lifecycle via HTTP calls.

**Source:** https://github.com/lofidewanto/java-ai-simple-demos/issues/8  
**Reference workflow:** https://github.com/lofidewanto/java-ai-simple-demos/issues/7 (Bestellabwicklung — Order Processing)

---

## Acceptance Criteria

| ID   | Criterion |
|------|-----------|
| AC-1 | Workflow definitions are loaded from YAML files at startup |
| AC-2 | Each definition has named states (one initial, one or more terminal) and named transitions with actions |
| AC-3 | A new workflow instance can be started via REST, placing it in the initial state |
| AC-4 | A transition can be triggered on a running instance via REST using an action name |
| AC-5 | Invalid transitions (wrong state, unknown action) are rejected with a clear error response |
| AC-6 | Completed (terminal) instances cannot be transitioned further |
| AC-7 | The full history of state changes is persisted per instance |
| AC-8 | Workflow definitions and instances are queryable via REST |
| AC-9 | Transitions carry an optional `taskType` (HUMAN, SERVICE, GATEWAY, EVENT); default is HUMAN |
| AC-10 | Three example YAML workflows ship with the application |
| AC-11 | A running workflow instance can be paused via REST, setting status to `PAUSED` |
| AC-12 | A paused workflow instance can be resumed via REST, setting status back to `RUNNING` |
| AC-13 | State transitions are rejected on paused instances with a clear error response |

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Persistence | Spring Data JPA + H2 (in-memory) |
| Migrations | Flyway |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| YAML parsing | `jackson-dataformat-yaml` |
| Build | Maven |

---

## Architecture

Four-layer architecture:

```
REST Controllers  →  Services  →  Repositories  →  Database (H2)
                         ↑
                   YAML Loader (startup)
```

### Package Structure

```
com.example.workflow
├── controller
│   ├── WorkflowDefinitionApiController.java
│   └── WorkflowInstanceApiController.java
├── service
│   ├── WorkflowDefinitionService.java
│   └── WorkflowInstanceService.java
├── repository
│   ├── WorkflowDefinitionRepository.java
│   ├── WorkflowInstanceRepository.java
│   └── WorkflowHistoryEntryRepository.java
├── domain
│   ├── WorkflowDefinition.java        (JPA entity)
│   ├── WorkflowInstance.java          (JPA entity)
│   ├── WorkflowHistoryEntry.java      (JPA entity)
│   └── TaskType.java                  (enum)
├── dto
│   ├── WorkflowDefinitionDto.java
│   ├── StateDto.java
│   ├── TransitionDto.java
│   ├── StartWorkflowRequest.java
│   ├── TriggerTransitionRequest.java
│   └── WorkflowInstanceResponse.java
├── exception
│   ├── WorkflowNotFoundException.java
│   ├── InstanceNotFoundException.java
│   ├── InvalidTransitionException.java
│   ├── WorkflowCompletedException.java
│   ├── WorkflowNotRunningException.java
│   ├── WorkflowPausedException.java
│   └── GlobalExceptionHandler.java
├── loader
│   └── WorkflowDefinitionLoader.java  (CommandLineRunner)
└── DemoSddWorkflowSpringApplication.java
```

---

## Domain Model

### WorkflowDefinition (JPA entity)

Stores a parsed workflow definition. The full YAML is stored as a `TEXT` column; states and transitions are stored as JSON blobs.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK, auto-generated |
| `name` | String | Unique, e.g. `order-processing` |
| `description` | String | Human-readable description |
| `source` | String | Optional URL (e.g. GitHub issue) |
| `statesJson` | String (TEXT) | JSON array of StateDto |
| `transitionsJson` | String (TEXT) | JSON array of TransitionDto |
| `createdAt` | LocalDateTime | Set on insert |

### WorkflowInstance (JPA entity)

Represents a running (or completed) execution of a workflow definition.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK, auto-generated |
| `workflowDefinition` | WorkflowDefinition | FK |
| `currentState` | String | Name of current state |
| `status` | String | `RUNNING`, `PAUSED`, or `COMPLETED` |
| `createdAt` | LocalDateTime | Set on insert |
| `updatedAt` | LocalDateTime | Updated on each transition or status change |

### WorkflowHistoryEntry (JPA entity)

Immutable record of each state transition taken by an instance.

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | PK, auto-generated |
| `workflowInstance` | WorkflowInstance | FK |
| `fromState` | String | State before transition (`null` for initial) |
| `toState` | String | State after transition |
| `action` | String | Action that triggered the transition |
| `taskType` | TaskType | HUMAN / SERVICE / GATEWAY / EVENT |
| `occurredAt` | LocalDateTime | Set on insert |

### TaskType (enum)

```java
public enum TaskType {
    HUMAN,    // manual human task (default)
    SERVICE,  // automated service call
    GATEWAY,  // conditional routing / decision
    EVENT     // external event trigger
}
```

---

## DTOs

### WorkflowDefinitionDto

```json
{
  "name": "order-processing",
  "description": "Bestellabwicklung",
  "source": "https://github.com/...",
  "states": [ { "name": "NEW", "initial": true, "terminal": false, "description": "Neue Bestellung" } ],
  "transitions": [ { "from": "NEW", "to": "CHECKING_AVAILABILITY", "action": "SUBMIT_ORDER", "taskType": "HUMAN", "description": "..." } ]
}
```

### StateDto

| Field | Type | Required |
|-------|------|----------|
| `name` | String | yes |
| `initial` | boolean | yes |
| `terminal` | boolean | yes |
| `description` | String | no |

### TransitionDto

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `from` | String | yes | — |
| `to` | String | yes | — |
| `action` | String | yes | — |
| `taskType` | TaskType | no | `HUMAN` |
| `description` | String | no | — |

### StartWorkflowRequest

```json
{ "workflowName": "order-processing" }
```

### TriggerTransitionRequest

```json
{ "action": "SUBMIT_ORDER" }
```

### WorkflowInstanceResponse

```json
{
  "id": 1,
  "workflowName": "order-processing",
  "currentState": "CHECKING_AVAILABILITY",
  "status": "RUNNING",
  "createdAt": "2026-03-07T10:00:00",
  "updatedAt": "2026-03-07T10:01:00",
  "history": [
    { "fromState": null, "toState": "NEW", "action": "START", "taskType": "HUMAN", "occurredAt": "..." },
    { "fromState": "NEW", "toState": "CHECKING_AVAILABILITY", "action": "SUBMIT_ORDER", "taskType": "HUMAN", "occurredAt": "..." }
  ]
}
```

---

## REST API Endpoints

### Workflow Definitions

> **Note:** Workflow definitions are loaded from YAML files at application startup. Runtime creation of definitions via API is not supported in this version.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/workflow-definitions` | List all loaded definitions |
| `GET` | `/api/workflow-definitions/{name}` | Get a single definition by name |

### Workflow Instances

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/workflow-instances` | Start a new instance |
| `GET` | `/api/workflow-instances` | List all instances |
| `GET` | `/api/workflow-instances/{id}` | Get instance with full history |
| `POST` | `/api/workflow-instances/{id}/transitions` | Trigger a transition |
| `POST` | `/api/workflow-instances/{id}/pause` | Pause a running instance |
| `POST` | `/api/workflow-instances/{id}/resume` | Resume a paused instance |
| `GET` | `/api/workflow-instances/{id}/history` | Get history entries only |
| `GET` | `/api/workflow-instances?workflowName=X` | Filter instances by definition name |

### Error Responses

| Scenario | HTTP Status | Error key |
|----------|-------------|-----------|
| Definition not found | 404 | `WORKFLOW_NOT_FOUND` |
| Instance not found | 404 | `INSTANCE_NOT_FOUND` |
| Invalid transition (bad action / wrong state) | 422 | `INVALID_TRANSITION` |
| Instance already completed | 422 | `WORKFLOW_COMPLETED` |
| Instance not running (for pause) | 422 | `WORKFLOW_NOT_RUNNING` |
| Instance not paused (for resume) | 422 | `WORKFLOW_NOT_PAUSED` |
| Transition attempted on paused instance | 422 | `WORKFLOW_PAUSED` |
| Validation error (missing fields) | 400 | `VALIDATION_ERROR` |

---

## YAML DSL Format

```yaml
name: order-processing
description: "Bestellabwicklung — Manages the order lifecycle"
source: "https://github.com/lofidewanto/java-ai-simple-demos/issues/7"

states:
  - name: NEW
    initial: true
    description: "Neue Bestellung eingegangen"
  - name: CHECKING_AVAILABILITY
    description: "Verfügbarkeit wird geprüft"
  - name: PAYMENT_PENDING
    description: "Zahlung ausstehend"
  - name: SHIPPED
    terminal: true
    description: "Bestellung versendet"
  - name: CANCELLED
    terminal: true
    description: "Bestellung storniert"

transitions:
  - from: NEW
    to: CHECKING_AVAILABILITY
    action: SUBMIT_ORDER
    taskType: HUMAN
    description: "Kunde gibt Bestellung auf"
  - from: CHECKING_AVAILABILITY
    to: PAYMENT_PENDING
    action: STOCK_AVAILABLE
    taskType: GATEWAY
    description: "Lagerbestand ausreichend"
  - from: CHECKING_AVAILABILITY
    to: CANCELLED
    action: STOCK_UNAVAILABLE
    taskType: GATEWAY
    description: "Lagerbestand nicht ausreichend"
  - from: PAYMENT_PENDING
    to: SHIPPED
    action: PAYMENT_COLLECTED
    taskType: SERVICE
    description: "Zahlung erfolgreich eingezogen"
```

**Validation rules:**
- Exactly one state with `initial: true`
- At least one state with `terminal: true`
- All `from` and `to` values must reference declared state names
- No duplicate `(from, action)` pairs

---

## Services

### WorkflowDefinitionService

| Method | Description |
|--------|-------------|
| `loadFromYaml(InputStream)` | Parse YAML → validate → persist `WorkflowDefinition` |
| `findAll()` | Return all definitions as DTOs |
| `findByName(String)` | Return one definition or throw `WorkflowNotFoundException` |

### WorkflowInstanceService

| Method | Description |
|--------|-------------|
| `startInstance(String workflowName)` | Create instance in initial state, write first history entry |
| `triggerTransition(Long id, String action)` | Validate + apply transition, write history entry |
| `pauseInstance(Long id)` | Set instance status to PAUSED if currently RUNNING |
| `resumeInstance(Long id)` | Set instance status to RUNNING if currently PAUSED |
| `findById(Long id)` | Return instance with history or throw `InstanceNotFoundException` |
| `findAll(String workflowName)` | List instances, optionally filtered by workflow name |

---

## Exceptions

| Class | HTTP | Trigger |
|-------|------|---------|
| `WorkflowNotFoundException` | 404 | Definition name not found |
| `InstanceNotFoundException` | 404 | Instance ID not found |
| `InvalidTransitionException` | 422 | No matching transition for current state + action |
| `WorkflowCompletedException` | 422 | Instance is already in a terminal state |
| `WorkflowNotRunningException` | 422 | Instance is not in RUNNING state (for pause) |
| `WorkflowPausedException` | 422 | Instance is PAUSED (for transitions or resume validation) |

`GlobalExceptionHandler` (`@ControllerAdvice`) maps all exceptions to a consistent JSON error body:

```json
{
  "error": "INVALID_TRANSITION",
  "message": "No transition found for action 'SHIP_ORDER' in state 'NEW'",
  "timestamp": "2026-03-07T10:05:00"
}
```

---

## Startup Loader

`WorkflowDefinitionLoader` implements `CommandLineRunner`. At startup it:

1. Scans `classpath:workflows/*.yml`
2. Parses each file using `jackson-dataformat-yaml`
3. Validates DSL rules
4. Persists each as a `WorkflowDefinition` (skips if already present by name)

---

## Database Migrations (Flyway)

### V1: workflow_definitions

```sql
CREATE TABLE workflow_definitions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    description  VARCHAR(1000),
    source       VARCHAR(1000),
    states_json  TEXT NOT NULL,
    transitions_json TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### V2: workflow_instances

```sql
CREATE TABLE workflow_instances (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_definition_id BIGINT NOT NULL,
    current_state          VARCHAR(255) NOT NULL,
    status                 VARCHAR(50)  NOT NULL DEFAULT 'RUNNING',
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_instance_definition
        FOREIGN KEY (workflow_definition_id)
        REFERENCES workflow_definitions(id)
);
```

### V3: workflow_history_entries

```sql
CREATE TABLE workflow_history_entries (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_instance_id BIGINT NOT NULL,
    from_state           VARCHAR(255),
    to_state             VARCHAR(255) NOT NULL,
    action               VARCHAR(255) NOT NULL,
    task_type            VARCHAR(50)  NOT NULL DEFAULT 'HUMAN',
    occurred_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_instance
        FOREIGN KEY (workflow_instance_id)
        REFERENCES workflow_instances(id)
);
```

---

## Testing Strategy

### Test Classes

| Class | Type | Focus |
|-------|------|-------|
| `WorkflowDefinitionLoaderTest` | Unit | YAML parsing, validation rules |
| `WorkflowDefinitionServiceTest` | Unit | Service logic, find/load |
| `WorkflowInstanceServiceTest` | Unit | State machine transitions, error cases |
| `WorkflowDefinitionApiControllerTest` | `@WebMvcTest` | Definition endpoints |
| `WorkflowInstanceApiControllerTest` | `@WebMvcTest` | Instance endpoints, error mapping |
| `WorkflowEngineIntegrationTest` | `@SpringBootTest` | Full lifecycle end-to-end |

### Key Test Cases

**WorkflowDefinitionLoaderTest**
- TC-01: Valid YAML is parsed into correct states and transitions
- TC-02: Missing `initial` state throws validation error
- TC-03: Missing `terminal` state throws validation error
- TC-04: Unknown `from` reference throws validation error
- TC-05: Duplicate `(from, action)` pair throws validation error
- TC-06: `taskType` defaults to `HUMAN` when omitted

**WorkflowDefinitionServiceTest**
- TC-07: `findAll()` returns all persisted definitions
- TC-08: `findByName()` returns correct definition
- TC-09: `findByName()` throws `WorkflowNotFoundException` for unknown name

**WorkflowInstanceServiceTest**
- TC-10: `startInstance()` creates instance in initial state
- TC-11: `startInstance()` writes initial history entry with action `START`
- TC-12: `triggerTransition()` moves instance to correct next state
- TC-13: `triggerTransition()` writes history entry with correct taskType
- TC-14: `triggerTransition()` on terminal instance throws `WorkflowCompletedException`
- TC-15: `triggerTransition()` with unknown action throws `InvalidTransitionException`
- TC-16: `triggerTransition()` with valid action in wrong state throws `InvalidTransitionException`
- TC-17: Instance reaches terminal state → status set to `COMPLETED`
- TC-18: `findById()` throws `InstanceNotFoundException` for unknown ID
- TC-19: `pauseInstance()` sets status to `PAUSED` for running instance
- TC-20: `pauseInstance()` throws `WorkflowNotRunningException` for non-running instance
- TC-21: `resumeInstance()` sets status to `RUNNING` for paused instance
- TC-22: `resumeInstance()` throws `WorkflowPausedException` for non-paused instance
- TC-23: `triggerTransition()` on paused instance throws `WorkflowPausedException`

**WorkflowDefinitionApiControllerTest**
- TC-24: `GET /api/workflow-definitions` returns 200 with list
- TC-25: `GET /api/workflow-definitions/{name}` returns 200 for known name
- TC-26: `GET /api/workflow-definitions/{name}` returns 404 for unknown name

**WorkflowInstanceApiControllerTest**
- TC-27: `POST /api/workflow-instances` returns 201 with new instance
- TC-28: `POST /api/workflow-instances` with unknown workflowName returns 404
- TC-29: `GET /api/workflow-instances/{id}` returns 200 with history
- TC-30: `GET /api/workflow-instances/{id}` returns 404 for unknown ID
- TC-31: `POST /api/workflow-instances/{id}/transitions` returns 200 on valid action
- TC-32: `POST /api/workflow-instances/{id}/transitions` returns 422 on invalid action
- TC-33: `POST /api/workflow-instances/{id}/transitions` returns 422 on completed instance
- TC-34: `POST /api/workflow-instances/{id}/transitions` returns 422 on paused instance
- TC-35: `POST /api/workflow-instances/{id}/pause` returns 200 on running instance
- TC-36: `POST /api/workflow-instances/{id}/pause` returns 422 on non-running instance
- TC-37: `POST /api/workflow-instances/{id}/resume` returns 200 on paused instance
- TC-38: `POST /api/workflow-instances/{id}/resume` returns 422 on non-paused instance
- TC-39: `GET /api/workflow-instances?workflowName=X` filters correctly

**WorkflowEngineIntegrationTest**
- TC-40: Full order-processing lifecycle: start → SUBMIT_ORDER → STOCK_AVAILABLE → PAYMENT_COLLECTED → COMPLETED
- TC-41: Cancellation path: start → SUBMIT_ORDER → STOCK_UNAVAILABLE → COMPLETED
- TC-42: Cannot trigger transition after reaching terminal state
- TC-43: History entries are correct after full lifecycle
- TC-44: Three example workflows are loaded at startup
- TC-45: Swagger UI is accessible at `/swagger-ui.html`
- TC-46: H2 console is accessible at `/h2-console`
- TC-47: Pause and resume lifecycle: start → pause → resume → continue transitions

---

## Implementation Task Plan

### Phase 1: Project Setup
- **T-01** Add `jackson-dataformat-yaml` dependency to `pom.xml`
- **T-02** Replace `application.properties` with full configuration (H2, Flyway, JPA, Swagger, H2 console)
- **T-03** Create `src/main/resources/db/migration/` and write 3 Flyway SQL scripts

### Phase 2: YAML Example Workflows
- **T-04** Create `src/main/resources/workflows/order-processing.yml`
- **T-05** Create `src/main/resources/workflows/employee-onboarding.yml`
- **T-06** Create `src/main/resources/workflows/support-ticket.yml`

### Phase 3: Domain Layer
- **T-07** Create `TaskType.java` enum
- **T-08** Create `WorkflowDefinition.java` JPA entity
- **T-09** Create `WorkflowInstance.java` JPA entity
- **T-10** Create `WorkflowHistoryEntry.java` JPA entity

### Phase 4: DTOs
- **T-11** Create `StateDto.java`
- **T-12** Create `TransitionDto.java`
- **T-13** Create `WorkflowDefinitionDto.java`
- **T-14** Create `StartWorkflowRequest.java`
- **T-15** Create `TriggerTransitionRequest.java`
- **T-16** Create `WorkflowInstanceResponse.java`

### Phase 5: Repositories
- **T-17** Create `WorkflowDefinitionRepository.java`
- **T-18** Create `WorkflowInstanceRepository.java`
- **T-19** Create `WorkflowHistoryEntryRepository.java`

### Phase 6: Exceptions
- **T-20** Create `WorkflowNotFoundException.java`
- **T-21** Create `InstanceNotFoundException.java`
- **T-22** Create `InvalidTransitionException.java`
- **T-23** Create `WorkflowCompletedException.java`
- **T-24** Create `WorkflowNotRunningException.java`
- **T-25** Create `WorkflowPausedException.java`
- **T-26** Create `GlobalExceptionHandler.java`

### Phase 7: Services
- **T-27** Create `WorkflowDefinitionService.java`
- **T-28** Create `WorkflowInstanceService.java` (including `pauseInstance()` and `resumeInstance()`)

### Phase 8: Startup Loader
- **T-29** Create `WorkflowDefinitionLoader.java` (CommandLineRunner)

### Phase 9: Controllers
- **T-30** Create `WorkflowDefinitionApiController.java`
- **T-31** Create `WorkflowInstanceApiController.java` (including pause/resume endpoints)

### Phase 10: Tests & Verification
- **T-32** Write `WorkflowDefinitionLoaderTest.java`
- **T-33** Write `WorkflowDefinitionServiceTest.java`
- **T-34** Write `WorkflowInstanceServiceTest.java` (including pause/resume tests)
- **T-35** Write `WorkflowDefinitionApiControllerTest.java`
- **T-36** Write `WorkflowInstanceApiControllerTest.java` (including pause/resume endpoint tests)
- **T-37** Write `WorkflowEngineIntegrationTest.java` (including pause/resume lifecycle tests)
- **T-38** Run `./mvnw clean package` and fix any failures

---

## Acceptance Criteria Traceability

| AC | Covered by Tasks |
|----|-----------------|
| AC-1 | T-02, T-03, T-04, T-05, T-06, T-29 |
| AC-2 | T-07, T-08, T-11, T-12, T-13 |
| AC-3 | T-09, T-10, T-28, T-31 |
| AC-4 | T-28, T-31 |
| AC-5 | T-22, T-26, T-28 |
| AC-6 | T-23, T-26, T-28 |
| AC-7 | T-10, T-19, T-28 |
| AC-8 | T-27, T-30, T-31 |
| AC-9 | T-07, T-12, T-16 |
| AC-10 | T-04, T-05, T-06 |
| AC-11 | T-24, T-26, T-28, T-31 |
| AC-12 | T-25, T-26, T-28, T-31 |
| AC-13 | T-25, T-26, T-28, T-31 |

---

## Test Results

**Date:** 2026-03-07  
**Build:** `./mvnw clean package` — **BUILD SUCCESS** (52 tests total, 0 failures, 0 errors)

### WorkflowDefinitionLoaderTest

| ID | Description | Result |
|----|-------------|--------|
| TC-01 | Valid YAML is parsed into correct states and transitions | PASSED |
| TC-02 | Missing `initial` state throws validation error | PASSED |
| TC-03 | Missing `terminal` state throws validation error | PASSED |
| TC-04 | Unknown `from` reference throws validation error | PASSED |
| TC-05 | Duplicate `(from, action)` pair throws validation error | PASSED |
| TC-06 | `taskType` defaults to `HUMAN` when omitted | PASSED |

### WorkflowDefinitionServiceTest

| ID | Description | Result |
|----|-------------|--------|
| TC-07 | `findAll()` returns all persisted definitions | PASSED |
| TC-08 | `findByName()` returns correct definition | PASSED |
| TC-09 | `findByName()` throws `WorkflowNotFoundException` for unknown name | PASSED |

### WorkflowInstanceServiceTest

| ID | Description | Result |
|----|-------------|--------|
| TC-10 | `startInstance()` creates instance in initial state | PASSED |
| TC-11 | `startInstance()` writes initial history entry with action `START` | PASSED |
| TC-12 | `triggerTransition()` moves instance to correct next state | PASSED |
| TC-13 | `triggerTransition()` writes history entry with correct taskType | PASSED |
| TC-14 | `triggerTransition()` on terminal instance throws `WorkflowCompletedException` | PASSED |
| TC-15 | `triggerTransition()` with unknown action throws `InvalidTransitionException` | PASSED |
| TC-16 | `triggerTransition()` with valid action in wrong state throws `InvalidTransitionException` | PASSED |
| TC-17 | Instance reaches terminal state → status set to `COMPLETED` | PASSED |
| TC-18 | `findById()` throws `InstanceNotFoundException` for unknown ID | PASSED |
| TC-19 | `pauseInstance()` sets status to `PAUSED` for running instance | PASSED |
| TC-20 | `pauseInstance()` throws `WorkflowNotRunningException` for non-running instance | PASSED |
| TC-21 | `resumeInstance()` sets status to `RUNNING` for paused instance | PASSED |
| TC-22 | `resumeInstance()` throws `WorkflowPausedException` for non-paused instance | PASSED |
| TC-23 | `triggerTransition()` on paused instance throws `WorkflowPausedException` | PASSED |

### WorkflowDefinitionApiControllerTest

| ID | Description | Result |
|----|-------------|--------|
| TC-24 | `GET /api/workflow-definitions` returns 200 with list | PASSED |
| TC-25 | `GET /api/workflow-definitions/{name}` returns 200 for known name | PASSED |
| TC-26 | `GET /api/workflow-definitions/{name}` returns 404 for unknown name | PASSED |

### WorkflowInstanceApiControllerTest

| ID | Description | Result |
|----|-------------|--------|
| TC-27 | `POST /api/workflow-instances` returns 201 with new instance | PASSED |
| TC-28 | `POST /api/workflow-instances` with unknown workflowName returns 404 | PASSED |
| TC-29 | `GET /api/workflow-instances/{id}` returns 200 with history | PASSED |
| TC-30 | `GET /api/workflow-instances/{id}` returns 404 for unknown ID | PASSED |
| TC-31 | `POST /api/workflow-instances/{id}/transitions` returns 200 on valid action | PASSED |
| TC-32 | `POST /api/workflow-instances/{id}/transitions` returns 422 on invalid action | PASSED |
| TC-33 | `POST /api/workflow-instances/{id}/transitions` returns 422 on completed instance | PASSED |
| TC-34 | `POST /api/workflow-instances/{id}/transitions` returns 422 on paused instance | PASSED |
| TC-35 | `POST /api/workflow-instances/{id}/pause` returns 200 on running instance | PASSED |
| TC-36 | `POST /api/workflow-instances/{id}/pause` returns 422 on non-running instance | PASSED |
| TC-37 | `POST /api/workflow-instances/{id}/resume` returns 200 on paused instance | PASSED |
| TC-38 | `POST /api/workflow-instances/{id}/resume` returns 422 on non-paused instance | PASSED |
| TC-39 | `GET /api/workflow-instances?workflowName=X` filters correctly | PASSED |

### WorkflowEngineIntegrationTest

| ID | Description | Result |
|----|-------------|--------|
| TC-40 | Full order-processing lifecycle: start → SUBMIT_ORDER → STOCK_AVAILABLE → PAYMENT_COLLECTED → COMPLETED | PASSED |
| TC-41 | Cancellation path: start → SUBMIT_ORDER → STOCK_UNAVAILABLE → COMPLETED | PASSED |
| TC-42 | Cannot trigger transition after reaching terminal state | PASSED |
| TC-43 | History entries are correct after full lifecycle | PASSED |
| TC-44 | Three example workflows are loaded at startup | PASSED |
| TC-45 | Swagger UI is accessible at `/swagger-ui.html` | PASSED |
| TC-46 | H2 console is accessible at `/h2-console` | PASSED |
| TC-47 | Pause and resume lifecycle: start → pause → resume → continue transitions | PASSED |
