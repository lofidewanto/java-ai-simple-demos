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
