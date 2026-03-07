package com.example.workflow.exception;

public class WorkflowCompletedException extends RuntimeException {

    public WorkflowCompletedException(Long id, String state) {
        super("Workflow instance " + id + " is already in a terminal state '" + state + "'");
    }
}
