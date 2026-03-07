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
