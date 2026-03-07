# 03 — API Specification

## Base URL

```
http://localhost:8080
```

All REST endpoints accept and return `application/json`. Interactive REST documentation is available at `http://localhost:8080/swagger-ui.html`.

The browser-based web UI is served from `http://localhost:8080/ui` — see [09 — UI Layer](09-ui-layer.md) for full UI route documentation.

---

## Workflow Definition Endpoints

### GET /api/workflow-definitions

List all loaded workflow definitions.

**Response 200 OK**

```json
[
  {
    "name": "order-processing",
    "description": "Bestellabwicklung — Manages the order lifecycle",
    "source": "https://github.com/lofidewanto/java-ai-simple-demos/issues/7",
    "states": [
      { "name": "NEW", "initial": true, "terminal": false, "description": "Neue Bestellung eingegangen" },
      { "name": "CHECKING_AVAILABILITY", "initial": false, "terminal": false, "description": "Verfügbarkeit wird geprüft" },
      { "name": "PAYMENT_PENDING", "initial": false, "terminal": false, "description": "Zahlung ausstehend" },
      { "name": "SHIPPED", "initial": false, "terminal": true, "description": "Bestellung versendet" },
      { "name": "CANCELLED", "initial": false, "terminal": true, "description": "Bestellung storniert" }
    ],
    "transitions": [
      { "from": "NEW", "to": "CHECKING_AVAILABILITY", "action": "SUBMIT_ORDER", "taskType": "HUMAN", "description": "Kunde gibt Bestellung auf" },
      { "from": "CHECKING_AVAILABILITY", "to": "PAYMENT_PENDING", "action": "STOCK_AVAILABLE", "taskType": "GATEWAY", "description": "Lagerbestand ausreichend" },
      { "from": "CHECKING_AVAILABILITY", "to": "CANCELLED", "action": "STOCK_UNAVAILABLE", "taskType": "GATEWAY", "description": "Lagerbestand nicht ausreichend" },
      { "from": "PAYMENT_PENDING", "to": "SHIPPED", "action": "PAYMENT_COLLECTED", "taskType": "SERVICE", "description": "Zahlung erfolgreich eingezogen" }
    ]
  }
]
```

---

### GET /api/workflow-definitions/{name}

Retrieve a single workflow definition by its unique name.

**Path parameter:** `name` — the workflow definition name (e.g. `order-processing`)

**Response 200 OK** — same shape as a single element from the list above.

**Response 404 Not Found**

```json
{
  "error": "WORKFLOW_NOT_FOUND",
  "message": "Workflow definition 'unknown-workflow' not found",
  "timestamp": "2026-03-07T10:00:00"
}
```

---

## Workflow Instance Endpoints

### POST /api/workflow-instances

Start a new workflow instance. The instance is created in the initial state of the named definition.

**Request body**

```json
{ "workflowName": "order-processing" }
```

**Response 201 Created**

```json
{
  "id": 1,
  "workflowName": "order-processing",
  "currentState": "NEW",
  "status": "RUNNING",
  "createdAt": "2026-03-07T10:00:00",
  "updatedAt": "2026-03-07T10:00:00",
  "history": [
    {
      "fromState": null,
      "toState": "NEW",
      "action": "START",
      "taskType": "HUMAN",
      "occurredAt": "2026-03-07T10:00:00"
    }
  ]
}
```

**Response 404 Not Found** — if `workflowName` does not match a loaded definition.

**Response 400 Bad Request** — if `workflowName` is missing or blank.

---

### GET /api/workflow-instances

List all workflow instances. Optionally filter by workflow definition name.

**Query parameter:** `workflowName` (optional) — filter to instances of a specific definition

**Examples:**
- `GET /api/workflow-instances` — all instances
- `GET /api/workflow-instances?workflowName=order-processing` — only order-processing instances

**Response 200 OK** — array of `WorkflowInstanceResponse` objects (same shape as POST response, each with full history).

---

### GET /api/workflow-instances/{id}

Retrieve a single workflow instance with its complete transition history.

**Path parameter:** `id` — numeric instance ID

**Response 200 OK** — `WorkflowInstanceResponse` with full `history` array.

**Response 404 Not Found**

```json
{
  "error": "INSTANCE_NOT_FOUND",
  "message": "Workflow instance with id 99 not found",
  "timestamp": "2026-03-07T10:00:00"
}
```

---

### POST /api/workflow-instances/{id}/transitions

Trigger a transition on a running workflow instance.

**Path parameter:** `id` — numeric instance ID

**Request body**

```json
{ "action": "SUBMIT_ORDER" }
```

**Response 200 OK** — updated `WorkflowInstanceResponse` with the new current state and updated history.

```json
{
  "id": 1,
  "workflowName": "order-processing",
  "currentState": "CHECKING_AVAILABILITY",
  "status": "RUNNING",
  "createdAt": "2026-03-07T10:00:00",
  "updatedAt": "2026-03-07T10:01:00",
  "history": [
    { "fromState": null, "toState": "NEW", "action": "START", "taskType": "HUMAN", "occurredAt": "2026-03-07T10:00:00" },
    { "fromState": "NEW", "toState": "CHECKING_AVAILABILITY", "action": "SUBMIT_ORDER", "taskType": "HUMAN", "occurredAt": "2026-03-07T10:01:00" }
  ]
}
```

**Response 404 Not Found** — if instance ID does not exist.

**Response 422 Unprocessable Entity** — invalid transition:

```json
{
  "error": "INVALID_TRANSITION",
  "message": "No transition found for action 'SHIP_ORDER' in state 'NEW'",
  "timestamp": "2026-03-07T10:01:00"
}
```

**Response 422 Unprocessable Entity** — instance already completed:

```json
{
  "error": "WORKFLOW_COMPLETED",
  "message": "Workflow instance 1 is already in a terminal state 'SHIPPED'",
  "timestamp": "2026-03-07T10:01:00"
}
```

---

### GET /api/workflow-instances/{id}/history

Retrieve only the history entries for an instance (without the full instance object).

**Path parameter:** `id` — numeric instance ID

**Response 200 OK**

```json
[
  { "fromState": null, "toState": "NEW", "action": "START", "taskType": "HUMAN", "occurredAt": "2026-03-07T10:00:00" },
  { "fromState": "NEW", "toState": "CHECKING_AVAILABILITY", "action": "SUBMIT_ORDER", "taskType": "HUMAN", "occurredAt": "2026-03-07T10:01:00" },
  { "fromState": "CHECKING_AVAILABILITY", "toState": "PAYMENT_PENDING", "action": "STOCK_AVAILABLE", "taskType": "GATEWAY", "occurredAt": "2026-03-07T10:02:00" }
]
```

**Response 404 Not Found** — if instance ID does not exist.

---

### POST /api/workflow-instances/{id}/pause

Pause a running workflow instance. Transitions are rejected while the instance is paused.

**Path parameter:** `id` — numeric instance ID

**Response 200 OK** — updated `WorkflowInstanceResponse` with `status = "PAUSED"`.

**Response 422 Unprocessable Entity** — if instance is not currently `RUNNING`:

```json
{
  "error": "WORKFLOW_NOT_RUNNING",
  "message": "Workflow instance 1 is not in RUNNING state",
  "timestamp": "2026-03-07T10:02:00"
}
```

---

### POST /api/workflow-instances/{id}/resume

Resume a paused workflow instance, returning it to `RUNNING` status.

**Path parameter:** `id` — numeric instance ID

**Response 200 OK** — updated `WorkflowInstanceResponse` with `status = "RUNNING"`.

**Response 422 Unprocessable Entity** — if instance is not currently `PAUSED`:

```json
{
  "error": "WORKFLOW_NOT_PAUSED",
  "message": "Workflow instance 1 is not in PAUSED state",
  "timestamp": "2026-03-07T10:02:00"
}
```

---

## Error Response Format

All error responses follow a consistent structure:

```json
{
  "error": "ERROR_KEY",
  "message": "Human-readable description of the error",
  "timestamp": "2026-03-07T10:00:00"
}
```

### Error Reference Table

| Scenario | HTTP Status | `error` value |
|----------|-------------|----------------|
| Workflow definition not found | 404 | `WORKFLOW_NOT_FOUND` |
| Workflow instance not found | 404 | `INSTANCE_NOT_FOUND` |
| Invalid action for current state | 422 | `INVALID_TRANSITION` |
| Instance is in terminal state | 422 | `WORKFLOW_COMPLETED` |
| Instance is not RUNNING (for pause) | 422 | `WORKFLOW_NOT_RUNNING` |
| Instance is not PAUSED (for resume) | 422 | `WORKFLOW_NOT_PAUSED` |
| Transition attempted on paused instance | 422 | `WORKFLOW_PAUSED` |
| Request body validation failure | 400 | `VALIDATION_ERROR` |

---

## Response Shape Reference

### WorkflowInstanceResponse

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Instance ID |
| `workflowName` | String | Definition name |
| `currentState` | String | Current state name |
| `status` | String | `RUNNING`, `PAUSED`, or `COMPLETED` |
| `createdAt` | LocalDateTime | ISO-8601 |
| `updatedAt` | LocalDateTime | ISO-8601 |
| `history` | List | See HistoryEntryResponse below |

### HistoryEntryResponse (embedded in WorkflowInstanceResponse)

| Field | Type | Notes |
|-------|------|-------|
| `fromState` | String | null for initial entry |
| `toState` | String | — |
| `action` | String | — |
| `taskType` | String | HUMAN / SERVICE / GATEWAY / EVENT |
| `occurredAt` | LocalDateTime | ISO-8601 |
