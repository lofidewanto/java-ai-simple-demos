package com.example.workflow.exception;

public class InstanceNotFoundException extends RuntimeException {

    public InstanceNotFoundException(Long id) {
        super("Workflow instance with id " + id + " not found");
    }
}
