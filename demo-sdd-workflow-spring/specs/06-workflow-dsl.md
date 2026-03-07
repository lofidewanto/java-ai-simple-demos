# 06 — Workflow DSL

## Overview

Workflow definitions are authored as **YAML files** placed under `src/main/resources/workflows/`. The `WorkflowDefinitionLoader` scans this directory at startup, parses each file, validates it, and persists it as a `WorkflowDefinition` entity.

The DSL is deliberately minimal: it declares states and transitions with enough metadata to drive the state machine and provide human-readable descriptions.

---

## File Format

```yaml
name: <unique-identifier>          # required — used as the API key
description: "<human text>"        # optional — free text
source: "<url>"                    # optional — link to origin (e.g. GitHub issue)

states:
  - name: <STATE_NAME>             # required — uppercase by convention
    initial: true                  # optional — mark as starting state (exactly one)
    terminal: true                 # optional — mark as ending state (one or more)
    description: "<human text>"    # optional

transitions:
  - from: <STATE_NAME>             # required — must reference a declared state
    to: <STATE_NAME>               # required — must reference a declared state
    action: <ACTION_NAME>          # required — uppercase by convention
    taskType: HUMAN                # optional — HUMAN (default) | SERVICE | GATEWAY | EVENT
    description: "<human text>"    # optional
```

### Rules

| Rule | Violation response |
|------|--------------------|
| Exactly one state with `initial: true` | Application fails to start |
| At least one state with `terminal: true` | Application fails to start |
| All `from` values must reference a declared state | Application fails to start |
| All `to` values must reference a declared state | Application fails to start |
| No two transitions may have the same `(from, action)` pair | Application fails to start |
| `taskType` is optional; defaults to `HUMAN` if omitted | — |

---

## Example Workflows

### order-processing.yml

Based on the Bestellabwicklung (Order Processing) workflow from [GitHub issue #7](https://github.com/lofidewanto/java-ai-simple-demos/issues/7).

```yaml
name: order-processing
description: "Bestellabwicklung — Manages the full order lifecycle from placement to shipping or cancellation"
source: "https://github.com/lofidewanto/java-ai-simple-demos/issues/7"

states:
  - name: NEW
    initial: true
    description: "Neue Bestellung eingegangen"
  - name: CHECKING_AVAILABILITY
    description: "Verfügbarkeit des Artikels wird geprüft"
  - name: PAYMENT_PENDING
    description: "Zahlung des Kunden ausstehend"
  - name: SHIPPED
    terminal: true
    description: "Bestellung wurde versendet"
  - name: CANCELLED
    terminal: true
    description: "Bestellung wurde storniert"

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
    description: "Lagerbestand ist ausreichend — weiter zur Zahlung"
  - from: CHECKING_AVAILABILITY
    to: CANCELLED
    action: STOCK_UNAVAILABLE
    taskType: GATEWAY
    description: "Lagerbestand nicht ausreichend — Bestellung storniert"
  - from: PAYMENT_PENDING
    to: SHIPPED
    action: PAYMENT_COLLECTED
    taskType: SERVICE
    description: "Zahlung erfolgreich eingezogen — Versand wird ausgelöst"
```

**State diagram:**

```
NEW ──SUBMIT_ORDER──► CHECKING_AVAILABILITY ──STOCK_AVAILABLE──► PAYMENT_PENDING ──PAYMENT_COLLECTED──► SHIPPED (terminal)
                              │
                        STOCK_UNAVAILABLE
                              │
                              ▼
                         CANCELLED (terminal)
```

---

### employee-onboarding.yml

Long-running process with a management approval step and a withdraw path.

```yaml
name: employee-onboarding
description: "Mitarbeiter-Onboarding — Manages the onboarding process for new employees"

states:
  - name: APPLICATION_RECEIVED
    initial: true
    description: "Bewerbung eingegangen"
  - name: DOCUMENTS_REQUESTED
    description: "Unterlagen wurden angefordert"
  - name: DOCUMENTS_RECEIVED
    description: "Unterlagen sind eingegangen"
  - name: BACKGROUND_CHECK
    description: "Hintergrundprüfung läuft"
  - name: MANAGER_APPROVAL
    description: "Wartet auf Genehmigung des Vorgesetzten"
  - name: ONBOARDED
    terminal: true
    description: "Mitarbeiter erfolgreich eingestellt"
  - name: WITHDRAWN
    terminal: true
    description: "Bewerbung zurückgezogen"

transitions:
  - from: APPLICATION_RECEIVED
    to: DOCUMENTS_REQUESTED
    action: REQUEST_DOCUMENTS
    taskType: HUMAN
    description: "HR fordert fehlende Unterlagen an"
  - from: DOCUMENTS_REQUESTED
    to: DOCUMENTS_RECEIVED
    action: SUBMIT_DOCUMENTS
    taskType: HUMAN
    description: "Kandidat reicht Unterlagen ein"
  - from: DOCUMENTS_RECEIVED
    to: BACKGROUND_CHECK
    action: START_BACKGROUND_CHECK
    taskType: SERVICE
    description: "Automatisierte Hintergrundprüfung gestartet"
  - from: BACKGROUND_CHECK
    to: MANAGER_APPROVAL
    action: BACKGROUND_CHECK_PASSED
    taskType: GATEWAY
    description: "Hintergrundprüfung bestanden"
  - from: MANAGER_APPROVAL
    to: ONBOARDED
    action: APPROVE
    taskType: HUMAN
    description: "Vorgesetzter genehmigt Einstellung"
  - from: MANAGER_APPROVAL
    to: WITHDRAWN
    action: REJECT
    taskType: HUMAN
    description: "Vorgesetzter lehnt Einstellung ab"
  - from: DOCUMENTS_REQUESTED
    to: WITHDRAWN
    action: WITHDRAW
    taskType: HUMAN
    description: "Kandidat zieht Bewerbung zurück"
```

---

### support-ticket.yml

Multiple resolution paths: direct close, escalation cycle, and re-open path.

```yaml
name: support-ticket
description: "Support-Ticket — Manages the lifecycle of a customer support request"

states:
  - name: OPEN
    initial: true
    description: "Ticket offen — wartet auf Bearbeitung"
  - name: IN_PROGRESS
    description: "Ticket wird bearbeitet"
  - name: ESCALATED
    description: "Ticket eskaliert an zweiten Support-Level"
  - name: RESOLVED
    description: "Problem gelöst — wartet auf Bestätigung"
  - name: CLOSED
    terminal: true
    description: "Ticket geschlossen"
  - name: REOPENED
    description: "Ticket wurde vom Kunden erneut geöffnet"

transitions:
  - from: OPEN
    to: IN_PROGRESS
    action: ASSIGN
    taskType: HUMAN
    description: "Support-Mitarbeiter übernimmt das Ticket"
  - from: IN_PROGRESS
    to: ESCALATED
    action: ESCALATE
    taskType: HUMAN
    description: "Problem zu komplex — wird eskaliert"
  - from: ESCALATED
    to: IN_PROGRESS
    action: RETURN_TO_AGENT
    taskType: HUMAN
    description: "Eskalationsteam gibt Ticket zurück"
  - from: IN_PROGRESS
    to: RESOLVED
    action: RESOLVE
    taskType: HUMAN
    description: "Support-Mitarbeiter markiert Problem als gelöst"
  - from: RESOLVED
    to: CLOSED
    action: CONFIRM_RESOLUTION
    taskType: EVENT
    description: "Kunde bestätigt Lösung — Ticket wird geschlossen"
  - from: RESOLVED
    to: REOPENED
    action: REOPEN
    taskType: HUMAN
    description: "Kunde ist nicht zufrieden — Ticket erneut geöffnet"
  - from: REOPENED
    to: IN_PROGRESS
    action: REASSIGN
    taskType: HUMAN
    description: "Ticket wird erneut einem Mitarbeiter zugewiesen"
```

---

## TaskType Reference

| Value | Meaning | Typical use |
|-------|---------|-------------|
| `HUMAN` | A human actor performs this step | Form submission, manual approval, customer action |
| `SERVICE` | An automated service or integration | Payment collection, background check API call |
| `GATEWAY` | A conditional routing decision | Stock availability check, rule engine outcome |
| `EVENT` | Triggered by an external event | Customer confirmation, timeout, webhook |

---

## Adding a Custom Workflow

1. Create a new `.yml` file in `src/main/resources/workflows/`
2. Follow the format above — ensure `name` is unique across all workflow files
3. Restart the application — the loader will pick up the new file automatically
4. Verify at `GET /api/workflow-definitions/{name}`
