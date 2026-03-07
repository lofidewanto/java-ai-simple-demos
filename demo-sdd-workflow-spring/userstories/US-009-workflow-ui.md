# US-009: Create a UI for the Workflow Engine

## User Story

> **As a** developer or tester,
> **I want** a simple web-based UI for the workflow engine,
> **so that** I can view workflows, start workflow instances, monitor their state, and interact with running workflows without using only APIs or logs.

**Source:** https://github.com/lofidewanto/java-ai-simple-demos/issues/9
**Depends on:** https://github.com/lofidewanto/java-ai-simple-demos/issues/8 (Backend Workflow Engine — US-008)

---

## Acceptance Criteria

| ID   | Criterion |
|------|-----------|
| AC-1 | The UI shows a list of available workflow definitions |
| AC-2 | A user can start a new workflow instance from the UI |
| AC-3 | The UI shows a list of workflow instances with their current state and status |
| AC-4 | A user can open a workflow instance and view its execution history and current state |
| AC-5 | If a workflow waits for an event, the user can trigger that event from the UI |
| AC-6 | The UI communicates with the workflow engine via REST APIs |
| AC-7 | The UI runs in a browser when the application is started locally |

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.11 |
| View layer | Thymeleaf (already on classpath via `spring-boot-starter-thymeleaf`) |
| UI styling | Bootstrap 5 (CDN — no build tools required) |
| UI data loading | Vanilla JS `fetch` API (calls existing REST endpoints) |
| Build | Maven |

---

## Architecture

```
Browser
  └── GET /ui/*  ──────────────────────────────►  WorkflowUiController  (Thymeleaf @Controller)
                                                          │
                                                          │  renders shell HTML template
                                                          ▼
                                               templates/*.html  (Bootstrap 5 + JS)
                                                          │
                                               JS fetch() on page load / button click
                                                          │
                                                          ▼
                                               Existing REST API  (/api/workflow-*)
                                                          │
                                                          ▼
                                               WorkflowInstanceService / WorkflowDefinitionService
```

The `WorkflowUiController` serves thin Thymeleaf page shells only. All data is loaded and rendered client-side using JavaScript calls to the existing `/api/*` endpoints. No model attributes containing workflow data are passed from the controller to the templates.

---

## Pages

### Page 1 — Workflow Definitions (`/ui/workflows`)

Covers: **AC-1, AC-2**

- Loads `GET /api/workflow-definitions` on page load
- Displays a card per definition: name, description, number of states, number of transitions
- Expandable section per definition showing states and transitions
- **"Start Instance"** button per definition: calls `POST /api/workflow-instances` then navigates to the new instance's detail page

### Page 2 — Workflow Instances (`/ui/instances`)

Covers: **AC-3**

- Loads `GET /api/workflow-instances` on page load
- Filter dropdown populated from `GET /api/workflow-definitions` to filter by workflow name
- Table columns: ID, Workflow Name, Current State, Status (badge), Created, Updated
- Status badges: green = RUNNING, orange = PAUSED, grey = COMPLETED
- Each row links to the instance detail page
- Manual refresh button

### Page 3 — Instance Detail (`/ui/instances/{id}`)

Covers: **AC-4, AC-5**

- Loads `GET /api/workflow-instances/{id}` on page load
- Shows: instance ID, workflow name, current state, status badge, created/updated timestamps
- **History timeline**: ordered list of history entries showing `fromState → toState` via action name, taskType badge, and timestamp
- **Available transitions**: derived by also fetching `GET /api/workflow-definitions/{name}` and matching transitions where `from == currentState`; rendered as action buttons
  - Clicking a button calls `POST /api/workflow-instances/{id}/transitions` with the action name and reloads the page
- **Pause** button (visible when status = RUNNING): calls `POST /api/workflow-instances/{id}/pause`
- **Resume** button (visible when status = PAUSED): calls `POST /api/workflow-instances/{id}/resume`
- **Back to Instances** link

### Shared Layout (`layout.html` — Thymeleaf fragment)

- Bootstrap 5 navbar with app title "Workflow Engine"
- Navigation links: Workflow Definitions | Instances
- Footer

---

## Controller Routes

| Method | Path | View | Description |
|--------|------|------|-------------|
| `GET` | `/ui` | redirect → `/ui/workflows` | Entry point redirect |
| `GET` | `/ui/workflows` | `workflows` | Workflow definitions page |
| `GET` | `/ui/instances` | `instances` | Workflow instances list page |
| `GET` | `/ui/instances/{id}` | `instance-detail` | Instance detail + history + actions |

---

## JavaScript API Call Map

| UI Action | JS fetch call | REST endpoint |
|-----------|--------------|---------------|
| Page load on `/ui/workflows` | `GET` | `/api/workflow-definitions` |
| Start instance button | `POST` | `/api/workflow-instances` |
| Page load on `/ui/instances` | `GET` | `/api/workflow-instances[?workflowName=X]` |
| Page load on `/ui/instances/{id}` | `GET` | `/api/workflow-instances/{id}` |
| Page load on `/ui/instances/{id}` (for transitions) | `GET` | `/api/workflow-definitions/{name}` |
| Trigger action button | `POST` | `/api/workflow-instances/{id}/transitions` |
| Pause button | `POST` | `/api/workflow-instances/{id}/pause` |
| Resume button | `POST` | `/api/workflow-instances/{id}/resume` |

---

## Files to Create

| File | Purpose |
|------|---------|
| `src/main/java/com/example/workflow/controller/WorkflowUiController.java` | `@Controller` with 3 page routes + redirect |
| `src/main/resources/templates/layout.html` | Shared Bootstrap 5 Thymeleaf fragment |
| `src/main/resources/templates/workflows.html` | Definitions list page |
| `src/main/resources/templates/instances.html` | Instances list page |
| `src/main/resources/templates/instance-detail.html` | Instance detail page |
| `src/test/java/com/example/workflow/controller/WorkflowUiControllerTest.java` | `@WebMvcTest` for UI controller |

---

## Test Cases

| ID | Class | Type | Description |
|----|-------|------|-------------|
| TC-UI-01 | `WorkflowUiControllerTest` | `@WebMvcTest` | `GET /ui` redirects to `/ui/workflows` |
| TC-UI-02 | `WorkflowUiControllerTest` | `@WebMvcTest` | `GET /ui/workflows` returns 200 and renders `workflows` view |
| TC-UI-03 | `WorkflowUiControllerTest` | `@WebMvcTest` | `GET /ui/instances` returns 200 and renders `instances` view |
| TC-UI-04 | `WorkflowUiControllerTest` | `@WebMvcTest` | `GET /ui/instances/{id}` returns 200 and renders `instance-detail` view |

---

## Implementation Task Plan

### Phase 1: Documentation
- **T-01** Write `userstories/US-009-workflow-ui.md` (this file)

### Phase 2: UI Controller
- **T-02** Create `WorkflowUiController.java` with redirect and 3 page routes

### Phase 3: Thymeleaf Templates
- **T-03** Create `layout.html` Thymeleaf fragment (Bootstrap 5 navbar + footer)
- **T-04** Create `workflows.html` (definitions list + start-instance button)
- **T-05** Create `instances.html` (instance table with status badges + filter dropdown)
- **T-06** Create `instance-detail.html` (history timeline, available action buttons, pause/resume)

### Phase 4: Tests
- **T-07** Create `WorkflowUiControllerTest.java` (TC-UI-01 to TC-UI-04)

### Phase 5: Verification
- **T-08** Run `./mvnw clean package` and fix any failures

---

## Acceptance Criteria Traceability

| AC | Covered by Tasks |
|----|-----------------|
| AC-1 | T-03, T-04 |
| AC-2 | T-04 |
| AC-3 | T-05 |
| AC-4 | T-06 |
| AC-5 | T-06 |
| AC-6 | T-04, T-05, T-06 (all data via JS fetch to `/api/*`) |
| AC-7 | T-02, T-03, T-04, T-05, T-06 |

---

## Test Results

**Date:** 2026-03-07  
**Build:** `./mvnw clean package` — **BUILD SUCCESS** (52 tests total, 0 failures, 0 errors)

### Unit / Integration Tests (TC-UI-01 to TC-UI-04)

| ID | Description | Result |
|----|-------------|--------|
| TC-UI-01 | `GET /ui` redirects to `/ui/workflows` | PASSED |
| TC-UI-02 | `GET /ui/workflows` returns 200 and renders `workflows` view | PASSED |
| TC-UI-03 | `GET /ui/instances` returns 200 and renders `instances` view | PASSED |
| TC-UI-04 | `GET /ui/instances/{id}` returns 200 and renders `instance-detail` view | PASSED |

### E2E Browser Tests

| ID | Description | Result |
|----|-------------|--------|
| E2E-01 | `GET /ui` redirects to `/ui/workflows` in browser | PASSED |
| E2E-02 | 3 definition cards render; Details collapse shows states/transitions table | PASSED |
| E2E-03 | "Start Instance" navigates to `/ui/instances/{id}` | PASSED |
| E2E-04 | `/ui/instances` table renders with status badge and filter dropdown | PASSED |
| E2E-05 | Instance detail shows metadata, history timeline, and transition buttons | PASSED |
| E2E-06 | Trigger transition updates current state and grows execution history | PASSED |
| E2E-07 | Pause → status PAUSED (Resume button appears) → Resume → status RUNNING | PASSED |
| E2E-08 | Navbar links and "Back to Instances" link navigate correctly | PASSED |

**Console errors during E2E:** none
