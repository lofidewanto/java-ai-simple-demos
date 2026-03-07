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
