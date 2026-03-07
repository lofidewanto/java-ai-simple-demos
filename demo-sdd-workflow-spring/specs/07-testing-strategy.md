# 07 — Testing Strategy

## Overview

The test suite covers all layers of the application with a mix of unit tests, slice tests, and a full integration test. All tests run with `./mvnw test` against an in-memory H2 database (no external dependencies required).

---

## Test Classes

| Class | Type | Layer |
|-------|------|-------|
| `WorkflowDefinitionLoaderTest` | Unit (`@ExtendWith(MockitoExtension.class)`) | Loader / YAML parsing |
| `WorkflowDefinitionServiceTest` | Unit (`@ExtendWith(MockitoExtension.class)`) | Service |
| `WorkflowInstanceServiceTest` | Unit (`@ExtendWith(MockitoExtension.class)`) | Service / state machine |
| `WorkflowDefinitionApiControllerTest` | `@WebMvcTest` | Controller (definition) |
| `WorkflowInstanceApiControllerTest` | `@WebMvcTest` | Controller (instance) |
| `WorkflowEngineIntegrationTest` | `@SpringBootTest` | Full stack end-to-end |

**Package:** `com.example.workflow` (same as main, under `src/test/java`)

---

## Test Cases

### WorkflowDefinitionLoaderTest

Tests YAML parsing and DSL validation in isolation (mocked repository).

| ID | Test case | Expected outcome |
|----|-----------|-----------------|
| TC-01 | Valid YAML with all required fields is parsed correctly | Definition persisted with correct name, states, transitions |
| TC-02 | YAML with no `initial: true` state fails validation | `IllegalArgumentException` thrown before persistence |
| TC-03 | YAML with no `terminal: true` state fails validation | `IllegalArgumentException` thrown |
| TC-04 | Transition `from` references a state not in the states list | `IllegalArgumentException` thrown |
| TC-05 | Transition `to` references a state not in the states list | `IllegalArgumentException` thrown |
| TC-06 | `taskType` omitted on transition → defaults to `HUMAN` | Parsed `TransitionDto` has `taskType == HUMAN` |

---

### WorkflowDefinitionServiceTest

Tests service logic with mocked repositories.

| ID | Test case | Expected outcome |
|----|-----------|-----------------|
| TC-07 | `findAll()` with two persisted definitions returns a list of two DTOs | List size 2, names match |
| TC-08 | `findByName("order-processing")` returns the correct definition DTO | Name matches, states and transitions correct |
| TC-09 | `findByName("does-not-exist")` throws `WorkflowNotFoundException` | Exception message contains the unknown name |

---

### WorkflowInstanceServiceTest

Tests state machine logic with mocked repositories.

| ID | Test case | Expected outcome |
|----|-----------|-----------------|
| TC-10 | `startInstance("order-processing")` creates instance in initial state `NEW` | `currentState == "NEW"`, `status == "RUNNING"` |
| TC-11 | `startInstance(...)` writes initial history entry | History entry: `fromState=null`, `toState="NEW"`, `action="START"`, `taskType=HUMAN` |
| TC-12 | `triggerTransition(id, "SUBMIT_ORDER")` from `NEW` moves to `CHECKING_AVAILABILITY` | `currentState == "CHECKING_AVAILABILITY"` |
| TC-13 | `triggerTransition(id, "STOCK_AVAILABLE")` writes history entry with `taskType=GATEWAY` | History entry `taskType == GATEWAY` |
| TC-14 | `triggerTransition(id, ...)` on a `COMPLETED` instance throws `WorkflowCompletedException` | Exception thrown, instance unchanged |
| TC-15 | `triggerTransition(id, "UNKNOWN_ACTION")` throws `InvalidTransitionException` | Exception message contains the unknown action name |
| TC-16 | `triggerTransition(id, "STOCK_AVAILABLE")` when instance is in `NEW` (wrong state) throws `InvalidTransitionException` | Exception thrown |
| TC-17 | Triggering `PAYMENT_COLLECTED` from `PAYMENT_PENDING` sets `status = "COMPLETED"` | Status is `COMPLETED`, `currentState == "SHIPPED"` |
| TC-18 | `findById(99L)` throws `InstanceNotFoundException` | Exception message contains id `99` |

---

### WorkflowDefinitionApiControllerTest

`@WebMvcTest` slice — tests HTTP layer, mocks `WorkflowDefinitionService`.

| ID | Test case | Expected status |
|----|-----------|----------------|
| TC-19 | `GET /api/workflow-definitions` returns list | 200, JSON array |
| TC-20 | `GET /api/workflow-definitions/order-processing` returns definition | 200, name field matches |
| TC-21 | `GET /api/workflow-definitions/unknown` → service throws `WorkflowNotFoundException` | 404, error body with `WORKFLOW_NOT_FOUND` |

---

### WorkflowInstanceApiControllerTest

`@WebMvcTest` slice — tests HTTP layer, mocks `WorkflowInstanceService`.

| ID | Test case | Expected status |
|----|-----------|----------------|
| TC-22 | `POST /api/workflow-instances` with valid body returns new instance | 201, `currentState == "NEW"` |
| TC-23 | `POST /api/workflow-instances` with unknown `workflowName` → service throws `WorkflowNotFoundException` | 404 |
| TC-24 | `GET /api/workflow-instances/1` returns instance with history | 200, history array present |
| TC-25 | `GET /api/workflow-instances/99` → service throws `InstanceNotFoundException` | 404, `INSTANCE_NOT_FOUND` |
| TC-26 | `POST /api/workflow-instances/1/transitions` with valid action | 200, updated `currentState` |
| TC-27 | `POST /api/workflow-instances/1/transitions` with invalid action → `InvalidTransitionException` | 422, `INVALID_TRANSITION` |
| TC-28 | `POST /api/workflow-instances/1/transitions` on completed instance → `WorkflowCompletedException` | 422, `WORKFLOW_COMPLETED` |
| TC-29 | `GET /api/workflow-instances?workflowName=order-processing` filters correctly | 200, all returned instances have `workflowName == "order-processing"` |

---

### WorkflowEngineIntegrationTest

`@SpringBootTest(webEnvironment = RANDOM_PORT)` — full stack with real H2 and Flyway.

| ID | Test case | Expected outcome |
|----|-----------|-----------------|
| TC-30 | Full order-processing happy path: START → SUBMIT_ORDER → STOCK_AVAILABLE → PAYMENT_COLLECTED | Final `status == "COMPLETED"`, `currentState == "SHIPPED"` |
| TC-31 | Cancellation path: START → SUBMIT_ORDER → STOCK_UNAVAILABLE | Final `status == "COMPLETED"`, `currentState == "CANCELLED"` |
| TC-32 | Trigger transition after terminal state reached | 422 response with `WORKFLOW_COMPLETED` |
| TC-33 | History entries after full lifecycle are correct count and order | 4 entries in chronological order for TC-30 |
| TC-34 | All three example workflows are loaded at startup | `GET /api/workflow-definitions` returns 3 items with correct names |
| TC-35 | Swagger UI is accessible | `GET /swagger-ui.html` returns 200 or 302 |
| TC-36 | H2 console is accessible | `GET /h2-console` returns 200 |

---

## Test Configuration

Integration tests use the same `application.properties` as the main application (H2 in-memory, Flyway enabled). No separate test properties file is needed.

For `@WebMvcTest` slice tests, Spring Security auto-configuration is excluded if present, and services are mocked with `@MockBean`.

---

## Running the Tests

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=WorkflowEngineIntegrationTest

# Run with verbose output
./mvnw test -Dsurefire.failIfNoSpecifiedTests=false
```

---

## Coverage Goals

| Layer | Target coverage |
|-------|----------------|
| Service layer (`WorkflowInstanceService`) | 90%+ line coverage |
| Controller layer | All endpoints and error paths covered |
| Loader / validation | All 5 validation rules have a negative test case |
| Integration | Both happy paths and all error scenarios |
