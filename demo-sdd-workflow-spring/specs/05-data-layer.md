# 05 — Data Layer

## Overview

Persistence is provided by **Spring Data JPA** backed by an **H2 in-memory database**. The schema is managed exclusively by **Flyway** — JPA's DDL auto is disabled (`spring.jpa.hibernate.ddl-auto=validate`). Three repositories cover the three domain entities.

---

## Repositories

### WorkflowDefinitionRepository

**Package:** `com.example.workflow.repository`  
**Extends:** `JpaRepository<WorkflowDefinition, Long>`

| Method | Description |
|--------|-------------|
| `findByName(String name)` | Returns `Optional<WorkflowDefinition>`; used by service and loader |
| `existsByName(String name)` | Returns `boolean`; used by loader to skip already-loaded definitions |

Standard `JpaRepository` methods in use: `findAll()`, `save()`.

---

### WorkflowInstanceRepository

**Package:** `com.example.workflow.repository`  
**Extends:** `JpaRepository<WorkflowInstance, Long>`

| Method | Description |
|--------|-------------|
| `findByWorkflowDefinition(WorkflowDefinition def)` | Returns `List<WorkflowInstance>`; used for `?workflowName=` filter |

Standard `JpaRepository` methods in use: `findById()`, `findAll()`, `save()`.

---

### WorkflowHistoryEntryRepository

**Package:** `com.example.workflow.repository`  
**Extends:** `JpaRepository<WorkflowHistoryEntry, Long>`

| Method | Description |
|--------|-------------|
| `findByWorkflowInstanceOrderByOccurredAtAsc(WorkflowInstance instance)` | Returns ordered history for an instance |

Standard `JpaRepository` methods in use: `save()`.

---

## Flyway Migrations

Migrations live in `src/main/resources/db/migration/`. Flyway runs automatically at startup before the application context is fully initialised.

---

### V1__Create_workflow_definitions.sql

```sql
CREATE TABLE workflow_definitions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(255)  NOT NULL,
    description      VARCHAR(1000),
    source           VARCHAR(1000),
    states_json      TEXT          NOT NULL,
    transitions_json TEXT          NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workflow_definitions_name UNIQUE (name)
);
```

---

### V2__Create_workflow_instances.sql

```sql
CREATE TABLE workflow_instances (
    id                     BIGINT       AUTO_INCREMENT PRIMARY KEY,
    workflow_definition_id BIGINT       NOT NULL,
    current_state          VARCHAR(255) NOT NULL,
    status                 VARCHAR(50)  NOT NULL DEFAULT 'RUNNING',
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_instances_definition
        FOREIGN KEY (workflow_definition_id)
        REFERENCES workflow_definitions (id)
);
```

---

### V3__Create_workflow_history_entries.sql

```sql
CREATE TABLE workflow_history_entries (
    id                   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    workflow_instance_id BIGINT       NOT NULL,
    from_state           VARCHAR(255),
    to_state             VARCHAR(255) NOT NULL,
    action               VARCHAR(255) NOT NULL,
    task_type            VARCHAR(50)  NOT NULL DEFAULT 'HUMAN',
    occurred_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_instance
        FOREIGN KEY (workflow_instance_id)
        REFERENCES workflow_instances (id)
);
```

---

## JPA Entity Mapping Notes

| Entity | Table | Key mapping notes |
|--------|-------|-------------------|
| `WorkflowDefinition` | `workflow_definitions` | `@Column(name="states_json", columnDefinition="TEXT")` for blob fields |
| `WorkflowInstance` | `workflow_instances` | `@ManyToOne` to `WorkflowDefinition`; `@PreUpdate` sets `updatedAt` |
| `WorkflowHistoryEntry` | `workflow_history_entries` | `@ManyToOne` to `WorkflowInstance`; `taskType` stored with `@Enumerated(EnumType.STRING)` |

---

## Configuration

Relevant `application.properties` entries for the data layer:

```properties
# H2
spring.datasource.url=jdbc:h2:mem:workflowdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA — schema is managed by Flyway, not Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

---

## H2 Console Access

The H2 console is available at `http://localhost:8080/h2-console` during local development.

Connection settings:
- **JDBC URL:** `jdbc:h2:mem:workflowdb`
- **Username:** `sa`
- **Password:** *(empty)*

---

## Design Notes

| Decision | Rationale |
|----------|-----------|
| `ddl-auto=validate` | Ensures Hibernate schema matches Flyway; catches migration drift at startup |
| JSON blobs for states/transitions | Avoids extra join tables for read-only definition data; ObjectMapper handles serialisation |
| H2 in-memory | Zero-configuration local dev; replace datasource URL for production DB |
| Flyway migration naming | `V{n}__{description}.sql` — Flyway convention; version must be monotonically increasing |
