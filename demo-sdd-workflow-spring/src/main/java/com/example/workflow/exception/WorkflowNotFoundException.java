package com.example.workflow.exception;

public class WorkflowNotFoundException extends RuntimeException {

    public WorkflowNotFoundException(String name) {
        super("Workflow definition '" + name + "' not found");
    }
}
