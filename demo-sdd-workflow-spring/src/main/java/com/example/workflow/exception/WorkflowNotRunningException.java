package com.example.workflow.exception;

public class WorkflowNotRunningException extends RuntimeException {

    public WorkflowNotRunningException(Long id, String status) {
        super("Workflow instance " + id + " is not in RUNNING status (current status: " + status + ")");
    }
}
