package com.example.workflow.exception;

public class WorkflowPausedException extends RuntimeException {

    public WorkflowPausedException(Long id) {
        super("Workflow instance " + id + " is PAUSED — resume it before triggering transitions");
    }
}
