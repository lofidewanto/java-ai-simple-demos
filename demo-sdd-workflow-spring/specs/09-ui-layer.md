# 09 — UI Layer

## Overview

The UI layer provides a browser-based interface for the workflow engine, served at `/ui`. It is built with **Thymeleaf** (server-rendered page shells) and **Bootstrap 5** (styling via CDN), with **vanilla JavaScript `fetch`** calls to the existing REST API for all data loading and mutations.

The UI covers all acceptance criteria from [US-009](../userstories/US-009-workflow-ui.md):
- View available workflow definitions
- Start new workflow instances
- Monitor instance state and status
- View instance execution history
- Trigger transitions, pause, and resume instances

No new backend services or repositories are required — the UI is a thin layer on top of the existing `/api/*` REST endpoints.

---

## Architecture

```
Browser
  │
  │  GET /ui/*
  ▼
WorkflowUiController  (@Controller)
  │  renders Thymeleaf template shells
  ▼
templates/*.html  (Bootstrap 5 layout + vanilla JS)
  │
  │  JS fetch() on page load / user interaction
  ▼
Existing REST API  (/api/workflow-definitions, /api/workflow-instances/*)
  │
  ▼
WorkflowDefinitionService / WorkflowInstanceService
```

The controller is intentionally thin — it only returns view names. All workflow data is fetched and rendered client-side.

---

## Controller

### WorkflowUiController

**Package:** `com.example.workflow.controller`  
**Annotation:** `@Controller` (not `@RestController`)

| Method | Path | Return | Description |
|--------|------|--------|-------------|
| `GET` | `/ui` | redirect:`/ui/workflows` | Entry-point redirect |
| `GET` | `/ui/workflows` | view:`workflows` | Workflow definitions page |
| `GET` | `/ui/instances` | view:`instances` | Workflow instances list page |
| `GET` | `/ui/instances/{id}` | view:`instance-detail` | Instance detail page |

The `{id}` path variable is accepted but not forwarded to the template as a model attribute — the JavaScript on the page reads it from `window.location.pathname`.

---

## Templates

All templates are located under `src/main/resources/templates/`. They use the shared `layout.html` Thymeleaf fragment for the Bootstrap navbar and footer.

---

### layout.html

Thymeleaf fragment providing the shared page structure.

**Elements:**
- `<html>` with Bootstrap 5 CDN stylesheet link in `<head>`
- Responsive navbar with brand "Workflow Engine" and two nav links:
  - **Workflow Definitions** → `/ui/workflows`
  - **Instances** → `/ui/instances`
- `<main>` content block (`th:fragment="content"`)
- Bootstrap 5 bundle CDN script at bottom of `<body>`

---

### workflows.html

**Route:** `GET /ui/workflows`  
**Covers:** AC-1 (list definitions), AC-2 (start instance)

**On page load (JS):**
1. `GET /api/workflow-definitions` — renders one Bootstrap card per definition

**Card content per definition:**
- Name (heading) and description
- State count and transition count badges
- Collapsible section: states table (name, initial/terminal flags, description) and transitions table (from → action → to, taskType badge)
- **"Start New Instance"** button: `POST /api/workflow-instances` with `{"workflowName": "<name>"}`, then redirects to `/ui/instances/{newId}`
- On POST failure: show a Bootstrap `alert-danger` div inside the card with the `message` field from the JSON error response

---

### instances.html

**Route:** `GET /ui/instances`  
**Covers:** AC-3 (list instances with state and status)

**On page load (JS):**
1. `GET /api/workflow-definitions` — populates filter dropdown
2. `GET /api/workflow-instances[?workflowName=X]` — renders instance table

**Table columns:** ID | Workflow | Current State | Status (badge) | Created | Updated | Actions

**Status badges:**
| Status | Bootstrap colour |
|--------|-----------------|
| `RUNNING` | `bg-success` (green) |
| `PAUSED` | `bg-warning text-dark` (orange) |
| `COMPLETED` | `bg-secondary` (grey) |

**Actions column:** "View" link to `/ui/instances/{id}`

**Filter:** Selecting a workflow name in the dropdown re-fetches with `?workflowName=X`. "All Workflows" option resets the filter.

**Refresh:** Manual "Refresh" button re-runs the fetch.

---

### instance-detail.html

**Route:** `GET /ui/instances/{id}`  
**Covers:** AC-4 (execution history and current state), AC-5 (trigger events)

**On page load (JS):**
1. Extract `id` from `window.location.pathname` using: `const id = window.location.pathname.split('/').filter(Boolean).pop()`
2. `GET /api/workflow-instances/{id}` — loads instance data
3. `GET /api/workflow-definitions/{workflowName}` — loads definition to derive available actions

**Page sections:**

#### Instance Summary
- Instance ID, workflow name, current state (highlighted), status badge, created/updated timestamps

#### Available Actions
Rendered conditionally based on `status`:
- **`RUNNING`**: For each transition in the definition where `from == currentState`, render a button labelled with the action name and a small taskType badge
  - Clicking: `POST /api/workflow-instances/{id}/transitions` with `{"action": "<action>"}` → reload page on success, show error alert on failure
- **`PAUSED`**: Show a muted info message — _"Workflow is paused — resume to trigger actions"_ — no action buttons rendered
- **`COMPLETED`**: Show a muted info message — _"Workflow has completed — no actions available"_ — no action buttons rendered

#### Pause / Resume Controls
- **Pause** button: shown when `status == "RUNNING"` — calls `POST /api/workflow-instances/{id}/pause`
- **Resume** button: shown when `status == "PAUSED"` — calls `POST /api/workflow-instances/{id}/resume`
- Both reload the page data after the call

#### Execution History Timeline
- Bootstrap list group, one entry per history item, ordered chronologically ascending
- Each entry shows: `fromState → toState`, action name, taskType badge, timestamp
- Initial entry (`fromState == null`) shown as "— → toState (START)"

#### Navigation
- "Back to Instances" link → `/ui/instances`

---

## TaskType Badge Colours

| TaskType | Bootstrap colour |
|----------|-----------------|
| `HUMAN` | `bg-primary` (blue) |
| `SERVICE` | `bg-info text-dark` (cyan) |
| `GATEWAY` | `bg-warning text-dark` (yellow) |
| `EVENT` | `bg-danger` (red) |

---

## JavaScript API Call Map

| Page | Trigger | JS call | REST endpoint |
|------|---------|---------|---------------|
| `workflows.html` | Page load | `fetch('/api/workflow-definitions')` | `GET /api/workflow-definitions` |
| `workflows.html` | "Start New Instance" click | `fetch('/api/workflow-instances', {method:'POST', ...})` | `POST /api/workflow-instances` |
| `instances.html` | Page load | `fetch('/api/workflow-definitions')` | `GET /api/workflow-definitions` |
| `instances.html` | Page load / filter change | `fetch('/api/workflow-instances[?workflowName=X]')` | `GET /api/workflow-instances` |
| `instance-detail.html` | Page load | `fetch('/api/workflow-instances/{id}')` | `GET /api/workflow-instances/{id}` |
| `instance-detail.html` | Page load (for transitions) | `fetch('/api/workflow-definitions/{name}')` | `GET /api/workflow-definitions/{name}` |
| `instance-detail.html` | Action button click | `fetch('/api/workflow-instances/{id}/transitions', {method:'POST', ...})` | `POST /api/workflow-instances/{id}/transitions` |
| `instance-detail.html` | Pause button click | `fetch('/api/workflow-instances/{id}/pause', {method:'POST'})` | `POST /api/workflow-instances/{id}/pause` |
| `instance-detail.html` | Resume button click | `fetch('/api/workflow-instances/{id}/resume', {method:'POST'})` | `POST /api/workflow-instances/{id}/resume` |

---

## Error Handling (Client Side)

All `fetch` calls check the response status. On error:
- A Bootstrap `alert-danger` div is shown at the top of the relevant section
- The error message is taken from the JSON `message` field in the REST error response body

---

## Files

| File | Type | Description |
|------|------|-------------|
| `src/main/java/com/example/workflow/controller/WorkflowUiController.java` | Java `@Controller` | Routes `/ui/*` to Thymeleaf view names |
| `src/main/resources/templates/layout.html` | Thymeleaf fragment | Shared Bootstrap 5 navbar + footer |
| `src/main/resources/templates/workflows.html` | Thymeleaf template | Definitions list + start-instance |
| `src/main/resources/templates/instances.html` | Thymeleaf template | Instance list with filter |
| `src/main/resources/templates/instance-detail.html` | Thymeleaf template | Instance detail + history + actions |
| `src/test/java/com/example/workflow/controller/WorkflowUiControllerTest.java` | `@WebMvcTest` | Tests all 4 UI routes |

---

## Test Cases

See [07 — Testing Strategy](07-testing-strategy.md) for TC-UI-01 through TC-UI-04.

| ID | Route tested | Assertion |
|----|-------------|-----------|
| TC-UI-01 | `GET /ui` | 302 redirect to `/ui/workflows` |
| TC-UI-02 | `GET /ui/workflows` | 200, view name `workflows` |
| TC-UI-03 | `GET /ui/instances` | 200, view name `instances` |
| TC-UI-04 | `GET /ui/instances/1` | 200, view name `instance-detail` |

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Thymeleaf template shells with JS data loading | Keeps controller thin; avoids passing large model objects; reuses existing REST API |
| Bootstrap 5 via CDN | No build tools (npm/webpack) needed; fits the Maven-only build |
| Vanilla JS `fetch` | No React/Vue overhead; appropriate for a lightweight demo UI |
| `window.location.pathname` for instance ID | Avoids needing to pass `id` from the controller into a hidden model attribute |
| All data via `/api/*` REST endpoints | UI is fully decoupled from the service layer; validates the REST API end-to-end |

---

## Known Limitations

| # | Area | Description |
|---|------|-------------|
| 1 | Test coverage — client-side JS | The `@WebMvcTest` tests (TC-UI-01 to TC-UI-04) verify only that the controller routes to the correct view name. Client-side JavaScript logic — filter dropdown behaviour, status badge rendering, history timeline, available-transitions derivation, pause/resume button visibility — is not covered by any automated test. This is acceptable for a demo project. |
| 2 | Payload overhead on instances list page | `GET /api/workflow-instances` embeds the full `history` array inside every instance object in the response. The `instances.html` page uses only the summary fields (ID, workflowName, currentState, status, timestamps) and discards the history data. The dedicated `GET /api/workflow-instances/{id}/history` endpoint exists but is intentionally not used on the list page to keep the number of fetch calls minimal. In a production UI this list endpoint should be a lightweight projection. |
