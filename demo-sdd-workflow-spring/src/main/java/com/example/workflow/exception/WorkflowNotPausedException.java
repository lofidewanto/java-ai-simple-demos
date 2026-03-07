package com.example.workflow.exception;

public class WorkflowNotPausedException extends RuntimeException {

    public WorkflowNotPausedException(Long id, String status) {
        super("Workflow instance " + id + " is not in PAUSED status (current status: " + status + ")");
    }
}
